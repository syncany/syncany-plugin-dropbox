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
import org.syncany.plugins.Encrypted;
import org.syncany.plugins.PluginOptionCallback;
import org.syncany.plugins.PluginOptionConverter;
import org.syncany.plugins.Setup;
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
	@Encrypted
	@Setup(order = 1, callback = DropboxAuthPluginOptionCallback.class, converter = DropboxAuthPluginOptionConverter.class)
	public String accessToken;

	@Element(name = "path", required = true)
	@Setup(order = 2, description = "Path relative to dropbox root")
	public File path;

	public String getAccessToken() {
		return accessToken;
	}

	public File getPath() {
		return path;
	}

	public static class DropboxAuthPluginOptionCallback implements PluginOptionCallback {

		@Override
		public String preQueryCallback() {
			webAuth = new DbxWebAuthNoRedirect(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, DropboxTransferPlugin.DROPBOX_APP_INFO);
			String authorizeUrl = webAuth.start();

			return String.format("Please open \n  %s\nto obtain an access token for your dropbox account", authorizeUrl);
		}

		@Override
		public String postQueryCallback(String optionValue) {
			try {
				DbxClient client = new DbxClient(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, optionValue);
				return String.format("Linked with %s's account", client.getAccountInfo().displayName);
			}
			catch (DbxException e) {
				throw new RuntimeException("Error requesting dropbox data: " + e.getMessage());
			}
		}

	}

	public static class DropboxAuthPluginOptionConverter implements PluginOptionConverter {

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
