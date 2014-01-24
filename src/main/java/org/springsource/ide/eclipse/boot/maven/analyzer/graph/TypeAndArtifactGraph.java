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
package org.springsource.ide.eclipse.boot.maven.analyzer.graph;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 * A directed graph the nodes of which are types or artifacts. It is meant to lookup 
 * types and determine what artifact can provide them on the classpath. 
 */
public class TypeAndArtifactGraph extends DirectedGraph {

	public List<Artifact> getArtifacts() {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		for (Object node : getNodes()) {
			if (node instanceof ArtifactNode) {
				artifacts.add(new DefaultArtifact(((ArtifactNode)node).getCoords()));
			}
		}
		return artifacts;
	}

}
