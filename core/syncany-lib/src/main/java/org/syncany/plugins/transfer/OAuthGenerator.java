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

import java.net.URI;

/**
 * For {@link TransferPlugin}s that base their authentication on OAuth,
 * a generator class can be used to create the authentication URL and 
 * check the user-provided token. The concrete implementation of this
 * interface should be provided to the {@link TransferSettings} class
 * via the {@link OAuth} annotation. 
 *  
 * @author Philipp Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public interface OAuthGenerator {
	  public URI generateAuthUrl() throws StorageException;
	  public void checkToken(String token) throws StorageException;
}
