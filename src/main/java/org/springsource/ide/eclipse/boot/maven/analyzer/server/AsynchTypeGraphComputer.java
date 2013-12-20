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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.plexus.PlexusContainerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springsource.ide.eclipse.boot.maven.analyzer.BootDependencyAnalyzer;

/**
 * Computes data for typegraph http request asyncronously, returning a Future.
 * This is because computing the graph can take a very long time and we can't
 * let the request be hanging that long. Most like it will be terminated
 * by impatient clients or nginx. On CF it will even kill and restart the
 * entire if request don't get handled within a certain time.
 */
@Component
public class AsynchTypeGraphComputer {

	private ExecutorService executor = Executors.newSingleThreadExecutor(); //TODO: inject with spring?

	public AsynchTypeGraphComputer() {
	}

	@Autowired
	private CacheManager caches;
	
	@Cacheable("default")
	public synchronized Future<byte[]> getTypeGraphResponseBody(final String springBootVersion) {
		return executor.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws Exception {
				return computeTypeGraph(springBootVersion);
			}
		});
	}
	

	private byte[] computeTypeGraph(String springBootVersion) throws Exception, PlexusContainerException, IOException {
		int retries = 3; //Flakyness of maven seems to make this fail often when starting from empty maven cache.
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (--retries>0) {
			try {
				try {
					if (springBootVersion.contains("XXX")) {
						throw new Exception("Bad version: "+springBootVersion);
					}
					BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer();
					analyzer.setXmlOut(out);
					analyzer.setUseSpringProvidesInfo(true); 
					analyzer.run();
					return out.toByteArray();
				} finally {
					//System.out.println(out.toString());
					out.close();
				}
			} catch (Exception e) {
				if (retries>0) {
					//Is tempting to reset the buffer like so:
					// out.reset()
					//But it seems that leads to concurrency issues as some stuff is still writing into the buffer even after we
					// received this exception.
					//Reallocating the buffer avoids that problem.
					out = new ByteArrayOutputStream(); 
				} else {
					throw e;
				}
			}
		}
		return out.toByteArray();
	}
	
}
