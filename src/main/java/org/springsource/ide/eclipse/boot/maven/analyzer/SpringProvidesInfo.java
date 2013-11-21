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

import org.sonatype.aether.artifact.Artifact;

public class SpringProvidesInfo {

	private static final String SPRING_PROVIDES_PROPS_FILE_PATH = "META-INF/spring.provides";
//	private Map<String, List<Artifact>> providers = new HashMap<String, List<Artifact>>();
//	
//	/**
//	 * Inverse lookup based on 'spring.provides' info contained in spring boot starter jars.
//	 */
//	public Collection<Artifact> getPreferedProviders(String artifactId) {
//		return providers.get(artifactId);
//	}
//	
//	public void process(Artifact artifact) {
//		try {
//			String providedCommaSeparated = getProvidesInfo(artifact);
//			if (providedCommaSeparated!=null) {
//				String[] provideds = providedCommaSeparated.split(",");
//				for (String provided : provideds) {
//					addProvided(artifact, provided);
//				}
//			}
//		} catch (Exception e) {
//			Logger.log(e);
//		}
//	}

	public static String getProvidesInfo(Artifact artifact) {
		try {
			File jarFile = artifact.getFile();
			if (jarFile!=null && jarFile.getName().toLowerCase().endsWith(".jar")) {
				ZipFile zipFile = new ZipFile(jarFile);
				try {
					ZipEntry entry = zipFile.getEntry(SPRING_PROVIDES_PROPS_FILE_PATH);
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
			Logger.log(e);
		}
		return null;
	}
	
//	private void addProvided(Artifact artifact, String provided) {
//		List<Artifact> list = providers.get(provided);
//		if (list==null) {
//			list = new ArrayList<Artifact>();
//			providers.put(provided, list);
//		}
//		list.add(artifact);
//	}
	
}
