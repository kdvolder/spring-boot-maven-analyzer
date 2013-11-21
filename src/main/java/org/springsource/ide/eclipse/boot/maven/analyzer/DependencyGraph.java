///*******************************************************************************
// * Copyright (c) 2013 GoPivotal, Inc.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * Contributors:
// *     GoPivotal, Inc. - initial API and implementation
// *******************************************************************************/
//package org.springsource.ide.eclipse.boot.maven.analyzer;
//
//import java.util.Collection;
//import java.util.HashSet;
//
//import org.sonatype.aether.artifact.Artifact;
//
//import edu.uci.ics.jung.graph.DirectedSparseGraph;
//import edu.uci.ics.jung.graph.Graph;
//
///**
// * Represents a maven dependency graph.
// */
//public class DependencyGraph {
//
//	int edgeCounter = 0; //To generate unique ids for edges. Every edge in a jung graph 
//						 // needs an object to represent it even if we don't particularly care 
//						 // to distinguish or label edges.
//	
//	private Graph<Artifact, Integer> graph = new DirectedSparseGraph<Artifact, Integer>();
//	
//	/**
//	 * Create empty dependency Graph
//	 */
//	public DependencyGraph() {
//	}
//	
//	/**
//	 * Add an edge between a 'parent' and a 'child'. The meaning of such an edges is that
//	 * parent dependsOn child. However it should be noted that a dependency graph is a typically
//	 * a dag but not a tree so a node may have more than one parent.
//	 */
//	public void addEdge(Artifact parent, Artifact child) {
//		graph.addEdge(++edgeCounter, parent, child);
//	}
//
//	public boolean addVertex(Artifact artifact) {
//		return graph.addVertex(artifact);
//	}
//
//	public Collection<Artifact> getVertices() {
//		return graph.getVertices();
//	}
//	
//	/**
//	 * Collect any artifiacts that directly or transitively depends on the given artifact.
//	 */
//	public Collection<Artifact> getAncestors(Artifact a) {
//		HashSet<Artifact> ancestors = new HashSet<Artifact>();
//		collectAncestors(a, ancestors);
//		return ancestors;
//	}
//	
//	/**
//	 * This returns a filtered and inverted dependency graph including only the given artifact and any
//	 * artifact that are ancestors of the artifact.
//	 */
//	public DependencyGraph getAncestorsGraph(Artifact a) {
//		DependencyGraph ancestors = new DependencyGraph();
//		copyAncestors(a, new HashSet<Artifact>(), ancestors);
//		return ancestors;
//	}
//
//	private void copyAncestors(Artifact child, HashSet<Artifact> processed, DependencyGraph ancestors) {
//		if (processed.contains(child)) {
//			//already processed
//			return;
//		}
//		processed.add(child); //avoid double processing (and cycles though there shouldn't be any).
//		Collection<Artifact> parents = graph.getPredecessors(child);
//		ancestors.addVertex(child);
//		for (Artifact parent : parents) {
//			ancestors.addVertex(parent); //We must add it before we can create an edge
//			ancestors.addEdge(child, parent); //direction of edges inverted. Grpah will be more treelike in
//			 								// most common case.
//		}
//	}
//
//	private void collectAncestors(Artifact a, HashSet<Artifact> ancestors) {
//		Collection<Artifact> parents = graph.getPredecessors(a);
//		for (Artifact parent : parents) {
//			if (!ancestors.contains(parent)) {
//				ancestors.add(parent);
//				collectAncestors(parent, ancestors);
//			}
//		}
//	}
//
//	/**
//	 * Gets all 'roots' of the dependency graph. A root is any node that has no edges leading into it.
//	 * I.e. the only way to add this artifact to a project is to include it directly.
//	 */
//	public Collection<Artifact> getRoots() {
//		
//		Collection<Artifact> artifacts = getVertices();
//		Collection<Artifact> roots = new HashSet<Artifact>();
//		for (Artifact artifact : artifacts) {
//			if (graph.getPredecessorCount(artifact)==0) {
//				roots.add(artifact);
//			}
//		}
//		return roots;
//	}
//	
//	public void print() {
//		for (Artifact root : getRoots()) {
//			print(0, root);
//		}
//	}
//
//	private void print(int indent, Artifact node) {
//		indent(indent);
//		System.out.println(node);
//		for (Artifact child : graph.getSuccessors(node)) {
//			print(indent+1, child);
//		}
//	}
//	
//
//	private void indent(int indent) {
//		for (int i = 0; i < indent; i++) {
//			System.out.print("   ");
//		}
//	}
//
//	public Collection<Artifact> getChildren(Artifact artifact) {
//		return graph.getSuccessors(artifact);
//	}
//	
//}
