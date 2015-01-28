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
package org.syncany.tests.cli;

import static org.junit.Assert.assertTrue;
import static org.syncany.tests.util.TestAssertUtil.assertFileEquals;
import static org.syncany.tests.util.TestAssertUtil.assertFileListEqualsExcludeLockedAndNoRead;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.syncany.cli.CommandLineClient;
import org.syncany.tests.util.TestCliUtil;
import org.syncany.tests.util.TestConfigUtil;
import org.syncany.tests.util.TestFileUtil;

public class WatchCommandTest {
	@Test
	public void testWatchCommand() throws Exception {
		final Map<String, String> connectionSettings = TestConfigUtil.createTestLocalConnectionSettings();
		final Map<String, String> clientA = TestCliUtil.createLocalTestEnvAndInit("A", connectionSettings);
		final Map<String, String> clientB = TestCliUtil.createLocalTestEnvAndConnect("B", connectionSettings);

		Thread clientThreadA = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new CommandLineClient(new String[] {
							"--localdir", clientA.get("localdir"),
							"watch",
							"--interval", "1"
					}).start();
				}
				catch (Exception e) {
					System.out.println("Interrupted.");
				}
			}
		});

		// Client A: Start 'watch'
		clientThreadA.start();

		// Client B: New file and up
		TestFileUtil.createRandomFile(new File(clientB.get("localdir") + "/file1"), 20 * 1024);

		new CommandLineClient(new String[] {
				"--localdir", clientB.get("localdir"),
				"up"
		}).start();

		// Client A: Wait for client A to sync it
		Thread.sleep(2000);
		assertFileEquals(new File(clientB.get("localdir") + "/file1"), new File(clientA.get("localdir") + "/file1"));
		assertFileListEqualsExcludeLockedAndNoRead(new File(clientB.get("localdir")), new File(clientA.get("localdir")));

		// Client A: New file, wait for it to sync it
		TestFileUtil.createRandomFile(new File(clientA.get("localdir") + "/file2"), 20 * 1024);
		Thread.sleep(2000);
		assertTrue(new File(clientB.get("repopath") + "/databases/database-A-0000000001").exists());

		clientThreadA.interrupt();

		TestCliUtil.deleteTestLocalConfigAndData(clientA);
		TestCliUtil.deleteTestLocalConfigAndData(clientB);
	}
}
