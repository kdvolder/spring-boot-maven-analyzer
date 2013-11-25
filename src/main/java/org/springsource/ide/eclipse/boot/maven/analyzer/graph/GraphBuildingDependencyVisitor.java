/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.graph;

import java.util.HashSet;
import java.util.Stack;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ArtifactTypeDiscovery;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ExternalType;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypeEntry;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

/**
 * Maven dependency visitor that builds a {@link DependencyGraph}.
 * The graph contains more or less the same information as the Graph
 * represented by a given 'root' {@link DependencyNode}.
 * Except that 
 *   - it doesn't retain 'scope' information (is this a problem?)
 *   - it uses a true graph datastructure as storage so that 
 *     it is easy to navigate from dependencies to ancestors (not possible
 *     with maven's DependencyNode which only points to its children but not
 *     its 'parents'.
 * 
 * @author Kris De Volder 
 */
public class GraphBuildingDependencyVisitor implements DependencyVisitor {
	
	private static final boolean DEBUG = false;
	
	int depth = 0;
	int line = 0;

	private DirectedGraph graph = new DirectedGraph();
	private Stack<Artifact> path = new Stack<Artifact>();
	
	private HashSet<Artifact> isSeen = new HashSet<Artifact>();

	private void print(Object node) {
		if (DEBUG) {
			System.out.print(String.format("%3d", ++line));
			for (int i = 0; i < depth; i++) {
				System.out.print("   ");
			}
			System.out.println(node);
		}
	}

	private Artifact getArtifact(DependencyNode node) {
		if (node!=null) {
			org.sonatype.aether.graph.Dependency dep = node.getDependency();
			if (dep!=null) {
				return dep.getArtifact();
			}
		}
		return null;
	}

	public boolean visitEnter(DependencyNode node) {
		//Note that even if an artifact was seen already we should still register
		// an edge to the parent node because we are encountering it
		// as descendant of a different parent.
		//However we should avoid revisting the node's children since we
		// already registered those edges.
		Artifact artifact = getArtifact(node);
		if (artifact==null) {
			//a node that's only there because it has dependencies. we must visit the children
			return true;
		}
		if (!isInterestingArtifact(artifact)) {
			return false;
		}
		try {
			if (isSeen(artifact)) {
				print(node+"...");
				return false; // don't repeat this tree's info again.
			} else {
				print(node);
				visitTypes(artifact);
				return true;
			}
		} finally {
			Artifact parent = getParent();
			if (parent!=null && artifact!=null) {
				//print("*"+parent+" dependsOn "+artifact);
				addEdge(parent, artifact);
			}
			path.push(artifact);
			depth++;
		}
	}
	
	private void visitTypes(final Artifact artifact) {
		ArtifactTypeDiscovery discoverer = new ArtifactTypeDiscovery(artifact);
		discoverer.getTypes(new Requestor<ExternalTypeEntry>() {
			public boolean receive(ExternalTypeEntry element) {
				try {
					ExternalType type = element.getType();
					addEdge(artifact, type);
					return true; // Yes, I want more.
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		});
	}
	
	public boolean visitLeave(DependencyNode node) {
		Artifact artifact = getArtifact(node);
		if (artifact==null) {
			return true;
		}
		if (isInterestingArtifact(artifact)) {
			depth--;
			path.pop();
		}
		return true;
	}
	
	
	private boolean isInterestingArtifact(Artifact artifact) {
		//For now only deal with artifacts that look like '${group}:${aid}:jar:${version}'
		// other more irregular stuff might be test dependencies sources jars etc.
		// let's keep it simple and leave all of those out for now
		
		String artifactStr = artifact.toString();
		String[] pieces = artifactStr.split(":");
		if (pieces.length==4) {
			return pieces[2].equals("jar");
		}
		return false;
	}
	

	private void addEdge(Object parent, Object child) {
		graph.addEdge(parent, child);
	}

	private Artifact getParent() {
		if (!path.isEmpty()) {
			return path.peek();
		}
		return null;
	}

	private boolean isSeen(Artifact artifact) {
		if (artifact!=null) {
			boolean isNew = isSeen.add(artifact);
			return !isNew;
		}
		return false;
	}

	public DirectedGraph getGraph() {
		return graph;
	}
}