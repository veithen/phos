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
package com.github.veithen.phos.enforcer.factory;

import com.github.veithen.phos.enforcer.factory.impl1.ServiceImpl1;
import com.github.veithen.phos.enforcer.factory.impl2.ServiceImpl2;

public final class Factory {
    public static final Factory IMPL1 = new Factory(ServiceImpl1.class);
    public static final Factory IMPL2 = new Factory(ServiceImpl2.class);

    private Class<? extends Service> implClass;

    public Factory(Class<? extends Service> implClass) {
        this.implClass = implClass;
    }

    public Service createInstance() {
        try {
            return implClass.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
