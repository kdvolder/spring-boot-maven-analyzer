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

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springsource.ide.eclipse.boot.maven.analyzer.BootDependencyAnalyzer;
import org.springsource.ide.eclipse.boot.maven.analyzer.aether.AetherHelper;
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.SimpleCache;

/**
 * Computes data for typegraph http request asyncronously, returning a Future.
 * This is because computing the graph can take a very long time and we can't
 * let the request be hanging that long. Most like it will be terminated
 * by impatient clients or nginx. On CF it will even kill and restart the
 * entire instance if request is not handled within a certain time.
 */
@Component
public class AsynchTypeGraphComputer {

	static Log log = LogFactory.getLog(AsynchTypeGraphComputer.class);

	/**
	 * Use single thread executor because it is not safe to run multiple maven operations
	 * in parallel.
	 */
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private AetherHelper aether;
	
	private SimpleCache<String, byte[]> cache = new SimpleCache<String, byte[]>(executor) {
		@Override
		protected byte[] compute(String springBootVersion) throws Exception {
			log.info("Computing typegraph: '"+springBootVersion+"'");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				//TODO: do some proper validation of version string
//				if (springBootVersion.contains("XXX")) {
//					throw new Exception("Bad version: "+springBootVersion);
//				}
				BootDependencyAnalyzer analyzer = new BootDependencyAnalyzer(aether);
				analyzer.setXmlOut(out);
				analyzer.setBootVersion(springBootVersion);
				analyzer.setUseSpringProvidesInfo(true); 
				analyzer.run();
				return out.toByteArray();
			} finally {
				//System.out.println(out.toString());
				out.close();
			}
		}
	};
	
	public AsynchTypeGraphComputer() {
		cache.setTimeToLive(Defaults.cacheTTL);
	}
	
	@Autowired(required=true) 
	public void setAetherHelper(AetherHelper aether) {
		this.aether = aether;
	}
	
	public Future<byte[]> getTypeGraphResponseBody(final String springBootVersion) {
		return cache.get(springBootVersion);
	}
}
