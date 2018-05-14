/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.fabric8.kubernetes.api.model.Secret;

public interface CertProvider {
    Secret provideCert(AddressSpace addressSpace, Endpoint endpoint);
}
