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
package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a place to write output to. This is essentially an output stream
 * but it tracks also whether the outputstream was opened by us (in which case
 * it is our responsibility to close it, or opened by a client, in which case
 * it is the client's responsibilty to close it
 * 
 * @author Kris De Volder
 */
public abstract class Outputter {

	static Log log = LogFactory.getLog(Outputter.class);
	
	/**
	 * Called when the outputter is no longer needed. This is a good time
	 * to close underlying stream if it is owned by this outputter.
	 * <p>
	 * The defaul implementation does nothing. Subclasses should 
	 * override as needed.
	 */
	public void dispose() {
	}
	
	/**
	 * Get the underlying output stream. This opens the stream as needed for
	 * those Outputters that 'own' their underlying stream. For outputters
	 * that don't own their stream, the stream is returned as is and it
	 * is assumed to be already opened by someone else.  
	 */
	public abstract OutputStream getOutputStream() throws Exception;

	/**
	 * Create an outputter that sends output to some output stream.
	 * The stream is assumed to be externally controlled so we are not
	 * responsible for closing it when we no longer need it.
	 */
	public static Outputter toStream(final OutputStream out) {
		return new Outputter() {
			@Override
			public OutputStream getOutputStream() {
				return out;
			}
		};
	}
	
	public static Outputter toFile(final File file) {
		return new Outputter() {
			
			private OutputStream stream = null;
			
			@Override
			public synchronized OutputStream getOutputStream() throws FileNotFoundException {
				if (stream==null) {
					stream = new FileOutputStream(file);
				}
				return stream;
			}
			
			@Override
			public synchronized void dispose() {
				if (stream!=null) {
					try {
						stream.close();
					} catch (Exception e) {
						//ignore
					}
					//Trying to close it once is enough.
					stream = null;
				}
			}
			
			@Override
			protected void finalize() throws Throwable {
				//Clients should call dispose before we get gc-ed. If this didn't happen then
				//we caught a resource leak, so make a loud noise about this.
				if (stream!=null) {
					//Note throwing exceptions here doesn't do anything. They are ignored by the JVM. 
					log.error("Resource leak detected, file not closed: "+stream);
				}
			}
		};
	}

}
