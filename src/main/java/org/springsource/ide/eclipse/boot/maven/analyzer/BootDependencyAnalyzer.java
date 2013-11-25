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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainerException;
import org.sonatype.aether.graph.DependencyNode;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.DirectedGraph;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.GraphBuildingDependencyVisitor;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.TypeDependencyGraphXmlWriter;
import org.springsource.ide.eclipse.boot.maven.analyzer.maven.DependencyCollector;
import org.springsource.ide.eclipse.boot.maven.analyzer.maven.MavenHelper;


/**
 * Java application that uses maven apis to build a dependency graph for all managed dependencies
 * in a typical spring boot app.
 */
public class BootDependencyAnalyzer {

	/**
	 * Example of how to use this analyzer.
	 */
	public static void main(String[] args) throws Exception {
		BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer();
		analyzer.setPomFile(new File("sample/pom.xml"));
		analyzer.setXmlFile(new File("boot-completion-data.txt"));
		analyzer.setUseSpringProvidesInfo(true); 
		analyzer.run();
	}

	private MavenHelper maven;
	private File pomFile = new File("sample/pom.xml");
	private File xmlFile = null;
	
	private boolean useSpringProvidesInfo = false;
	private SpringProvidesInfo providesInfo = null;
	
	public File getPomFile() {
		return pomFile;
	}
	public void setPomFile(String path) {
		this.pomFile = new File(path);
	}
	public void setPomFile(File path) {
		this.pomFile = path;
	}
	
	public void setUseSpringProvidesInfo(boolean useSpringProvidesInfo) {
		this.useSpringProvidesInfo = useSpringProvidesInfo;
	}
	
	public void setXmlFile(File xmlFile) {
		this.xmlFile = xmlFile;
	}
	
	public OutputStream openXmlOutputStream() throws Exception {
		if (xmlFile!=null) {
			return new FileOutputStream(xmlFile);
		}
		return System.out;
	}


	public BootDependencyAnalyzer() throws PlexusContainerException {
		//TODO: dependency injection of some sort needed here? (configure various maven options).
		maven = new MavenHelper();
	}
	


	private void run() throws Exception {
		File pomFile = getPomFile();
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
		
//		//Step 1 use collect the infos from 'spring.provides' properties file so they can be used in the next stage.
		if (useSpringProvidesInfo) {
			SpringProvidesDependencyVisitor springProvidesCollector = new SpringProvidesDependencyVisitor();
			tree.accept(springProvidesCollector);
			providesInfo = springProvidesCollector.getInfo();
		}
		
		
		//Step 2 build a copy of our types + dependencies graph.
		GraphBuildingDependencyVisitor graphBuilder = new GraphBuildingDependencyVisitor();
		tree.accept(graphBuilder);
		DirectedGraph graph = graphBuilder.getGraph();
		
		if (useSpringProvidesInfo) {
			GraphSimplifier.simplify(graph, providesInfo);
		}
		//TODO: Step 3 massage the graph with to disambiguate type suggestions based on springprovides infos.
		
		
		//Step 4: save massaged graph to designated output stream.
		saveAsXML(graph);
		
		//Below is the old way of writing the graph data directly into an xml file without actually building it
		// into a in-memory data structure. That's obviouslty more efficient but also much harder to manipulate the graph
//		XMLWritingDependencyVisitor visitor = new XMLWritingDependencyVisitor("boot-completion-data-REFERENCE.txt");
//		tree.accept(visitor);
//		visitor.close();
		
//		System.out.println("# types = "+visitor.getKnownTypes().size());

	}


	public void saveAsXML(DirectedGraph graph) throws Exception {
		OutputStream out = openXmlOutputStream();
		try {
			new TypeDependencyGraphXmlWriter(out, graph);
		} finally {
			//Careful not to close System.out. So only close stream
			// if it was created from a file.
			if (xmlFile!=null && out!=null) {
				out.close();
			}
		}
	}
}
