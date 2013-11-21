package org.springsource.ide.eclipse.boot.maven.analyzer;

import org.sonatype.aether.artifact.Artifact;

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
