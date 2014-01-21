package org.springsource.ide.eclipse.boot.maven.analyzer.aether;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
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
	
}
