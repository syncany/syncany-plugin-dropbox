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

import org.syncany.config.Config;
import org.syncany.plugins.Plugin;
import org.syncany.plugins.transfer.TransferManager;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * Identifies the FTP-based storage {@link Plugin} for Syncany. 
 * 
 * <p>This class implements defines the identifier, name and 
 * version of the plugin. It furthermore allows the instantiation 
 * of a plugin-specific {@link FtpTransferSettings}. 
 * 
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FtpPlugin extends TransferPlugin {
	public FtpPlugin() {
		super("ftp");
	}

	@Override
	public TransferManager createTransferManager(TransferSettings connection, Config config) {
		return new FtpTransferManager((FtpTransferSettings) connection, config);
	}

	@Override
	public TransferSettings createSettings() {
		return new FtpTransferSettings();
	}
}
