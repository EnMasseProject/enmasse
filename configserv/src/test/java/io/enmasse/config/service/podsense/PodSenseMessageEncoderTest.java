/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.podsense;

import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.fabric8.kubernetes.api.model.*;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@SuppressWarnings("unchecked")
public class PodSenseMessageEncoderTest {
    @Test
    public void testEncode() throws IOException {
        MessageEncoder<Pod> encoder = new PodSenseMessageEncoder();
        Message message = encoder.encode(createPods());

        assertNotNull(message);
        AmqpValue seq = (AmqpValue) message.getBody();
        List pods = (List) seq.getValue();
        assertNotNull(pods);
        assertThat(pods.size(), is(2));

        assertPod((Map<String, Object>) pods.get(0), "p1", "192.168.0.1", 5672, "amqp");
        assertPod((Map<String, Object>) pods.get(1), "p2", "192.168.0.2", 5671, "amqps");
    }

    private static void assertPod(Map<String, Object> encodedPod, String expectedName, String expectedIp, int expectedPort, String expectedPortName) {
        assertThat(encodedPod.get("name"), is(expectedName));
        assertThat(encodedPod.get("host"), is(expectedIp));
        assertThat(encodedPod.get("phase"), is("Running"));
        assertThat(encodedPod.get("ready"), is("False"));
        Map<String, Map<String, Integer>> ports = (Map<String, Map<String, Integer>>) encodedPod.get("ports");
        assertPodPort(ports.get("c"), expectedPort, expectedPortName);
    }

    private static void assertPodPort(Map<String, Integer> ports, int expectedPort, String expectedPortName) {
        assertThat(ports.get(expectedPortName), is(expectedPort));
    }

    private static Set<Pod> createPods() {
        Set<Pod> pods = new LinkedHashSet<>();
        pods.add(createPod("p1", "192.168.0.1", "Running", Collections.singletonMap("amqp", 5672)));
        pods.add(createPod("p2", "192.168.0.2", "Running", Collections.singletonMap("amqps", 5671)));
        return pods;
    }

    static Pod createPod(String name, String ip, String phase, Map<String, Integer> portMap) {
        return new Pod(new PodBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .addToLabels("my", "label")
                        .build())
                .withSpec(new PodSpecBuilder()
                        .withContainers(new ContainerBuilder()
                                .withName("c")
                                .withPorts(createPorts(portMap))
                                .build())
                        .build())
                .withStatus(new PodStatusBuilder()
                        .withConditions(new PodConditionBuilder()
                                .withType("Ready")
                                .withStatus("False")
                                .build())
                        .withPodIP(ip)
                        .withPhase(phase)
                        .build())
                .build());
    }

    static List<ContainerPort> createPorts(Map<String, Integer> portMap) {
        return portMap.entrySet().stream()
                .map(e -> new ContainerPortBuilder()
                        .withName(e.getKey())
                        .withContainerPort(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
