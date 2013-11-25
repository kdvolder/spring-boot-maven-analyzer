package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

public class DependencyNodeUtil {

	public static Artifact getArtifact(DependencyNode node) {
		if (node!=null) {
			org.sonatype.aether.graph.Dependency dep = node.getDependency();
			if (dep!=null) {
				return dep.getArtifact();
			}
		}
		return null;
	}

}
