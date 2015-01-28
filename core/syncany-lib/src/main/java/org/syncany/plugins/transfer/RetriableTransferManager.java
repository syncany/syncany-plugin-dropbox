/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2015 Philipp C. Heckel <philipp.heckel@gmail.com> 
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
package org.syncany.plugins.transfer;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.syncany.plugins.transfer.files.RemoteFile;

/**
 * The retriable transfer manager implements a simple try-sleep-retry mechanism
 * for regular {@link TransferManager}s. It encapsules a single transfer manager and
 * proxies all of its methods. If a method fails with a {@link StorageException}, the 
 * method is retried N times before the exception is actually thrown to the caller. 
 * Between retries, the method waits M seconds. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class RetriableTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(RetriableTransferManager.class.getSimpleName());

	// Values are public to enable quicker testing

	private static int RETRY_MAX_COUNT = 3;
	public static int RETRY_SLEEP_MILLIS = 3000;

	private interface RetriableMethod {
		public Object execute() throws StorageException;
	}

	private TransferManager underlyingTransferManager;
	private int tryCount;

	public RetriableTransferManager(TransferManager underlyingTransferManager) {
		this.underlyingTransferManager = underlyingTransferManager;
		this.tryCount = 0;
	}

	@Override
	public void connect() throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.connect();
				return null;
			}
		});
	}

	@Override
	public void disconnect() throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.disconnect();
				return null;
			}
		});
	}

	@Override
	public void init(final boolean createIfRequired) throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.init(createIfRequired);
				return null;
			}
		});
	}

	@Override
	public void download(final RemoteFile remoteFile, final File localFile) throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.download(remoteFile, localFile);
				return null;
			}
		});
	}

	@Override
	public void move(final RemoteFile sourceFile, final RemoteFile targetFile) throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.move(sourceFile, targetFile);
				return null;
			}
		});
	}

	@Override
	public void upload(final File localFile, final RemoteFile remoteFile) throws StorageException {
		retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				underlyingTransferManager.upload(localFile, remoteFile);
				return null;
			}
		});
	}

	@Override
	public boolean delete(final RemoteFile remoteFile) throws StorageException {
		return (Boolean) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.delete(remoteFile);
			}
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends RemoteFile> Map<String, T> list(final Class<T> remoteFileClass) throws StorageException {
		return (Map<String, T>) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.list(remoteFileClass);
			}
		});
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		return underlyingTransferManager.test(testCreateTarget);
	}

	@Override
	public boolean testTargetExists() throws StorageException {
		return (Boolean) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.testTargetExists();
			}
		});
	}

	@Override
	public boolean testTargetCanWrite() throws StorageException {
		return (Boolean) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.testTargetCanWrite();
			}
		});
	}

	@Override
	public boolean testTargetCanCreate() throws StorageException {
		return (Boolean) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.testTargetCanCreate();
			}
		});
	}

	@Override
	public boolean testRepoFileExists() throws StorageException {
		return (Boolean) retryMethod(new RetriableMethod() {
			@Override
			public Object execute() throws StorageException {
				return underlyingTransferManager.testRepoFileExists();
			}
		});
	}

	private Object retryMethod(RetriableMethod retryableMethod) throws StorageException {
		tryCount = 0;

		while (true) {
			try {
				if (tryCount > 0) {
					logger.log(Level.WARNING, "Retrying method: " + tryCount + "/" + RETRY_MAX_COUNT + " ...");
				}

				Object result = retryableMethod.execute();

				tryCount = 0;
				return result;
			}
			catch (StorageMoveException | StorageFileNotFoundException e) {
				logger.log(Level.INFO, "StorageException caused by missing file, not the connection. Not retrying.");
				throw e;
			}
			catch (StorageException e) {
				tryCount++;

				if (tryCount >= RETRY_MAX_COUNT) {
					logger.log(Level.WARNING, "Transfer method failed. No retries left. Throwing exception.", e);
					throw e;
				}
				else {
					logger.log(Level.WARNING, "Transfer method failed. " + tryCount + "/" + RETRY_MAX_COUNT + " retries. Sleeping "
							+ RETRY_SLEEP_MILLIS
							+ "ms ...", e);

					try {
						Thread.sleep(RETRY_SLEEP_MILLIS);
					}
					catch (Exception e1) {
						throw new StorageException(e1);
					}
				}
			}
		}
	}

}
