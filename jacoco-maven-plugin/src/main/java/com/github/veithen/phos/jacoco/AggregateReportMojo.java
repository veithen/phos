/*-
 * #%L
 * Phos
 * %%
 * Copyright (C) 2016 - 2018 Andreas Veithen
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
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.codehaus.plexus.archiver.UnArchiver;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

@Mojo(name="aggregate-report", requiresDependencyResolution=ResolutionScope.COMPILE,
      defaultPhase=LifecyclePhase.SITE, threadSafe=true)
public class AggregateReportMojo extends AbstractMojo {
    @Parameter(property="project", required=true, readonly=true)
    private MavenProject project;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @required
     */
    @Component(hint="jar")
    private UnArchiver jarUnArchiver;

    @Parameter(defaultValue="${project.build.directory}/site", required=true, readonly=true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File sourcesDirectory = new File(project.getBuild().getDirectory(), "sources");
        sourcesDirectory.mkdirs();
        FilterArtifacts filter = new FilterArtifacts();
        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(), true));
        filter.addFilter(new ScopeFilter(DefaultArtifact.SCOPE_COMPILE, null));
        Set<Artifact> artifacts;
        try {
            artifacts = filter.filter(project.getArtifacts());
        } catch (ArtifactFilterException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
        ExecFileLoader loader = new ExecFileLoader();
        for (Artifact artifact : artifacts) {
            if ("jacoco".equals(artifact.getClassifier()) && artifact.getType().equals("exec")) {
                try {
                    loader.load(artifact.getFile());
                } catch (IOException ex) {
                    throw new MojoExecutionException(String.format("Failed to load exec file %s: %s", artifact.getFile(), ex.getMessage()), ex);
                }
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (Artifact artifact : artifacts) {
            if ("sources".equals(artifact.getClassifier()) && artifact.getType().equals("jar")) {
                jarUnArchiver.setSourceFile(artifact.getFile());
                jarUnArchiver.setDestDirectory(sourcesDirectory);
                jarUnArchiver.extract();
            } else if (artifact.getClassifier() == null && artifact.getType().equals("jar")) {
                try {
                    analyzer.analyzeAll(artifact.getFile());
                } catch (IOException ex) {
                    throw new MojoExecutionException(String.format("Failed to analyze %s: %s", artifact.getFile(), ex.getMessage()), ex);
                }
            }
        }
        IBundleCoverage bundle = builder.getBundle("aggregated");
        HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding("utf-8");
        htmlFormatter.setLocale(Locale.ENGLISH);
        try {
            IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(outputDirectory));
            visitor.visitInfo(
                    loader.getSessionInfoStore().getInfos(),
                    loader.getExecutionDataStore().getContents());
            visitor.visitBundle(bundle, new DirectorySourceFileLocator(sourcesDirectory, "utf-8", 4));
        } catch (IOException ex) {
            throw new MojoExecutionException(String.format("Failed to generate coverage report: %s", ex.getMessage()), ex);
        }
    }
}
