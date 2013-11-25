package org.springsource.ide.eclipse.boot.maven.analyzer.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.cli.ConsoleMavenTransferListener;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

/**
 * Maven APIs are a bit bewildering to me (at this point). This class is an attempt to provide some
 * simple operations that I need. 
 * 
 * Disclaimer: do not expect this to be a great API as I'm only cobling this together as I figure out how to use
 * maven apis. 
 *  
 * @author Kris De Volder
 */
public class MavenHelper {

	private PlexusContainer plexus;

	public MavenHelper() throws PlexusContainerException {
	    ContainerConfiguration mavenCoreCC = new DefaultContainerConfiguration().setClassWorld(
	        new ClassWorld("plexus.core", ClassWorld.class.getClassLoader())).setName("mavenCore"); //$NON-NLS-1$
	    mavenCoreCC.setAutoWiring(true);
		plexus =  new DefaultPlexusContainer(mavenCoreCC /*, new ExtensionModule()*/);
	}

	public MavenProject readMavenProject(File pomFile) throws Exception {
		MavenExecutionRequest request = createExecutionRequest();
		ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
		configuration.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
		configuration.setRepositorySession(createRepositorySession(request));
		ProjectBuildingResult result = lookup(ProjectBuilder.class).build(pomFile, configuration);
		if (isOk(result)) {
			return result.getProject();
		} else {
			throw problemException(result);
		}
	}

	/**
	 * Create something that can be thrown, and reflects errors that occurred in the
	 * build.
	 */
	private Exception problemException(ProjectBuildingResult result) {
		StringBuilder message = new StringBuilder();
		for (ModelProblem problem : result.getProblems()) {
			message.append(problem+"\n"); //TODO: is the 'toString' of ModelProblem a reasonable thing to turn into a error message?
		}
		return new RuntimeException(message.toString());
	}

	/**
	 * Checks whether result has errors. 
	 */
	private boolean isOk(ProjectBuildingResult result) {
		List<ModelProblem> problems = result.getProblems();
		if (problems==null || problems.isEmpty()) {
			return true;
		}
		for (ModelProblem problem : problems) {
			if (problem.getSeverity()==Severity.WARNING) {
				//ok
			} else {
				return false;
			}
		}
		//only found warnings
		return true;
	}

	private <C> C lookup(Class<C> role) throws ComponentLookupException {
		return plexus.lookup(role);
	}

	public MavenExecutionRequest createExecutionRequest() throws MavenExecutionRequestPopulationException, ComponentLookupException {
		MavenExecutionRequest request = new DefaultMavenExecutionRequest();
		request.setTransferListener(new SimpleTransferListener());
		//request.setTransferListener(new ConsoleMavenTransferListener(System.out)); //disabled too much noise!
		return lookup(MavenExecutionRequestPopulator.class).populateDefaults(request);
	}

	public DefaultRepositorySystemSession createRepositorySession(MavenExecutionRequest request) throws ComponentLookupException {
		DefaultRepositorySystemSession session = (DefaultRepositorySystemSession) ((DefaultMaven) lookup(Maven.class))
				.newRepositorySession(request);
		session.setIgnoreInvalidArtifactDescriptor(true);
		return session;
	}

	public RepositorySystem getRepositorySystem() throws ComponentLookupException {
		return lookup(RepositorySystem.class);
	}

	public ClassLoader getProjectRealm(MavenProject project) {
		//code from org.eclipse.m2e.core.internal.embedder.MavenImpl.getProjectRealm(MavenProject)
		ClassLoader classLoader = project.getClassRealm();
		if(classLoader == null) {
			classLoader = plexus.getContainerRealm();
		}
		return classLoader;
	}

	public void error(String msg, Throwable ex) {
		System.err.println("ERROR: "+msg);
		if (ex!=null) {
			ex.printStackTrace(System.err);
		}
	}


}
