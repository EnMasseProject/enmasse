/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpaceQuota;
import io.enmasse.address.model.AddressSpaceQuotaList;

import java.util.Map;
import java.util.Optional;

public interface AddressSpaceQuotaApi {
    AddressSpaceQuotaList listAddressSpaceQuotasWithLabels(Map<String,String> labels);
    AddressSpaceQuotaList listAddressSpaceQuotas();
    Optional<AddressSpaceQuota> getAddressSpaceQuotaWithName(String addressSpaceQuotaName);
    void createAddressSpaceQuota(AddressSpaceQuota addressSpaceQuota);
    void deleteAddressSpace(AddressSpaceQuota addressSpaceQuota);
}
