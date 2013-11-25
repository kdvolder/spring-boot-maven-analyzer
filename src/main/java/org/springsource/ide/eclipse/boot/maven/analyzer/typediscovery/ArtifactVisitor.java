package org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery;

import java.util.HashSet;
import java.util.Set;

import org.apache.maven.model.IssueManagement;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.DependencyNodeUtil;

/**
 * Dependency visitor that traverses a dependency graph calling 
 * an abstract visitArtifact method on each artifact in the graph exactly once.
 * 
 * @author Kris De Volder
 */
public abstract class ArtifactVisitor implements DependencyVisitor {

	private Set<Artifact> seenArtifacts = new HashSet<Artifact>();

	
	
	public abstract void visitArtifact(Artifact artifact);
	@Override
	public boolean visitEnter(DependencyNode node) {
		Artifact a = DependencyNodeUtil.getArtifact(node);
		if (a!=null && !isSeen(a)) {
			visitArtifact(a);
		}
		return true;
	}

	private boolean isSeen(Artifact a) {
		boolean isNew = seenArtifacts.add(a);
		return !isNew;
	}
	
	@Override
	public boolean visitLeave(DependencyNode node) {
		return true;
	}

}
