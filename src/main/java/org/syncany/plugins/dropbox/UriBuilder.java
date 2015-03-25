package org.syncany.plugins.dropbox;

import java.net.URI;
import java.util.List;

import org.syncany.util.StringUtil;
import com.google.common.collect.Lists;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */

public class UriBuilder {

	public static final String SEPARATOR = "/";

	private final String root;
	private final List<String> children = Lists.newArrayList();
	private boolean endingSeparator = false;

	public static UriBuilder fromRoot(String root) {
		return new UriBuilder(root);
	}

	private UriBuilder(String root) {
		this.root = root;
	}

	public UriBuilder toChild(String child) {
		children.add(child);

		return this;
	}

	public UriBuilder withEndingSeparator() {
		endingSeparator = true;

		return this;
	}

	public URI build() {
		String fullPath = root;

		if (children.size() > 0) {
			fullPath = fullPath + SEPARATOR + StringUtil.join(children, SEPARATOR);
		}

		if (endingSeparator) {
			fullPath = fullPath + SEPARATOR;
		}

		return URI.create(cleanString(fullPath)).normalize();
	}

	private String cleanString(String toClean) {
		toClean = toClean.replaceAll("\\\\", "/");
		toClean = toClean.replaceAll("/{2,}", "/");

		if (!endingSeparator) {
			toClean = toClean.replaceAll("^(.+)/$", "$1");
		}

		return toClean;
	}

}