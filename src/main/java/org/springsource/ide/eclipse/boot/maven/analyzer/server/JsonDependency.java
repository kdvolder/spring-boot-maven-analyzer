package org.springsource.ide.eclipse.boot.maven.analyzer.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;

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
		this.group = d.getGroupId();
		this.artifact = d.getArtifactId();
		this.version = d.getVersion();
		this.type = d.getType();
		this.classifier = d.getClassifier();
		this.scope = d.getScope();
	}

	public static List<JsonDependency> from(List<Dependency> dependencies) {
		ArrayList<JsonDependency> result = new ArrayList<>(dependencies.size());
		for (Dependency d : dependencies) {
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
