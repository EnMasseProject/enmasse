/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.v1.quota.AddressSpaceQuota;
import io.enmasse.address.model.v1.quota.AddressSpaceQuotaList;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConfigMapAddressSpaceQuotaApi implements AddressSpaceQuotaApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigMapAddressSpaceQuotaApi.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final NamespacedKubernetesClient client;

    private static String getConfigMapName(String quotaName) {
        return "address-space-quota." + quotaName;
    }

    public ConfigMapAddressSpaceQuotaApi(NamespacedKubernetesClient client) {
        this.client = client;
    }

    @Override
    public AddressSpaceQuotaList listAddressSpaceQuotasWithLabels(Map<String, String> labels) {
        Map<String, String> labelSet = new HashMap<>(labels);
        labelSet.put(LabelKeys.TYPE, "address-space-quota");
        return new AddressSpaceQuotaList(client.configMaps().withLabels(labelSet).list().getItems().stream()
                .map(this::decodeQuota)
                .collect(Collectors.toList()));
    }

    @Override
    public AddressSpaceQuotaList listAddressSpaceQuotas() {
        return listAddressSpaceQuotasWithLabels(Collections.emptyMap());
    }

    @Override
    public Optional<AddressSpaceQuota> getAddressSpaceQuotaWithName(String addressSpaceQuotaName) {
        return Optional.ofNullable(client.configMaps().withName(getConfigMapName(addressSpaceQuotaName)).get())
                .map(this::decodeQuota);
    }

    private AddressSpaceQuota decodeQuota(ConfigMap configMap) {
        try {
            return mapper.readValue(configMap.getData().get("definition"), AddressSpaceQuota.class);
        } catch (IOException e) {
            log.warn("Error reading AddressSpaceQuota", e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void createAddressSpaceQuota(AddressSpaceQuota addressSpaceQuota) {
        Map<String, String> labels = new HashMap<>();
        if (addressSpaceQuota.getMetadata().getLabels() != null) {
            labels.putAll(addressSpaceQuota.getMetadata().getLabels());
        }
        labels.put(LabelKeys.TYPE, "address-space-quota");
        try {
            client.configMaps().createNew()
                    .editOrNewMetadata()
                    .withName(getConfigMapName(addressSpaceQuota.getMetadata().getName()))
                    .addToLabels(labels)
                    .endMetadata()
                    .addToData("definition", mapper.writeValueAsString(addressSpaceQuota))
                    .done();
        } catch (IOException e) {
            log.warn("Error writing AddressSpaceQuota {}", addressSpaceQuota.getMetadata().getName(), e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void deleteAddressSpaceQuota(String addressSpaceQuotaName) {
        client.configMaps().withName(getConfigMapName(addressSpaceQuotaName)).delete();
    }
}
