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
package org.syncany.plugins.raid0;

import org.syncany.config.Config;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferPlugin;
import org.syncany.plugins.transfer.TransferSettings;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
public class Raid0TransferPlugin extends TransferPlugin {
	public Raid0TransferPlugin() {
		super("raid0");
	}

	@Override
	public Raid0TransferSettings createEmptySettings() throws StorageException {
		return new Raid0TransferSettings();
	}

	@Override
	public Raid0TransferManager createTransferManager(TransferSettings transferSettings, Config config) throws StorageException {
		return new Raid0TransferManager((Raid0TransferSettings) transferSettings, config);
	}
}
