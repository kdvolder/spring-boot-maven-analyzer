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
import org.springsource.ide.eclipse.boot.maven.analyzer.JarTypeDiscovery;

public class ArtifactTypeDiscovery extends JarTypeDiscovery {

	private ArtifactTypeSource typeSource;

	public ArtifactTypeDiscovery(Artifact artifact) {
		super(artifact.getFile());
		this.typeSource = new ArtifactTypeSource(artifact);
	}

	@Override
	protected ExternalTypeSource getTypeSource() {
		return typeSource;
	}

}
