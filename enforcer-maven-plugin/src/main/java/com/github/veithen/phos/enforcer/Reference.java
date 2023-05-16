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

final class Reference<T> {
    private final T from;
    private final T to;

    Reference(T from, T to) {
        this.from = from;
        this.to = to;
    }

    public T getFrom() {
        return from;
    }

    public T getTo() {
        return to;
    }

    @Override
    public int hashCode() {
        return 31 * from.hashCode() + to.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reference) {
            Reference<?> other = (Reference<?>) obj;
            return from.equals(other.from) && to.equals(other.to);
        } else {
            return false;
        }
    }
}
