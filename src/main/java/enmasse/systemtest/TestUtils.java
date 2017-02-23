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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import enmasse.amqp.SyncRequestClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * TODO: Description
 */
public class TestUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void setReplicas(OpenShift openShift, Destination destination, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        openShift.setDeploymentReplicas(destination.getGroup(), numReplicas);
        waitForNReplicas(openShift, destination.getGroup(), numReplicas, budget);
    }

    public static void waitForNReplicas(OpenShift openShift, String group, int expectedReplicas, TimeoutBudget budget) throws InterruptedException {
        boolean done = false;
        int actualReplicas = 0;
        do {
            List<Pod> pods = openShift.listPods(Collections.singletonMap("group_id", group));
            actualReplicas = numReady(pods);
            System.out.println("Have " + actualReplicas + " out of " + pods.size() + " replicas. Expecting " + expectedReplicas);
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
                System.out.println("POD " + pod.getMetadata().getName() + " in status : " + pod.getStatus().getPhase());
            }
        }
        return numReady;
    }

    public static void waitForExpectedPods(OpenShift client, int numExpected, TimeoutBudget budget) throws InterruptedException {
        List<Pod> pods = listRunningPods(client);
        while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
            Thread.sleep(2000);
            pods = listRunningPods(client);
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

    public static List<Pod> listRunningPods(OpenShift openShift) {
        return openShift.listPods().stream()
                .filter(pod -> pod.getStatus().getPhase().equals("Running"))
                .collect(Collectors.toList());
    }

    public static void waitForBrokerPod(OpenShift openShift, String group, TimeoutBudget budget) throws InterruptedException {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("group_id", group);
        labels.put("role", "broker");


        int numReady = 0;
        List<Pod> pods = null;
        while (budget.timeLeft() >= 0 && numReady != 1) {
            pods = openShift.listPods(labels);
            numReady = numReady(pods);
            if (numReady != 1) {
                Thread.sleep(5000);
            }
        }
        if (numReady != 1) {
            throw new IllegalStateException("Unable to find broker pod for " + group + " within timeout. Found " + pods);
        }
    }

    public static void deploy(HttpClient httpClient, OpenShift openShift, TimeoutBudget budget, Destination ... destinations) throws Exception {
        ObjectNode config = mapper.createObjectNode();
        config.put("apiVersion", "v3");
        config.put("kind", "AddressList");
        ObjectNode addresses = config.putObject("addresses");
        for (Destination destination : destinations) {
            ObjectNode entry = addresses.putObject(destination.getAddress());
            entry.put("store_and_forward", destination.isStoreAndForward());
            entry.put("multicast", destination.isMulticast());
            entry.put("group", destination.getGroup());
            destination.getFlavor().ifPresent(e -> entry.put("flavor", e));
        }
        Endpoint restApi = openShift.getRestEndpoint();

        CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest request = httpClient.put(restApi.getPort(), restApi.getHost(), "/v3/address");
        request.putHeader("content-type", "application/json");
        request.handler(event -> {
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            }
        });
        request.end(Buffer.buffer(mapper.writeValueAsBytes(config)));
        latch.await(30, TimeUnit.SECONDS);
        int expectedPods = openShift.getExpectedPods();
        for (Destination destination : destinations) {
            if (destination.isStoreAndForward()) {
                waitForBrokerPod(openShift, destination.getGroup(), budget);
                if (!destination.isMulticast()) {
                    waitForAddress(openShift, destination.getAddress(), budget);
                }
                expectedPods++;
            }
        }
        System.out.println("Waiting for " + expectedPods + " pods");
        waitForExpectedPods(openShift, expectedPods, budget);
    }

    public static void waitForAddress(OpenShift openShift, String address, TimeoutBudget budget) throws Exception {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode data = root.addObject();
        data.put("name", address);
        data.put("store_and_forward", true);
        data.put("multicast", false);
        String json = mapper.writeValueAsString(root);
        Message message = Message.Factory.create();
        message.setAddress("health-check");
        message.setSubject("health-check");
        message.setBody(new AmqpValue(json));

        int numConfigured = 0;
        List<Pod> agents = openShift.listPods(Collections.singletonMap("name", "ragent"));

        while (budget.timeLeft() >= 0 && numConfigured < agents.size()) {
            numConfigured = 0;
            for (Pod pod : agents) {
                SyncRequestClient client = new SyncRequestClient(pod.getStatus().getPodIP(), pod.getSpec().getContainers().get(0).getPorts().get(0).getContainerPort());
                Message response = client.request(message, budget.timeLeft(), TimeUnit.MILLISECONDS);
                AmqpValue value = (AmqpValue) response.getBody();
                if ((Boolean) value.getValue() == true) {
                    numConfigured++;
                }
            }
            Thread.sleep(1000);
        }
        if (numConfigured != agents.size()) {
            throw new IllegalStateException("Timed out while waiting for EnMasse to be configured");
        }
    }

    public static List<String> generateMessages(String prefix, int numMessages) {
        return IntStream.range(0, numMessages).mapToObj(i -> prefix + i).collect(Collectors.toList());
    }

    public static List<String> generateMessages(int numMessages) {
        return generateMessages("testmessage", numMessages);
    }
}
