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

        InfraConfig infraConfig = InfraConfigs.parseCurrentInfraConfig(addressSpace);

        if (infraConfig instanceof StandardInfraConfig) {
            if (!addressSpace.getStatus().getConnectorStatuses().isEmpty()) {
                checkConnectorStatuses(addressSpace);
            }
        }
        return addressSpace;
    }

    private static final RouterEntity connection = new RouterEntity( "org.apache.qpid.dispatch.connection", "operStatus", "opened", "host");

    private void checkConnectorStatuses(AddressSpace addressSpace) {
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
        List<Pod> routerPods = client.pods().withLabel(LabelKeys.CAPABILITY, "router").withLabel(LabelKeys.INFRA_UUID, infraUuid).list().getItems();
        Map<String, List<List>> results = new HashMap<>();

        for (Pod router : routerPods) {
            if (Readiness.isPodReady(router)) {
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
                        Map<RouterEntity, List<List>> response = routerManagement.query(router.getStatus().getPodIP(), port, connection);
                        results.put(router.getMetadata().getName(), response.get(connection));
                    }
                } catch (Exception e) {
                    log.info("Error requesting registered topics from {}. Ignoring", router.getMetadata().getName(), e);
                }
            }
        }

        Map<String, AddressSpaceSpecConnector> connectorMap = new HashMap<>();
        for (AddressSpaceSpecConnector connector : addressSpace.getSpec().getConnectors()) {
            connectorMap.put(connector.getName(), connector);
        }

        for (AddressSpaceStatusConnector connector : addressSpace.getStatus().getConnectorStatuses()) {
            checkConnectorStatus(connector, connectorMap.get(connector.getName()), results);
        }
    }

    /*
     * Until the connector entity allows querying for the status, we have to go through all connections and
     * see if we can find our connector host in there.
     */
    private void checkConnectorStatus(AddressSpaceStatusConnector connectorStatus, AddressSpaceSpecConnector connector, Map<String, List<List>> response) {
        int hostIdx = connection.getAttributeIndex("host");
        int openedIdx = connection.getAttributeIndex("opened");
        int operStatusIdx = connection.getAttributeIndex("operStatus");

        Map<String, ConnectionStatus> connectionStatuses = new HashMap<>();
        for (AddressSpaceSpecConnectorEndpoint endpoint : connector.getEndpointHosts()) {
            String host = String.format("%s:%d", endpoint.getHost(), connector.getPort(endpoint.getPort()));
            connectionStatuses.put(host, new ConnectionStatus());
        }

        for (Map.Entry<String, List<List>> entry : response.entrySet()) {
            List<String> hosts = filterOnAttribute(String.class, hostIdx, entry.getValue());
            List<Boolean> opened = filterOnAttribute(Boolean.class, openedIdx, entry.getValue());
            List<String> operStatus = filterOnAttribute(String.class, operStatusIdx, entry.getValue());

            for (int i = 0; i < hosts.size(); i++) {
                ConnectionStatus status = connectionStatuses.get(hosts.get(i));
                if (status != null) {
                    status.setFound(true);
                    if (operStatus.get(i).equals("up")) {
                        status.setConnected(true);
                    }
                    if (opened.get(i)) {
                        status.setOpened(true);
                    }
                }
            }
        }

        // If the primary of failover for any connector is up, we are ok
        List<ConnectionStatus> found = connectionStatuses.values().stream()
                .filter(ConnectionStatus::isFound)
                .collect(Collectors.toList());

        List<ConnectionStatus> isConnected = found.stream()
                .filter(ConnectionStatus::isConnected)
                .collect(Collectors.toList());

        List<ConnectionStatus> isOpened = isConnected.stream()
                .filter(ConnectionStatus::isOpened)
                .collect(Collectors.toList());

        if (found.isEmpty()) {
            connectorStatus.setReady(false);
            connectorStatus.appendMessage("Unable to find active connection for connector '" + connector.getName() + "'");
            return;
        }

        if (isConnected.isEmpty()) {
            connectorStatus.setReady(false);
            connectorStatus.appendMessage("Unable to find connection in the connected state for connector '" + connector.getName() + "'");
        }

        if (isOpened.isEmpty()) {
            connectorStatus.setReady(false);
            connectorStatus.appendMessage("Unable to find connection in the opened state for connector '" + connector.getName() + "'");
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
