package enmasse.config.service.podsense;

import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.Resource;
import io.fabric8.kubernetes.api.model.*;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PodSenseMessageEncoderTest {
    @Test
    public void testEncode() throws IOException {
        MessageEncoder encoder = new PodSenseMessageEncoder();
        Message message = encoder.encode(createPods());

        assertNotNull(message);
        AmqpSequence seq = (AmqpSequence) message.getBody();
        List pods = seq.getValue();
        assertNotNull(pods);
        assertThat(pods.size(), is(2));

        assertPod((Map<String, Object>) pods.get(0), "192.168.0.1", 5672, "amqp");
        assertPod((Map<String, Object>) pods.get(1), "192.168.0.2", 5671, "amqps");
    }

    private static void assertPod(Map<String, Object> encodedPod, String expectedIp, int expectedPort, String expectedPortName) {
        assertThat(encodedPod.get("host"), is(expectedIp));
        Map<String, Map<String, Integer>> ports = (Map<String, Map<String, Integer>>) encodedPod.get("ports");
        assertPodPort(ports.get("c"), expectedPort, expectedPortName);
    }

    private static void assertPodPort(Map<String, Integer> ports, int expectedPort, String expectedPortName) {
        assertThat(ports.get(expectedPortName), is(expectedPort));
    }

    private static Set<Resource<Pod>> createPods() {
        Set<Resource<Pod>> pods = new LinkedHashSet<>();
        pods.add(createPod("p1", "192.168.0.1", "Running", Collections.singletonMap("amqp", 5672)));
        pods.add(createPod("p2", "192.168.0.2", "Running", Collections.singletonMap("amqps", 5671)));
        pods.add(createPod("p3", "192.168.0.3", "Terminating", Collections.singletonMap("http", 8080)));
        return pods;
    }

    private static Resource<Pod> createPod(String name, String ip, String phase, Map<String, Integer> portMap) {
        return new Resource<>(new PodBuilder()
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
                        .withPodIP(ip)
                        .withPhase(phase)
                        .build())
                .build());
    }

    private static List<ContainerPort> createPorts(Map<String, Integer> portMap) {
        return portMap.entrySet().stream()
                .map(e -> new ContainerPortBuilder()
                        .withName(e.getKey())
                        .withContainerPort(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }
}
