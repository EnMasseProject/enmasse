/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.List;
import java.util.Map;

public interface AddressSpacePlan extends HasMetadata {
    Map<String, Double> getResourceLimits();
    List<String> getAddressPlans();
    String getShortDescription();
    String getDisplayName();
    int getDisplayOrder();
    String getAddressSpaceType();
    String getInfraConfigRef();
}
