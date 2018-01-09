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

final class Package {
    static final Package DEFAULT = new Package(null);

    private final String name;

    private Package(String name) {
        this.name = name;
    }

    static Package byName(String name) {
        return name == null ? DEFAULT : new Package(name);
    }

    String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof Package && ((Package)obj).name.equals(name);
    }

    @Override
    public String toString() {
        return name == null ? "<default>" : name;
    }
}
