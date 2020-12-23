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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.SessionInfo;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

@Mojo(
        name = "aggregate-report",
        requiresDependencyResolution = ResolutionScope.TEST,
        defaultPhase = LifecyclePhase.SITE,
        threadSafe = true)
public class AggregateReportMojo extends AbstractMojo {
    private static final Pattern autoSessionIdPattern = Pattern.compile(".*-([0-9a-f]{1,8})");

    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "session", required = true, readonly = true)
    private MavenSession session;

    @Component private RepositorySystem repositorySystem;

    @Component private ArtifactResolver resolver;

    /**
     * The Jar archiver.
     *
     * @component role="org.codehaus.plexus.archiver.Archiver" roleHint="jar"
     * @required
     */
    @Component(hint = "jar")
    private UnArchiver jarUnArchiver;

    @Parameter(defaultValue = "${project.build.directory}/site", required = true, readonly = true)
    private File outputDirectory;

    private Set<Artifact> getArtifactsInScope(String scope) throws MojoExecutionException {
        FilterArtifacts filter = new FilterArtifacts();
        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(), true));
        filter.addFilter(new ScopeFilter(scope, null));
        filter.addFilter(new TypeFilter("jar", null));
        try {
            return filter.filter(project.getArtifacts());
        } catch (ArtifactFilterException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private File resolveArtifact(
            Artifact baseArtifact,
            String classifier,
            String type,
            boolean localOnly,
            boolean allowMissing)
            throws MojoExecutionException {
        ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
        if (localOnly) {
            projectBuildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);
            projectBuildingRequest.setRemoteRepositories(
                    Collections.<ArtifactRepository>emptyList());
        }
        Dependency dependency = new Dependency();
        dependency.setGroupId(baseArtifact.getGroupId());
        dependency.setArtifactId(baseArtifact.getArtifactId());
        dependency.setVersion(baseArtifact.getVersion());
        dependency.setType(type);
        dependency.setClassifier(classifier);
        dependency.setScope(Artifact.SCOPE_COMPILE);
        Artifact artifact = repositorySystem.createDependencyArtifact(dependency);
        try {
            return resolver.resolveArtifact(projectBuildingRequest, artifact)
                    .getArtifact()
                    .getFile();
        } catch (ArtifactResolverException ex) {
            if (allowMissing) {
                return null;
            } else {
                throw new MojoExecutionException("Unable to resolve artifact", ex);
            }
        }
    }

    static SessionInfo anonymize(SessionInfo sessionInfo) {
        Matcher matcher = autoSessionIdPattern.matcher(sessionInfo.getId());
        if (matcher.matches()) {
            return new SessionInfo(
                    matcher.group(1),
                    sessionInfo.getStartTimeStamp(),
                    sessionInfo.getDumpTimeStamp());
        } else {
            return sessionInfo;
        }
    }

    private static List<SessionInfo> anonymize(List<SessionInfo> sessionInfos) {
        List<SessionInfo> result = new ArrayList<>(sessionInfos.size());
        for (SessionInfo sessionInfo : sessionInfos) {
            result.add(anonymize(sessionInfo));
        }
        return result;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File sourcesDirectory = new File(project.getBuild().getDirectory(), "sources");
        sourcesDirectory.mkdirs();
        ExecFileLoader loader = new ExecFileLoader();
        for (Artifact baseArtifact : getArtifactsInScope(DefaultArtifact.SCOPE_TEST)) {
            File file = resolveArtifact(baseArtifact, "jacoco", "exec", true, true);
            if (file != null) {
                try {
                    loader.load(file);
                } catch (IOException ex) {
                    throw new MojoExecutionException(
                            String.format("Failed to load exec file %s: %s", file, ex.getMessage()),
                            ex);
                }
            }
        }
        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
        for (Artifact baseArtifact : getArtifactsInScope(DefaultArtifact.SCOPE_COMPILE)) {
            jarUnArchiver.setSourceFile(
                    resolveArtifact(baseArtifact, "sources", "jar", false, false));
            jarUnArchiver.setDestDirectory(sourcesDirectory);
            jarUnArchiver.extract();
            try {
                analyzer.analyzeAll(baseArtifact.getFile());
            } catch (IOException ex) {
                throw new MojoExecutionException(
                        String.format(
                                "Failed to analyze %s: %s",
                                baseArtifact.getFile(), ex.getMessage()),
                        ex);
            }
        }
        IBundleCoverage bundle = builder.getBundle("Coverage Report");
        HTMLFormatter htmlFormatter = new HTMLFormatter();
        htmlFormatter.setOutputEncoding("utf-8");
        htmlFormatter.setLocale(Locale.ENGLISH);
        try {
            IReportVisitor visitor =
                    htmlFormatter.createVisitor(new FileMultiReportOutput(outputDirectory));
            visitor.visitInfo(
                    anonymize(loader.getSessionInfoStore().getInfos()),
                    loader.getExecutionDataStore().getContents());
            visitor.visitBundle(
                    bundle, new DirectorySourceFileLocator(sourcesDirectory, "utf-8", 4));
            visitor.visitEnd();
        } catch (IOException ex) {
            throw new MojoExecutionException(
                    String.format("Failed to generate coverage report: %s", ex.getMessage()), ex);
        }
    }
}
