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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Simple cache implementation that stores the result of some computations for a limited amount of time
 * (this is to ensure that cached values are periodically recomputed).
 * 
 * The cache uses Futures so that it can cache a computation result even before it is computed thereby
 * allowing request for a result that happen while it is still being computed to also benefit from
 * the cache (which would not be the case with a more naive implementation that only stores the
 * result of the computation upon its completetion).
 * 
 * @author Kris De Volder
 */
public abstract class SimpleCache<Key, Value> {
	
	//TODO: Limit memory footprint or number of entries in the cache
	
	private long timeToLive = 24*3600*1000; //1 day is the default amount of time that cache entries are kept before they are
											// considered expired.
	private ExecutorService executor;
	
	public SimpleCache(ExecutorService executor) {
		this.executor = executor;
	}
	
	public void setTimeToLive(long ttl) {
		this.timeToLive = ttl;
	}
	
	/**
	 * CacheEntry contains a future value of the computation and remembers when the
	 * entry was created.
	 */
	private class CacheEntry {
		Future<Value> future;
		long timeCreated;
		public CacheEntry(Callable<Value> task) {
			this.timeCreated = System.currentTimeMillis();
			this.future = executor.submit(task);
		}
		public boolean isExpired() {
			long age = System.currentTimeMillis()-timeCreated;
			return age>timeToLive;
		}
	}
	
	private Map<Key, CacheEntry> contents = new HashMap<Key, CacheEntry>();
	
	/**
	 * Override and implement this method to define the computation that is being cached.
	 * @throws Exception 
	 */
	protected abstract Value compute(Key key) throws Exception; 
	
	public synchronized Future<Value> get(final Key key) {
		CacheEntry entry = contents.get(key);
		if (entry==null || entry.isExpired()) {
			entry = new CacheEntry(new Callable<Value>() {
				public Value call() throws Exception {
					return compute(key);
				}
			});
			contents.put(key, entry);
		}
		return entry.future;
	}

}
