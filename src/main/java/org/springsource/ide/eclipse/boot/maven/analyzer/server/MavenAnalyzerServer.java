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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

/**
 * Spring Boot Application to serve serve jar-type content assist type graph
 * via a rest end point.
 */
@ComponentScan({"org.springsource.ide.eclipse.boot.maven.analyzer"})
@EnableAutoConfiguration
public class MavenAnalyzerServer {

    public static void main(String[] args) {
        SpringApplication.run(MavenAnalyzerServer.class, args);
    }

    @Bean
    public TemplateEngine templateEngine() {
    	return createDefaultTemplateEngine();
    }

	public static TemplateEngine createDefaultTemplateEngine() {
		TemplateEngine engine = new TemplateEngine();
    	ITemplateResolver resolver = new ClassLoaderTemplateResolver();
		engine.setTemplateResolver(resolver);
    	return engine;
	}
    
//    @Bean  
//    public CacheManager cacheManager() {
//    	SimpleCacheManager cm = new SimpleCacheManager();
//    	cm.setCaches(Arrays.asList(
//    			new ConcurrentMapCache("default")
//    	));
//    	return cm;
//    }    
}
