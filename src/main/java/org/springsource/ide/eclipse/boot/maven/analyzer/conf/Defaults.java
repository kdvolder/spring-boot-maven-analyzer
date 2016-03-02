package org.springsource.ide.eclipse.boot.maven.analyzer.conf;

import java.util.concurrent.TimeUnit;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class Defaults {

	/**
	 * Default spring boot version. Use this if spring boot version isn't specified in the
	 * request.
	 */
	public static final String defaultVersion = "1.3.3.RELEASE";
	public static final String localRepoPath = "target/local-repo";

	/**
	 * Time before entry in typegraph cache expires and needs to be refreshed.
	 * A good value is probably a day or so, so that the typegraphs are
	 * computed when they are a day old so they can pick up changes to boot snapshot
	 * releases once a day but don't get bogged down running doing maven
	 * resolution overly often.
	 */
	public static long cacheTTL = TimeUnit.DAYS.toMillis(1);

	/**
	 * Spring boot parent pom artifact. This will be used to determine managed
	 * dependencies for spring boot projects.
	 */
	public static Artifact parentPom(String bootVersion) {
		return new DefaultArtifact("org.springframework.boot", "spring-boot-starter-parent", "jar", bootVersion);
	}

	public static Artifact defaultParentPom = parentPom(defaultVersion);


//	/**
//	 * Default pom file to analyze.
//	 */
//	public static File pomFile() {
//		try {
//			return new File(Defaults.class.getClassLoader().getResource("pom.xml").toURI());
//		} catch (URISyntaxException e) {
//			throw new Error(e);
//		}
//	}

}
