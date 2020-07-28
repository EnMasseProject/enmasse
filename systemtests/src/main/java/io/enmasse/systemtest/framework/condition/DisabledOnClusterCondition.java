/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.framework.condition;

import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.platform.cluster.ClusterType;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class DisabledOnClusterCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<DisabledOnCluster> annotation = findAnnotation(context.getElement(), DisabledOnCluster.class);
        if (annotation.isPresent()) {
            ClusterType type = annotation.get().type();
            if (Kubernetes.getInstance().getCluster().toString().toLowerCase().equals(type.toString().toLowerCase())) {
                return ConditionEvaluationResult.disabled("Test is not supported on current cluster");
            } else {
                return ConditionEvaluationResult.enabled("Test is supported on current cluster");
            }
        }
        return ConditionEvaluationResult.enabled("No rule set, test is enabled");
    }
}
