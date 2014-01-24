/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.aether;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;

import junit.framework.TestCase;

public class AetherHelperTest extends TestCase {
	
	AetherHelper helper;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		helper = new AetherHelper();
	}

	public void testGetManagedDependencies() throws Exception {
		//Test that at least some expeced artifact ids are found.
		String[] expectedAIDs = {
				"spring-boot-starter-web",
				"spring-boot-starter",
				"spring-starter-logging"
		};
		
		assertExpectedArtifacts(expectedAIDs, helper.getManagedDependencies(Defaults.defaultParentPom));
	}

	private void assertExpectedArtifacts(String[] expectedAIDs, List<Dependency> managedDependencies) {
		HashSet<String> expected = new HashSet<String>(Arrays.asList(expectedAIDs));
		for (Dependency d : managedDependencies) {
			Artifact foundArtifact = d.getArtifact();
			if (foundArtifact!=null) {
				expected.remove(foundArtifact.getArtifactId());
			}
		}
	}
	
	public void testResolveArtifacts() throws Exception {
		String[] artifactsCoords = {
				"org.springframework.boot:spring-boot:jar:0.5.0.BUILD-SNAPSHOT", //spring snapshot artifact
				"org.springframework:spring-jdbc:jar:4.0.0.RELEASE", // spring release artifact
				"commons-logging:commons-logging:jar:1.1.3" //general purpose (from maven central)
		};
		List<Artifact> artifacts = new ArrayList<Artifact>();
		for (String c : artifactsCoords) {
			artifacts.add(new DefaultArtifact(c));
		}
		for (Artifact artifact : artifacts) {
			assertNull(artifact.getFile());
		}
		
		artifacts = helper.resolve(artifacts);
		
		assertEquals(artifactsCoords.length, artifacts.size());

		for (Artifact artifact : artifacts) {
			assertNotNull("Artifact not resolved: '"+ artifact +"'", artifact.getFile());
			assertTrue("Artifact not a file: '"+ artifact +"'", artifact.getFile().isFile());
			assertTrue("Artifact not a jar file: '"+ artifact +"'", artifact.getFile().toString().endsWith(".jar"));
		}
		
	}
	
}
