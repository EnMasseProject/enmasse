/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceStatus;
import io.enmasse.address.model.AddressSpaceStatusConnector;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.EndpointStatus;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.address.model.Phase;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.time.SystemtestsOperation;
import io.enmasse.systemtest.time.TimeMeasuringSystem;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class AddressSpaceUtils {
    private static Logger log = CustomLogger.getLogger();

    private AddressSpaceUtils() {
        //utility class no need to instantiate it
    }

    public static void syncAddressSpaceObject(AddressSpace addressSpace) {
        AddressSpace data = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace())
                .withName(addressSpace.getMetadata().getName()).get();
        addressSpace.setMetadata(data.getMetadata());
        addressSpace.setSpec(data.getSpec());
        addressSpace.setStatus(data.getStatus());
    }

    public static JsonObject addressSpaceToJson(AddressSpace addressSpace) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(addressSpace));
    }

    public static String getAddressSpaceInfraUuid(AddressSpace addressSpace) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        if (infraUuid == null) {
            return KubeUtil.infraUuid(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
        }
        return infraUuid;
    }

    public static boolean existAddressSpace(String namespace, String addressSpaceName) {
        log.info("Following addressspaces are deployed {} in namespace {}", Kubernetes.getInstance().getAddressSpaceClient(namespace).list().getItems().stream()
                .map(addressSpace -> addressSpace.getMetadata().getName()).collect(Collectors.toList()), namespace);
        return Kubernetes.getInstance().getAddressSpaceClient(namespace).list().getItems().stream()
                .map(addressSpace -> addressSpace.getMetadata().getName()).collect(Collectors.toList()).contains(addressSpaceName);
    }

    public static BooleanSupplier addressSpaceExists(final String namespace, final String name) {
        return () -> existAddressSpace(namespace, name);
    }

    public static boolean isAddressSpaceReady(AddressSpace addressSpace) {
        return addressSpace != null && addressSpace.getStatus().isReady();
    }

    public static AddressSpace waitForAddressSpaceReady(AddressSpace addressSpace) throws Exception {
        return waitForAddressSpaceReady(addressSpace, new TimeoutBudget(15, TimeUnit.MINUTES));
    }

    public static AddressSpace waitForAddressSpaceReady(AddressSpace addressSpace, TimeoutBudget budget) throws Exception {
        boolean isReady = false;
        var client = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace());

        String name = addressSpace.getMetadata().getName();
        AddressSpace clientAddressSpace = addressSpace;
        while (budget.timeLeft() >= 0 && !isReady) {
            clientAddressSpace = client.withName(name).get();
            isReady = isAddressSpaceReady(clientAddressSpace);
            if (!isReady) {
                Thread.sleep(10000);
            }
            log.info("Waiting until Address space: '{}' messages {} will be in ready state", name,
                    (clientAddressSpace != null && clientAddressSpace.getStatus() != null) ? clientAddressSpace.getStatus().getMessages() : null);
        }

        if (!isReady) {
            String status = (clientAddressSpace != null && clientAddressSpace.getStatus() != null) ? clientAddressSpace.getStatus().toString() : null;
            throw new IllegalStateException(String.format("Address Space %s is not in Ready state within timeout: %s", name, status));
        }
        log.info("Address space {} is ready for use", clientAddressSpace);
        return clientAddressSpace;
    }

    public static AddressSpace waitForAddressSpaceStatusMessage(AddressSpace addressSpace, String expected, TimeoutBudget budget) throws Exception {
        var client = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace());

        String name = addressSpace.getMetadata().getName();
        while (!budget.timeoutExpired()) {
            addressSpace = client.withName(name).get();
            if (String.join("", addressSpace.getStatus().getMessages()).contains(expected)) {
                break;
            }
            Thread.sleep(10000);
            log.info("Waiting until Address space: '{}' messages {} contains {}", addressSpace.getMetadata().getName(), addressSpace.getStatus().getMessages(), expected);
        }
        addressSpace = client.withName(name).get();
        if (!String.join("", addressSpace.getStatus().getMessages()).contains(expected)) {
            throw new IllegalStateException(String.format("Address space: '%s' messages %s does not contain %s", addressSpace.getMetadata().getName(), addressSpace.getStatus().getMessages(), expected));
        }
        return addressSpace;
    }

    public static void waitForAddressSpaceConfigurationApplied(AddressSpace addressSpace, String currentConfig) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);

        boolean isConfigApplied = false;
        while (budget.timeLeft() >= 0 && !isConfigApplied) {
            addressSpace = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get();
            isConfigApplied = !addressSpace.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION).equals(currentConfig);
            if (!isConfigApplied) {
                Thread.sleep(2000);
            }
            log.info("Waiting until Address space configuration will be applied. Current: {}",
                    addressSpace.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION));
        }
        isConfigApplied = !addressSpace.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION).equals(currentConfig);
        if (!isConfigApplied) {
            String jsonStatus = addressSpace.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION);
            throw new IllegalStateException("Address Space " + addressSpace + " contains wrong configuration: " + jsonStatus);
        }
        log.info("Address space configuration for {}/{} successfully applied", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());
    }

    public static void deleteAddressSpaceAndWait(AddressSpace addressSpace, GlobalLogCollector logCollector) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS_SPACE);
        deleteAddressSpace(addressSpace, logCollector);
        waitForAddressSpaceDeleted(addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void deleteAddressSpace(AddressSpace addressSpace, GlobalLogCollector logCollector) throws Exception {
        logCollector.collectEvents();
        logCollector.collectLogsTerminatedPods();
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddressSpace");
        List<Address> addresses = Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace()).list().getItems().stream()
                .filter(address -> address.getMetadata().getName().startsWith(addressSpace.getMetadata().getName() + "."))
                .collect(Collectors.toList());
        Kubernetes.getInstance().getAddressClient(addressSpace.getMetadata().getNamespace()).delete(addresses);
        Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).cascading(true).delete();
    }

    public static void waitForAddressSpaceDeleted(AddressSpace addressSpace) throws Exception {
        Kubernetes kube = Kubernetes.getInstance();
        log.info("Waiting for AddressSpace {} to be deleted", addressSpace.getMetadata().getName());
        TimeoutBudget budget = new TimeoutBudget(20, TimeUnit.MINUTES);
        waitForItems(addressSpace, budget, () -> kube.listPods(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listConfigMaps(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listServices(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listSecrets(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listDeployments(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listStatefulSets(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listServiceAccounts(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
        waitForItems(addressSpace, budget, () -> kube.listPersistentVolumeClaims(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))));
    }

    private static <T> void waitForItems(AddressSpace addressSpace, TimeoutBudget budget, Callable<List<T>> callable) throws Exception {
        List<T> resources = null;
        while (budget.timeLeft() >= 0) {
            resources = callable.call();
            if (resources == null || resources.isEmpty()) {
                break;
            }
            Thread.sleep(1000);
        }
        resources = callable.call();
        if (resources != null && resources.size() > 0) {
            throw new TimeoutException("Timed out waiting for address space " + addressSpace.getMetadata().getName() + " to disappear. Resources left: " + resources);
        }
    }

    public static Endpoint getEndpointByName(AddressSpace addressSpace, String endpoint) {
        for (EndpointSpec addrSpaceEndpoint : addressSpace.getSpec().getEndpoints()) {
            if (addrSpaceEndpoint.getName().equals(endpoint)) {
                EndpointStatus status = getEndpointByName(addrSpaceEndpoint.getName(), addressSpace.getStatus().getEndpointStatuses());
                log.debug("Got endpoint: name: {}, service-name: {}, host: {}, port: {}",
                        addrSpaceEndpoint.getName(), addrSpaceEndpoint.getService(), status.getExternalHost(),
                        status.getExternalPorts().values().stream().findAny().get());
                if (status.getExternalHost() == null) {
                    return null;
                } else {
                    return new Endpoint(status.getExternalHost(), status.getExternalPorts().values().stream().findAny().get());
                }
            }
        }
        throw new IllegalStateException(String.format("Endpoint wih name '%s-%s' doesn't exist in address space '%s'",
                endpoint, getAddressSpaceInfraUuid(addressSpace), addressSpace.getMetadata().getName()));
    }

    public static Endpoint getEndpointByServiceName(AddressSpace addressSpace, String endpointService) {
        for (EndpointSpec addrSpaceEndpoint : addressSpace.getSpec().getEndpoints()) {
            if (addrSpaceEndpoint.getService().equals(endpointService)) {
                EndpointStatus status = getEndpointByServiceName(addrSpaceEndpoint.getService(), addressSpace.getStatus().getEndpointStatuses());
                log.info("Got endpoint: name: {}, service-name: {}, host: {}, port: {}",
                        addrSpaceEndpoint.getName(), addrSpaceEndpoint.getService(), status.getExternalHost(),
                        status.getExternalPorts().values().stream().findAny());
                if (status.getExternalHost() == null) {
                    return null;
                } else {
                    return new Endpoint(status.getExternalHost(), status.getExternalPorts().values().stream().findAny().get());
                }
            }
        }
        throw new IllegalStateException(String.format("Endpoint with service name '%s' doesn't exist in address space '%s'",
                endpointService, addressSpace.getMetadata().getName()));
    }

    public static String getExternalEndpointName(AddressSpace addressSpace, String service) {
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getService().equals(service) && endpoint.getName() != null && !endpoint.getName().isEmpty()) {
                return endpoint.getName();
            }
        }
        return service;
    }

    private static EndpointStatus getEndpointByName(String name, List<EndpointStatus> endpoints) {
        return endpoints.stream().filter(endpointStatus -> endpointStatus.getName().equals(name)).findAny().get();
    }

    private static EndpointStatus getEndpointByServiceName(String serviceName, List<EndpointStatus> endpoints) {
        return endpoints.stream().filter(endpointStatus -> endpointStatus.getServiceHost().startsWith(serviceName)).findAny().get();
    }

    public static boolean isBrokered(AddressSpace addressSpace) {
        return addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString());
    }

    public static void waitForAddressSpaceConnectorsNotReady(AddressSpace addressSpace) throws Exception {
        TestUtils.waitUntilCondition("Connectors report not ready", phase -> {
            try {
                AddressSpaceUtils.waitForAddressSpaceConnectorsReady(addressSpace, new TimeoutBudget(20, TimeUnit.SECONDS));
                return false;
            } catch (Exception ex) {
                return ex instanceof IllegalStateException;
            }
        }, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public static AddressSpace waitForAddressSpaceConnectorsReady(AddressSpace addressSpace) throws Exception {
        return waitForAddressSpaceConnectorsReady(addressSpace, new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    public static AddressSpace waitForAddressSpaceConnectorsReady(AddressSpace addressSpace, TimeoutBudget budget) throws Exception {
        waitForAddressSpaceReady(addressSpace);
        boolean isReady = false;
        var client = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace());

        String name = addressSpace.getMetadata().getName();
        AddressSpace clientAddressSpace = addressSpace;
        while (budget.timeLeft() >= 0 && !isReady) {
            clientAddressSpace = client.withName(name).get();
            isReady = areAddressSpaceConnectorsReady(clientAddressSpace);
            if (!isReady) {
                Thread.sleep(10000);
            }
            log.info("Waiting until connectors of address space: '{}' messages {} will be in ready state", name, getConnectorStatuses(clientAddressSpace));
        }
        clientAddressSpace = client.withName(name).get();
        isReady = areAddressSpaceConnectorsReady(clientAddressSpace);
        if (!isReady) {
            throw new IllegalStateException(String.format("Connectors of Address Space %s are not in Ready state within timeout: %s", name, getConnectorStatuses(clientAddressSpace)));
        }
        log.info("Connectors of address space {} are ready for use", name);
        return clientAddressSpace;
    }

    public static void waithForAuthServiceApplied(AddressSpace addressSpace, String expectedAuthServiceName) throws Exception {
        TestUtils.waitUntilCondition("Auth service applied", waitPhase -> {
            AddressSpace addrSpaceObject = Kubernetes.getInstance().getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).withName(addressSpace.getMetadata().getName()).get();
            return addrSpaceObject.getMetadata().getAnnotations().get("enmasse.io/applied-configuration").contains("\"authenticationService\":{\"name\":\"" + expectedAuthServiceName + "\"}")
                    && isAddressSpaceReady(addrSpaceObject) && addrSpaceObject.getStatus().getPhase().equals(Phase.Active);
        }, new TimeoutBudget(5, TimeUnit.MINUTES));

    }

    /**
     * Returns true only if all connectorStatuses report isReady=true
     *
     * @param addressSpace
     * @return
     */
    public static boolean areAddressSpaceConnectorsReady(AddressSpace addressSpace) {
        return Optional.ofNullable(addressSpace)
                .map(AddressSpace::getStatus)
                .map(AddressSpaceStatus::getConnectors).stream()
                .flatMap(Collection::stream)
                .allMatch(AddressSpaceStatusConnector::isReady);
    }

    public static String getConnectorStatuses(AddressSpace addressSpace) {
        return Optional.ofNullable(addressSpace)
                .map(AddressSpace::getStatus)
                .map(AddressSpaceStatus::getConnectors).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
                .toString();
    }

    public static Endpoint getMessagingRoute(AddressSpace addressSpace) throws Exception {
        Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
            messagingEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName);
        }
        if (TestUtils.resolvable(messagingEndpoint)) {
            return messagingEndpoint;
        } else {
            return Kubernetes.getInstance().getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), addressSpace.getMetadata().getNamespace(), "amqps");
        }
    }

    public static Endpoint getConsoleEndpoint(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "console");
        if (consoleEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "console");
            consoleEndpoint = Kubernetes.getInstance().getExternalEndpoint(externalEndpointName);
        }
        return consoleEndpoint;
    }

    public static Endpoint getMessagingWssRoute(AddressSpace addressSpace) throws Exception {
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString())) {
            Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByName(addressSpace, "messaging-wss");
            if (TestUtils.resolvable(messagingEndpoint)) {
                return messagingEndpoint;
            } else {
                return Kubernetes.getInstance().getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), addressSpace.getMetadata().getNamespace(), "https");
            }
        } else {
            return AddressSpaceUtils.getMessagingRoute(addressSpace);
        }
    }
}
