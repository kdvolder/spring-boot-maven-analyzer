/*******************************************************************************
 * Copyright (c) 2014 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.VariablesMap;

@Component
public class PomGenerator {
	
	private TemplateEngine engine;

	/**
	 * Generated pom stored in a file. The files are cached. This is not so much
	 * because generating the pom is slow (it is not) but because maven apis we
	 * are using expect to read it from a file.
	 */
	private Map<String, File> cache = new HashMap<String, File>();

	public TemplateEngine getEngine() {
		return engine;
	}
	
	@Autowired
	public void setEngine(TemplateEngine engine) {
		this.engine = engine;
	}

	public String getPom(String bootVersion) {
		IContext context = new Context();
		VariablesMap<String, Object> vars = context.getVariables();
		vars.put("bootVersion", bootVersion);
		return engine.process("pom-template.xml", context);
	}

	public synchronized File getPomFile(String springBootVersion) throws Exception {
		File file = cache.get(springBootVersion);
		if (file!=null && file.isFile()) {
			return file;
		}
		//Cache miss. Generate and store.
		String contents = getPom(springBootVersion);
		if (contents!=null) {
			file = File.createTempFile("pom", ".xml");
			FileUtils.writeStringToFile(file, contents, "UTF8", false);
		}
		return file;
	}

}
