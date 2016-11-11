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
import com.openshift.internal.restclient.model.KubernetesResource;
import com.openshift.restclient.IClient;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IDeploymentConfig;
import com.openshift.restclient.model.IPod;
import enmasse.amqp.SyncRequestClient;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.jboss.dmr.ModelNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static enmasse.systemtest.Environment.namespace;

/**
 * TODO: Description
 */
public class TestUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void setReplicas(Destination destination, int numReplicas, TimeoutBudget budget) throws InterruptedException {
        IDeploymentConfig controller = Environment.getClient().get(ResourceKind.DEPLOYMENT_CONFIG, destination.getAddress(), Environment.namespace);
        controller.setReplicas(numReplicas);
        Environment.getClient().update(controller);
        waitForNReplicas(destination.getAddress(), numReplicas, budget);
    }

    public static void waitForNReplicas(String address, int expectedReplicas, TimeoutBudget budget) throws InterruptedException {
        boolean done = false;
        int actualReplicas = 0;
        do {
            List<IPod> pods = Environment.getClient().list(ResourceKind.POD, Environment.namespace, Collections.singletonMap("address", address));
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

    private static int numReady(List<IPod> pods) {
        int numReady = 0;
        for (IPod pod : pods) {
            if ("Running".equals(pod.getStatus())) {
                numReady++;
            } else {
                System.out.println("POD " + pod.getName() + " in status : " + pod.getStatus());
            }
        }
        return numReady;
    }

    public static void waitForExpectedPods(IClient client, int numExpected, TimeoutBudget budget) throws InterruptedException {
        List<IPod> pods = listRunningPods(client);
        while (budget.timeLeft() >= 0 && pods.size() != numExpected) {
            Thread.sleep(2000);
            pods = listRunningPods(client);
        }
        if (pods.size() != numExpected) {
            throw new IllegalStateException("Unable to find " + numExpected + " pods. Found : " + printPods(pods));
        }
    }

    public static String printPods(List<IPod> pods) {
        return pods.stream()
                .map(IPod::getName)
                .collect(Collectors.joining(","));
    }

    public static List<IPod> listRunningPods(IClient client) {
        return client.<IPod>list(ResourceKind.POD, namespace).stream()
                .filter(pod -> !pod.getName().endsWith("-deploy"))
                .filter(pod -> pod.getStatus().equals("Running"))
                .collect(Collectors.toList());
    }

    public static void waitForBrokerPod(IClient client, String address, TimeoutBudget budget) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("address", address);
        labels.put("role", "broker");


        int numReady = 0;
        while (budget.timeLeft() >= 0 && numReady != 1) {
            List<IPod> pods = client.list(ResourceKind.POD, namespace, labels);
            numReady = 0;
            for (IPod pod : pods) {
                if (pod.getStatus().equals("Running")) {
                    numReady++;
                }
            }
        }
        if (numReady != 1) {
            throw new IllegalStateException("Unable to find broker pod for " + address + " within timeout");
        }
    }

    public static void deploy(IClient client, TimeoutBudget budget, Destination ... destinations) throws Exception {
        KubernetesResource map = client.get(ResourceKind.CONFIG_MAP, "maas", namespace);

        ModelNode root = map.getNode();

        ModelNode config = new ModelNode();
        for (Destination destination : destinations) {
            ModelNode entry = new ModelNode();
            entry.get("store_and_forward").set(destination.isStoreAndForward());
            entry.get("multicast").set(destination.isMulticast());
            destination.getFlavor().ifPresent(e -> entry.get("flavor").set(e));
            config.get(destination.getAddress()).set(entry);
        }
        ModelNode json = new ModelNode();
        json.set("json", destinations.length > 0 ? config.toJSONString(false) : "{}");
        root.get("data").set(json);

        client.update(map);
        int expectedPods = 5;
        for (Destination destination : destinations) {
            if (destination.isStoreAndForward()) {
                waitForBrokerPod(client, destination.getAddress(), budget);
                if (!destination.isMulticast()) {
                    waitForAddress(destination.getAddress(), budget);
                }
                expectedPods++;
            }
        }
        waitForExpectedPods(client, expectedPods, budget);
    }

    public static void waitForAddress(String address, TimeoutBudget budget) throws Exception {
        ArrayNode root = mapper.createArrayNode();
        ObjectNode data = root.addObject();
        data.put("name", address);
        data.put("store-and-forward", true);
        data.put("multicast", false);
        String json = mapper.writeValueAsString(root);
        Message message = Message.Factory.create();
        message.setAddress("health-check");
        message.setSubject("health-check");
        message.setBody(new AmqpValue(json));

        int numConfigured = 0;
        List<IPod> agents = Environment.getClient().list(ResourceKind.POD, namespace, Collections.singletonMap("name", "ragent"));

        while (budget.timeLeft() >= 0 && numConfigured < agents.size()) {
            numConfigured = 0;
            for (IPod pod : agents) {
                SyncRequestClient client = new SyncRequestClient(pod.getIP(), pod.getContainerPorts().iterator().next().getContainerPort());
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
}
