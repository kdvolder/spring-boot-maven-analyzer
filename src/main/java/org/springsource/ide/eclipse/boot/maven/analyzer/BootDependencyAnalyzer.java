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

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainerException;
import org.sonatype.aether.graph.DependencyNode;


/**
 * Java application that uses maven apis to build a dependency graph for all managed dependencies
 * in a typical spring boot app.
 */
public class BootDependencyAnalyzer {

	public static void main(String[] args) throws Exception {
		new BootDependencyAnalyzer().run();
	}

	private MavenHelper maven;

	public BootDependencyAnalyzer() throws PlexusContainerException {
		maven = new MavenHelper();
	}

	private void run() throws Exception {
		File pomFile = new File("sample/pom.xml");

		MavenProject project = maven.readMavenProject(pomFile);
		
		DependencyCollector dependencies = new DependencyCollector(maven, project);
		
		DependencyManagement depMan = project.getDependencyManagement();
		if (depMan!=null) {
			List<Dependency> deps = depMan.getDependencies();
			if (deps!=null) {
				for (Dependency d : deps) {
					dependencies.addRoot(d);
				}
			}
		}
		
		DependencyNode tree = dependencies.resolveDependencyTree();
		//Step 1 use collect the infos from 'spring.provides' properties file so they can be used in the next stage.
		SpringProvidesDependencyVisitor springProvidesCollector = new SpringProvidesDependencyVisitor();
		tree.accept(springProvidesCollector);
		
		XMLWritingDependencyVisitor visitor = new XMLWritingDependencyVisitor("boot-completion-data.txt");
//		GraphBuildingDependencyVisitor visitor = new GraphBuildingDependencyVisitor();
		tree.accept(visitor);
		visitor.close();
		
		System.out.println("# types = "+visitor.getKnownTypes().size());

	}

}
