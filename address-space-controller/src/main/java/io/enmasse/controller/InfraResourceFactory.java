/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.io.IOException;
import java.util.List;

public interface InfraResourceFactory {
    List<HasMetadata> createInfraResources(AddressSpace addressSpace, InfraConfig infraConfig);
}
