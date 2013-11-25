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

import org.sonatype.aether.artifact.Artifact;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ArtifactVisitor;

/**
 * Maven dependency visitor that gathers up info from spring.provides property files
 * that are contained within the spring boot starter jars:
 * 
 * Quote Dave Syer: "New snapshots of Spring Boot have starters with META-INF/spring.provides, 
 * a properties file in the form: provides: <comma-separated list of artifact ids>
 * 
 * @author Kris De Volder
 */
public class SpringProvidesDependencyVisitor extends ArtifactVisitor {

	private SpringProvidesInfo infos;

	public SpringProvidesDependencyVisitor() {
		this.infos = new SpringProvidesInfo();
	}
	
	@Override
	public void visitArtifact(Artifact artifact) {
		infos.process(artifact);
	}

	public SpringProvidesInfo getInfo() {
		return infos;
	}

	
}
