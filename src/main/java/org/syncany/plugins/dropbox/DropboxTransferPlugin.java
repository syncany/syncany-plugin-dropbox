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

import java.util.Locale;

import org.syncany.plugins.transfer.TransferPlugin;
import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxRequestConfig;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class DropboxTransferPlugin extends TransferPlugin {
	private static final String APP_KEY = "dlg8wdfpf3xa5jv";
	private static final String APP_SECRET = "ya9t3c6xk9o3ndg";

	public static final DbxAppInfo DROPBOX_APP_INFO = new DbxAppInfo(APP_KEY, APP_SECRET);
	public static final DbxRequestConfig DROPBOX_REQ_CONFIG = new DbxRequestConfig("syncany", Locale.ENGLISH.toString());

	public DropboxTransferPlugin() {
		super("dropbox");
	}
}
