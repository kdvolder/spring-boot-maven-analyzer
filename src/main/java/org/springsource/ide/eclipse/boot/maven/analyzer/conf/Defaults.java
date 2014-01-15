package org.springsource.ide.eclipse.boot.maven.analyzer.conf;

import java.io.File;
import java.net.URISyntaxException;

public class Defaults {

	/**
	 * Default pom file to analyze.
	 */
	public static File pomFile() {
		try {
			return new File(Defaults.class.getClassLoader().getResource("pom.xml").toURI());
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
	}

}
