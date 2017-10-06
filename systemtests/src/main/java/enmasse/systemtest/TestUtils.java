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

package enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {
    public static void setReplicas(OpenShift openShift, String addressSpace, Destination destination, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        openShift.setDeploymentReplicas(destination.getGroup(), numReplicas);
        waitForNReplicas(openShift, addressSpace, destination.getGroup(), numReplicas, budget);
    }

    public static void waitForNReplicas(OpenShift openShift, String addressSpace, String group, int expectedReplicas, TimeoutBudget budget) throws InterruptedException {
        boolean done = false;
        int actualReplicas = 0;
        do {
            List<Pod> pods = openShift.listPods(addressSpace, Collections.singletonMap("role", "broker"), Collections.singletonMap("cluster_id", group));
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

    public static void waitForExpectedPods(OpenShift client, String addressSpace, int numExpected, TimeoutBudget budget) throws InterruptedException {
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

    public static List<Pod> listRunningPods(OpenShift openShift, String addressSpace) {
        return openShift.listPods(addressSpace).stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    public static void waitForBrokerPod(OpenShift openShift, String addressSpace, String group, TimeoutBudget budget) throws InterruptedException {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("role", "broker");

        Map<String, String> annotations = new LinkedHashMap<>();
        annotations.put("cluster_id", group);


        int numReady = 0;
        List<Pod> pods = null;
        while (budget.timeLeft() >= 0 && numReady != 1) {
            pods = openShift.listPods(addressSpace, labels, annotations);
            numReady = numReady(pods);
            if (numReady != 1) {
                Thread.sleep(5000);
            }
        }
        if (numReady != 1) {
            throw new IllegalStateException("Unable to find broker pod for " + group + " within timeout. Found " + pods);
        }
    }


    public static void delete(AddressApiClient apiClient, String addressSpace, Destination... destinations) throws Exception {
        apiClient.deleteAddresses(addressSpace, destinations);
    }

    public static void deploy(AddressApiClient apiClient, OpenShift openShift, TimeoutBudget budget, String addressSpace, HttpMethod httpMethod, Destination... destinations) throws Exception {
        apiClient.deploy(addressSpace, httpMethod, destinations);
        Set<String> groups = new HashSet<>();
        for (Destination destination : destinations) {
            if (Destination.isQueue(destination) || Destination.isTopic(destination)) {
                waitForBrokerPod(openShift, addressSpace, destination.getGroup(), budget);
                groups.add(destination.getGroup());
            }
        }
        int expectedPods = openShift.getExpectedPods() + groups.size();
        Logging.log.info("Waiting for " + expectedPods + " pods");
        waitForExpectedPods(openShift, addressSpace, expectedPods, budget);
        waitForDestinationsReady(apiClient, addressSpace, budget, destinations);
    }

    public static boolean existAddressSpace(AddressApiClient apiClient, String addressSpaceName) throws InterruptedException, TimeoutException, ExecutionException {
        return apiClient.listAddressSpaces().contains(addressSpaceName);
    }

    public static boolean isAddressSpaceReady(JsonObject address) {
        boolean isReady = false;
        if (address != null) {
            isReady = address.getJsonObject("status").getBoolean("isReady");
        }
        return isReady;
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

    public static Future<List<String>> getAddresses(AddressApiClient apiClient, String addressSpace, Optional<String> addressName) throws Exception {
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
    public static void waitForDestinationsReady(AddressApiClient apiClient, String addressSpace, TimeoutBudget budget, Destination... destinations) throws Exception {
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
        }

        if (!notReadyAddresses.isEmpty()) {
            throw new IllegalStateException(notReadyAddresses.size() + " out of " + destinations.length
                    + " addresses are not ready: " + notReadyAddresses.values());
        }
    }

    private static Map<String, JsonObject> checkAddressesReady(JsonObject addressList, Destination ...destinations) {
        Logging.log.info("Checking {} for ready state", destinations);
        Map<String, JsonObject> notReadyAddresses = new HashMap<>();
        for (Destination destination : destinations) {
            JsonObject addressObject = lookupAddress(addressList, destination.getAddress());
            if (addressObject == null) {
                notReadyAddresses.put(destination.getAddress(),  null);
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

    public static boolean resolvable(Endpoint endpoint) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            try {
                InetAddress[] addresses = Inet4Address.getAllByName(endpoint.getHost());
                return addresses.length > 0;
            } catch (Exception e) {
            }
            Thread.sleep(1000);
        }
        return false;
    }

    public static void waitForAddressSpaceDeleted(OpenShift openShift, String addressSpace) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(2, TimeUnit.MINUTES);
        while (budget.timeLeft() >= 0 && openShift.listNamespaces().contains(addressSpace)) {
            Thread.sleep(1000);
        }
        if (openShift.listNamespaces().contains(addressSpace)) {
            throw new TimeoutException("Timed out waiting for namespace " + addressSpace + " to disappear");
        }
    }
}
