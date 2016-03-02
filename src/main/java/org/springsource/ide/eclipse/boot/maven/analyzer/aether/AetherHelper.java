/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.boot.maven.analyzer.aether;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.springframework.stereotype.Component;
import org.springsource.ide.eclipse.boot.maven.analyzer.conf.Defaults;
import org.springsource.ide.eclipse.boot.maven.analyzer.server.AetherialController;
import org.springsource.ide.eclipse.boot.maven.analyzer.util.Assert;

/**
 * Using eclipse Aether to analuyze dependencies, resolve artifacts etc. We use this now instead
 * of trying to use Maven embedding which we have found to be difficult to use and unstable (unstability
 * is probably more due to the fact that its difficult to use and things most likely didn't get wired together
 * properly because of my own misunderstandings rather than the actual quality of maven).
 *
 * However, aether seems to do exactly what we need and has much clearer documentation on how
 * to setup and use properly).
 *
 * See: http://eclipse.org/aether/documentation/
 *
 * Especially the demo project is very helpful:
 * http://git.eclipse.org/c/aether/aether-demo.git/tree/
 *
 * @author Kris De Volder
 */
@Component
public class AetherHelper {

	static Log log = LogFactory.getLog(AetherialController.class);

	public RepositorySystem newRepositorySystem() {
		/*
		 * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
		 * prepopulated DefaultServiceLocator, we only need to register the repository connector and transporter
		 * factories.
		 */
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
		locator.addService( TransporterFactory.class, FileTransporterFactory.class );
		locator.addService( TransporterFactory.class, HttpTransporterFactory.class );

		locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler()
		{
			@Override
			public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception )
			{
				exception.printStackTrace();
			}
		} );

		return locator.getService( RepositorySystem.class );
	}

    public DefaultRepositorySystemSession newRepositorySystemSession( RepositorySystem system )
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		//session.set(resolutionErrorPolicy);

        LocalRepository localRepo = new LocalRepository(Defaults.localRepoPath);
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener() );

        // uncomment to generate dirty trees
        // session.setDependencyGraphTransformer( null );

        return session;
    }

    public List<RemoteRepository> newRepositories( RepositorySystem system, RepositorySystemSession session )
    {
        return addSpringRepos(new ArrayList<RemoteRepository>( Arrays.asList(
        		newCentralRepository()
        )));
    }

	private static RemoteRepository newCentralRepository()
    {
        return new RemoteRepository.Builder( "central", "default", "http://central.maven.org/maven2/" ).build();
    }

	//TODO: These details don't belong in here. Should be contributed via dependency injection somehow.
    private static List<RemoteRepository> addSpringRepos(ArrayList<RemoteRepository> repos) {
    	Builder builder = new RemoteRepository.Builder("spring-milestones", "default", "http://repo.springsource.org/milestone");
    	builder.setSnapshotPolicy(new RepositoryPolicy(
    			/*enabled*/ false,
    			RepositoryPolicy.UPDATE_POLICY_DAILY,
    			RepositoryPolicy.CHECKSUM_POLICY_IGNORE
    	));
    	repos.add(builder.build());
    	builder = new RemoteRepository.Builder("spring-snapshots", "default", "http://repo.springsource.org/snapshot");
    	builder.setSnapshotPolicy(new RepositoryPolicy(
    			/*enabled*/ true,
    			RepositoryPolicy.UPDATE_POLICY_DAILY,
    			RepositoryPolicy.CHECKSUM_POLICY_IGNORE
    	));
    	repos.add(builder.build());
    	return repos;
	}

	public List<Dependency> getManagedDependencies(Artifact artifact) throws Exception {
        RepositorySystem system = newRepositorySystem();
//
        DefaultRepositorySystemSession session = newRepositorySystemSession( system );

        session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, true );
        session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact( artifact );
        descriptorRequest.setRepositories( newRepositories( system, session ) );
        ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );

        return descriptorResult.getManagedDependencies();

//        collectRequest.setManagedDependencies( descriptorResult.getManagedDependencies() );
//        collectRequest.setRepositories( descriptorRequest.getRepositories() );
//
//        CollectResult collectResult = system.collectDependencies( session, collectRequest );
//
//        collectResult.getRoot().accept( new ConsoleDependencyGraphDumper() );
 	}


	/**
	 * Given a a 'parentPom' artifact constructs a dependency graph with all
	 * managed dependencies as roots.
	 * @return
	 */
	public CollectResult getManagedDependencyGraph(Artifact parentPom) throws Exception {
        RepositorySystem system = newRepositorySystem();
        DefaultRepositorySystemSession session = newRepositorySystemSession( system );

        session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, true );
        session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, true );

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact( parentPom );
        descriptorRequest.setRepositories( newRepositories( system, session ) );
        ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor( session, descriptorRequest );

        CollectRequest collectRequest = new CollectRequest();


        //collectRequest.setRootArtifact( descriptorResult.getArtifact() );
        collectRequest.setDependencies( descriptorResult.getManagedDependencies() );
        collectRequest.setManagedDependencies( descriptorResult.getManagedDependencies() );
        collectRequest.setRepositories( descriptorRequest.getRepositories() );

        CollectResult collectResult = system.collectDependencies( session, collectRequest );

        return collectResult;

	}

	public List<Artifact> resolve(List<Artifact> artifacts, boolean ignoreUnresolved) throws Exception {
        RepositorySystem system = newRepositorySystem();
        DefaultRepositorySystemSession session = newRepositorySystemSession( system );

		List<ArtifactRequest> requests = new ArrayList<ArtifactRequest>(artifacts.size());
        List<RemoteRepository> repos = newRepositories(system, session);
        for (Artifact artifact : artifacts) {
        	ArtifactRequest request = new ArtifactRequest();
        	request.setRepositories(repos);
        	request.setArtifact(artifact);
        	requests.add(request);
		}

		List<ArtifactResult> resolveResults = safeResolve(system, session, requests);
		ArrayList<Artifact> resolvedArtifacts = new ArrayList<Artifact>(resolveResults.size());
        for (ArtifactResult ra : resolveResults) {
        	if (ra.getArtifact()==null) {
        		Assert.isLegal(ignoreUnresolved);
        	} else {
        		resolvedArtifacts.add(ra.getArtifact());
        	}
		}
        return resolvedArtifacts;
	}

	private List<ArtifactResult> safeResolve(RepositorySystem system, DefaultRepositorySystemSession session,
			List<ArtifactRequest> requests) throws ArtifactResolutionException {
		try {
			return system.resolveArtifacts(session, requests);
		} catch (ArtifactResolutionException e) {
			return e.getResults();
		}
	}

	public void cleanLocalRepo() {
		File localRepo = new File(Defaults.localRepoPath);
		log.info("Cleaning local repo at '"+localRepo.getAbsolutePath()+"' ...");
		FileUtils.deleteQuietly(localRepo);
		log.info("DONE Cleaning local repo");
	}
}
