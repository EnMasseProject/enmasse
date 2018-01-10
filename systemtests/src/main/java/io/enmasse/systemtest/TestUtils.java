/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    public static void setReplicas(Kubernetes kubernetes, AddressSpace addressSpace, Destination destination, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        kubernetes.setDeploymentReplicas(addressSpace.getNamespace(), destination.getGroup(), numReplicas);
        waitForNReplicas(
                kubernetes,
                addressSpace.getNamespace(),
                numReplicas,
                Collections.singletonMap("role", "broker"),
                Collections.singletonMap("cluster_id", destination.getGroup()),
                budget);
    }

    public static void setReplicas(Kubernetes kubernetes, String tenantNamespace, String deployment, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        kubernetes.setDeploymentReplicas(tenantNamespace, deployment, numReplicas);
        waitForNReplicas(
                kubernetes,
                tenantNamespace,
                numReplicas,
                Collections.singletonMap("name", deployment),
                budget);
    }

    public static void waitForNReplicas(Kubernetes kubernetes, String tenantNamespace, int expectedReplicas, Map<String, String> labelSelector, TimeoutBudget budget) throws InterruptedException {
        waitForNReplicas(kubernetes, tenantNamespace, expectedReplicas, labelSelector, Collections.emptyMap(), budget);
    }

    public static void waitForNReplicas(Kubernetes kubernetes, String tenantNamespace, int expectedReplicas, Map<String, String> labelSelector, Map<String, String> annotationSelector, TimeoutBudget budget) throws InterruptedException {
        boolean done = false;
        int actualReplicas = 0;
        do {
            List<Pod> pods;
            if (annotationSelector.isEmpty()) {
                pods = kubernetes.listPods(tenantNamespace, labelSelector);
            } else {
                pods = kubernetes.listPods(tenantNamespace, labelSelector, annotationSelector);
            }
            actualReplicas = numReady(pods);
            Logging.log.info("Have " + actualReplicas + " out of " + pods.size() + " replicas. Expecting " + expectedReplicas);
            if (actualReplicas != pods.size() || actualReplicas != expectedReplicas) {
                Thread.sleep(5000);
            } else {
                done = true;
            }
        } while (budget.timeLeft() >= 0 && !done);

        if (!done) {
            throw new RuntimeException("Only " + actualReplicas + " out of " + expectedReplicas + " in state 'Running' before timeout");
        }
    }

    private static int numReady(List<Pod> pods) {
        int numReady = 0;
        for (Pod pod : pods) {
            if ("Running".equals(pod.getStatus().getPhase())) {
                numReady++;
            } else {
                Logging.log.info("POD " + pod.getMetadata().getName() + " in status : " + pod.getStatus().getPhase());
            }
        }
        return numReady;
    }

    public static void waitForExpectedPods(Kubernetes client, AddressSpace addressSpace, int numExpected, TimeoutBudget budget) throws InterruptedException {
        List<Pod> pods = listRunningPods(client, addressSpace);
        while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
            Thread.sleep(2000);
            pods = listRunningPods(client, addressSpace);
        }
        if (pods.size() != numExpected) {
            throw new IllegalStateException("Unable to find " + numExpected + " pods. Found : " + printPods(pods));
        }
    }

    public static String printPods(List<Pod> pods) {
        return pods.stream()
                .map(pod -> pod.getMetadata().getName())
                .collect(Collectors.joining(","));
    }

    public static List<Pod> listRunningPods(Kubernetes kubernetes, AddressSpace addressSpace) {
        return kubernetes.listPods(addressSpace.getNamespace()).stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    public static void waitForBrokerPod(Kubernetes kubernetes, AddressSpace addressSpace, String group, TimeoutBudget budget) throws InterruptedException {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("role", "broker");

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("cluster_id", group);


        int numReady = 0;
        List<Pod> pods = null;
        while (budget.timeLeft() >= 0 && numReady != 1) {
            pods = kubernetes.listPods(addressSpace.getNamespace(), labels, annotations);
            numReady = numReady(pods);
            if (numReady != 1) {
                Thread.sleep(5000);
            }
        }
        if (numReady != 1) {
            throw new IllegalStateException("Unable to find broker pod for " + group + " within timeout. Found " + pods);
        }
    }


    public static void delete(AddressApiClient apiClient, AddressSpace addressSpace, Destination... destinations) throws Exception {
        apiClient.deleteAddresses(addressSpace, destinations);
    }

    public static void deploy(AddressApiClient apiClient, Kubernetes kubernetes, TimeoutBudget budget, AddressSpace addressSpace, HttpMethod httpMethod, Destination... destinations) throws Exception {
        apiClient.deploy(addressSpace, httpMethod, destinations);
        JsonObject addrSpaceObj = apiClient.getAddressSpace(addressSpace.getName());
        if (getAddressSpaceType(addrSpaceObj).equals("standard")) {
            Set<String> groups = new HashSet<>();
            for (Destination destination : destinations) {
                if (Destination.isQueue(destination) || Destination.isTopic(destination)) {
                    waitForBrokerPod(kubernetes, addressSpace, destination.getGroup(), budget);
                    groups.add(destination.getGroup());
                }
            }
            int expectedPods = kubernetes.getExpectedPods() + groups.size();
            Logging.log.info("Waiting for " + expectedPods + " pods");
            waitForExpectedPods(kubernetes, addressSpace, expectedPods, budget);
        }
        waitForDestinationsReady(apiClient, addressSpace, budget, destinations);
    }

    public static boolean existAddressSpace(AddressApiClient apiClient, String addressSpaceName) throws Exception {
        return apiClient.listAddressSpaces().contains(addressSpaceName);
    }

    public static boolean isAddressSpaceReady(JsonObject address) {
        boolean isReady = false;
        if (address != null) {
            isReady = address.getJsonObject("status").getBoolean("isReady");
        }
        return isReady;
    }

    public static String getAddressSpaceType(JsonObject address) {
        String addrSpaceType = "";
        if (address != null) {
            addrSpaceType = address.getJsonObject("spec").getString("type");
        }
        return addrSpaceType;
    }

    /**
     * wait until isReady parameter of Address Space is set to true within timeout
     *
     * @param apiClient    instance of AddressApiClient
     * @param addressSpace name of addressSpace
     * @throws Exception IllegalStateException if address space is not ready within timeout
     */
    public static void waitForAddressSpaceReady(AddressApiClient apiClient, String addressSpace) throws Exception {
        JsonObject addressObject = null;
        TimeoutBudget budget = null;

        boolean isReady = false;
        budget = new TimeoutBudget(3, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && !isReady) {
            addressObject = apiClient.getAddressSpace(addressSpace);
            isReady = isAddressSpaceReady(addressObject);
            if (!isReady) {
                Thread.sleep(5000);
            }
            Logging.log.info("Waiting until Address space: " + addressSpace + " will be in ready state");
        }
        if (!isReady) {
            throw new IllegalStateException("Address Space " + addressSpace + " is not in Ready state within timeout.");
        }
    }

    public static Future<List<String>> getAddresses(AddressApiClient apiClient, AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        JsonObject response = apiClient.getAddresses(addressSpace, addressName);
        CompletableFuture<List<String>> listOfAddresses = new CompletableFuture<>();
        listOfAddresses.complete(convertToList(response));
        return listOfAddresses;
    }

    public static boolean isAddressReady(JsonObject address) {
        boolean isReady = false;
        if (address != null) {
            isReady = address.getJsonObject("status").getBoolean("isReady");
        }
        return isReady;
    }

    /**
     * Pulling out names of queues from json object
     *
     * @param htmlResponse JsonObject with specified structure returned from rest api
     * @return list of address names
     */
    private static List<String> convertToList(JsonObject htmlResponse) {
        String kind = htmlResponse.getString("kind");
        List<String> addresses = new ArrayList<>();
        switch (kind) {
            case "Address":
                addresses.add(htmlResponse.getJsonObject("metadata").getString("name"));
                break;
            case "AddressList":
                JsonArray items = htmlResponse.getJsonArray("items");
                if (items != null) {
                    items.forEach(address -> {
                        addresses.add(((JsonObject) address).getJsonObject("metadata").getString("name"));
                    });
                }
                break;
            default:
                Logging.log.warn("Unspecified kind: " + kind);
        }
        return addresses;
    }

    /**
     * wait until destinations isReady parameter is set to true with 1 MINUTE timeout for each destination
     *
     * @param apiClient    instance of AddressApiClient
     * @param addressSpace name of addressSpace
     * @param budget       the timeout budget for this operation
     * @param destinations variable count of destinations
     * @throws Exception IllegalStateException if destinations are not ready within timeout
     */
    public static void waitForDestinationsReady(AddressApiClient apiClient, AddressSpace addressSpace, TimeoutBudget budget, Destination... destinations) throws Exception {
        Map<String, JsonObject> notReadyAddresses = new HashMap<>();

        while (budget.timeLeft() >= 0) {
            JsonObject addressList = apiClient.getAddresses(addressSpace, Optional.empty());
            notReadyAddresses = checkAddressesReady(addressList, destinations);
            if (notReadyAddresses.isEmpty()) {
                break;
            }
            Thread.sleep(5000);
        }

        if (!notReadyAddresses.isEmpty()) {
            JsonObject addressList = apiClient.getAddresses(addressSpace, Optional.empty());
            notReadyAddresses = checkAddressesReady(addressList, destinations);
            throw new IllegalStateException(notReadyAddresses.size() + " out of " + destinations.length
                    + " addresses are not ready: " + notReadyAddresses.values());
        }
    }

    private static Map<String, JsonObject> checkAddressesReady(JsonObject addressList, Destination... destinations) {
        Logging.log.info("Checking {} for ready state", destinations);
        Map<String, JsonObject> notReadyAddresses = new HashMap<>();
        for (Destination destination : destinations) {
            JsonObject addressObject = lookupAddress(addressList, destination.getAddress());
            if (addressObject == null) {
                notReadyAddresses.put(destination.getAddress(), null);
            } else if (!isAddressReady(addressObject)) {
                notReadyAddresses.put(destination.getAddress(), addressObject);
            }
        }
        return notReadyAddresses;
    }

    private static JsonObject lookupAddress(JsonObject addressList, String address) {
        JsonArray items = addressList.getJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
            JsonObject addressObject = items.getJsonObject(i);
            if (addressObject.getJsonObject("spec").getString("address").equals(address)) {
                return addressObject;
            }
        }
        return null;
    }

    public static List<String> generateMessages(String prefix, int numMessages) {
        return IntStream.range(0, numMessages).mapToObj(i -> prefix + i).collect(Collectors.toList());
    }

    public static List<String> generateMessages(int numMessages) {
        return generateMessages("testmessage", numMessages);
    }

    public static boolean resolvable(Endpoint endpoint) {
        for (int i = 0; i < 10; i++) {
            try {
                InetAddress[] addresses = Inet4Address.getAllByName(endpoint.getHost());
                Thread.sleep(1000);
                return addresses.length > 0;
            } catch (Exception e) {
                Thread.interrupted();
            }
        }
        return false;
    }

    public static void waitForAddressSpaceDeleted(Kubernetes kubernetes, AddressSpace addressSpace) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && kubernetes.listNamespaces().contains(addressSpace.getNamespace())) {
            Thread.sleep(1000);
        }
        if (kubernetes.listNamespaces().contains(addressSpace.getNamespace())) {
            throw new TimeoutException("Timed out waiting for namespace " + addressSpace + " to disappear");
        }
    }

    public static <T> T doRequestNTimes(int retry, Callable<T> fn) throws Exception {
        try {
            return fn.call();
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("{} remaining iterations", retry);
                    return doRequestNTimes(retry - 1, fn);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                if (ex.getCause() != null) {
                    ex.getCause().printStackTrace();
                } else {
                    ex.printStackTrace();
                }
                throw ex;
            }
        }
    }
}
