package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FileBrowser {


	@RequestMapping("/file/**")
	public void getFile(HttpServletRequest req, HttpServletResponse resp) throws Exception {
//		resp.setStatus(HttpServletResponse.SC_OK);
//		resp.setContentType("text/plain");
//		resp.getWriter().println("Contextpath: '"+req.getContextPath()+"'");
//		resp.getWriter().println("ServletPath: '"+req.getServletPath()+"'");
//		resp.getWriter().println("PathInfo   : '"+req.getPathInfo()+"'");
		
		String filePath = req.getServletPath().substring("/file".length());
//		System.out.println("Request for file: "+filePath);
		ServletUtils.sendFile(new File(filePath), resp);
	}
	
}
