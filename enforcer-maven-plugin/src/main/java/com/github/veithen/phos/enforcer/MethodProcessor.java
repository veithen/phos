/*-
 * #%L
 * Phos
 * %%
 * Copyright (C) 2016 - 2023 Andreas Veithen
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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

final class MethodProcessor extends MethodVisitor {
    private final ReferenceProcessor referenceProcessor;

    MethodProcessor(ReferenceProcessor referenceProcessor) {
        super(Opcodes.ASM9);
        this.referenceProcessor = referenceProcessor;
    }

    @Override
    public void visitLocalVariable(
            String name, String desc, String signature, Label start, Label end, int index) {
        referenceProcessor.processType(Type.getType(desc), false);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        referenceProcessor.processType(Type.getObjectType(type), false);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        referenceProcessor.processType(Type.getObjectType(owner), false);
        referenceProcessor.processType(Type.getType(desc), false);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        // AspectJ inter-method dispatch methods are defined on the aspect and the first argument
        // is "this". We need to ignore the reference to the aspect here.
        if (!name.startsWith("ajc$interMethodDispatch")) {
            referenceProcessor.processType(Type.getObjectType(owner), false);
        }
        referenceProcessor.processType(Type.getMethodType(desc), false);
    }

    @Override
    public void visitLdcInsn(Object value) {
        if (value instanceof Type) {
            referenceProcessor.processType((Type) value, false);
        }
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type != null) {
            referenceProcessor.processType(Type.getObjectType(type), false);
        }
    }
}
