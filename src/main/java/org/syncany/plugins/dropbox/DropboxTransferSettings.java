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

import java.util.Map;

import org.simpleframework.xml.Element;
import org.syncany.plugins.transfer.PluginOptionCallback;
import org.syncany.plugins.transfer.Setup;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferSettings;

public class DropboxTransferSettings extends TransferSettings {
	@Element(name = "authToken", required = true)
	@Setup(callback = DropboxAuthPluginOptionCallback.class)
	private String authToken;
	
	private String hostname;
	private String username;
	private String password;
	private String path;
	private int port;

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return DropboxTransferSettings.class.getSimpleName() + "[hostname=" + hostname + ":" + port + ", username=" + username + ", path=" + path + "]";
	}
	
	private class DropboxAuthPluginOptionCallback implements PluginOptionCallback {

		@Override
		public String preQueryCallback() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
