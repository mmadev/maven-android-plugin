/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package com.jayway.maven.plugins.android.phase_prebuild;

import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.BuildHelper;
import com.jayway.maven.plugins.android.common.DependencyResolver;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystem;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Adds the classes from AAR dependencies to the project classpath.
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "AarMavenLifecycleListener" )
public final class AarMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant
{
    /**
     * The entry point to Aether, i.e. the component doing all the work.
     */
    @SuppressWarnings( "unused" )
    @Requirement
    private RepositorySystem repoSystem;

    @SuppressWarnings( "unused" )
    @Requirement
    private ArtifactHandler artifactHandler;

    @SuppressWarnings( "unused" )
    @Requirement
    private Logger log;

    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.debug( "" );
        log.debug( "AMLP afterProjectsRead" );
        log.debug( "AMLP afterProjectsRead" );
        log.debug( "AMLP afterProjectsRead" );
        log.debug( "" );

        log.debug( "CurrentProject=" + session.getCurrentProject() );
        final List<MavenProject> projects = session.getProjects();

        for ( MavenProject project : projects )
        {
            log.debug( "" );
            log.debug( "project=" + project.getArtifact() );

            final BuildHelper helper = new BuildHelper(
                repoSystem, session.getRepositorySession(),
                project,
                log
            );

            final Collection<Artifact> artifacts = getProjectsArtifacts( session, project );
            log.debug( "projects deps: : " + artifacts );
            for ( Artifact artifact : artifacts )
            {
                final String type = artifact.getType();
                if ( type.equals( AndroidExtension.AAR ) )
                {
                    addAarClassesToClasspath( helper, project, artifact );
                }
            }
        }
    }

    private Collection<Artifact> getProjectsArtifacts( MavenSession session, MavenProject project )
        throws MavenExecutionException
    {
        final DependencyResolver resolver = new DependencyResolver(
            repoSystem,
            session.getRepositorySession(),
            project.getRemoteProjectRepositories(),
            artifactHandler );

        try
        {
            return resolver.getDependenciesFor( project.getArtifact() );
        }
        catch ( MojoExecutionException e )
        {
            throw new MavenExecutionException( "Could not resolve dependencies for " + project.getArtifact(), e );
        }
    }

    /**
     * If Aar dependency then add the Aar classes to the project classpath.
     */
    private void addAarClassesToClasspath( BuildHelper helper, MavenProject project, Artifact artifact )
        throws MavenExecutionException
    {
        final String type = artifact.getType();
        if ( type.equals( AndroidExtension.AAR ) )
        {
            // Work out where the aar will be extracted and calculate the file path to the aar classes.
            log.debug( "Adding to classpath : " + artifact );

            // This is location where the GenerateSourcesMojo will extract the classes.
            final File aarClassesJar = helper.getUnpackedAarClassesJar( artifact );
            log.info( "                    : " + aarClassesJar );

            // In order to satisfy the LifecycleDependencyResolver on execution up to a phase that
            // has a Mojo requiring dependency resolution I need to create a dummy aarClassesJar here.
            aarClassesJar.getParentFile().mkdirs();
            try
            {
                aarClassesJar.createNewFile();
                log.debug( "Created dummy " + aarClassesJar.getName() + " exist=" + aarClassesJar.exists() );
            }
            catch ( IOException e )
            {
                throw new MavenExecutionException( "Could not add " + aarClassesJar.getName() + " as dependency", e );
            }

            // Add the Aar classes to the classpath
            final Dependency dependency = createSystemScopeDependency( artifact, aarClassesJar );
            project.getModel().addDependency( dependency );
        }
    }

    private Dependency createSystemScopeDependency( Artifact artifact, File location )
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setArtifactId( artifact.getArtifactId() );
        dependency.setVersion( artifact.getVersion() );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location.getAbsolutePath() );
        return dependency;
    }
}
  
