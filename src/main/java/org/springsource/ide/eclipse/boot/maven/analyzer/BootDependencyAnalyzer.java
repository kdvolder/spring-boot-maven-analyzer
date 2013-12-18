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
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Outputter;


/**
 * Java application that uses maven apis to build a dependency graph for all managed dependencies
 * in a typical spring boot app.
 */
public class BootDependencyAnalyzer {

	static Log log = LogFactory.getLog(BootDependencyAnalyzer.class);
	
	/**
	 * Example of how to use this analyzer.
	 */
	public static void main(String[] args) throws Exception {
		log.info("Starting BootDependencyAnalyzer...");
		BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer();
		analyzer.setPomFile(new File("sample/pom.xml"));
		analyzer.setXmlFile(new File("boot-completion-data.txt"));
		analyzer.setUseSpringProvidesInfo(true); 
		analyzer.run();
	}

	/**
	 * Set a file to which the xml dependency data should be written.
	 */
	public void setXmlFile(File file) {
		Outputter outFile = Outputter.toFile(file);
		setXmlOutputter(outFile);
	}

	public synchronized void setXmlOutputter(Outputter outFile) {
		if (xmlOutput!=null) {
			xmlOutput.dispose();
		}
		xmlOutput = outFile;
	}

	private MavenHelper maven;
	private File pomFile = defaultPomFile();
	
	private boolean useSpringProvidesInfo = false;
	private SpringProvidesInfo providesInfo = null;
	
	/**
	 * Where to write the xml file that is the main result of this analysis.
	 * If not explicitly set the result is printed to System.out
	 */
	private Outputter xmlOutput = null;
	
	public File getPomFile() {
		if (pomFile==null) {
			pomFile = defaultPomFile();
		}
		return pomFile;
	}
	private File defaultPomFile() {
		try {
			return new File(getClass().getClassLoader().getResource("pom.xml").toURI());
		} catch (URISyntaxException e) {
			throw new Error(e);
		}
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
	
	private synchronized Outputter getXmlOutputter() throws Exception {
		if (xmlOutput==null) {
			//Ensure we have some place to write to by default
			xmlOutput = Outputter.toStream(System.out);
		}
		return xmlOutput;
	}


	public BootDependencyAnalyzer() throws PlexusContainerException {
		//TODO: dependency injection of some sort needed here? (configure various maven options).
		maven = new MavenHelper();
	}

	public void run() throws Exception {
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
		
		//Step 3 massage the graph to disambiguate type suggestions based on springprovides infos.
		if (useSpringProvidesInfo) {
			GraphSimplifier.simplify(graph, providesInfo);
		}
		
		//Step 4: save massaged graph to designated output stream.
		saveAsXML(graph);
		
	}


	public void saveAsXML(DirectedGraph graph) throws Exception {
		Outputter outputter = getXmlOutputter();
		try {
			new TypeDependencyGraphXmlWriter(outputter.getOutputStream(), graph);
		} finally {
			outputter.dispose();
		}
	}

	public void setXmlOut(OutputStream out) {
		setXmlOutputter(Outputter.toStream(out));
	}

}
