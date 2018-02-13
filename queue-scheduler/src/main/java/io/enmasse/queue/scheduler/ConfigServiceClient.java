/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.ConfigMapAddressApi;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client connecting to the configuration service.
 */
public class ConfigServiceClient extends AbstractVerticle implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(ConfigServiceClient.class.getName());
    private final ConfigListener configListener;
    private final ConfigMapAddressApi addressApi;
    private volatile Watch watch;

    public ConfigServiceClient(ConfigListener configListener, KubernetesClient kubernetesClient, String namespace) {
        this.addressApi = new ConfigMapAddressApi(kubernetesClient, namespace);
        this.configListener = configListener;
    }

    @Override
    public void start() {
        try {
            this.watch = addressApi.watchAddresses(this, false);
        } catch (Exception e) {
            stop();
            vertx.setTimer(5000, id -> {
                start();
            });
        }
    }


    private Map<String, Set<Address>> groupByClusterId(Set<Address> addressList) {
        Map<String, Set<Address>> addressMap = new LinkedHashMap<>();
        for (Address address : addressList) {
            if (isQueue(address)) {
                String clusterId = getClusterIdForQueue(address);
                Set<Address> addresses = addressMap.computeIfAbsent(clusterId, k -> new HashSet<>());
                addresses.add(address);
            }
        }
        return addressMap;
    }

    private boolean isQueue(Address address) {
        return "queue".equals(address.getType());
    }


    // TODO: Put this constant somewhere appropriate
    private boolean isPooled(Address address) {
        return address.getPlan().startsWith("pooled");
    }

    //TODO: This logic is replicated from AddressController (and is also horrid and broken)
    private String getClusterIdForQueue(Address address) {
        if (isPooled(address)) {
            return address.getPlan();
        } else {
            return address.getName();
        }
    }

    @Override
    public void stop() {
        if (watch != null) {
            try {
                watch.close();
                watch = null;
            } catch (Exception e) {
                log.info("Error stopping watcher");
            }
        }
    }

    @Override
    public void resourcesUpdated(Set<Address> resources) throws Exception {
        Map<String, Set<Address>> addressConfig = groupByClusterId(resources);

        configListener.addressesChanged(addressConfig);
    }
}
