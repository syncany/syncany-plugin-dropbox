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
package org.syncany.tests.scenarios.framework;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;

import org.syncany.util.EnvironmentUtil;

public class ChangePermissionsOfFolder extends AbstractClientAction {
	@Override
	public void execute() throws Exception {
		File file = pickFolder(1922);
		Path filePath = Paths.get(file.getAbsolutePath());
				
		if (EnvironmentUtil.isWindows()) {
			Files.setAttribute(filePath, "dos:hidden", true);
			Files.setAttribute(filePath, "dos:system", true);
			Files.setAttribute(filePath, "dos:readonly", true);
			Files.setAttribute(filePath, "dos:archive", true);
		}
		else if (EnvironmentUtil.isUnixLikeOperatingSystem()) {
			log(this, "rwxrwxrwx "+file.getAbsolutePath());
			Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxrwxrwx"));
		}		
	}		
}	
