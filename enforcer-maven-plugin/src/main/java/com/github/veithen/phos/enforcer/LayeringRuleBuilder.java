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

public final class LayeringRuleBuilder {
    private String[] packages;
    private VisibilityRuleBuilder[] visibilityRuleBuilders;

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public VisibilityRuleBuilder[] getVisibilityRules() {
        return visibilityRuleBuilders;
    }

    public void setVisibilityRules(VisibilityRuleBuilder[] visibilityRules) {
        visibilityRuleBuilders = visibilityRules;
    }

    LayeringRule build() {
        VisibilityRule[] visibilityRules;
        if (visibilityRuleBuilders == null) {
            visibilityRules = new VisibilityRule[0];
        } else {
            visibilityRules = new VisibilityRule[visibilityRuleBuilders.length];
            for (int i = 0; i < visibilityRuleBuilders.length; i++) {
                visibilityRules[i] = visibilityRuleBuilders[i].build();
            }
        }
        return new LayeringRule(PackageMatcher.from(packages), visibilityRules);
    }
}
