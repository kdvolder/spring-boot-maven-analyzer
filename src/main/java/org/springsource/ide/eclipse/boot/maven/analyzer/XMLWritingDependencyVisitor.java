package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Requestor;

public class XMLWritingDependencyVisitor implements DependencyVisitor {

	private Stack<Artifact> path = new Stack<Artifact>();
	private Set<Artifact> seenArtifacts = new HashSet<Artifact>();
	private XMLStreamWriter writer;
	
	private SpringProvidesInfo springProvides = new SpringProvidesInfo();
	
	private final boolean writeTypes = true;
	
	/**
	 * If not null fq names of any types written to the xml file are added to this collection.
	 */
	private final HashSet<String> knownTypes = new HashSet<String>();
	
	
	public XMLWritingDependencyVisitor(OutputStream out) throws XMLStreamException {
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		writer = factory.createXMLStreamWriter(out, "utf8");
		writer.writeStartDocument();
		writer.writeStartElement("document");
	}
	
	public XMLWritingDependencyVisitor(String file) throws Exception {
		this(new FileOutputStream(new File(file)));
	}

	private Artifact getArtifact(DependencyNode node) {
		if (node!=null) {
			org.sonatype.aether.graph.Dependency dep = node.getDependency();
			if (dep!=null) {
				return dep.getArtifact();
			}
		}
		return null;
	}

	public void close() {
		try {
			if (writer!=null) {
				indent();
				writer.writeEndElement();
				writer.writeEndDocument();
				writer.flush();
				writer.close();
			}
		} catch (Exception e) {
			throw new Error(e);
		} finally {
			writer = null;
		}
	}
	
	@Override
	public boolean visitEnter(DependencyNode node) {
		try {
			Artifact artifact = getArtifact(node);
			path.push(artifact);
			if (artifact==null) {
				//nothing interesting in this node. It's the children we care about
				return true;
			} else {
				boolean visitChildren = false;
				if (isInterestingArtifact(artifact)) {
					indent();
					String aid = artifact.getArtifactId();
					String gid = artifact.getGroupId();
					String version = artifact.getBaseVersion();
					String artifactStr = gid+":"+aid+":"+version;
					System.out.println(artifact);
					writer.writeStartElement("artifact");
					writer.writeAttribute("id", artifactStr);
					
					if (seenArtifacts.add(artifact)) {
						visitChildren = true;
						if (writeTypes) {
							visitTypes(artifact);
						}
						//TODO: It would be better if we somehow transform the graph based on this info
						// rather than just dmp it in the XML file. But this is harder (e.g. may require multiple 
						// passes, or building an in-memory representation of the graph to transform.
						//The benefit of transforming the graph is that the graph will be smaller (i.e
						// the provides info can be used to remove
						String info = SpringProvidesInfo.getProvidesInfo(artifact);
						if (info!=null) {
							writer.writeEmptyElement("provides");
							writer.writeAttribute("aids", info);
						}
					}
				}
				
				return visitChildren;
			}
		} catch (Exception e) {
			//Can't throw checked exceptions because the interface we implement won't let us.
			throw new Error(e);
		}
	}

	private boolean isInterestingArtifact(Artifact artifact) {
		//For now only deal with artifacts that look like '${group}:${aid}:jar:${version}'
		// other more irregular stuff might be test dependencies sources jars etc.
		// let's keep it simple and leave all of those out for now
		
		String artifactStr = artifact.toString();
		String[] pieces = artifactStr.split(":");
		if (pieces.length==4) {
			return pieces[2].equals("jar");
		}
		return false;
	}

	private void indent() throws XMLStreamException {
		writer.writeCharacters("\n");
		int level = path.size();
		for (int i = 0; i < level; i++) {
			writer.writeCharacters("\t");
		}
	}

	private void visitTypes(Artifact artifact) {
		ArtifactTypeDiscovery discoverer = new ArtifactTypeDiscovery(artifact);
		discoverer.getTypes(new Requestor<ExternalTypeEntry>() {
			public boolean receive(ExternalTypeEntry element) {
				try {
					indent();
					writer.writeCharacters("\t"); //types don't go onto the path stack so need one extra indent.
					writer.writeEmptyElement("type");
					String fullyQualifiedName = element.getType().getFullyQualifiedName();
					writer.writeAttribute("id", fullyQualifiedName);
					if (knownTypes!=null) {
						knownTypes.add(fullyQualifiedName);
					}
					return true; // Yes, I want more.
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		});
	}

	@Override
	public boolean visitLeave(DependencyNode node) {
		try {
			Artifact artifact = path.peek();
			//Careful if we aren't currently in an 'interesting artifact' node we shouldn't 'end' the element 
			// since none was started.
			if (artifact!=null && isInterestingArtifact(artifact)) {
				indent();
				writer.writeEndElement();
			}
			path.pop();
			return true;
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	public Collection<String> getKnownTypes() {
		return knownTypes;
	}

}
