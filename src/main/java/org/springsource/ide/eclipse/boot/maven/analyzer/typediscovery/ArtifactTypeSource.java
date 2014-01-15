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
package org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery;

import org.eclipse.aether.artifact.Artifact;

/**
 * An instance of this class represents a 'source' from which we can get
 * some external types. The main purpose of an instance is to define
 * how the type can be added to a project's classpath.
 * 
 * @author Kris De Volder
 */
public class ArtifactTypeSource implements ExternalTypeSource {
	
	private Artifact artifact;
	
	public ArtifactTypeSource(Artifact artifact) {
		super();
		this.artifact = artifact;
	}

	@Override
	public String toString() {
		return artifact.toString();
	}
		
}
