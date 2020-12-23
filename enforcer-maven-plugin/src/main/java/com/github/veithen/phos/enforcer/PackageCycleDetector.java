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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;

final class PackageCycleDetector extends ReferenceCollector {
    private final Graph<Package, Reference<Package>> packageGraph;
    private final Map<Reference<Package>, Reference<Clazz>> classReferenceSamples = new HashMap<>();

    PackageCycleDetector() {
        packageGraph =
                new DefaultDirectedGraph<>(
                        new EdgeFactory<Package, Reference<Package>>() {
                            @Override
                            public Reference<Package> createEdge(
                                    Package sourceVertex, Package targetVertex) {
                                return new Reference<Package>(sourceVertex, targetVertex);
                            }
                        });
    }

    @Override
    void collectClassReference(Reference<Clazz> classReference, boolean isPublic) {
        Package fromPackage = classReference.getFrom().getPackage();
        Package toPackage = classReference.getTo().getPackage();
        if (!fromPackage.equals(toPackage)) {
            packageGraph.addVertex(fromPackage);
            packageGraph.addVertex(toPackage);
            Reference<Package> packageReference = packageGraph.addEdge(fromPackage, toPackage);
            if (packageReference != null) {
                classReferenceSamples.put(packageReference, classReference);
            }
        }
    }

    Set<Reference<Clazz>> getClassReferencesForPackageCycle() {
        List<Graph<Package, Reference<Package>>> cycles =
                new KosarajuStrongConnectivityInspector<>(packageGraph)
                        .getStronglyConnectedComponents();
        for (Graph<Package, Reference<Package>> cycle : cycles) {
            if (cycle.vertexSet().size() > 1) {
                Set<Reference<Clazz>> classReferences = new HashSet<>();
                for (Reference<Package> packageReference : cycle.edgeSet()) {
                    classReferences.add(classReferenceSamples.get(packageReference));
                }
                return classReferences;
            }
        }
        return null;
    }
}
