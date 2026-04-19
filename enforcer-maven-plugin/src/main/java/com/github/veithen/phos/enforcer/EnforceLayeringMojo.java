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
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "enforce-layering", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnforceLayeringMojo extends AbstractEnforcerMojo {
    @Parameter(required = true)
    private LayeringRuleBuilder[] layers;

    private LayeringChecker layeringChecker;

    @Override
    void addReferenceCollectors(ReferenceCollectorSet collectors) {
        LayeringRule[] layeringRules = new LayeringRule[layers.length];
        for (int i = 0; i < layers.length; i++) {
            layeringRules[i] = layers[i].build();
        }
        layeringChecker = new LayeringChecker(layeringRules);
        collectors.addReferenceCollector(layeringChecker);
    }

    @Override
    void checkResults() throws MojoFailureException {
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
}
