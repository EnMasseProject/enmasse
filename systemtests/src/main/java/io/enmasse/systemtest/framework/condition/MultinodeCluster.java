/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework.condition;

import java.util.function.Predicate;
import java.util.stream.Stream;

public enum MultinodeCluster {
    YES(v -> v > 1),
    NO(v -> v == 0),
    WHATEVER(v -> true);

    private final Predicate<Integer> nodeChecker;

    MultinodeCluster(Predicate<Integer> nodeChecker) {
        this.nodeChecker = nodeChecker;
    }

    public static MultinodeCluster isMultinode(int nodeCount) {
        return Stream.of(MultinodeCluster.values())
                .filter(v -> v.nodeChecker.test(nodeCount))
                .findFirst()
                .orElse(NO);
    }
}