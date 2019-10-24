/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.amqp.RouterEntity;
import io.enmasse.amqp.RouterManagement;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class RouterStatusController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusController.class);

    private final Vertx vertx = Vertx.vertx();
    private final NamespacedKubernetesClient client;
    private final String namespace;
    private final Duration connectTimeout;
    private final Duration queryTimeout;

    RouterStatusController(NamespacedKubernetesClient client, String namespace, AddressSpaceControllerOptions options) {
        this.client = client;
        this.namespace = namespace;
        this.connectTimeout = options.getManagementConnectTimeout();
        this.queryTimeout = options.getManagementQueryTimeout();
    }

    public AddressSpace reconcile(AddressSpace addressSpace) throws Exception {

        InfraConfig infraConfig = InfraConfigs.parseCurrentInfraConfig(null, addressSpace);

        if (infraConfig instanceof StandardInfraConfig) {
            checkRouterStatus(addressSpace, node);
        }
        return addressSpace;
    }

    private static final RouterEntity node = new RouterEntity("org.apache.qpid.dispatch.router.node", "id");

    private void checkRouterStatus(AddressSpace addressSpace, RouterEntity ... entities) {
        String addressSpaceCaSecretName = KubeUtil.getAddressSpaceCaSecretName(addressSpace);
        Secret addressSpaceCa = client.secrets().inNamespace(namespace).withName(addressSpaceCaSecretName).get();
        if (addressSpaceCa == null) {
            log.warn("Unable to check router status, missing address space CA secret for {}!", addressSpace);
            return;
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] key = decoder.decode(addressSpaceCa.getData().get("tls.key"));
        byte[] cert = decoder.decode(addressSpaceCa.getData().get("tls.crt"));
        if (key == null) {
            log.warn("Unable to check router status, missing address space CA key for {}!", addressSpace);
            return;
        }

        if (cert == null) {
            log.warn("Unable to check router status, missing address space CA cert for {}!", addressSpace);
            return;
        }

        RouterManagement routerManagement = RouterManagement.withCerts(vertx, "address-space-controller", connectTimeout, queryTimeout, cert, cert, key);

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        Map<RouterEntity, Map<String, List<List>>> results = new HashMap<>();

        List<Pod> routerPods = client.pods().withLabel(LabelKeys.CAPABILITY, "router").withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems().stream()
                .filter(Readiness::isPodReady)
                .collect(Collectors.toList());

        for (Pod router : routerPods) {
            try {
                int port = 0;
                for (Container container : router.getSpec().getContainers()) {
                    if (container.getName().equals("router")) {
                        for (ContainerPort containerPort : container.getPorts()) {
                            if (containerPort.getName().equals("amqps-normal")) {
                                port = containerPort.getContainerPort();
                            }
                        }
                    }
                }

                if (port != 0) {
                    // Until the connector entity allows querying for the status, we have to list
                    // all connections and match with the connector host.
                    Map<RouterEntity, List<List>> response = routerManagement.query(router.getStatus().getPodIP(), port, entities);
                    for (RouterEntity entity : entities) {
                        Map<String, List<List>> entityResponse = results.computeIfAbsent(entity, e -> new HashMap<>());
                        entityResponse.put(router.getMetadata().getName(), response.get(entity));
                    }
                }
            } catch (Exception e) {
                log.info("Error requesting registered topics from {}. Ignoring", router.getMetadata().getName(), e);
            }
        }

        if (results.containsKey(node)) {
            Map<String, List<List>> response = results.get(node);
            checkRouterMesh(addressSpace, routerPods.stream().map(pod -> pod.getMetadata().getName()).collect(Collectors.toList()), response);
        }
    }

    private void checkRouterMesh(AddressSpace addressSpace, List<String> routerIds, Map<String, List<List>> response) {
        for (String routerId : routerIds) {
            List<List> routerResponse = response.get(routerId);
            if (routerResponse == null) {
                log.warn("No response received from router {}. Will not check mesh connectivity.", routerId);
                continue;
            }
            List<String> neighbours = filterOnAttribute(String.class, 0, routerResponse);
            log.info("Router {} has neighbours: {}", routerId, neighbours);
            if (!neighbours.containsAll(routerIds)) {
                Set<String> missing = new HashSet<>(routerIds);
                missing.removeAll(neighbours);
                String msg = String.format("Router %s is missing connection to %s", routerId, missing);
                log.warn(msg);
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage(msg);
            }
        }
    }


    private static <T> List<T> filterOnAttribute(Class<T> type, int attrNum, List<List> list) {
        List<T> filtered = new ArrayList<>();
        for (List entry : list) {
            T filteredValue = type.cast(entry.get(attrNum));
            if (filteredValue != null) {
                filtered.add(filteredValue);
            }
        }
        return filtered;
    }


    @Override
    public String toString() {
        return "RouterStatusController";
    }

    private static class ConnectionStatus {
        private boolean isFound = false;
        private boolean isConnected = false;
        private boolean isOpened = false;

        boolean isConnected() {
            return isConnected;
        }

        void setConnected(boolean connected) {
            isConnected = connected;
        }

        boolean isOpened() {
            return isOpened;
        }

        void setOpened(boolean opened) {
            isOpened = opened;
        }

        boolean isFound() {
            return isFound;
        }

        void setFound(boolean found) {
            isFound = found;
        }
    }
}
