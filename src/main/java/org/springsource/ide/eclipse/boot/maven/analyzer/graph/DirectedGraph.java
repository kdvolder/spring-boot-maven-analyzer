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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Assert;

/**
 * Simple directed graph implementation. A directed graph is simply a
 * collection of directed edges connection two nodes to eachother.
 * <p>
 * The set of nodes is represented implicitly (any object involved in 
 * an edge is a node).
 * <p>
 * The edge set is stored as two multimaps (one indexed by start node
 * and the other by end node).
 * 
 * @author Kris De Volder
 */
public class DirectedGraph {
	
	//TODO: cleanup all generics/raw types warnings.
	
	private MultiMap dgraph;
	private MultiMap inverted; //Edges in the oposite direction.

	public DirectedGraph() {
		this.dgraph = new MultiValueMap();
		this.inverted = new MultiValueMap();
	}
	
	public void addEdge(Object parent, Object child) {
		dgraph.put(parent, child);
		inverted.put(child, parent);
	}
	public void removeEdge(Object parent, Object child) {
		dgraph.remove(parent, child);
		inverted.remove(child, parent);
	}
	
	/**
	 * Retrieve all nodes in the graph that are reachable from a given starting node.
	 * The starting node itself is not included in the result unless there is cycle
	 * in the graph leading back to the starting node.
	 */
	@SuppressWarnings("rawtypes")
	public Set getDescendants(Object node) {
		Set descendants = new LinkedHashSet();
		return getDescendants(node, descendants);
	}

	/**
	 * Retrieve all nodes in the graph that are reachable from a given starting node.
	 * The starting node itself is not included in the result unless there is cycle
	 * in the graph leading back to the starting node.
	 * 
	 * @param descendants a (emtpy) collection that will be used to collect the
	 *     result into (this allows client to determine the type of collection used 
	 *     (e.g. HashSet versus LinkedHashSet).
	 */
	private Set getDescendants(Object node, Set descendants) {
		Assert.isLegal(descendants.isEmpty());
		collectDescendants(node, descendants);
		return descendants;
	}

	public Collection<Object> getAncestors(Object node) {
		Set<Object> ancestors = new HashSet<Object>();
		return getAncestors(node, ancestors);
	}

	public Collection<Object> getAncestors(Object node, Set<Object> ancestors) {
		Assert.isLegal(ancestors.isEmpty());
		collectAncestors(node, ancestors);
		return ancestors;
	}

	private void collectAncestors(Object node, Set<Object> ancestors) {
		Collection<Object> parents = getPredecessors(node);
		for (Object parent : parents) {
			boolean isNew = ancestors.add(parent);
			if (isNew) {
				collectAncestors(parent, ancestors);
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void collectDescendants(Object node, Set ancestors) {
		Collection children = (Collection) dgraph.get(node);
		if (children!=null && !children.isEmpty()) {
			for (Object child : children) {
				boolean isNew = ancestors.add(child);
				if (isNew) {
					collectDescendants(child, ancestors);
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Collection<Object> getSuccessors(Object node) {
		Collection nodes = (Collection)dgraph.get(node);
		if (nodes!=null) {
			return nodes;
		}
		return Collections.EMPTY_SET;
	}


	/**
	 * Get all nodes in the graph that have at least one successor.
	 */
	@SuppressWarnings("rawtypes")
	public Set getNonLeafNodes() {
		return dgraph.keySet();
	}

	public void eachEdge(EdgeAction doWithEdge) {
		for (Object parent : dgraph.keySet()) {
			Collection children = (Collection)dgraph.get(parent);
			for (Object child : children) {
				doWithEdge.run(parent, child);
			}
		}
	}
	
	/**
	 * Retrieve all vertices in the graph that have only outgoing but no
	 * incoming edges.
	 */
	public Collection<Object> getRoots() {
		final LinkedHashSet<Object> roots = new LinkedHashSet<Object>();
		for (Object node : getNodes()) {
			Collection<Object> parents = getPredecessors(node);
			if (parents.isEmpty()) {
				roots.add(node);
			}
		}
		return roots;
	}
	
	public Collection<Object> getPredecessors(Object node) {
		Collection preds = (Collection)inverted.get(node);
		if (preds != null) {
			return preds;
		}
		return Collections.EMPTY_SET;
	}

	public Collection<Object> getNodes() {
		final LinkedHashSet<Object> nodes = new LinkedHashSet<Object>();
		eachEdge(new EdgeAction() {
			@Override
			public void run(Object from, Object to) {
				nodes.add(from);
				nodes.add(to);
			}
		});
		return nodes;
	}

//	/**
//	 * Create a copy of this graph inverting all the edges.
//	 */
//	public DirectedGraph invert() {
//		DirectedGraph g = new DirectedGraph();
//		for (Object _entry : dgraph.entrySet()) {
//			Entr
//		}
//	}

}
