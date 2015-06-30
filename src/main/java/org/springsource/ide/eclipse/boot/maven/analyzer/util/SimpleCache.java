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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple cache implementation that stores the result of some computations for a limited amount of time
 * (this is to ensure that cached values are periodically recomputed).
 *
 * The cache uses Futures so that it can cache a computation result even before it is computed thereby
 * allowing request for a result that happen while it is still being computed to also benefit from
 * the cache (which would not be the case with a more naive implementation that only stores the
 * result of the computation upon its completion).
 *
 * @author Kris De Volder
 */
public abstract class SimpleCache<Key, Value> {

	static Log log = LogFactory.getLog(SimpleCache.class);


	//TODO: Limit memory footprint or number of entries in the cache

	private long timeToLive = 24*3600*1000; //1 day is the default amount of time that cache entries are kept before they are
											// considered expired.
	private ExecutorService executor;

	public SimpleCache(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Sets the time after which entries are considered 'expired'. Note that entries will expire
	 * relative to their time of creation rather than the time of last use. This to ensure that
	 * all entries are periodically refreshed.
	 */
	public void setTimeToLive(long ttl) {
		this.timeToLive = ttl;
	}

	/**
	 * CacheEntry contains a future value of the computation and remembers when the
	 * entry was created.
	 */
	private class CacheEntry {
		/**
		 * Future representing value of the cache entry once computed.
		 */
		private Future<Value> future;
		/**
		 * Future for an old expired value. This can be returned as a
		 * 'best we can do' until actual value has been computed.
		 * This value may be null if there's no oldValue (i.e. we are computing
		 * something for the first time).
		 * It will also be set to null once the new value is computed.
		 */
		private Future<Value> oldFuture;

		long timeCreated;

		public CacheEntry(CacheEntry oldEntry, final Callable<Value> task) {
			this.timeCreated = System.currentTimeMillis();
			this.future = executor.submit(new Callable<Value>() {
				@Override
				public Value call() throws Exception {
					try {
						return task.call();
					} finally {
						executor.execute(new Runnable() {
							@Override
							public void run() {
								oldFuture = null;
							}
						});
					}
				}
			});
			if (oldEntry!=null) {
				oldFuture = oldEntry.future;
			}
		}
		public boolean isExpired() {
			long age = System.currentTimeMillis()-timeCreated;
			return age>timeToLive;
		}
		public String timeToLiveString() {
			long age = System.currentTimeMillis()-timeCreated;
			long timeLeft = Math.abs(timeToLive-age);
			if (timeLeft>60*1000) {
				return (timeLeft / 60 / 1000) + " minutes";
			} else {
				return timeLeft/1000+ " seconds";
			}
		}
		public Future<Value> getFuture() {
			if (!future.isDone() && oldFuture!=null) {
				//When the new value is not ready yet it is better to return the old expired value
				// than a "I'm busy" result.
				log.info("returning expired data");
				return oldFuture;
			}
			return future;
		}
	}

	private Map<Key, CacheEntry> contents = new HashMap<Key, CacheEntry>();

	/**
	 * Override and implement this method to define the computation that is being cached.
	 * @throws Exception
	 */
	protected abstract Value compute(Key key) throws Exception;

	public synchronized Future<Value> get(final Key key) {
		//TODO: we could do better here. When a cache entry is expired. right now
		// we will return new entry which has no data yet. It would be nicer to
		// keep on returning the old data for as long as the new data is not yet ready.

		CacheEntry entry = contents.get(key);
		if (entry==null || entry.isExpired()) {
			log.info("miss: "+key);
			entry = new CacheEntry(entry, new Callable<Value>() {
				public Value call() throws Exception {
					return compute(key);
				}
			});
			contents.put(key, entry);
		} else {
			log.info("hit: "+key+ " expires in: "+entry.timeToLiveString());
		}
		return entry.getFuture();
	}

	public synchronized void showState(PrintWriter out) {
		out.println("num entries: "+contents.size());
		out.println("ttl: "+TimeUnit.MILLISECONDS.toMinutes(timeToLive)+" minutes");
		out.println("------------------------------------------");
		TreeSet<Key> keys = new TreeSet<>(contents.keySet());
		int expired = 0;
		int alive = 0;
		for (Key k : keys) {
			out.println(k+":");
			CacheEntry entry = contents.get(k);
			Date date = new Date(entry.timeCreated);
			SimpleDateFormat df = new SimpleDateFormat("MMM dd, HH:mm:ss zzz");
			df.setTimeZone(TimeZone.getTimeZone("PST"));
			out.println("   created      : "+df.format(date));
			if (entry.isExpired()) {
				expired++;
                out.println("   EXPIRED for  : "+entry.timeToLiveString());
			} else {
				alive++;
				out.println("   time to live : "+entry.timeToLiveString());
				Future<Value> future = entry.getFuture();
				out.println("   status       : "+statusString(future));
			}
		}
		out.println("------------------------------------------");
		out.println("expired : "+expired);
		out.println("alive   : "+alive);
	}

	private String statusString(Future<Value> future) {
		if (future.isDone()) {
			try {
				future.get();
				return "READY";
			} catch (Exception e) {
				return "ERROR: "+e.getMessage();
			}
		} else {
			return "Computing ...";
		}
	}

}
