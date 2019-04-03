/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.admin.model;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.Map;

public interface AddressPlan extends HasMetadata {
    String getShortDescription();
    String getAddressType();
    Map<String, Double> getResources();
    int getPartitions();
}
