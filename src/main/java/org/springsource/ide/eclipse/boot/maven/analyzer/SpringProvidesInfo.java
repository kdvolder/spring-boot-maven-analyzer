package org.springsource.ide.eclipse.boot.maven.analyzer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.aether.artifact.Artifact;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.ArtifactNode;
import org.springsource.ide.eclipse.boot.maven.analyzer.graph.ArtifactNodeFactory;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.ExceptionUtil;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Logger;

public class SpringProvidesInfo {

	static final String SPRING_PROVIDES_PROPS_FILE_PATH = "META-INF/spring.provides";
	private Map<String, List<ArtifactNode>> providers = new HashMap<String, List<ArtifactNode>>();
	private ArtifactNodeFactory anf;
	
	public SpringProvidesInfo(ArtifactNodeFactory anf) {
		this.anf = anf;
	}
	
	/**
	 * Inverse lookup based on 'spring.provides' info contained in spring boot starter jars.
	 */
	public Collection<ArtifactNode> getPreferedProviders(String artifactId) {
		return providers.get(artifactId);
	}

	public static String getProvidesInfo(Artifact artifact) {
		File jarFile = artifact.getFile();
		try {
			if (jarFile!=null && jarFile.getName().toLowerCase().endsWith(".jar")) {
				ZipFile zipFile = new ZipFile(jarFile);
				try {
					ZipEntry entry = zipFile.getEntry(SpringProvidesInfo.SPRING_PROVIDES_PROPS_FILE_PATH);
					if (entry!=null && !entry.isDirectory()) {
						InputStream input = zipFile.getInputStream(entry);
						try {
							Properties props = new Properties();
							props.load(input);
							return props.getProperty("provides");
						} finally {
							input.close();
						}
					}
				} finally {
					zipFile.close();
				}
			}
		} catch (Exception e) {
			Logger.error("Bad zip ("+jarFile+")? " +ExceptionUtil.getMessage(e));
		}
		return null;
	}
	
	public void process(Artifact artifact) {
		try {
			String providedCommaSeparated = getProvidesInfo(artifact);
			if (providedCommaSeparated!=null) {
				String[] provideds = providedCommaSeparated.split(",");
				for (String provided : provideds) {
					addProvided(artifact, provided);
				}
			}
		} catch (Exception e) {
			Logger.log(e);
		}
	}
	
	private void addProvided(Artifact artifact, String provided) {
		List<ArtifactNode> list = providers.get(provided);
		if (list==null) {
			list = new ArrayList<ArtifactNode>();
			providers.put(provided, list);
		}
		list.add(anf.create(artifact));
	}
	
}
