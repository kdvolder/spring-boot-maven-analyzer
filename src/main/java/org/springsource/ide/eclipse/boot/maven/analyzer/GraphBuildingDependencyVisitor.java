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
//import java.util.Stack;
//
//import org.sonatype.aether.artifact.Artifact;
//import org.sonatype.aether.graph.DependencyNode;
//import org.sonatype.aether.graph.DependencyVisitor;
//
///**
// * Maven dependency visitor that builds a {@link DependencyGraph}.
// * The graph contains more or less the same information as the Graph
// * represented by a given 'root' {@link DependencyNode}.
// * Except that 
// *   - it doesn't retain 'scope' information (is this a problem?)
// *   - it uses a true graph datastructure as storage so that 
// *     it is easy to navigate from dependencies to ancestors (not possible
// *     with maven's DependencyNode which only points to its children but not
// *     its 'parents'.
// * 
// * @author Kris De Volder 
// */
//public class GraphBuildingDependencyVisitor implements DependencyVisitor {
//	int depth = 0;
//	int line = 0;
//
//	private DependencyGraph graph = new DependencyGraph();
//	private Stack<Artifact> path = new Stack<Artifact>();
//
//	private void print(Object node) {
//		System.out.print(String.format("%3d", ++line));
//		for (int i = 0; i < depth; i++) {
//			System.out.print("   ");
//		}
//		System.out.println(node);
//	}
//
//	private Artifact getArtifact(DependencyNode node) {
//		if (node!=null) {
//			org.sonatype.aether.graph.Dependency dep = node.getDependency();
//			if (dep!=null) {
//				return dep.getArtifact();
//			}
//		}
//		return null;
//	}
//
//	public boolean visitEnter(DependencyNode node) {
//		Artifact artifact = getArtifact(node);
//		try {
//			if (isSeen(artifact)) {
//				print(node+"...");
//				return false; // don't repeat this tree's info again.
//			} else {
//				print(node);
//				return true;
//			}
//		} finally {
//			Artifact parent = getParent();
//			if (parent!=null) {
//				//print("*"+parent+" dependsOn "+artifact);
//				addEdge(parent, artifact);
//			}
//			path.push(artifact);
//			depth++;
//		}
//	}
//
//	private void addEdge(Artifact parent, Artifact artifact) {
//		graph.addEdge(parent, artifact);
//	}
//
//	private Artifact getParent() {
//		if (!path.isEmpty()) {
//			return path.peek();
//		}
//		return null;
//	}
//
//	private boolean isSeen(Artifact artifact) {
//		if (artifact!=null) {
//			boolean isNew = graph.addVertex(artifact);
//			return !isNew;
//		}
//		return false;
//	}
//
//	public boolean visitLeave(DependencyNode node) {
//		depth--;
//		path.pop();
//		return true;
//	}
//
//	public Collection<Artifact> getArtifacts() {
//		return graph.getVertices();
//	}
//
//	public DependencyGraph getGraph() {
//		return graph;
//	}
//}