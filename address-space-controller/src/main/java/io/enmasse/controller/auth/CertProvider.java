/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.auth;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.fabric8.kubernetes.api.model.Secret;

import java.util.Collection;
import java.util.Set;

public interface CertProvider {
    Secret provideCert(AddressSpace addressSpace, EndpointInfo endpointInfo);
}
