/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
//import org.eclipse.aether.RepositorySystem;
//import org.eclipse.aether.RepositorySystemSession;
//import org.eclipse.aether.artifact.Artifact;
//import org.eclipse.aether.artifact.DefaultArtifact;
//import org.eclipse.aether.examples.ResolveArtifact;
//import org.eclipse.aether.examples.util.Booter;
//import org.eclipse.aether.resolution.ArtifactRequest;
//import org.eclipse.aether.resolution.ArtifactResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springsource.ide.eclipse.boot.maven.analyzer.aether.AetherHelper;
import org.springsource.ide.eclipse.boot.maven.analyzer.aether.ConsoleDependencyGraphDumper;
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;

@Controller
public class AetherialController {

	static Log log = LogFactory.getLog(AetherialController.class);
	
	public AetherialController() {
	}
	
	private AsynchTypeGraphComputer computer = null;
	
	private AetherHelper aether;

	@Autowired(required=true)
	public void setAsynchTypeGraphComputer(AsynchTypeGraphComputer computer) {
		this.computer = computer;
	}
	
	@Autowired(required=true)
	public void setAetherHelper(AetherHelper aether) {
		this.aether = aether;
	}
	
	@RequestMapping(value = "/boot/typegraph/{version:.*}", produces = {"text/xml; charset=UTF-8"})
	public void getTypeGraphMaybe(
			@PathVariable("version") String springBootVersion,
			HttpServletResponse resp
			) throws Exception {
		log.info("type graph request received for '"+springBootVersion+"'");
		Future<byte[]> result = computer.getTypeGraphResponseBody(springBootVersion);
		sendResponse(resp, result);
	}
	
	@RequestMapping(value = "/boot/typegraph", produces = {"text/xml; charset=UTF-8"})
	public void getTypeGraphMaybe(HttpServletResponse resp) throws Exception {
		log.info("type graph request received NO VERSION");
		Future<byte[]> result = computer.getTypeGraphResponseBody(Defaults.defaultVersion);
		sendResponse(resp, result);
	}

	/**
	 * Helper method to send a 'Future' as a response to the client. 
	 * If the future is 'done' actual content (or error) is sent. Otherwise
	 * a 'try later' result is sent instead.
	 */
	private void sendResponse(HttpServletResponse resp, Future<byte[]> result)
			throws IOException, UnsupportedEncodingException {
		if (!result.isDone()) {
			//Return something quickly to let client know we are working on their request but
			// it may be a while.
			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
			resp.setContentType("text/plain");
			resp.setCharacterEncoding("utf8");
			resp.getWriter().println("I'm working on it... ask me again later");
		} else {
			//Done: could be a actual result or an error
			try {
				byte[] resultData = result.get(); //this will throw in case of an error in processing.
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setContentType("text/xml");
				resp.setCharacterEncoding("utf8");
				resp.getOutputStream().write(resultData);
			} catch (Exception e) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("utf8");
				e.printStackTrace(new PrintStream(resp.getOutputStream(), true, "utf8"));
			}
		}
	}

//	@RequestMapping(value="/ulimit/{opt}", produces = {"text/plain; charset=UTF-8"})
//	@ResponseBody
//	public String ulimitDashOpt(@PathVariable("opt") String opt) throws Exception {
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		Process process = Runtime.getRuntime().exec(new String[] {
//			"bash", "-c", "ulimit -"+opt	
//		});
//		process.waitFor();
//		IOUtil.pipe(process.getInputStream(), out);
//		IOUtil.pipe(process.getErrorStream(), out);
//		return out.toString("UTF-8");
//	}
	
	@RequestMapping(value="/maven/artifact/{group}/{arti}/{version}")
	public void getArtifact(
			@PathVariable("group") String gid,
			@PathVariable("arti") String aid,
			@PathVariable("version") String version,
			HttpServletRequest req,
			HttpServletResponse resp
	) throws Exception {
		throw new Error("Not implemented");
//        System.out.println( "------------------------------------------------------------" );
//        System.out.println( req.getServletPath() );
//
//        RepositorySystem system = Booter.newRepositorySystem();
//
//        RepositorySystemSession session = Booter.newRepositorySystemSession( system );
//
//        Artifact artifact = new DefaultArtifact( "org.eclipse.aether:aether-util:0.9.0.M3" );
//
//        ArtifactRequest artifactRequest = new ArtifactRequest();
//        artifactRequest.setArtifact( artifact );
//        artifactRequest.setRepositories( Booter.newRepositories( system, session ) );
//
//        ArtifactResult artifactResult = system.resolveArtifact( session, artifactRequest );
//
//        artifact = artifactResult.getArtifact();
//
//        System.out.println( artifact + " resolved to  " + artifact.getFile() );
	}

	@RequestMapping(value="/boot/mdeps", produces = {"application/json; charset=UTF-8"})
	@ResponseBody
	public List<JsonDependency> getManagedDependencies() throws Exception {
		String bootVersion = Defaults.defaultVersion;
		return getManagedDependencies(bootVersion);
	}

	@RequestMapping(value="/boot/mdeps/{version:.*}", produces = {"application/json; charset=UTF-8"})
	@ResponseBody
	private List<JsonDependency> getManagedDependencies(String bootVersion)
			throws Exception {
		Artifact parentPom = Defaults.parentPom(bootVersion);
		List<Dependency> managedDeps = aether.getManagedDependencies(parentPom);
		System.out.println("==== managed deps ====");
		for (Dependency d : managedDeps) {
			//System.out.println(d);
			Artifact a = d.getArtifact();
			if (a!=null) {
				if (a.getExtension().equals("jar")) {
					System.out.println(a.getGroupId()+":"+a.getArtifactId()+":"+a.getVersion());
				}
			}
		}
		System.out.println("==== managed deps ====");
		return JsonDependency.from(managedDeps);
	}

	@RequestMapping(value="/boot/mdgraph"/*, produces = {"text/plain; charset=UTF-8"}*/)
	public void getManagedDependencyGraph(HttpServletResponse resp) throws Exception {
		getManagedDependencyGraph(Defaults.defaultVersion, resp);
	}
	
	@RequestMapping(value="/boot/mdgraph/{version:.*}"/*, produces = {"text/plain; charset=UTF-8"}*/)
	public void getManagedDependencyGraph(@PathVariable("version")String bootVersion, HttpServletResponse resp) throws Exception {
		Artifact parentPom = Defaults.parentPom(bootVersion);
		CollectResult dgraphResult = aether.getManagedDependencyGraph(parentPom);
		
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/plain");
		OutputStream out = resp.getOutputStream();
		PrintStream pout = new PrintStream(out, true, "UTF-8");
		
		
		ConsoleDependencyGraphDumper dgraphDumper = new ConsoleDependencyGraphDumper(pout);
		dgraphResult.getRoot().accept(dgraphDumper);
	}
	
}
