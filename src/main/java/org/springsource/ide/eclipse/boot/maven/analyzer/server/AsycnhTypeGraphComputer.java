package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.plexus.PlexusContainerException;
import org.springsource.ide.eclipse.boot.maven.analyzer.BootDependencyAnalyzer;

/**
 * Computes data for typegraph http request asyncronously, returning a Future.
 */
public class AsycnhTypeGraphComputer {

	private ExecutorService executor = Executors.newSingleThreadExecutor(); //TODO: inject with spring?

	public AsycnhTypeGraphComputer() {
	}
	
	/**
	 * For now we don't yet have the ability to make the result specific to spring boot version
	 * Therefor only one result is returned and its always the same.
	 * <p>
	 * TODO: This should be generalized to some type LRU cache map to keep multiple results for some time.
	 */
	private Future<byte[]> result = null;

	public synchronized Future<byte[]> getTypeGraphResponseBody(final String springBootVersion) {
		if (result==null) {
			result = executor.submit(new Callable<byte[]>() {
				@Override
				public byte[] call() throws Exception {
					return computeTypeGraph(springBootVersion);
				}
			});
		}
		return result;
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
				} finally {
					out.close();
				}
			} catch (Exception e) {
				if (retries>0) {
					//will loop again
					out = new ByteArrayOutputStream();
				} else {
					//no more loops
					throw e;
				}
			}
		}
		return out.toByteArray();
	}
	

}
