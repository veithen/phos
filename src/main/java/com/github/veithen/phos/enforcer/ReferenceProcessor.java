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
package com.github.veithen.phos.enforcer;

import org.objectweb.asm.Type;

final class ReferenceProcessor {
    private final ReferenceCollector referenceCollector;
    private final Clazz origin;
    private final boolean isPublic;
    
    ReferenceProcessor(ReferenceCollector referenceCollector, Clazz origin, boolean isPublic) {
        this.referenceCollector = referenceCollector;
        this.origin = origin;
        this.isPublic = isPublic;
    }

    public void processType(Type type, boolean isPublic) {
        switch (type.getSort()) {
            case Type.OBJECT:
                referenceCollector.collectClassReference(new Reference<Clazz>(origin, new Clazz(type.getClassName())), this.isPublic && isPublic);
                break;
            case Type.ARRAY:
                processType(type.getElementType(), isPublic);
                break;
            case Type.METHOD:
                processType(type.getReturnType(), isPublic);
                for (Type argumentType : type.getArgumentTypes()) {
                    processType(argumentType, isPublic);
                }
        }
    }
}
