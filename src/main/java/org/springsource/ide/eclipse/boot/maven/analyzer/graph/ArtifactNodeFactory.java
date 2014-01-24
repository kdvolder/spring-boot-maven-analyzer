/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.graph;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;

/**
 * An instance of this class is responsible for creating ArtifactNode instances
 * to use as nodes in a TypeDependencyGraph. The factory must ensure that
 * only one Artifact instance is created even if an artifact with the
 * same groupid:artifactid:version etc is encountered multiple times while
 * building the graph.
 * 
 * @author Kris De Volder
 */
public class ArtifactNodeFactory {

	private Map<String, ArtifactNode> instances = new HashMap<>();
	
	public ArtifactNode create(Artifact fromAether) {
		String descriptor = ArtifactIdUtils.toBaseId(fromAether);
		ArtifactNode node = instances.get(descriptor);
		if (node!=null) {
			return node;
		}
		node = new ArtifactNode(descriptor);
		instances.put(descriptor, node);
		return node;
	}
	
}
