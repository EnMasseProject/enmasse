/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import io.enmasse.systemtest.platform.Kubernetes;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

public class AssumeOpenshiftVersionCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<AssumeOpenshiftVersion> annotation = findAnnotation(context.getElement(), AssumeOpenshiftVersion.class);
        if (annotation.isPresent()) {
            int version = annotation.get().version();
            if (Kubernetes.getInstance().getOcpVersion() != version) {
                return ConditionEvaluationResult.disabled("Test is not supported on current cluster");
            } else {
                return ConditionEvaluationResult.enabled("Test is supported on current cluster");
            }
        }
        return ConditionEvaluationResult.enabled("No rule set, test is enabled");
    }
}
