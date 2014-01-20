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
package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.DirectedGraph;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ExternalType;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Assert;

/**
 * This class implements an algorithm to transform a types + dependencies 
 * graph based on 'spring.provides' properties.
 * <p>
 * The provides properties are used to reduce the number of choices represented
 * in the graph.
 * 
 * @author Kris De Volder
 */
public class GraphSimplifier {

	private static final boolean DEBUG = false;

	/**
	 * Types that are typically already on a 'empty' spring boot project
	 * classpath don't really need to be disambiguated because they can't
	 * be added if they are already there. Here we make a list of
	 * the artifact ids we expect to see on a empty project (transitive
	 * dependencies don't need to be listed as they are implied.
	 * <p>
	 * Note: not including spring-boot-starter-test for now because, though
	 * it is added to a new spring boot app automatically it can be
	 * removed via 'Edit Starters' dialog.
	 */
	private String[] DEFAULT_STARTERS = {
		"spring-boot-starter", "spring-boot-starter-test"
	};
	
	/**
	 * Set to true to also move types even if preferedProvider algo
	 * finds multiple preferedProviders. This ends up making the graph
	 * bigger because single type edge is copied onto multiple 
	 * preferred nodes.
	 */
	private boolean moveAmbiguous = true;
	
	/**
	 * If set to true, then cases where a jar is provided by multiple starters
	 * will be decided based on the 'graph distance' preferring closer starters
	 * over further ones.
	 * <p>
	 * If set to false then jars provided by multiple starters will be treated
	 * as if it is provided by neither starter (thus the jar itself will 
	 * become the preferred way of adding itself to the classpath).
	 */
	private boolean useClosestHeuristic = false;
		
	public boolean isWarningExempt(Artifact a) {
		if (isWarningExempt(a.getArtifactId())) {
			return true;
		}
		for (Object _ancestor : graph.getAncestors(a)) {
			//System.out.println("ancestor: "+_ancestor);
			Artifact ancestor = (Artifact) _ancestor;
			if (isWarningExempt(ancestor.getArtifactId())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isWarningExempt(String artifactId) {
		for (String exemptId : DEFAULT_STARTERS) {
			if (exemptId.equals(artifactId)) {
				return true;
			}
		}
		return false;
	}
	
	private DirectedGraph graph;
	private SpringProvidesInfo providesInfo;

	
	public GraphSimplifier(DirectedGraph graph, SpringProvidesInfo providesInfo) {
		Assert.isNotNull(providesInfo);
		this.graph = graph;
		this.providesInfo = providesInfo;
	}

	public static void simplify(DirectedGraph graph, SpringProvidesInfo providesInfo) {
		new GraphSimplifier(graph, providesInfo).run();
	}

	private void run() {
		detectVersionConflicts();
		//removeRedundantEdges(); //Makes the disambiguation rule more effective?
		disambiguate();
		removeEmptyArtifacts();
	}

	/**
	 * An edge in the graph is 'redundant' if it is impied by other edges.
	 * I.e. if a dependency is expressed directly as well as implied.
	 * Then the expressed dependency is redundant.
	 * 
	 * Note this method no used aymore as it didn't seem to improve the
	 * results of using 'closest' heuristic but actually sometimes
	 * had the oposite effect.
	 */
	private void removeRedundantEdges() {
		ArrayList<Object> nodes = new ArrayList<Object>(graph.getNodes());
		for (Object _node : nodes) {
			if (_node instanceof Artifact) {
				Artifact child = (Artifact) _node;
				Collection<Object> parents = new ArrayList<Object>(graph.getPredecessors(child));
				for (Object parent : parents) {
					//Is the edge between child and parent redundant?
					checkAndRemoveRedundantEdge(parents, parent, child);
				}
			}			
		}
	}

	/**
	 * Check whether an edge between a parent and a child is redundant.
	 * @param parents (copy of the collection of the parents of child)
	 * @param parent 
	 * @param child
	 */
	private void checkAndRemoveRedundantEdge(Collection<Object> parents, Object parent, Object child) {
		for (Object otherParent : parents) {
			if (!otherParent.equals(parent)) {
				Collection<Object> ancestors = graph.getAncestors(otherParent);
				if (ancestors.contains(parent)) {
					//yes it is redundant
					graph.removeEdge(parent, child);
					debug("Removing redundant egde: "+parent + " --> "+child);
					debug("  implied via "+otherParent);
					//We can stop now
					return;
				}
			}
		}
	}


	private void removeEmptyArtifacts() {
		ArrayList<Object> nodes = new ArrayList<Object>(graph.getNodes());
		for (Object _node : nodes) {
			if (_node instanceof Artifact) {
				Artifact node = (Artifact) _node;
				Collection<Object> types = graph.getSuccessors(node);
				if (types.isEmpty()) {
					deleteIfEmpty(node);
				}
//				else {
//					//This jar has some types. Check if it has more than one way to be
//					// added to the cp and maybe log some warnings.
//					Map<Object, Integer> ancestors = graph.getAncestorsWithDistance(node);
//					if (!ancestors.isEmpty() && !isWarningExempt((Artifact)node)) {
//						//warn("Ambigous: types from "+node.getGroupId()+":"+node.getArtifactId()+" can also be added via:");
//						for (Object _parent : ancestors.keySet()) {
//							Artifact parent = (Artifact) _parent;
//							//warn("     "+parent.getGroupId()+":"+parent.getArtifactId()+"(distance "+ ancestors.get(parent)+")");
//						}
//					}
//				}
			}
		}
	}

	/**
	 * Delete an empty node... An empty node is one that has no children. Such nodes are 
	 * not useful in the graph unless they are type nodes. So we should delete them.
	 * <p>
	 * If the deletion makes the parent node empty as well, also delete the parent etc.
	 * @param node
	 */
	private void deleteIfEmpty(Object node) {
		if (isEmpty(node)) {
			debug("Deleting empty node: "+node);
			for (Object parent : new ArrayList<Object>(graph.getPredecessors(node))) {
				graph.removeEdge(parent, node);
				deleteIfEmpty(parent);
			}
		}
	}

	private boolean isEmpty(Object node) {
		return graph.getSuccessors(node).isEmpty();
	}
	
	private void detectVersionConflicts() {
		Map<String, Artifact> idmap = new HashMap<String, Artifact>();
		for (Object node : graph.getNodes()) {
			if (node instanceof Artifact) {
				Artifact art = (Artifact) node;
				String key = art.getGroupId()+":"+art.getArtifactId();
				Artifact existing = idmap.get(key);
				if (existing!=null && !existing.equals(art)) {
					warn("version conflict");
					warn("   "+existing);
					warn("   "+art);
				}
			}
		}
	}
	
	public void disambiguate() {
		Collection<Object> nodes = graph.getNodes();
		for (Object _node : nodes) {
			if (_node instanceof Artifact) {
				Artifact node = (Artifact) _node;
				Collection<Artifact> preferedProviders = getPreferedProviders(node);
				if (preferedProviders.isEmpty()) {
					// No information available to adjust the graph with. 
					// Nothing to do. 
				} else {
					boolean ambiguous = preferedProviders.size()>1;
					//Any types provided by the current artifact should be moved instead to the 'prefered' artifacts.
					if (preferedProviders.size()>1) {
						//If there are multiple prefered providers then the graph will become bigger!
						// A single type edge will be copied to link the type to more than one preferred 
						// location. Issue a warning. This probably means some more disambiguation info is needed from
						// 'spring.provides' files. But it should not be terribly problematic 
						// (The eclipse UI we developed can handle the ambiguity... the only
						// bad effect here is the graph size).
//						warn("Ambigous "+node+" has multiple prefered providers:");
//						for (Artifact a : preferedProviders) {
//							warn("   "+a);
//						}
					}
					if (!ambiguous || moveAmbiguous) {
						Collection<ExternalType> types = graph.getSuccessors(node, ExternalType.class);
						if (!types.isEmpty()) {
							for (Object prefer : preferedProviders) {
								if (node.equals(prefer)) {
									//Don't do anything.. it only amounts to lots of work just copying things 
									//from a node into itself.
								} else {
									debug("Moving "+types.size()+" types\n"
											+ "   from: "+node+"\n"
											+ "     to: "+prefer
									);
									for (Object type : types) {
										graph.removeEdge(node, type);
										graph.addEdge(prefer, type);
										Assert.isTrue(graph.getSuccessors(prefer).contains(type));
										Assert.isTrue(graph.getPredecessors(type).contains(prefer));
										Assert.isTrue(!graph.getSuccessors(node).contains(type));
										Assert.isTrue(!graph.getPredecessors(type).contains(node));
									}
								}
							};
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected Collection<Artifact> getPreferedProviders(Artifact node) {
		//1: consult spring.provides info. This is always 'authorative' if it exists, since it was explicitly
		//  provided by spring-boot developers.
		Collection<Artifact> preferedProviders = providesInfo.getPreferedProviders(node.getArtifactId());
		if (preferedProviders==null) {
			preferedProviders = Collections.EMPTY_SET;
		} else {
			Collection<Object> validProviders = graph.getAncestors(node);
			//It is not safe to mutate the preferedProviders collection directly (it is 'owned' by the providesInfo object).
			preferedProviders = new ArrayList<Artifact>(preferedProviders);
			if (!preferedProviders.isEmpty()) {
				preferedProviders.retainAll(validProviders);
				if (preferedProviders.isEmpty()) {
					warn("INVALID   prefered providers: "+providesInfo.getPreferedProviders(node.getArtifactId()));
					warn("   not ancestor for artifact: "+node.getArtifactId());
				}
			}
		}
		if (!preferedProviders.isEmpty()) {
			if (preferedProviders.size()>1) {
				warn("AMBIGUOUS "+node.getArtifactId()+" (info from spring.provides files):");
				for (Artifact artifact : preferedProviders) {
					warn("    "+artifact.getArtifactId());
				}
			}
			return preferedProviders;
		}
		//2: infer based on graph structure
		return getPreferedProvidersFromGraphStructure(node);
	}

	/**
	 * Infer 'prefered' providers based on heuristics. This is called after consulting
	 * 'spring.provides' info and only if no such information was found.
	 * <p>
	 * It uses the following rules:
	 *   0) if one of the ancestors is in the DEFAULT_STARTERS list then this ancestor
	 *       is always prefered if both are on the list the one with lower index in the
	 *       list wins.
	 *   1) if a jar can be provided by adding a starter then that is preferable over any 
	 *      other means of adding this jar.
	 *   2.a) (useClosestHeuristic enabled)
	 *      if after applying rule 1 there is still ambiguity then choose the jar(s) that 
	 *      are 'closest' in the graph.
	 *   2.b( (useClosestHeuristic disabled)
	 *      if after applying rule 1 there is still ambiguity then let the jar itself
	 *      be its own 'provider'.
	 * 
	 * Distances are computed by following parent links and counting how many links there are
	 * to follow in the shortest path.
	 * 
	 * Observations:
	 *   - if a jar is a starter then the jar itself is always the one that provides itself according to these rules.
	 *   - for a jar that is not related to a starter the jar itself is always the closest jar (distance 0)
	 *   - for a jar is are related to multiple starters pick the starters that are closest.
	 *   - it is still possible to have ambiguity after applying both rules. I.e. for
	 *      a non-starter jar that has multiple starters as ancestors at equal distance.
	 *   
	 * @param node
	 * @return
	 */
	public Collection<Artifact> getPreferedProvidersFromGraphStructure(Artifact node) {
		if (isStarter(node)) {
			//Starters always 'provide themselves'.
			return singleton(node);
		}
		
		Map<Object, Integer> ancestorDistances = graph.getAncestorsWithDistance(node);
		//Rule 0: check the default starters list
		int defaultStarterIndex = Integer.MAX_VALUE;
		Artifact defaultStarter = null;
		for (Object _anc : ancestorDistances.keySet()) {
			Artifact anc = (Artifact) _anc;
			for (int i = 0; i < DEFAULT_STARTERS.length; i++) {
				if (anc.getArtifactId().equals(DEFAULT_STARTERS[i])) {
					if (i<defaultStarterIndex) {
						//remember smallest index
						defaultStarterIndex = i;
						defaultStarter = anc;
					}
				}
			}
			if (defaultStarter!=null) {
				return singleton(defaultStarter);
			}
		}
		
		ArrayList<Artifact> starters = new ArrayList<Artifact>();
		for (Object _anc : ancestorDistances.keySet()) {
			Artifact anc = (Artifact) _anc;
			if (isStarter(anc)) {
				starters.add(anc);
			}
		}
		if (starters.isEmpty()) {
			//The jar is not a starter and has no starters as ancestors.
			return singleton(node);
		} else if (starters.size()==1) {
			//No ambiguity after applying rule 1 since there's only one starter
			return starters;
		}

		//Multiple starter ancestors. Apply rule 2.
		if (useClosestHeuristic) {
			//2.a keep the 'closest one(s)'
			warn("Using retain closest rule for:  "+node.getArtifactId()); 
			for (Artifact artifact : starters) {
				warn("    "+artifact.getArtifactId() + " dist "+ancestorDistances.get(artifact));
			}
			retainClosest(starters, ancestorDistances);
			warn("==> "+(starters.size()==1?"RESOLVED":"AMBIGUOUS"));
			for (Artifact artifact : starters) {
				warn("    "+artifact.getArtifactId() + " dist "+ancestorDistances.get(artifact));
			}
			return starters;
		} else {
			//2.b when unclear who 'owns' a jar, nobody owns it.
			warn("AMBIGUOUS: Several starters provide: "+node.getArtifactId());
			for (Artifact artifact : starters) {
				warn("    "+artifact.getArtifactId());
			}
			warn("==> RESOLVED: no starter provides this jar");
			return singleton(node);
		}
	}

	private Collection<Artifact> singleton(Artifact node) {
		return Collections.singleton(node);
//		ArrayList<Artifact> collection = new ArrayList<Artifact>();
//		collection.add(node);
//		return collection;
	}

	/**
	 * Given a collection of Artifact nodes and distances 
	 * remove from the collection all nodes except the ones at
	 * the shortest distance.
	 */
	private void retainClosest(Collection<Artifact> nodes, Map<Object, Integer> distances) {
		//Determine shortest distance ...
		int shortestDistance = Integer.MAX_VALUE;
		for (Artifact node : nodes) {
			int dist = distances.get(node);
			if (dist<shortestDistance) {
				shortestDistance = dist;
			}
		}
		//Remove all but the one(s) that are at shortest distance.
		for (Iterator<Artifact> iterator = nodes.iterator(); iterator.hasNext();) {
			Artifact artifact = iterator.next();
			int dist = distances.get(artifact);
			if (dist!=shortestDistance) {
				iterator.remove();
			}
		}
	}

// Implementation that *only* uses the spring.provides info in starter jars.
//	public Collection<Artifact> getPreferedProviders(Artifact node) {
//		return providesInfo.getPreferedProviders(node.getArtifactId());
//	}

	private boolean isStarter(Artifact node) {
		return node.getArtifactId().startsWith("spring-boot-starter");
	}

	private void warn(String string) {
		System.out.println(string);
	}
	
	private void debug(String string) {
		if (DEBUG) {
			System.out.println(string);
		}
	}
}
