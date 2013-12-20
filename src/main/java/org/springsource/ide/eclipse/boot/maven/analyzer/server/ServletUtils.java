package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.File;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springsource.ide.eclipse.boot.maven.analyzer.util.IOUtil;

public class ServletUtils {

	public static void sendFile(File file, HttpServletResponse resp) throws Exception {
		if (file.isFile()) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.addHeader("Content-Disposition", "attachment; filename="+file.getName());
			resp.setContentType("application/octet-stream");
			resp.setContentLength((int)file.length());
			ServletOutputStream out = resp.getOutputStream();
			IOUtil.pipe(file, out);
			out.close();
		} else if (file.isDirectory()) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("text/html");
			writeDirectoryListing(file, resp.getWriter());
		} else {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			resp.setContentType("test/plain");
			resp.getWriter().write("404 - not found");
		}
	}

	private static void writeDirectoryListing(File dir, PrintWriter writer) {
		//This code is bad on so many levels. 
		// I think we should be using spring mvc and thymeleaf rather than this cruft... but...
		writer.println("<!DOCTYPE html>\n" + 
				"<html>\n" + 
				"<body>");
		
		String path = dir.getPath();
		writer.println("<h1>"+path+"</h1>\n");
		
		writer.println("<ul>\n");
		String[] names = dir.list();
		
		for (String name : names) {
			writer.print("<li>");
			//TODO: properly encode url below?
			writer.print("<a href=\"/file"+dir+"/"+name+"\">"+name+"</a>");
			writer.println("</li>");
		}
		
		writer.println("</ul>\n");
		
		writer.println("</body>\n" + 
				"</html>");
	}
}