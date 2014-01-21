package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;

/**
 * Contains more or less same info as {@link Dependency} but in a form where jackson mapper will
 * find it easy to map into json.
 *  
 * @author Kris De Volder
 */
public class JsonDependency {

	private String group;
	private String artifact;
	private String classifier;
	private String version;
	private String type;
	private String scope;

	public JsonDependency(Dependency d) {
		Artifact a = d.getArtifact();
		this.group = a.getGroupId();
		this.artifact = a.getArtifactId();
		this.version = a.getVersion();
		this.type = a.getExtension();
		this.classifier = a.getClassifier();
		this.scope = d.getScope();
	}

	public static List<JsonDependency> from(List<Dependency> managedDeps) {
		ArrayList<JsonDependency> result = new ArrayList<>(managedDeps.size());
		for (Dependency d : managedDeps) {
			result.add(JsonDependency.from(d));
		}
		return result;
	}

	public static JsonDependency from(Dependency d) {
		return new JsonDependency(d);
	}

	public String getGroup() {
		return group;
	}

	public String getArtifact() {
		return artifact;
	}

	public String getClassifier() {
		return classifier;
	}

	public String getVersion() {
		return version;
	}

	public String getType() {
		return type;
	}

	public String getScope() {
		return scope;
	}

	@Override
	public String toString() {
		return "Dependency [group=" + group + ", artifact=" + artifact
				+ ", classifier=" + classifier + ", version=" + version
				+ ", type=" + type + ", scope=" + scope + "]";
	}

	
	
}
