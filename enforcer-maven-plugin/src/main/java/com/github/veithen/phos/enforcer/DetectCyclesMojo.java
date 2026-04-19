/*-
 * #%L
 * Phos
 * %%
 * Copyright (C) 2016 - 2026 Andreas Veithen-Knowles
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

import java.util.Set;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "detect-cycles", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class DetectCyclesMojo extends AbstractEnforcerMojo {
    private PackageCycleDetector packageCycleDetector;

    @Override
    void addReferenceCollectors(ReferenceCollectorSet collectors) {
        packageCycleDetector = new PackageCycleDetector();
        collectors.addReferenceCollector(packageCycleDetector);
    }

    @Override
    void checkResults() throws MojoFailureException {
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
    }
}
