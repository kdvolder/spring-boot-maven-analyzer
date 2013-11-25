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

import org.sonatype.aether.artifact.Artifact;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.DirectedGraph;
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
	public static final String[] DONT_WARN_ABOUT = {
		"spring-boot-starter"
	};
	
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
		for (String exemptId : DONT_WARN_ABOUT) {
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
		disambiguate();
		removeEmptyArtifacts();
	}

	private void removeEmptyArtifacts() {
		// TODO implement this it can make the xml file smaller.
		// Though it likley won't make a huge difference (seeing that the nodes are already empty, there xml will
		//  be pretty small.
	}

	public void disambiguate() {
		Collection<Object> nodes = graph.getNodes();
		for (Object _node : nodes) {
			if (_node instanceof Artifact) {
				Artifact node = (Artifact) _node;
				Collection<Artifact> preferedProviders = providesInfo.getPreferedProviders(node.getArtifactId());
				if (preferedProviders!=null) {
					Collection<Object> validProviders = graph.getAncestors(node);
					preferedProviders.retainAll(validProviders);
					
					if (preferedProviders.isEmpty()) {
						// No information available to adjust the graph with. 
						// Nothing to do. Well... except for checking whether there is 
						// ambiguity here and printing some warnings if that's the case.
						Collection<Object> types = graph.getSuccessors(node);
						if (!types.isEmpty()) {
							//This jar has some types. Check if it has more than one way to be
							// added to the cp.
							Collection<Object> ancestors = graph.getAncestors(node);
							if (!ancestors.isEmpty() && !isWarningExempt((Artifact)node)) {
								warn("Ambigous: types from "+node.getArtifactId()+" can also be added via:");
								for (Object parent : ancestors) {
									warn("     "+((Artifact)parent).getArtifactId());
								}
							}
						}
					} else {
						//Any types provided by the current artifact should be moved instead to the 'prefered' artifacts.
						if (preferedProviders.size()>1) {
							//If there are multiple prefered providers then the graph will become bigger!
							// A single type edge will be copied to link the type to more than one preferred 
							// location. Issue a warning. This probably means some more disambiguation info is needed from
							// 'spring.provides' files. But it should not be terribly problematic 
							// (The eclipse UI we developed can handle the ambiguity... the only
							// bad effect here is the graph size).
							warn("Ambigous "+node+" has multiple prefered providers:");
							for (Artifact a : preferedProviders) {
								warn("   "+a);
							}
						}
						for (Object prefer : preferedProviders) {
							ArrayList<Object> types = new ArrayList<Object>(graph.getSuccessors(node));
							if (!types.isEmpty()) {
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
							};
						}
					}
				}
			}
		}
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
