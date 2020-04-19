/*-
 * #%L
 * Phos
 * %%
 * Copyright (C) 2016 - 2020 Andreas Veithen
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.github.veithen.phos.jacoco;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name="install-data-file", defaultPhase=LifecyclePhase.INSTALL)
public class InstallDataFileMojo extends AbstractMojo {
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    @Parameter(property="localRepository", required=true, readonly=true)
    private ArtifactRepository localRepository;

    @Component
    private ArtifactFactory artifactFactory;

    @Component
    private ArtifactInstaller installer;

    @Parameter(defaultValue="${project.build.directory}/jacoco.exec", required=true)
    private File dataFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (dataFile.exists()) {
            Artifact artifact = artifactFactory.createArtifactWithClassifier(
                    project.getGroupId(), project.getArtifactId(), project.getVersion(), "exec", "jacoco");
            try {
                installer.install(dataFile, artifact, localRepository);
            } catch (ArtifactInstallationException ex) {
                throw new MojoExecutionException(
                        String.format("Error installing artifact '%s': %s",
                                      artifact.getDependencyConflictId(), ex.getMessage()),
                        ex);
            }
        }
    }
}
