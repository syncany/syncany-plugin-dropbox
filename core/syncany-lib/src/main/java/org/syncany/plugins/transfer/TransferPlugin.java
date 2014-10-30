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
package org.syncany.plugins.transfer;

import org.syncany.config.Config;
import org.syncany.plugins.Plugin;

/**
 * The transfer plugin is a special plugin responsible for transferring files
 * to the remote storage. Implementations must provide implementations for
 * {@link TransferPlugin} (this class), {@link TransferSettings} (connection
 * details) and {@link TransferManager} (transfer methods).<br/><br/>
 *
 * <p>Links between the classes can be created by annotating this class with
 * {@link org.syncany.plugins.transfer.PluginSettings} and {@link org.syncany.plugins.transfer.PluginManager},
 * respectively.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class TransferPlugin extends Plugin {
	public TransferPlugin(String pluginId) {
		super(pluginId);
	}

	public abstract TransferSettings createEmptySettings() throws StorageException;
	public abstract TransferManager createTransferManager(TransferSettings transferSettings, Config config) throws StorageException;
}
