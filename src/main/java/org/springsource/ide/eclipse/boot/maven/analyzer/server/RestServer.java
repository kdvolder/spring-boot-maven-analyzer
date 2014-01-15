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
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Application to serve serve jar-type content assist type graph
 * via a rest end point.
 */
@ComponentScan
@EnableAutoConfiguration
public class RestServer {

    public static void main(String[] args) {
        SpringApplication.run(RestServer.class, args);
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
