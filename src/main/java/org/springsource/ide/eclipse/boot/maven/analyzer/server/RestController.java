package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.File;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RestController {

	static Log log = LogFactory.getLog(RestController.class);
	
	public RestController() {
		System.out.println("Creating RestController");
		String userHome = System.getProperty("user.home");
		System.out.println("user.home="+userHome);
		if (userHome.contains("/vcap")) {
			//Darn you maven!
			File mvnCache = new File(userHome+"/.m2/repository");
			if (mvnCache.exists()) {
				System.out.println("Deleting maven cache.");
				FileUtils.deleteQuietly(mvnCache);
			} else {
				System.out.println("Maven cache is empty");
			}
		}
	}
	
	private AsycnhTypeGraphComputer computer = new AsycnhTypeGraphComputer();
	
//	@RequestMapping(value = "/typegraph/{version}", produces = {"text/xml; charset=UTF-8"})
//	@ResponseBody
//	public byte[] getTypeGraph(@PathVariable("version") String springBootVersion) throws Exception {
////		return "<hello></hello>".getBytes();
//		return computeTypeGraph(springBootVersion);
//	}
	
	@RequestMapping(value = "/typegraph/{version}", produces = {"text/xml; charset=UTF-8"})
	public void getTypeGraphMaybe(
			@PathVariable("version") String springBootVersion,
			HttpServletResponse resp
		) throws Exception {
			log.info("type grap request received");
			Future<byte[]> result = computer.getTypeGraphResponseBody(springBootVersion);
			if (!result.isDone()) {
				//Return something quickly to let client know we are working on their request but
				// it may be a while.
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
				resp.setContentType("text/plain");
				resp.setCharacterEncoding("utf8");
				resp.getWriter().println("I'm working on it... ask me again later");
			} else {
				resp.setStatus(HttpServletResponse.SC_ACCEPTED);
				resp.setContentType("text/xml");
				resp.setCharacterEncoding("utf8");
				resp.getOutputStream().write(result.get());
			}
	}
	

}
