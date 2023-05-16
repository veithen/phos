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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

final class PackageCycleDetector extends ReferenceCollector {
    @SuppressWarnings("serial")
    private class Edge extends DefaultEdge {
        Reference<Clazz> classReferenceSample;
    }

    private final Graph<Package, Edge> packageGraph;

    PackageCycleDetector() {
        packageGraph = new DefaultDirectedGraph<>(null, Edge::new, false);
    }

    @Override
    void collectClassReference(Reference<Clazz> classReference, boolean isPublic) {
        Package fromPackage = classReference.getFrom().getPackage();
        Package toPackage = classReference.getTo().getPackage();
        if (!fromPackage.equals(toPackage)) {
            packageGraph.addVertex(fromPackage);
            packageGraph.addVertex(toPackage);
            Edge edge = packageGraph.addEdge(fromPackage, toPackage);
            if (edge != null) {
                edge.classReferenceSample = classReference;
            }
        }
    }

    Set<Reference<Clazz>> getClassReferencesForPackageCycle() {
        List<Graph<Package, Edge>> cycles =
                new KosarajuStrongConnectivityInspector<>(packageGraph)
                        .getStronglyConnectedComponents();
        for (Graph<Package, Edge> cycle : cycles) {
            if (cycle.vertexSet().size() > 1) {
                Set<Reference<Clazz>> classReferences = new HashSet<>();
                for (Edge edge : cycle.edgeSet()) {
                    classReferences.add(edge.classReferenceSample);
                }
                return classReferences;
            }
        }
        return null;
    }
}
