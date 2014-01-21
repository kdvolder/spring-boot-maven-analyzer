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
package org.springsource.ide.eclipse.boot.maven.analyzer.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.springsource.ide.eclipse.boot.maven.analyzer.aether.AetherHelper;

/**
 * Provides methods build up a request to collect dependencies and eventually retrieve the result.
 * <p>
 * This class does no caching at all. Requests are executed each time the methods to retrieve the result
 * is called. 
 */
public class DependencyCollector {

	private AetherHelper maven;

	/**
	 * Collect roots that form the basis of this request. Will be nulled once the
	 * request starts executing.
	 */
	private List<Dependency> roots = new ArrayList<Dependency>();

	public DependencyCollector(AetherHelper maven) {
		this.maven = maven;
	}

	/**
	 * Adds a 'root' to the request. Request can have multiple roots so we can collect/resolve all dependencies
	 * for a bunch of things at once rather than have to make multiple requests.
	 */
	public void addRoot(Dependency d) throws Exception {
		roots.add(d);
	}

	private synchronized DependencyNode readDependencyTree(boolean resolve) throws Exception {
//		//code in here based on org.eclipse.m2e.core.embedder.MavenModelManager.readDependencyTree(RepositorySystemSession, MavenProject, String)
//
//		MavenExecutionRequest executionRequest = maven.createExecutionRequest();
//		DefaultRepositorySystemSession session = maven.createRepositorySession(executionRequest);
//
////	    DependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(
////	    		new JavaEffectiveScopeCalculator()
////	            new NearestVersionConflictResolver() 
//	    			//Watch out the nearest NearestVersionConflictResolver from maven 'aether' package.
//	    			//destroys the graph's structure (it resolves conflicts by deleting nodes from the graph).
//	    			//Although all artfifacts we care about are still in the graph, deleting nodes
//	    			//removes also the corresponding 'dependsOn' links which we also care about.
//	    			//TODO: implement an alternative
////	    );
////	    session.setDependencyGraphTransformer(transformer);
//
//		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
//		try {
//			Thread.currentThread().setContextClassLoader(maven.getProjectRealm(mavenProject));
//
//			ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();
//
//			CollectRequest request = new CollectRequest();
//			request.setRequestContext("project"); //$NON-NLS-1$
//			request.setRepositories(mavenProject.getRemoteProjectRepositories());
//
//			for(org.apache.maven.model.Dependency dependency : roots) {
//				request.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
//			}
//
//			DependencyManagement depMngt = mavenProject.getDependencyManagement();
//			if(depMngt != null) {
//				for(org.apache.maven.model.Dependency dependency : depMngt.getDependencies()) {
//					request.addManagedDependency(RepositoryUtils.toDependency(dependency, stereotypes));
//				}
//			}
//			DependencyNode node;
//			if (!resolve) {
//				node = maven.getRepositorySystem().collectDependencies(session, request).getRoot();
//			} else {
//				node = maven.getRepositorySystem().resolveDependencies(session, new DependencyRequest(request, null)).getRoot();
//			}
//
//			return node;
//		} finally {
//			Thread.currentThread().setContextClassLoader(oldClassLoader);
//		}
		return null;
	}

	public DependencyNode readDependencyTree() throws Exception {
		return readDependencyTree(false);
	}

	public DependencyNode resolveDependencyTree() throws Exception {
		return readDependencyTree(true);
	}
	
}
