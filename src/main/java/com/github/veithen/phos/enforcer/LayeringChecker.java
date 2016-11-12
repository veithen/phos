/*-
 * #%L
 * Phos
 * %%
 * Copyright (C) 2016 Andreas Veithen
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

import java.util.HashSet;
import java.util.Set;

final class LayeringChecker extends ReferenceCollector {
    private final LayeringRule[] layeringRules;
    private final Set<Reference<Clazz>> violatingReferences = new HashSet<>();

    LayeringChecker(LayeringRule[] layeringRules) {
        this.layeringRules = layeringRules;
    }

    @Override
    void collectClassReference(Reference<Clazz> classReference, boolean isPublic) {
        Reference<Package> packageReference = new Reference<Package>(classReference.getFrom().getPackage(), classReference.getTo().getPackage());
        for (LayeringRule layeringRule : layeringRules) {
            if (!layeringRule.isSatisfied(packageReference, isPublic)) {
                violatingReferences.add(classReference);
            }
        }
    }

    Set<Reference<Clazz>> getViolatingReferences() {
        return violatingReferences;
    }
}
