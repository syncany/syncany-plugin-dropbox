package org.syncany.plugins.dropbox;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */


public class UriBuilderTest {

	@Test
	public void testLinuxStyleSeparator() {
		assertEquals(URI.create("/a"), UriBuilder.fromRoot("/a").build());
		assertEquals(URI.create("/a/"), UriBuilder.fromRoot("/a").withEndingSeparator().build());

		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("/a").toChild("b").build());
		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("/a").toChild("b").withEndingSeparator().build());

		assertEquals(URI.create("/a/b/c"), UriBuilder.fromRoot("/a").toChild("b").toChild("c").build());
		assertEquals(URI.create("/a/b/c/"), UriBuilder.fromRoot("/a").toChild("b").toChild("c").withEndingSeparator().build());

		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("/a").toChild("/b").build());
		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("/a").toChild("/b/").build());

		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("/a").toChild("/b").withEndingSeparator().build());
		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("/a").toChild("/b/").withEndingSeparator().build());
	}

	@Test
	public void testWindowsStyleSeparator() {
		assertEquals(URI.create("/a"), UriBuilder.fromRoot("\\a").build());
		assertEquals(URI.create("/a/"), UriBuilder.fromRoot("\\a").withEndingSeparator().build());

		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("\\a").toChild("b").build());
		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("\\a").toChild("b").withEndingSeparator().build());

		assertEquals(URI.create("/a/b/c"), UriBuilder.fromRoot("\\a").toChild("b").toChild("c").build());
		assertEquals(URI.create("/a/b/c/"), UriBuilder.fromRoot("\\a").toChild("b").toChild("c").withEndingSeparator().build());

		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("\\a").toChild("\\b").build());
		assertEquals(URI.create("/a/b"), UriBuilder.fromRoot("\\a").toChild("\\b\\").build());

		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("\\a").toChild("\\b").withEndingSeparator().build());
		assertEquals(URI.create("/a/b/"), UriBuilder.fromRoot("\\a").toChild("\\b\\").withEndingSeparator().build());
	}

	@Test
	public void testEmptyUris() {
		assertEquals(URI.create("/"), UriBuilder.fromRoot("/").build());
		assertEquals(URI.create("/"), UriBuilder.fromRoot("//").build());
		assertEquals(URI.create("/"), UriBuilder.fromRoot("/").toChild("/").build());
		assertEquals(URI.create("/"), UriBuilder.fromRoot("//").toChild("/").build());
	}
}
