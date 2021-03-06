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
package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.PlexusContainerException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.DependencyNode;
import org.springsource.ide.eclipse.boot.maven.analyzer.aether.AetherHelper;
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.ArtifactNodeFactory;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.DirectedGraph;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.GraphBuildingDependencyVisitor;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.TypeAndArtifactGraph;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.TypeDependencyGraphXmlWriter;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ArtifactTypeDiscovery;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ExternalType;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypeEntry;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Outputter;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

/**
 * Java application that uses maven apis to build a dependency graph for all managed dependencies
 * in a typical spring boot app.
 */
public class BootDependencyAnalyzer {

	/**
	 * Server log... not accessible via end points only on the server itself.
	 */
	static Log log = LogFactory.getLog(BootDependencyAnalyzer.class);

//	/**
//	 * Example of how to use this analyzer.
//	 */
//	public static void main(String[] args) throws Exception {
//		log.info("Starting BootDependencyAnalyzer...");
//		log.info("============== starting =============================");
//		BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer(new AetherHelper());
//		analyzer.setBootVersion(Defaults.defaultVersion);
//		analyzer.setXmlFile(new File("boot-completion-data.txt"));
//		analyzer.setUseSpringProvidesInfo(true);
//		analyzer.run();
//		log.info("============== Exiting ============================");
//
//	}


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

	private AetherHelper aether;
	private ArtifactNodeFactory anf = new ArtifactNodeFactory();

	private String bootVersion;
	private boolean useSpringProvidesInfo = false;
	private SpringProvidesInfo providesInfo = null;

	/**
	 * Where to write the xml file that is the main result of this analysis.
	 * If not explicitly set the result is printed to System.out
	 */
	private Outputter xmlOutput = null;
	private UserLog userLog;

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

	public BootDependencyAnalyzer(AetherHelper aether) throws PlexusContainerException {
		this.aether = aether;
	}

	public void addTypesFrom(final Artifact artifact, final TypeAndArtifactGraph graph) {
		ArtifactTypeDiscovery discoverer = new ArtifactTypeDiscovery(artifact);
		discoverer.getTypes(new Requestor<ExternalTypeEntry>() {
			public boolean receive(ExternalTypeEntry element) {
				try {
					ExternalType type = element.getType();
					graph.addEdge(anf.create(artifact), type);
					return true; // Yes, I want more.
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		});
	}

	public void run() throws Exception {
		UserLog userLog = getUserLog();
		try {
			userLog.println("=== Dependency Graph Analysis Report ===");
			Artifact parentPom = Defaults.parentPom(bootVersion);
			userLog.println("Boot Version: "+bootVersion);
			if (getMajorVersion(bootVersion) >= 2) {
				// See: https://www.pivotaltracker.com/story/show/156152874
				userLog.println("JarType assist support disabled from Boot 2.0.0 onward.");
				userLog.println("Creating a dummy typegraph to satisfy api contract");
				TypeAndArtifactGraph graph = new TypeAndArtifactGraph();
				saveAsXML(graph);
			} else {
				CollectResult collectResult = aether.getManagedDependencyGraph(parentPom);
				DependencyNode tree = collectResult.getRoot();
	
	
				//The tree/graph just computed is not yet resolved. I.e. dependency structure is known
				// but the binary jar artefacts are not yet downloaded or resolved to actual files.
				//The graph also has verbose options for conflict resolution and managed dependencies enabled.
				//This means that the graph still contains data about conflicts and changes made based on dependency management.
				//IMPORTANT: This graph may contain duplicate artifacts and is not suitable for resolution (according to aether docs).
	
				GraphBuildingDependencyVisitor graphBuilder = new GraphBuildingDependencyVisitor(anf);
				tree.accept(graphBuilder);
				TypeAndArtifactGraph graph = graphBuilder.getGraph();
	
				boolean ignoreUnresolved = true;
				List<Artifact> resolvedArtifacts = aether.resolve(graph.getArtifacts(), ignoreUnresolved);
				for (Artifact artifact : resolvedArtifacts) {
					addTypesFrom(artifact, graph);
				}
	
				//Step 1 collect the infos from 'spring.provides' properties file so they can be used in the next stage.
				if (useSpringProvidesInfo) {
					providesInfo = new SpringProvidesInfo(anf);
					for (Artifact artifact : resolvedArtifacts) {
						providesInfo.process(artifact);
					}
				}
	
				//Step 2 massage the graph to disambiguate type suggestions based on springprovides infos.
				if (useSpringProvidesInfo) {
					GraphSimplifier.simplify(graph, providesInfo, userLog);
				}
	
				//Step 4: save massaged graph to designated output stream.
				saveAsXML(graph);
			}
		} finally {
			userLog.dispose();
		}

	}


	private static int getMajorVersion(String bootVersion) {
		int dot = bootVersion.indexOf('.');
		if (dot>=0) {
			return Integer.parseInt(bootVersion.substring(0, dot));
		}
		return 0;
	}

	private synchronized UserLog getUserLog() {
		if (userLog==null) {
			//By default write user  on System out.
			userLog = new UserLog(System.out);
		}
		return userLog;
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

	public synchronized void setLog(ByteArrayOutputStream log) {
		userLog = new UserLog(log);
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}


}
