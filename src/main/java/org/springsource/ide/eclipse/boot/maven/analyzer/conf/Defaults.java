package org.springsource.ide.eclipse.boot.maven.analyzer.conf;

import java.io.File;
import java.net.URISyntaxException;

public class Defaults {

	//Timeunit constants expressed in millis
	private static final int SECOND = 1000;
	private static final int MINUTE = 60*SECOND;
	private static final int HOUR   = 60*MINUTE;
	private static final int DAY    = 24*HOUR;
	
	/**
	 * Time before entry in typegraph cache expires and needs to be refreshed. 
	 * A good value is probably a day or so, so that the typegraphs are
	 * computed when they are a day old so they can pick up changes to boot snapshot
	 * releases once a day.
	 */
	public static long cacheTTL = 5 * MINUTE;

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
