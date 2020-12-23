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
package com.github.veithen.phos.enforcer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

@Mojo(name = "enforce", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnforceMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private File classesDir;

    @Parameter private String[] includes = new String[] {"**/*.class"};

    @Parameter private String ignore;

    @Parameter private LayeringRuleBuilder[] layers;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!classesDir.exists()) {
            return;
        }
        ReferenceCollectorSet referenceCollectors = new ReferenceCollectorSet();
        Set<Reference<Clazz>> ignoredClassReferences = new HashSet<>();
        if (ignore != null) {
            for (String ignoreRule : ignore.split(",")) {
                String[] s = ignoreRule.split("->");
                ignoredClassReferences.add(
                        new Reference<Clazz>(new Clazz(s[0].trim()), new Clazz(s[1].trim())));
            }
        }
        ReferenceFilter referenceCollector =
                new ReferenceFilter(referenceCollectors, ignoredClassReferences);

        PackageCycleDetector packageCycleDetector = new PackageCycleDetector();
        referenceCollectors.addReferenceCollector(packageCycleDetector);

        LayeringChecker layeringChecker;
        if (layers == null) {
            layeringChecker = null;
        } else {
            LayeringRule[] layeringRules = new LayeringRule[layers.length];
            for (int i = 0; i < layers.length; i++) {
                layeringRules[i] = layers[i].build();
            }
            layeringChecker = new LayeringChecker(layeringRules);
            referenceCollectors.addReferenceCollector(layeringChecker);
        }

        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes(includes);
        ds.setBasedir(classesDir);
        ds.scan();
        int classCount = 0;
        for (String relativePath : ds.getIncludedFiles()) {
            try {
                InputStream in = new FileInputStream(new File(classesDir, relativePath));
                try {
                    ClassProcessor.processDefinition(in, referenceCollector);
                } finally {
                    in.close();
                }
            } catch (IOException ex) {
                throw new MojoExecutionException(
                        "Failed to read " + relativePath + ": " + ex.getMessage(), ex);
            }
            classCount++;
        }
        getLog().info(String.format("Scanned %s classes", classCount));

        Set<Reference<Clazz>> references = packageCycleDetector.getClassReferencesForPackageCycle();
        if (references != null) {
            StringBuilder buffer = new StringBuilder("Package cycle detected. Classes involved:");
            for (Reference<Clazz> reference : references) {
                buffer.append("\n  ");
                buffer.append(reference.getFrom());
                buffer.append(" -> ");
                buffer.append(reference.getTo());
            }
            throw new MojoFailureException(buffer.toString());
        }

        if (layeringChecker != null) {
            Set<Reference<Clazz>> violatingReferences = layeringChecker.getViolatingReferences();
            if (!violatingReferences.isEmpty()) {
                StringBuilder buffer =
                        new StringBuilder("Layering violation detected. Classes involved:");
                for (Reference<Clazz> reference : violatingReferences) {
                    buffer.append("\n  ");
                    buffer.append(reference.getFrom());
                    buffer.append(" -> ");
                    buffer.append(reference.getTo());
                }
                throw new MojoFailureException(buffer.toString());
            }
        }

        Set<Reference<Clazz>> unusedIgnoredClassReferences =
                referenceCollector.getUnusedIgnoredClassReferences();
        if (!unusedIgnoredClassReferences.isEmpty()) {
            StringBuilder buffer = new StringBuilder("Found unused ignored class references:");
            for (Reference<Clazz> reference : unusedIgnoredClassReferences) {
                buffer.append("\n  ");
                buffer.append(reference.getFrom());
                buffer.append(" -> ");
                buffer.append(reference.getTo());
            }
            throw new MojoFailureException(buffer.toString());
        }
    }
}
