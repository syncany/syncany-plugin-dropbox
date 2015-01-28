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
package org.syncany.operations.daemon.messages;

import org.simpleframework.xml.Element;

public class UpUploadFileInTransactionSyncExternalEvent extends SyncExternalEvent {
	@Element
	private int currentFileIndex;

	@Element
	private long currentFileSize;
	
	@Element
	private int totalFileCount;
	
	@Element
	private long totalFileSize;
	
	public UpUploadFileInTransactionSyncExternalEvent() {
		// Nothing
	}
	
	public UpUploadFileInTransactionSyncExternalEvent(String root, int currentFileIndex, int totalFileCount, long currentFileSize, long totalFileSize) {
		super(root);
		
		this.currentFileIndex = currentFileIndex;
		this.currentFileSize = currentFileSize;
		this.totalFileCount = totalFileCount;
		this.totalFileSize = totalFileSize;
	}

	public int getCurrentFileIndex() {
		return currentFileIndex;
	}

	public long getCurrentFileSize() {
		return currentFileSize;
	}

	public int getTotalFileCount() {
		return totalFileCount;
	}

	public long getTotalFileSize() {
		return totalFileSize;
	}
}
