package org.springsource.ide.eclipse.boot.maven.analyzer.typediscovery;

import org.sonatype.aether.artifact.Artifact;
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
