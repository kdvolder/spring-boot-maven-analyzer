package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

public class DependencyNodeUtil {

	public static Artifact getArtifact(DependencyNode node) {
		if (node!=null) {
			Dependency dep = node.getDependency();
			if (dep!=null) {
				return dep.getArtifact();
			}
		}
		return null;
	}

}
