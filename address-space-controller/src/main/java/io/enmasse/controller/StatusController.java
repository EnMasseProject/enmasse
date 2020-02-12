/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import static io.enmasse.controller.InfraConfigs.parseCurrentInfraConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.enmasse.address.model.AddressSpaceSpecConnector;
import io.enmasse.address.model.AddressSpaceSpecConnectorEndpoint;
import io.enmasse.address.model.AddressSpaceStatusConnector;
import io.enmasse.address.model.AddressSpaceStatusRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.AddressSpaceSpec;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.address.model.ExposeType;
import io.enmasse.address.model.Phase;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.user.api.RealmApi;
import io.fabric8.kubernetes.api.model.HasMetadata;

public class StatusController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(StatusController.class.getName());
    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final AuthenticationServiceResolver authenticationServiceResolver;
    private final RealmApi realmApi;
    private final RouterStatusCache routerStatusCache;

    public StatusController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, AuthenticationServiceRegistry authenticationServiceRegistry, RealmApi realmApi, RouterStatusCache routerStatusCache) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.authenticationServiceResolver = new AuthenticationServiceResolver(authenticationServiceRegistry);
        this.realmApi = realmApi;
        this.routerStatusCache = routerStatusCache;
    }

    @Override
    public AddressSpace reconcileActive(AddressSpace addressSpace) throws IOException {
        if (addressSpace.getStatus().isReady()) {
            checkComponentsReady(addressSpace);
            checkRouterStatus(addressSpace);
            checkAuthServiceReady(addressSpace);
            checkExposedEndpoints(addressSpace);
        }

        if (addressSpace.getStatus().isReady()) {
            final AppliedConfig appliedConfig = AppliedConfig.parseCurrentAppliedConfig(addressSpace);
            final AddressSpaceSpec spec = AppliedConfig.normalize(AddressSpaceSpec.class, addressSpace.getSpec());

            if (spec.equals(appliedConfig.getAddressSpaceSpec())) {
                addressSpace.getStatus().setPhase(Phase.Active);
            } else if (log.isDebugEnabled()) {
                log.debug("Applied config does not match requested\nApplied  : {}\nRequested: {}", appliedConfig.getAddressSpaceSpec(), spec);
            }
        } else {
            if (Phase.Active.equals(addressSpace.getStatus().getPhase())) {
                addressSpace.getStatus().setPhase(Phase.Failed);
            }
        }
        return addressSpace;
    }

    private void checkExposedEndpoints(AddressSpace addressSpace) {
        Map<String, EndpointSpec> exposedEndpoints = new HashMap<>();

        if (addressSpace.getSpec() != null && addressSpace.getSpec().getEndpoints() != null) {
            for (EndpointSpec endpointSpec : addressSpace.getSpec().getEndpoints()) {
                if (endpointSpec != null
                        && endpointSpec.getExpose() != null
                        && endpointSpec.getExpose().getType() != null
                        && endpointSpec.getExpose().getType().equals(ExposeType.route)) {
                    exposedEndpoints.put(endpointSpec.getName(), endpointSpec);
                }
            }
        }

        if (addressSpace.getStatus().getEndpointStatuses() == null) {
            addressSpace.getStatus().setEndpointStatuses(new ArrayList<>());
        }

        for (EndpointStatus endpointStatus : addressSpace.getStatus().getEndpointStatuses()) {
            if (exposedEndpoints.containsKey(endpointStatus.getName())) {
                if (endpointStatus.getExternalHost() == null) {
                    String msg = String.format("Endpoint '%s' is not yet exposed", endpointStatus.getName());
                    addressSpace.getStatus().setReady(false);
                    addressSpace.getStatus().appendMessage(msg);
                }
            }
        }
    }

    private InfraConfig getInfraConfig(AddressSpace addressSpace) {
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        return addressSpaceResolver.getInfraConfig(addressSpace.getSpec().getType(), addressSpace.getSpec().getPlan());
    }

    private void checkComponentsReady(AddressSpace addressSpace) {
        try {
            InfraConfig infraConfig = Optional.ofNullable(parseCurrentInfraConfig(addressSpace)).orElseGet(() -> getInfraConfig(addressSpace));
            List<HasMetadata> requiredResources = infraResourceFactory.createInfraResources(addressSpace, infraConfig, authenticationServiceResolver.resolve(addressSpace));

            checkDeploymentsReady(addressSpace, requiredResources);
            checkStatefulSetsReady(addressSpace, requiredResources);
        } catch (Exception e) {
            String msg = String.format("Error checking for ready components: %s", e.getMessage());
            log.warn(msg, e);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage(msg);
        }
    }

    private void checkStatefulSetsReady(AddressSpace addressSpace, List<HasMetadata> requiredResources) {
        Set<String> readyStatefulSets = kubernetes.getReadyStatefulSets(addressSpace).stream()
                .map(statefulSet -> statefulSet.getMetadata().getName())
                .collect(Collectors.toSet());


        Set<String> requiredStatefulSets = requiredResources.stream()
                .filter(KubernetesHelper::isStatefulSet)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        boolean isReady = readyStatefulSets.containsAll(requiredStatefulSets);
        if (!isReady) {
            Set<String> missing = new HashSet<>(requiredStatefulSets);
            missing.removeAll(readyStatefulSets);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("The following stateful sets are not ready: " + missing);
        }
    }

    private void checkDeploymentsReady(AddressSpace addressSpace, List<HasMetadata> requiredResources) {
        Set<String> readyDeployments = kubernetes.getReadyDeployments(addressSpace).stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());


        Set<String> requiredDeployments = requiredResources.stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        boolean isReady = readyDeployments.containsAll(requiredDeployments);
        if (!isReady) {
            Set<String> missing = new HashSet<>(requiredDeployments);
            missing.removeAll(readyDeployments);
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("The following deployments are not ready: " + missing);
        }
    }

    private void checkAuthServiceReady(AddressSpace addressSpace) {
        AuthenticationService authenticationService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService()).orElse(null);
        if (authenticationService != null && AuthenticationServiceType.standard.equals(authenticationService.getSpec().getType())) {
            String realm = authenticationService.getSpec().getRealm();
            if (realm == null) {
                realm = addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
            }
            try {
                boolean isReady = realmApi.getRealmNames(authenticationService).contains(realm);
                if (!isReady) {
                    addressSpace.getStatus().setReady(false);
                    addressSpace.getStatus().appendMessage("Authentication service is not configured with realm " + addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
                }
            } catch (Exception e) {
                String msg = String.format("Error checking authentication service status: %s", e.getMessage());
                log.warn(msg);
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage(msg);
            }
        }
    }

    private void checkRouterStatus(AddressSpace addressSpace) throws IOException {
        InfraConfig infraConfig = InfraConfigs.parseCurrentInfraConfig(addressSpace);
        if (infraConfig instanceof StandardInfraConfig) {
            List<RouterStatus> routerStatusList = routerStatusCache.getLatestResult(addressSpace);
            if (routerStatusList == null) {
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage("No router status found.");
            } else {
                if (!addressSpace.getStatus().getConnectors().isEmpty()) {
                    checkRouterConnectorStatus(addressSpace, routerStatusList);
                } else {
                    checkRouterMesh(addressSpace, routerStatusList);
                }
            }
        }
    }

    void checkRouterConnectorStatus(AddressSpace addressSpace, List<RouterStatus> routerStatusList) {
        Map<String, AddressSpaceSpecConnector> connectorMap = new HashMap<>();
        for (AddressSpaceSpecConnector connector : addressSpace.getSpec().getConnectors()) {
            connectorMap.put(connector.getName(), connector);
        }

        for (AddressSpaceStatusConnector connector : addressSpace.getStatus().getConnectors()) {
            checkConnectorStatus(connector, connectorMap.get(connector.getName()), routerStatusList);
        }
    }

    /*
     * Until the connector entity allows querying for the status, we have to go through all connections and
     * see if we can find our connector host in there.
     */
    private void checkConnectorStatus(AddressSpaceStatusConnector connectorStatus, AddressSpaceSpecConnector connector, List<RouterStatus> response) {
        Map<String, ConnectionStatus> connectionStatuses = new HashMap<>();
        for (AddressSpaceSpecConnectorEndpoint endpoint : connector.getEndpointHosts()) {
            String host = String.format("%s:%d", endpoint.getHost(), connector.getPort(endpoint.getPort()));
            connectionStatuses.put(host, new ConnectionStatus());
        }

        for (RouterStatus routerStatus : response) {
            List<String> hosts = routerStatus.getConnections().getHosts();
            List<Boolean> opened = routerStatus.getConnections().getOpened();
            List<String> operStatus = routerStatus.getConnections().getOperStatus();

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

        // Assumption/decision: If the primary or failover for any connector is up, we are ok
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

    private void checkRouterMesh(AddressSpace addressSpace, List<RouterStatus> routerStatusList) {
        final List<AddressSpaceStatusRouter> routers = new ArrayList<>();
        Set<String> routerIds = routerStatusList.stream().map(RouterStatus::getRouterId).collect(Collectors.toSet());

        for (RouterStatus routerStatus : routerStatusList) {
            String routerId = routerStatus.getRouterId();
            List<String> neighbors = routerStatus.getNeighbors();

            if (!neighbors.containsAll(routerIds)) {
                Set<String> missing = new HashSet<>(routerIds);
                missing.removeAll(neighbors);
                String msg = String.format("Router %s is missing connection to %s.", routerId, missing);
                log.warn(msg);
                addressSpace.getStatus().setReady(false);
                addressSpace.getStatus().appendMessage(msg);
            }

            AddressSpaceStatusRouter addressSpaceStatusRouter = new AddressSpaceStatusRouter();
            addressSpaceStatusRouter.setId(routerId);
            addressSpaceStatusRouter.setNeighbors(neighbors);
            addressSpaceStatusRouter.setUndelivered(routerStatus.getUndelivered());

            log.debug("Router {} has neighbors: {} and undelivered: {}", routerId, neighbors, routerStatus.getUndelivered());
            routers.add(addressSpaceStatusRouter);
        }
        addressSpace.getStatus().setRouters(routers);
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

    @Override
    public String toString() {
        return "StatusController";
    }

}
