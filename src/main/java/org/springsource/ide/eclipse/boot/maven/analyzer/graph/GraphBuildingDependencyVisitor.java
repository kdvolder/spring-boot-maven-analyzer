/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.graph;

import java.util.Stack;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

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

	private TypeAndArtifactGraph graph;
	private Stack<ArtifactNode> path = new Stack<ArtifactNode>();
	
//	private HashSet<Artifact> isSeen = new HashSet<Artifact>();
	
	private ArtifactNodeFactory artifactFactory;

	public GraphBuildingDependencyVisitor(ArtifactNodeFactory anf) {
		this.artifactFactory = anf;
		this.graph = new TypeAndArtifactGraph();
	}

	private void print(Object node) {
		if (DEBUG) {
			System.out.print(String.format("%3d", ++line));
			for (int i = 0; i < depth; i++) {
				System.out.print("   ");
			}
			System.out.println(node);
		}
	}

	private ArtifactNode getArtifact(DependencyNode node) {
		
		if (node!=null) {
			Artifact a = node.getArtifact();
			if (a!=null) {
				//The graph is 'verbose' and so annotated with conflict infos and managed dependencies info.
				//We need to do this otherwise edges are lost (aether conflict resolver makes
				//sure only one version of any lib is used throughout the tree and removes duplicate nodes
				//so the graph becomes a tree.
				//That's not good for us because it means we loose dependency edges in the graph.
			
				// case 1: the node represents a conflict that was resolved.
		        DependencyNode winner = (DependencyNode) node.getData().get( ConflictResolver.NODE_DATA_WINNER );
		        if ( winner != null ) {
		        	return artifactFactory.create(winner.getArtifact());
		        }
		        
		        //case 2: dependency version was changed by dep management
		        // Actually nothing to do. Just use the node as is. It's version is already changed
		        // by dep management. There's just some extra info in the node telling what
		        // it would have been otherwise. But that doesn't matter to us.
		        
		        //case 3: nothing special
		        return artifactFactory.create(a);
			}
		}
		return null;
	}

	public boolean visitEnter(DependencyNode node) {
		//Note that even if an artifact was seen already we should still register
		// an edge to the parent node because we are encountering it
		// as descendant of a different parent.
		//However we should avoid revisiting the node's children since we
		// already registered those edges.
		ArtifactNode artifact = getArtifact(node);
		if (artifact==null) {
			//empty root node that's only there because it has dependencies. we must visit the children
			return true;
		}
//		if (!isInterestingArtifact(artifact)) {
//			return false;
//		}
		try {
//			if (isSeen(artifact)) {
//				print(node+"...");
//				return false; // don't repeat this tree's info again.
//			} else {
				print(node);
//				visitTypes(artifact);
				return true;
//			}
		} finally {
			ArtifactNode parent = getParent();
			if (parent!=null && artifact!=null) {
				//print("*"+parent+" dependsOn "+artifact);
				addEdge(parent, artifact);
			}
			path.push(artifact);
			depth++;
		}
	}
	
//	private void visitTypes(final Artifact artifact) {
//		ArtifactTypeDiscovery discoverer = new ArtifactTypeDiscovery(artifact);
//		discoverer.getTypes(new Requestor<ExternalTypeEntry>() {
//			public boolean receive(ExternalTypeEntry element) {
//				try {
//					ExternalType type = element.getType();
//					addEdge(artifact, type);
//					return true; // Yes, I want more.
//				} catch (Exception e) {
//					throw new Error(e);
//				}
//			}
//		});
//	}
	
	public boolean visitLeave(DependencyNode node) {
		ArtifactNode artifact = getArtifact(node);
		if (artifact==null) {
			return true;
		}
//		if (isInterestingArtifact(artifact)) {
			depth--;
			path.pop();
//		}
		return true;
	}
	
	
//	private boolean isInterestingArtifact(ArtifactNode artifact) {
//		//For now only deal with artifacts that look like '${group}:${aid}:jar:${version}'
//		// other more irregular stuff might be test dependencies sources jars etc.
//		// let's keep it simple and leave all of those out for now
//		
//		String artifactStr = artifact.toString();
//		String[] pieces = artifactStr.split(":");
//		if (pieces.length==4) {
//			return pieces[2].equals("jar");
//		}
//		return false;
//	}
	

	private void addEdge(Object parent, Object child) {
		graph.addEdge(parent, child);
	}

	private ArtifactNode getParent() {
		if (!path.isEmpty()) {
			return path.peek();
		}
		return null;
	}

//	private boolean isSeen(Artifact artifact) {
//		if (artifact!=null) {
//			boolean isNew = isSeen.add(artifact);
//			return !isNew;
//		}
//		return false;
//	}

	public TypeAndArtifactGraph getGraph() {
		return graph;
	}
}