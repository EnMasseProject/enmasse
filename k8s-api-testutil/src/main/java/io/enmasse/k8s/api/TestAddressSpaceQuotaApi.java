/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.v1.quota.AddressSpaceQuota;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaList;

import java.util.*;

public class TestAddressSpaceQuotaApi implements AddressSpaceQuotaApi {
    Map<String, AddressSpaceQuota> quotaMap = new HashMap<>();
    public RuntimeException throwException;

    @Override
    public AddressSpaceQuotaList listAddressSpaceQuotasWithLabels(Map<String, String> labels) {
        if (throwException != null) {
            throw throwException;
        }
        List<AddressSpaceQuota> quotas = new ArrayList<>();
        for (AddressSpaceQuota quota : quotaMap.values()) {
            if (labels.equals(quota.getMetadata().getLabels())) {
                quotas.add(quota);
            }
        }
        return new AddressSpaceQuotaList(quotas);
    }

    @Override
    public AddressSpaceQuotaList listAddressSpaceQuotas() {
        if (throwException != null) {
            throw throwException;
        }
        return new AddressSpaceQuotaList(quotaMap.values());
    }

    @Override
    public Optional<AddressSpaceQuota> getAddressSpaceQuotaWithName(String addressSpaceQuotaName) {
        if (throwException != null) {
            throw throwException;
        }
        return Optional.ofNullable(quotaMap.get(addressSpaceQuotaName));
    }

    @Override
    public void createAddressSpaceQuota(AddressSpaceQuota addressSpaceQuota) {
        if (throwException != null) {
            throw throwException;
        }
        quotaMap.put(addressSpaceQuota.getMetadata().getName(), addressSpaceQuota);
    }

    @Override
    public void deleteAddressSpaceQuota(String addressSpaceQuotaName) {
        if (throwException != null) {
            throw throwException;
        }
        quotaMap.remove(addressSpaceQuotaName);
    }
}
