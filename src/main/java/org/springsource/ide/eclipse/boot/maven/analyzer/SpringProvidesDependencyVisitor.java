package org.springsource.ide.eclipse.boot.maven.analyzer;

import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

/**
 * Maven dependency visitor that gathers up info from spring.provides property files
 * that are contained within the spring boot starter jars:
 * 
 * Quote Dave Syer: "New snapshots of Spring Boot have starters with META-INF/spring.provides, 
 * a properties file in the form: provides: <comma-separated list of artifact ids>
 * 
 * @author Kris De Volder
 */
public class SpringProvidesDependencyVisitor implements DependencyVisitor {

	@Override
	public boolean visitEnter(DependencyNode node) {
		return false;
	}

	@Override
	public boolean visitLeave(DependencyNode node) {
		// TODO Auto-generated method stub
		return false;
	}

}
