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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import com.github.veithen.phos.enforcer.factory.Factory;
import com.github.veithen.phos.enforcer.factory.Service;
import com.github.veithen.phos.enforcer.factory.impl1.ServiceImpl1;
import com.github.veithen.phos.enforcer.factory.impl2.ServiceImpl2;

public class PackageCycleDetectorTest {
    @Test
    public void test() {
        PackageCycleDetector packageCycleDetector = new PackageCycleDetector();
        ClassProcessor.processClass(Factory.class, packageCycleDetector);
        ClassProcessor.processClass(Service.class, packageCycleDetector);
        ClassProcessor.processClass(ServiceImpl1.class, packageCycleDetector);
        ClassProcessor.processClass(ServiceImpl2.class, packageCycleDetector);
        Set<Reference<Clazz>> result = packageCycleDetector.getClassReferencesForPackageCycle();
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }
}
