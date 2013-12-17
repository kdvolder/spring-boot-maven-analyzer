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
package org.springsource.ide.eclipse.boot.maven.analyzer.graph;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.sonatype.aether.artifact.Artifact;
import org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery.ExternalType;

/**
 * Helper class to save the contents of a 'Types + Dependencies' graph onto an xml file.
 * 
 * @author Kris De Volder
 */
public class TypeDependencyGraphXmlWriter {
	
	private XMLStreamWriter writer;
	private DirectedGraph graph;
	
	private int level = 0; //for indentation of the output. More readable.
	
	private HashSet<Artifact> seenArtifacts = new HashSet<Artifact>(); //so we don't write out the same info twice.

	private void indent() throws XMLStreamException {
		writer.writeCharacters("\n");
		for (int i = 0; i < level; i++) {
			writer.writeCharacters("\t");
		}
	}
	
	public TypeDependencyGraphXmlWriter(OutputStream out, DirectedGraph graph) throws Exception {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		writer = factory.createXMLStreamWriter(out, "UTF-8");
		this.graph = graph;
		writer.writeStartDocument();
		writer.writeStartElement("document");
		
		Collection<Object> roots = graph.getRoots();
		try {
			for (Object root : roots) {
				writeNode(root);
			}
		} finally {
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.close();
		}
	}

	private void writeNode(Object node) throws Exception {
		level++;
		try {
			if (node instanceof Artifact) {
				Artifact a = ((Artifact) node);
				indent();
				writer.writeStartElement("artifact");
				writer.writeAttribute("id", a.getGroupId()+":"+a.getArtifactId()+":"+a.getBaseVersion());
				
				if (!isSeen(a)) {
					writeChildren(node);
				}
				indent();
				writer.writeEndElement();
			} else if (node instanceof ExternalType) {
				indent();
				writer.writeEmptyElement("type");
				writer.writeAttribute("id", ((ExternalType) node).getFullyQualifiedName());
			}
		} finally {
			level--;
		}
	}

	private boolean isSeen(Artifact a) {
		boolean isNew = seenArtifacts.add(a);
		return !isNew;
	}

	private void writeChildren(Object node) throws Exception {
		for (Object child : graph.getSuccessors(node)) {
			writeNode(child);
		}
	}
}
