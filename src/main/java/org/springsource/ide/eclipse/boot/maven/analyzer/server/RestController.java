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
package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.project.MavenProject;
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
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;
import org.springsource.ide.eclipse.boot.maven.analyzer.maven.MavenHelper;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.IOUtil;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.PomGenerator;

@Controller
public class RestController {

	static Log log = LogFactory.getLog(RestController.class);
	
	@Autowired
	private PomGenerator pomGenerator;
	
	public RestController() {
	}
	
	private AsynchTypeGraphComputer computer = null;
	
	@Autowired(required=true)
	public void setAsynchTypeGraphComputer(AsynchTypeGraphComputer computer) {
		this.computer = computer;
	}
	
	@RequestMapping(value = "/typegraph/{version:.*}", produces = {"text/xml; charset=UTF-8"})
	public void getTypeGraphMaybe(
			@PathVariable("version") String springBootVersion,
			HttpServletResponse resp
			) throws Exception {
		log.info("type graph request received for '"+springBootVersion+"'");
		Future<byte[]> result = computer.getTypeGraphResponseBody(springBootVersion);
		sendResponse(resp, result);
	}
	
	@RequestMapping(value = "/typegraph", produces = {"text/xml; charset=UTF-8"})
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

	@RequestMapping(value="/ulimit/{opt}", produces = {"text/plain; charset=UTF-8"})
	@ResponseBody
	public String ulimitDashOpt(@PathVariable("opt") String opt) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Process process = Runtime.getRuntime().exec(new String[] {
			"bash", "-c", "ulimit -"+opt	
		});
		process.waitFor();
		IOUtil.pipe(process.getInputStream(), out);
		IOUtil.pipe(process.getErrorStream(), out);
		return out.toString("UTF-8");
	}
	
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

	@RequestMapping(value="/maven/depMan", produces = {"application/json; charset=UTF-8"})
	@ResponseBody
	public List<JsonDependency> getManagedDependencies() throws Exception {
		MavenHelper mvn = new MavenHelper();
		String bootVersion = Defaults.defaultVersion;
		MavenProject mvnProject = mvn.readMavenProject(pomGenerator.getPomFile(bootVersion));
		
		return JsonDependency.from(mvnProject.getDependencyManagement().getDependencies());
	}

	/**
	 * Not really an 'end user' rest end point. But can be useful during debugging to
	 * see if pomGenerator returns what we'd expect.
	 */
	@RequestMapping(value="/pom/{version:.*}", produces = {"text/xml; charset=UTF-8"})
	@ResponseBody
	public String genPom(@PathVariable("version") String bootVersion) throws Exception {
		//TemplateEngine engine = pomGenerator.getEngine();
		return pomGenerator.getPom(bootVersion);
	}
	
}
