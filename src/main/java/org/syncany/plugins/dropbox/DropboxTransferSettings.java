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

import java.io.File;

import org.simpleframework.xml.Element;
import org.syncany.plugins.transfer.Encrypted;
import org.syncany.plugins.transfer.Setup;
import org.syncany.plugins.transfer.TransferPluginOptionCallback;
import org.syncany.plugins.transfer.TransferPluginOptionConverter;
import org.syncany.plugins.transfer.TransferSettings;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWebAuthNoRedirect;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class DropboxTransferSettings extends TransferSettings {
	private static DbxWebAuthNoRedirect webAuth;

	@Element(name = "accessToken", required = true)
	@Setup(order = 1, sensitive = true, description = "Access token", callback = DropboxAuthPluginOptionCallback.class, converter = DropboxAuthPluginOptionConverter.class)
	@Encrypted
	public String accessToken;

	@Element(name = "path", required = true)
	@Setup(order = 2, description = "Path relative to syncany's app root")
	public File path;

	public String getAccessToken() {
		return accessToken;
	}

	public File getPath() {
		return path;
	}

	public static class DropboxAuthPluginOptionCallback implements TransferPluginOptionCallback {
		@Override
		public String preQueryCallback() {
			webAuth = new DbxWebAuthNoRedirect(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, DropboxTransferPlugin.DROPBOX_APP_INFO);
			String authorizeUrl = webAuth.start();

			return String.format(
				      "\n"
					+ "The Dropbox plugin needs to obtain an access token from Dropbox\n"
					+ "to read and write to your Dropbox. Please follow the instructions\n"
					+ "on the following site to authorize Syncany:\n"
					+ "\n"
					+ "    %s\n", authorizeUrl);
		}

		@Override
		public String postQueryCallback(String optionValue) {
			try {
				DbxClient client = new DbxClient(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, optionValue);
				return String.format("\nSuccessfully linked with %s's account!\n", client.getAccountInfo().displayName);
			}
			catch (DbxException e) {
				throw new RuntimeException("Error requesting dropbox data: " + e.getMessage());
			}
		}
	}

	public static class DropboxAuthPluginOptionConverter implements TransferPluginOptionConverter {
		@Override
		public String convert(String input) {
			try {
				return webAuth.finish(input).accessToken;
			}
			catch (DbxException e) {
				throw new RuntimeException("Unable to extract oauth token: " + e.getMessage());
			}
		}
	}
}
