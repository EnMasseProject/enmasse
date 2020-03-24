/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.platform.Kubernetes;

/**
 * The different ways of installing the operator via OLM.
 */
public enum OLMInstallationType {

    /**
     * Used to install the operator in the olm namespace of the kubernetes-based cluster
     * @see Kubernetes#getOlmNamespace()
     */
    DEFAULT,

    /**
     * Used to install the operator in the normal infra namespace
     * @see Kubernetes#getInfraNamespace()
     */
    SPECIFIC

}
