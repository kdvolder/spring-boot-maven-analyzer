package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springsource.ide.eclipse.boot.maven.analyzer.server.RestServer;

import junit.framework.TestCase;

public class PomGeneratorTest extends TestCase {
	
	private static final String TEST_STRING = "1.2.3.456456-234234";
	private PomGenerator gen = new PomGenerator();
	
	public PomGeneratorTest() {
		gen.setEngine(RestServer.createDefaultTemplateEngine());
	}
	
	public void testVersionSubstitution() throws Exception {
		assertContains("<version>"+TEST_STRING+"</version>", gen.getPom(TEST_STRING));
	}

	private void assertContains(String needle, String haystack) {
		if (haystack.contains(needle)) {
			return;
		}
		fail("Couldn't find '"+needle+"' in \n"+haystack);
	}

}
