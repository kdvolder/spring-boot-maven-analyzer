package org.springsource.ide.eclipse.boot.maven.analyzer.util;

/**
 * Poor man's 'logging framework'.
 */
public class Logger {

	public static void log(Exception e) {
		e.printStackTrace();
	}
	
}
