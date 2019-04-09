/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class AddressSpaceUtils {
    private static Logger log = CustomLogger.getLogger();

    public static AddressSpace createAddressSpaceObject(String name) {
        return createAddressSpaceObject(name, null, AddressSpaceType.STANDARD, AuthenticationServiceType.NONE);
    }

    public static AddressSpace createAddressSpaceObject(String name, AuthenticationServiceType authService) {
        return createAddressSpaceObject(name, null, AddressSpaceType.STANDARD, authService);
    }

    public static AddressSpace createAddressSpaceObject(String name, AddressSpaceType type) {
        return createAddressSpaceObject(name, null, type, AuthenticationServiceType.NONE);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace) {
        return createAddressSpaceObject(name, namespace, AddressSpaceType.STANDARD, AuthenticationServiceType.NONE);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, AuthenticationServiceType authService) {
        return createAddressSpaceObject(name, namespace, AddressSpaceType.STANDARD, authService);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, String plan) {
        return createAddressSpaceObject(name, namespace, AddressSpaceType.STANDARD, plan);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, String plan, AuthenticationServiceType authService) {
        return createAddressSpaceObject(name, namespace, AddressSpaceType.STANDARD, plan, authService);
    }

    public static AddressSpace createAddressSpaceObject(String name, AddressSpaceType type, String plan) {
        return createAddressSpaceObject(name, null, type, plan);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, AddressSpaceType type, String plan) {
        return createAddressSpaceObject(name, namespace, type, plan, AuthenticationServiceType.STANDARD);
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, AddressSpaceType type) {
        return createAddressSpaceResource(name, type, AuthenticationServiceType.NONE)
                .editMetadata()
                .withNamespace(namespace)
                .withAnnotations(Collections.singletonMap("enmasse.io/kc-idp-hint", "none"))
                .endMetadata()
                .done();
    }

    public static AddressSpace createAddressSpaceObject(String name, AddressSpaceType type, AuthenticationServiceType authService) {
        return createAddressSpaceResource(name, type, authService)
                .editMetadata()
                .withAnnotations(Collections.singletonMap("enmasse.io/kc-idp-hint", "none"))
                .endMetadata()
                .done();
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, AddressSpaceType type, AuthenticationServiceType authService) {
        return createAddressSpaceResource(name, type, authService)
                .editMetadata()
                .withNamespace(namespace)
                .endMetadata()
                .done();
    }

    public static AddressSpace createAddressSpaceObject(String name, String namespace, AddressSpaceType type, String plan, AuthenticationServiceType authService) {
        return createAddressSpaceResource(name, type, authService)
                .editMetadata()
                .withAnnotations(Collections.singletonMap("enmasse.io/kc-idp-hint", "none"))
                .withNamespace(namespace)
                .endMetadata()
                .editSpec()
                .withPlan(plan)
                .endSpec()
                .done();
    }

    public static AddressSpace createAddressSpaceObject(String name, AddressSpaceType type, String plan, AuthenticationServiceType authService) {
        return createAddressSpaceObject(name, null, type, plan, authService);
    }

    public static AddressSpace createAddressSpaceObject(String name, AddressSpaceType type, String plan, String authServiceName) {
        return createAddressSpaceResource(name, type, authServiceName)
                .editSpec()
                .withPlan(plan)
                .endSpec()
                .done();
    }

    private static DoneableAddressSpace createAddressSpaceResource(String name, AddressSpaceType type, AuthenticationServiceType auth) {
        return createAddressSpaceResource(name, type, auth.equals(AuthenticationServiceType.STANDARD) ? "standard-authservice" : "none-authservice");
    }

    private static DoneableAddressSpace createAddressSpaceResource(String name, AddressSpaceType type, String authServiceName) {
        return new DoneableAddressSpace(new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withType(type.toString())
                .withPlan(type.equals(AddressSpaceType.BROKERED) ? AddressSpacePlans.BROKERED : AddressSpacePlans.STANDARD_UNLIMITED_WITH_MQTT)
                .withNewAuthenticationService()
                .withName(authServiceName)
                .endAuthenticationService()
                .endSpec()
                .build());
    }

    public static JsonObject addressSpaceToJson(AddressSpace addressSpace) throws Exception {
        return new JsonObject(new ObjectMapper().writeValueAsString(addressSpace));
    }

    public static AddressSpace jsonToAdressSpace(JsonObject addressSpace) throws Exception {
        return new ObjectMapper().readValue(addressSpace.toString(), AddressSpace.class);
    }

    public static String getAddressSpaceInfraUuid(AddressSpace addressSpace) {
        return addressSpace.getMetadata().getAnnotations().get("enmasse.io/infra-uuid");
    }

    public static boolean existAddressSpace(AddressApiClient apiClient, String addressSpaceName) throws Exception {
        return apiClient.listAddressSpaces().contains(addressSpaceName);
    }

    public static boolean isAddressSpaceReady(AddressSpace addressSpace) {
        return addressSpace != null && addressSpace.getStatus().isReady();
    }

    public static boolean matchAddressSpacePlan(AddressSpace received, AddressSpace expected) {
        return received != null && received.getMetadata().getAnnotations().get("enmasse.io/applied-plan").equals(expected.getSpec().getPlan());
    }

    public static AddressSpace waitForAddressSpaceReady(AddressApiClient apiClient, AddressSpace addressSpace) throws Exception {
        TimeoutBudget budget;

        boolean isReady = false;
        budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && !isReady) {
            addressSpace = jsonToAdressSpace(apiClient.getAddressSpace(addressSpace.getMetadata().getName()));
            isReady = isAddressSpaceReady(addressSpace);
            if (!isReady) {
                Thread.sleep(10000);
            }
            log.info("Waiting until Address space: '{}' will be in ready state", addressSpace.getMetadata().getName());
        }
        if (!isReady) {
            String jsonStatus = addressSpace != null ? addressSpace.getStatus().toString() : "";
            throw new IllegalStateException("Address Space " + addressSpace + " is not in Ready state within timeout: " + jsonStatus);
        }

        return addressSpace;
    }

    public static void waitForAddressSpacePlanApplied(AddressApiClient apiClient, AddressSpace addressSpace) throws Exception {
        AddressSpace addressSpaceObject = null;
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);

        boolean isPlanApplied = false;
        while (budget.timeLeft() >= 0 && !isPlanApplied) {
            addressSpaceObject = AddressSpaceUtils.jsonToAdressSpace(apiClient.getAddressSpace(addressSpace.getMetadata().getName()));
            isPlanApplied = matchAddressSpacePlan(addressSpaceObject, addressSpace);
            if (!isPlanApplied) {
                Thread.sleep(1000);
            }
            log.info("Waiting until Address space plan will be applied: '{}', current: {}",
                    addressSpace.getSpec().getPlan(),
                    addressSpaceObject.getMetadata().getAnnotations().get("enmasse.io/applied-plan"));
        }
        isPlanApplied = matchAddressSpacePlan(addressSpaceObject, addressSpace);
        if (!isPlanApplied) {
            String jsonStatus = addressSpaceObject != null ? addressSpaceObject.getMetadata().getAnnotations().get("enmasse.io/applied-plan") : "";
            throw new IllegalStateException("Address Space " + addressSpace + " contains wrong plan: " + jsonStatus);
        }
        log.info("Address plan {} successfully applied", addressSpace.getSpec().getPlan());
    }

    public static AddressSpace getAddressSpaceObject(AddressApiClient apiClient, String addressSpaceName) throws Exception {
        JsonObject addressSpaceJson = apiClient.getAddressSpace(addressSpaceName);
        log.info("address space received {}", addressSpaceJson.toString());
        return convertToAddressSpaceObject(addressSpaceJson).get(0);
    }

    public static void syncAddressSpaceObject(AddressSpace addressSpace, AddressApiClient apiClient) throws Exception {
        AddressSpace results = jsonToAdressSpace(apiClient.getAddressSpace(addressSpace.getMetadata().getName()));
        addressSpace.setSpec(results.getSpec());
        addressSpace.setMetadata(results.getMetadata());
        addressSpace.setStatus(results.getStatus());
    }


    public static List<AddressSpace> getAddressSpacesObjects(AddressApiClient apiClient) throws Exception {
        JsonObject addressSpaceJson = apiClient.listAddressSpacesObjects();
        return convertToAddressSpaceObject(addressSpaceJson);
    }

    public static Future<List<AddressSpace>> getAllAddressSpacesObjects(AddressApiClient apiClient) throws Exception {
        JsonObject response = apiClient.getAllAddresseSpaces();
        CompletableFuture<List<AddressSpace>> listOfAddresses = new CompletableFuture<>();
        listOfAddresses.complete(AddressSpaceUtils.convertToAddressSpaceObject(response));
        return listOfAddresses;
    }

    public static List<AddressSpace> convertToAddressSpaceObject(JsonObject addressSpaceJson) throws Exception {
        if (addressSpaceJson == null) {
            throw new IllegalArgumentException("null response can't be converted to AddressSpace");
        }
        String kind = addressSpaceJson.getString("kind");
        List<AddressSpace> resultAddrSpace = new ArrayList<>();
        switch (kind) {
            case "AddressSpace":
                resultAddrSpace.add(jsonToAdressSpace(addressSpaceJson));
                break;
            case "AddressSpaceList":
                JsonArray items = addressSpaceJson.getJsonArray("items");
                for (int i = 0; i < items.size(); i++) {
                    resultAddrSpace.add(jsonToAdressSpace(items.getJsonObject(i)));
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown kind: '%s'", kind));
        }
        return resultAddrSpace;
    }

    public static void deleteAddressSpaceAndWait(AddressApiClient addressApiClient, Kubernetes kubernetes, AddressSpace addressSpace, GlobalLogCollector logCollector) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS_SPACE);
        deleteAddressSpace(addressApiClient, addressSpace, logCollector);
        waitForAddressSpaceDeleted(kubernetes, addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    private static void deleteAddressSpace(AddressApiClient addressApiClient, AddressSpace addressSpace, GlobalLogCollector logCollector) throws Exception {
        logCollector.collectEvents();
        logCollector.collectApiServerJmapLog();
        logCollector.collectLogsTerminatedPods();
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddressSpace");
        addressApiClient.deleteAddressSpace(addressSpace);
    }

    public static void deleteAllAddressSpaces(AddressApiClient addressApiClient, GlobalLogCollector logCollector) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.DELETE_ADDRESS_SPACE);
        logCollector.collectEvents();
        logCollector.collectApiServerJmapLog();
        logCollector.collectLogsTerminatedPods();
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddressSpace");
        addressApiClient.deleteAddressSpaces(200);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    public static void waitForAddressSpaceDeleted(Kubernetes kubernetes, AddressSpace addressSpace) throws Exception {
        log.info("Waiting for AddressSpace {} to be deleted", addressSpace.getMetadata().getName());
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        waitForItems(addressSpace, budget, () -> kubernetes.listPods(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listConfigMaps(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listServices(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listSecrets(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listDeployments(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listStatefulSets(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listServiceAccounts(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
        waitForItems(addressSpace, budget, () -> kubernetes.listPersistentVolumeClaims(Collections.singletonMap("infraUuid", getAddressSpaceInfraUuid(addressSpace))).size());
    }

    private static void waitForItems(AddressSpace addressSpace, TimeoutBudget budget, Callable<Integer> callable) throws Exception {
        while (budget.timeLeft() >= 0 && callable.call() > 0) {
            Thread.sleep(1000);
        }
        if (callable.call() > 0) {
            throw new TimeoutException("Timed out waiting for namespace " + addressSpace.getMetadata().getName() + " to disappear");
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
                        status.getExternalPorts().values().stream().findAny().get());
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

    public static Future<AddressSpaceSchemaList> getSchema(AddressApiClient apiClient) throws Exception {
        JsonObject response = apiClient.getSchema();
        CompletableFuture<AddressSpaceSchemaList> schema = new CompletableFuture<>();
        log.info("Got Schema object: {}", response.toString());
        schema.complete(new ObjectMapper().readValue(response.toString(), AddressSpaceSchemaList.class));
        return schema;
    }
}
