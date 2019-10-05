/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;


import io.fabric8.openshift.client.OpenShiftClient;

public interface EnmasseOpenShiftClient extends OpenShiftClient {
    EnmasseOpenShiftAPIGroupDSL enmasse();
}
