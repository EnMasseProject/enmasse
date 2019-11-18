/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.condition;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.enmasse.systemtest.platform.Kubernetes;

public class OLMRequiredCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Kubernetes kube = Kubernetes.getInstance();
        if(kube.getCRD("clusterserviceversions.operators.coreos.com") != null
                && kube.getCRD("subscriptions.operators.coreos.com") != null) {
            return ConditionEvaluationResult.enabled("OLM is installed in cluster");
        } else {
            return ConditionEvaluationResult.disabled("OLM is not installed in cluster");
        }
    }

}
