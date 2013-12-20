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
package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ExternalTypeSource;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.AsmUtils;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExceptionUtil;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypeEntry;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExternalTypesDiscovery;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Logger;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

/**
 * Discover external types from a jar file.
 * 
 * @author Kris De Volder
 */
public abstract class JarTypeDiscovery implements ExternalTypesDiscovery {

	private static final boolean DEBUG = false; 
	
	static Log log = LogFactory.getLog(JarTypeDiscovery.class);
	
	private final File jarFile;
	
	public JarTypeDiscovery(File jarFile) {
		this.jarFile = jarFile; 
	}

	public void getTypes(Requestor<ExternalTypeEntry> requestor) {
		if (jarFile==null) {
			//can happen for unresolved or unresolveable artifacts.
			// In that case there's nothing to find. Move along!
			return;
		}
		ZipFile unzipper = null;
		try {
			unzipper = new ZipFile(jarFile);
			Enumeration<? extends ZipEntry> entries = unzipper.entries();
			boolean continu = true;
			while (entries.hasMoreElements() && continu) {
				ZipEntry e = entries.nextElement();
				String path = e.getName();
				//We are interested in class files that aren't inner or anonymous classes or classes in
				// the default package
				if (path.endsWith(".class") && !path.contains("$") && path.lastIndexOf('/')>1) {
					InputStream classFile = unzipper.getInputStream(e);
					try {
						if (AsmUtils.isPublic(classFile)) {
							//TODO: can optimize a little to do less string copying here.
							int beg = path.charAt(0)=='/'?1:0;
							String fqName = path.substring(beg, path.length()-6/*".class".length()*/);
							fqName = fqName.replace('/', '.');
							continu = requestor.receive(new ExternalTypeEntry(fqName, getTypeSource()));
						} else {
							if (DEBUG) {
								System.out.println("Filtered type (not public): "+e.getName());
							}
						}
					} catch (Exception e2) {
						String msg = ExceptionUtil.getMessage(e2);
						System.err.println("Error in jar file: "+jarFile+"\n"
								+ "   entry: "+path+"\n"
								+ "   msg  : "+msg);
						continu = false; //zip file corrupt? bail out here now.
					} finally {
						try {
							classFile.close();
						} catch (IOException ignore) {
							//ignore
						}
					}
				}
			}
		} catch (Exception e) {
			Logger.log(e);
		} finally {
			if (unzipper!=null) {
				try {
					unzipper.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}
	}

	protected abstract ExternalTypeSource getTypeSource();

	@Override
	public String toString() {
		return "JarTypeDiscovery["+jarFile+"]";
	}
	


}
