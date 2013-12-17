package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.codehaus.plexus.PlexusContainerException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springsource.ide.eclipse.boot.maven.analyzer.BootDependencyAnalyzer;

@Controller
public class RestController {

	public RestController() {
		System.out.println("Creating RestController");
	}
	
	@RequestMapping(value = "/typegraph/{version}", produces = {"text/xml; charset=UTF-8"})
	@ResponseBody
	public byte[] getTypeGraph(@PathVariable("version") String springBootVersion) throws Exception {
//		return "<hello></hello>".getBytes();
		return computeTypeGraph(springBootVersion);
	}

	public byte[] computeTypeGraph(String springBootVersion) throws Exception, PlexusContainerException, IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			if (springBootVersion.contains("XXX")) {
				throw new Exception("Bad version: "+springBootVersion);
			}
			BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer();
			analyzer.setXmlOut(out);
			analyzer.setUseSpringProvidesInfo(true); 
			analyzer.run();
		} finally {
			out.close();
		}
		return out.toByteArray();
	}

}
