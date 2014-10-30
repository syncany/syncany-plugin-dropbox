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
package org.syncany.plugins.ftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.syncany.config.Config;
import org.syncany.plugins.transfer.AbstractTransferManager;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.StorageFileNotFoundException;
import org.syncany.plugins.transfer.StorageMoveException;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.files.ActionRemoteFile;
import org.syncany.plugins.transfer.files.DatabaseRemoteFile;
import org.syncany.plugins.transfer.files.MultichunkRemoteFile;
import org.syncany.plugins.transfer.files.RemoteFile;
import org.syncany.plugins.transfer.files.SyncanyRemoteFile;
import org.syncany.plugins.transfer.files.TempRemoteFile;
import org.syncany.plugins.transfer.files.TransactionRemoteFile;
import org.syncany.util.StringUtil;

/**
 * Implements a {@link TransferManager} based on an FTP storage backend for the
 * {@link FtpPlugin}. 
 * 
 * <p>Using an {@link FtpTransferSettings}, the transfer manager is configured and uses 
 * a well defined FTP folder to store the Syncany repository data. While repo and
 * master file are stored in the given folder, databases and multichunks are stored
 * in special sub-folders:
 * 
 * <ul>
 *   <li>The <tt>databases</tt> folder keeps all the {@link DatabaseRemoteFile}s</li>
 *   <li>The <tt>multichunks</tt> folder keeps the actual data within the {@link MultiChunkRemoteFile}s</li>
 * </ul>
 * 
 * <p>All operations are auto-connected, i.e. a connection is automatically
 * established. Connecting is retried a few times before throwing an exception.
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpTransferManager extends AbstractTransferManager {
	private static final Logger logger = Logger.getLogger(FtpTransferManager.class.getSimpleName());

	private static final int CONNECT_RETRY_COUNT = 2;
	private static final int TIMEOUT_DEFAULT = 5000;
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_DATA = 5000;

	private FTPClient ftp;
	private boolean ftpIsLoggedIn;

	private String repoPath;
	private String multichunksPath;
	private String databasesPath;
	private String actionsPath;
	private String transactionsPath;
	private String temporaryPath;

	public FtpTransferManager(FtpTransferSettings connection, Config config) {
		super(connection, config);

		this.ftp = new FTPClient();
		this.ftpIsLoggedIn = false;

		this.repoPath = connection.getPath().startsWith("/") ? connection.getPath() : "/" + connection.getPath();
		this.multichunksPath = repoPath + "/multichunks";
		this.databasesPath = repoPath + "/databases";
		this.actionsPath = repoPath + "/actions";
		this.transactionsPath = repoPath + "/transactions";
		this.temporaryPath = repoPath + "/temporary";
	}

	@Override
	public FtpTransferSettings getSettings() {
		return (FtpTransferSettings) super.getSettings();
	}

	@Override
	public void connect() throws StorageException {
		for (int i = 0; i < CONNECT_RETRY_COUNT; i++) {
			try {
				if (ftp.isConnected() && ftpIsLoggedIn) {
					logger.log(Level.INFO, "FTP client already connected. Skipping connect().");
					return;
				}

				if (logger.isLoggable(Level.INFO)) {
					logger.log(Level.INFO, "FTP client connecting to {0}:{1} ...", new Object[] { getSettings().getHostname(),
							getSettings().getPort() });
				}

				ftp.setConnectTimeout(TIMEOUT_CONNECT);
				ftp.setDataTimeout(TIMEOUT_DATA);
				ftp.setDefaultTimeout(TIMEOUT_DEFAULT);

				ftp.connect(getSettings().getHostname(), getSettings().getPort());

				if (!ftp.login(getSettings().getUsername(), getSettings().getPassword())) {
					throw new StorageException("Invalid FTP login credentials. Cannot login.");
				}

				ftp.enterLocalPassiveMode();
				ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

				ftpIsLoggedIn = true;
				return; // no loop!
			}
			catch (Exception ex) {
				if (i == CONNECT_RETRY_COUNT - 1) {
					logger.log(Level.WARNING, "FTP client connection failed. Retrying failed.", ex);

					ftpIsLoggedIn = false;
					throw new StorageException(ex);
				}
				else {
					logger.log(Level.WARNING, "FTP client connection failed. Retrying " + (i + 1) + "/" + CONNECT_RETRY_COUNT + " ...", ex);
				}
			}
		}
	}

	@Override
	public void disconnect() {
		try {
			ftp.disconnect();
			ftpIsLoggedIn = false;
		}
		catch (Exception ex) {
			// Nothing
		}
	}

	@Override
	public void init(boolean createIfRequired) throws StorageException {
		connect();

		try {
			if (!testRepoFileExists() && createIfRequired) {
				ftp.mkd(repoPath);
			}

			ftp.mkd(multichunksPath);
			ftp.mkd(databasesPath);
			ftp.mkd(actionsPath);
			ftp.mkd(transactionsPath);
			ftp.mkd(temporaryPath);
		}
		catch (IOException e) {
			forceFtpDisconnect();
			throw new StorageException("Cannot create directory " + multichunksPath + ", or " + databasesPath, e);
		}
	}

	@Override
	public void download(RemoteFile remoteFile, File localFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			// Download file
			File tempFile = createTempFile(localFile.getName());
			OutputStream tempFOS = new FileOutputStream(tempFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Downloading {0} to temp file {1}", new Object[] { remotePath, tempFile });
			}

			boolean success = ftp.retrieveFile(remotePath, tempFOS);

			if (!success) {
				throw new StorageFileNotFoundException("Could not find remoteFile to download " + remoteFile.getName());
			}

			tempFOS.close();

			// Move file
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempFile, localFile });
			}

			localFile.delete();
			FileUtils.moveFile(tempFile, localFile);
			tempFile.delete();
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Error while downloading file " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void upload(File localFile, RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);
		String tempRemotePath = repoPath + "/temp-" + remoteFile.getName();

		try {
			// Upload to temp file
			InputStream fileFIS = new FileInputStream(localFile);

			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Uploading {0} to temp file {1}", new Object[] { localFile, tempRemotePath });
			}

			ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

			if (!ftp.storeFile(tempRemotePath, fileFIS)) {
				throw new IOException("Error uploading file " + remoteFile.getName());
			}

			fileFIS.close();

			// Move
			if (logger.isLoggable(Level.INFO)) {
				logger.log(Level.INFO, "FTP: Renaming temp file {0} to file {1}", new Object[] { tempRemotePath, remotePath });
			}

			ftp.rename(tempRemotePath, remotePath);
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Could not upload file " + localFile + " to " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public boolean delete(RemoteFile remoteFile) throws StorageException {
		connect();

		String remotePath = getRemoteFile(remoteFile);

		try {
			logger.log(Level.INFO, "FTP: Deleting file " + remotePath + " ...");

			// Try deleting; returns 'false' if file does not exist
			if (ftp.deleteFile(remotePath)) {
				return true;
			}

			// Double check if above command returned 'false' (if non-existent file)
			String[] fileList = ftp.listNames(remotePath);
			boolean remotePathDeleted = fileList != null && fileList.length == 0;

			return remotePathDeleted;
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Could not delete file " + remoteFile.getName(), ex);
			throw new StorageException(ex);
		}
	}

	@Override
	public void move(RemoteFile sourceFile, RemoteFile targetFile) throws StorageException {
		connect();

		String sourcePath = getRemoteFile(sourceFile);
		String targetPath = getRemoteFile(targetFile);

		try {
			logger.log(Level.INFO, "FTP: Renaming " + sourceFile + " to " + targetFile);

			boolean success = ftp.rename(sourcePath, targetPath);
			if (!success) {
				logger.log(Level.INFO, "FTP: SourceFile does not exist: " + sourceFile);
				throw new StorageMoveException("Could not find sourceFile to move " + sourceFile.getName());
			}
		}
		catch (IOException e) {
			forceFtpDisconnect();
			logger.log(Level.SEVERE, "Could not rename" + sourceFile + " to " + targetFile, e);
			throw new StorageException(e);
		}

	}

	@Override
	public <T extends RemoteFile> Map<String, T> list(Class<T> remoteFileClass) throws StorageException {
		connect();

		try {
			// List folder
			String remoteFilePath = getRemoteFilePath(remoteFileClass);
			FTPFile[] ftpFiles = ftp.listFiles(remoteFilePath + "/");

			// Create RemoteFile objects
			Map<String, T> remoteFiles = new HashMap<String, T>();

			for (FTPFile file : ftpFiles) {
				try {
					T remoteFile = RemoteFile.createRemoteFile(file.getName(), remoteFileClass);
					remoteFiles.put(file.getName(), remoteFile);
				}
				catch (Exception e) {
					logger.log(Level.INFO, "Cannot create instance of " + remoteFileClass.getSimpleName() + " for file " + file
							+ "; maybe invalid file name pattern. Ignoring file.");
				}
			}

			return remoteFiles;
		}
		catch (IOException ex) {
			forceFtpDisconnect();

			logger.log(Level.SEVERE, "Unable to list FTP directory.", ex);
			throw new StorageException(ex);
		}
	}

	private void forceFtpDisconnect() {
		try {
			ftp.disconnect();
		}
		catch (IOException e) {
			// Nothing
		}
	}

	private String getRemoteFile(RemoteFile remoteFile) {
		return getRemoteFilePath(remoteFile.getClass()) + "/" + remoteFile.getName();
	}

	private String getRemoteFilePath(Class<? extends RemoteFile> remoteFile) {
		if (remoteFile.equals(MultichunkRemoteFile.class)) {
			return multichunksPath;
		}
		else if (remoteFile.equals(DatabaseRemoteFile.class)) {
			return databasesPath;
		}
		else if (remoteFile.equals(ActionRemoteFile.class)) {
			return actionsPath;
		}
		else if (remoteFile.equals(TransactionRemoteFile.class)) {
			return transactionsPath;
		}
		else if (remoteFile.equals(TempRemoteFile.class)) {
			return temporaryPath;
		}
		else {
			return repoPath;
		}
	}

	@Override
	public boolean testTargetCanWrite() {
		try {
			if (ftp.changeWorkingDirectory(repoPath)) {
				String tempRemoteFilePath = repoPath + "/syncany-write-test";

				ftp.setFileType(FTPClient.BINARY_FILE_TYPE); // Important !!!

				if (ftp.storeFile(tempRemoteFilePath, new ByteArrayInputStream(new byte[] { 0x01, 0x02, 0x03 }))) {
					ftp.deleteFile(tempRemoteFilePath);

					logger.log(Level.INFO, "testTargetCanWrite: Can write, test file created/deleted successfully.");
					return true;
				}
				else {
					logger.log(Level.INFO, "testTargetCanWrite: Can NOT write, target does not exist.");
					return false;
				}
			}
			else {
				logger.log(Level.INFO, "testTargetCanWrite: Can NOT write, target does not exist.");
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testTargetCanWrite: Can NOT write to target.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetExists() {
		try {
			boolean targetExists = ftp.changeWorkingDirectory(repoPath);

			if (targetExists) {
				logger.log(Level.INFO, "testTargetExists: Target exists. Chdir successful.");
				return true;
			}
			else {
				logger.log(Level.INFO, "testTargetExists: Target does NOT exist. Chdir not successful.");
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testTargetExists: Target does NOT exist. Chdir threw exception.", e);
			return false;
		}
	}

	@Override
	public boolean testTargetCanCreate() {
		try {
			if (testTargetExists()) {
				logger.log(Level.INFO, "testTargetCanCreate: Target already exists, so 'can create' test successful.");
				return true;
			}
			else {
				if (ftp.makeDirectory(repoPath)) {
					ftp.removeDirectory(repoPath);

					logger.log(Level.INFO, "testTargetCanCreate: Target can be created (test-created successfully).");
					return true;
				}
				else {
					logger.log(Level.INFO, "testTargetCanCreate: Target can NOT be created. Test creation failed.");
					return false;
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testTargetCanCreate: Target can NOT be created.", e);
			return false;
		}
	}

	@Override
	public boolean testRepoFileExists() {
		try {
			SyncanyRemoteFile repoFile = new SyncanyRemoteFile();
			String repoFilePath = getRemoteFile(repoFile);
			
			logger.log(Level.INFO, "- DEBUG listNames(" + repoFilePath + "): " + StringUtil.join(ftp.listNames(repoFilePath), ", "));
			logger.log(Level.INFO, "- DEBUG listFiles(" + repoFilePath + "): " + StringUtil.join(ftp.listFiles(repoFilePath), ", "));
			
			String repoFileParentPath = (repoFilePath.indexOf("/") != -1) ? repoFilePath.substring(0, repoFilePath.lastIndexOf("/")) : "";
			FTPFile[] listRepoFile = ftp.listFiles(repoFileParentPath);

			if (listRepoFile != null) {
				for (FTPFile ftpFile : listRepoFile) {
					if (ftpFile.getName().equals(repoFile.getName())) {

						logger.log(Level.INFO, "testRepoFileExists: Repo file exists, list(repo) contained 'syncany' file.");
						return true;
					}
				}
				
				logger.log(Level.INFO, "testRepoFileExists: Repo file DOES NOT exist: list(repo) DID NOT contain 'syncany' file:\n" + StringUtil.join(listRepoFile, "\n"));
				return false;				
			}
			else {
				logger.log(Level.INFO, "testRepoFileExists: Repo file DOES NOT exist: list(repo) was NULL.");
				return false;
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, "testRepoFileExists: Target does NOT exist. Chdir threw exception.", e);
			return false;
		}
	}

}
