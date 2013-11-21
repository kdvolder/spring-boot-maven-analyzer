package org.springsource.ide.eclipse.boot.maven.analyzer.util;

public class Assert {

	public static void isNotNull(Object x) {
		if (x==null) {
			throw new IllegalArgumentException("Shoud not be null");
		}
	}

}
