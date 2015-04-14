/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.dropbox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.syncany.config.Config;
import org.syncany.plugins.transfer.AbstractTransferManager;
import org.syncany.plugins.transfer.FileType;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageMoveException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.features.PathAware;
import org.syncany.plugins.transfer.features.PathAwareFeatureExtension;
import org.syncany.plugins.transfer.features.PathAwareFeatureTransferManager.PathAwareRemoteFileAttributes;
import org.syncany.plugins.transfer.files.ActionRemoteFile;
import org.syncany.plugins.transfer.files.CleanupRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.SyncanyRemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.util.FileUtil;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxException.BadResponseCode;
import com.dropbox.core.DbxWriteMode;
import com.google.common.collect.Maps;

/**
 * Implements a {@link TransferManager} based on an Dropbox storage backend for the
 * {@link DropboxTransferPlugin}.
 * 
 * <p>Using an {@link DropboxTransferSettings}, the transfer manager is configured and uses
 * a well defined Samba share and folder to store the Syncany repository data. While repo and
 * master file are stored in the given folder, databases and multichunks are stored
 * in special sub-folders:
 * 
 * <ul>
 *   <li>The <tt>databases</tt> folder keeps all the {@link DatabaseRemoteFile}s</li>
 *   <li>The <tt>multichunks</tt> folder keeps the actual data within the {@link MultiChunkRemoteFile}s</li>
 * </ul>
 * 
 * <p>All operations are auto-connected, i.e. a connection is automatically
 * established.
 *
 * @author Christian Roth <christian.roth@port17.de>
 */
@PathAware(extension = DropboxTransferManager.DropboxTransferManagerFeatureExtension.class)
public class DropboxTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(DropboxTransferManager.class.getSimpleName());

	private final DbxClient client;
	private final URI path;
	private final URI multichunksPath;
	private final URI databasesPath;
	private final URI actionsPath;
	private final URI transactionsPath;
	private final URI tempPath;

	public DropboxTransferManager(DropboxTransferSettings settings, Config config) {
		super(settings, config);

		this.path = UriBuilder.fromRoot("/").toChild(settings.getPath()).build();
		this.multichunksPath = UriBuilder.fromRoot("/").toChild(settings.getPath()).toChild("multichunks").build();
		this.databasesPath = UriBuilder.fromRoot("/").toChild(settings.getPath()).toChild("databases").build();
		this.actionsPath = UriBuilder.fromRoot("/").toChild(settings.getPath()).toChild("actions").build();
		this.transactionsPath = UriBuilder.fromRoot("/").toChild(settings.getPath()).toChild("transactions").build();
		this.tempPath = UriBuilder.fromRoot("/").toChild(settings.getPath()).toChild("temporary").build();

		this.client = new DbxClient(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, settings.getAccessToken());
	}

	@Override
	public void connect() throws StorageException {
		// make a connect
		try {
			logger.log(Level.INFO, "Using dropbox account from {0}", new Object[]{client.getAccountInfo().displayName});
		}
		catch (DbxException.InvalidAccessToken e) {
			throw new StorageException("The accessToken in use is invalid", e);
		}
		catch (Exception e) {
			throw new StorageException("Unable to connect to dropbox", e);
		}
	}

	@Override
	public void disconnect() {
		// Nothing
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (!testTargetExists() && createIfRequired) {
				client.createFolder(path.toString());
			}

			client.createFolder(multichunksPath.toString());
			client.createFolder(databasesPath.toString());
			client.createFolder(actionsPath.toString());
			client.createFolder(transactionsPath.toString());
			client.createFolder(tempPath.toString());
		}
		catch (DbxException e) {
			throw new StorageException("init: Cannot create required directories", e);
		}
		finally {
			disconnect();
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		String remotePath = getRemoteFile(remoteFile);

		if (!remoteFile.getName().equals(".") && !remoteFile.getName().equals("..")) {
			try {
				// Download file
				File tempFile = createTempFile(localFile.getName());
				OutputStream tempFOS = new FileOutputStream(tempFile);

				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "Dropbox: Downloading {0} to temp file {1}", new Object[]{remotePath, tempFile});
				}

				client.getFile(remotePath, null, tempFOS);

				tempFOS.close();

				// Move file
				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "Dropbox: Renaming temp file {0} to file {1}", new Object[]{tempFile, localFile});
				}

				localFile.delete();
				FileUtils.moveFile(tempFile, localFile);
				tempFile.delete();
			}
			catch (DbxException | IOException ex) {
				logger.log(Level.SEVERE, "Error while downloading file " + remoteFile.getName(), ex);
				throw new StorageException(ex);
			}
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		String remotePath = getRemoteFile(remoteFile);
		String tempRemotePath = path + "/temp-" + remoteFile.getName();

		try {
			// Upload to temp file
			InputStream fileFIS = new FileInputStream(localFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "Dropbox: Uploading {0} to temp file {1}", new Object[]{localFile, tempRemotePath});
			}

			client.uploadFile(tempRemotePath, DbxWriteMode.add(), localFile.length(), fileFIS);

			fileFIS.close();

			// Move
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "Dropbox: Renaming temp file {0} to file {1}", new Object[]{tempRemotePath, remotePath});
			}

			client.move(tempRemotePath, remotePath);
		}
		catch (DbxException | IOException ex) {
			logger.log(Level.SEVERE, "Could not upload file " + localFile + " to " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		String remotePath = getRemoteFile(remoteFile);

		try {
			client.delete(remotePath);
			return true;
		}
		catch (BadResponseCode e) {
			if (e.statusCode == 404) {
				logger.log(Level.INFO, "File does not exist. Doing nothing: " + remoteFile.getName(), e);
				return true;
			}
			else {
				logger.log(Level.SEVERE, "Could not delete file " + remoteFile.getName(), e);
				throw new StorageException(e);
			}
		}
		catch (DbxException e) {
			logger.log(Level.SEVERE, "Could not delete file " + remoteFile.getName(), e);
			throw new StorageException(e);
		}
	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {
		String sourceRemotePath = getRemoteFile(sourceFile);
		String targetRemotePath = getRemoteFile(targetFile);

		try {
			client.move(sourceRemotePath, targetRemotePath);
		}
		catch (DbxException e) {
			logger.log(Level.SEVERE, "Could not rename file " + sourceRemotePath + " to " + targetRemotePath, e);
			throw new StorageMoveException("Could not rename file " + sourceRemotePath + " to " + targetRemotePath, e);
		}
	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		// TransferManager.list(Class<T> remoteFileClass) has been superseded by PathAwareFeatureExtension.list(String path)
		throw new UnsupportedOperationException("Extension is path aware! Hence, TransferManager.list(Class<T> remoteFileClass) has been superseded by PathAwareFeatureExtension.list(String path)");
	}

	@Override
	public String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultichunkRemoteFile.class)) {
			return multichunksPath.toString();
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class) || remoteFile.equals(CleanupRemoteFile.class)) {
			return databasesPath.toString();
		}
		else if (remoteFile.equals(ActionRemoteFile.class)) {
			return actionsPath.toString();
		}
		else if (remoteFile.equals(TransactionRemoteFile.class)) {
			return transactionsPath.toString();
		}
		else if (remoteFile.equals(TempRemoteFile.class)) {
			return tempPath.toString();
		}
		else {
			return path.toString();
		}
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		String rootPath = getRemoteFilePath(remoteFile.getClass());
		String subfolder = "";

		try {
			PathAwareRemoteFileAttributes attributes = remoteFile.getAttributes(PathAwareRemoteFileAttributes.class);

			if (attributes.hasPath()) {
				subfolder = attributes.getPath();
			}
		}
		catch (NoSuchFieldException e) {
			logger.log(Level.WARNING, "TransferManager is annotated with @PathAware but files do not possess path aware attributes");
		}

		return Paths.get(rootPath, subfolder, remoteFile.getName()).toString();
	}

	@Override
	public boolean testTargetCanWrite() {
		try {
			if (testTargetExists()) {
				String tempRemoteFile = path + "/syncany-write-test";
				File tempFile = File.createTempFile("syncany-write-test", "tmp");

				client.uploadFile(tempRemoteFile, DbxWriteMode.add(), 0, new ByteArrayInputStream(new byte[0]));
				client.delete(tempRemoteFile);

				tempFile.delete();

				logger.log(Level.INFO, "testTargetCanWrite: Can write, test file created/deleted successfully.");
				return true;
			}
			else {
				logger.log(Level.INFO, "testTargetCanWrite: Can NOT write, target does not exist.");
				return false;
			}
		}
		catch (DbxException | IOException e) {
			logger.log(Level.INFO, "testTargetCanWrite: Can NOT write to target.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetExists() {
		try {
			DbxEntry metadata = client.getMetadata(path.toString());

			if (metadata != null && metadata.isFolder()) {
				logger.log(Level.INFO, "testTargetExists: Target does exist.");
				return true;
			}
			else {
				logger.log(Level.INFO, "testTargetExists: Target does NOT exist.");
				return false;
			}
		}
		catch (DbxException e) {
			logger.log(Level.WARNING, "testTargetExists: Target does NOT exist, error occurred.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetCanCreate() {
		// Find parent path
		String repoPathNoSlash = FileUtil.removeTrailingSlash(path.toString());
		int repoPathLastSlash = repoPathNoSlash.lastIndexOf("/");
		String parentPath = (repoPathLastSlash > 0) ? repoPathNoSlash.substring(0, repoPathLastSlash) : "/";

		// Test parent path permissions
		try {
			DbxEntry metadata = client.getMetadata(parentPath);

			// our app has read/write for EVERY folder inside a dropbox. as long as it exists, we can write in it
			if (metadata.isFolder()) {
				logger.log(Level.INFO, "testTargetCanCreate: Can create target at " + parentPath);
				return true;
			}
			else {
				logger.log(Level.INFO, "testTargetCanCreate: Can NOT create target (parent does not exist)");

				return false;
			}
		}
		catch (DbxException e) {
			logger.log(Level.INFO, "testTargetCanCreate: Can NOT create target at " + parentPath, e);
			return false;
		}
	}

	@Override
	public boolean testRepoFileExists() {
		try {
			String repoFilePath = getRemoteFile(new SyncanyRemoteFile());
			DbxEntry metadata = client.getMetadata(repoFilePath);

			if (metadata != null && metadata.isFile()) {
				logger.log(Level.INFO, "testRepoFileExists: Repo file exists at " + repoFilePath);
				return true;
			}
			else {
				logger.log(Level.INFO, "testRepoFileExists: Repo file DOES NOT exist at " + repoFilePath);
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testRepoFileExists: Exception when trying to check repo file existence.", e);
			return false;
		}
	}

	public static class DropboxTransferManagerFeatureExtension implements PathAwareFeatureExtension {
		private final DropboxTransferManager transferManager;

		public DropboxTransferManagerFeatureExtension(DropboxTransferManager transferManager) {
			this.transferManager = transferManager;
		}

		@Override
		public boolean createPath(String path) throws StorageException {
			// Dropbox always creates a path structure implicitly.
			return true;
		}

		@Override
		public boolean removeFolder(String path) throws StorageException {
			logger.log(Level.FINE, "Deleting folder " + path);

			try {
				transferManager.client.delete(path);
				return true;
			}
			catch (DbxException e) {
				logger.log(Level.SEVERE, "Unable to delete remote path", e);
				return false;
			}

		}

		@Override
		public Map<String, FileType> listFolder(String path) throws StorageException {
			logger.log(Level.FINE, "Listing folder " + path);

			Map<String, FileType> contents = Maps.newHashMap();

			try {
				DbxEntry.WithChildren listing = transferManager.client.getMetadataWithChildren(path);

				for (DbxEntry child : listing.children) {
					if (child.isFile()) {
						contents.put(child.name, FileType.FILE);
					}
					else if (child.isFolder()) {
						contents.put(child.name, FileType.FOLDER);
					}
				}
			}
			catch (DbxException e) {
				logger.log(Level.SEVERE, "Unable to list folder", e);
				throw new StorageException("Unable to list folder", e);
			}

			return contents;
		}
	}
}
