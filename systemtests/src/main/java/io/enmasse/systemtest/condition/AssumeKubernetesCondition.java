/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import io.enmasse.systemtest.platform.cluster.ClusterType;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class AssumeKubernetesCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<Kubernetes> annotation = findAnnotation(context.getElement(), Kubernetes.class);
        if (annotation.isPresent()) {
            ClusterType cluster = annotation.get().type();
            if (!io.enmasse.systemtest.platform.Kubernetes.getInstance().getCluster().toString().equals(cluster.toString().toLowerCase())) {
                return ConditionEvaluationResult.disabled("Test is not supported on current cluster");
            } else {
                return ConditionEvaluationResult.enabled("Test is supported on current cluster");
            }
        }
        return ConditionEvaluationResult.enabled("No rule set, test is enabled");
    }
}
