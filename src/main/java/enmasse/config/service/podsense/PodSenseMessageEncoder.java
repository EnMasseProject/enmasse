package enmasse.config.service.podsense;

import enmasse.config.service.openshift.MessageEncoder;
import enmasse.config.service.openshift.Resource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.*;

/**
 * Encodes podsense responses
 */
public class PodSenseMessageEncoder implements MessageEncoder<Pod> {

    @Override
    public Message encode(Set<Resource<Pod>> set) throws IOException {
        Message message = Message.Factory.create();
        List<Map<String, Object>> root = new ArrayList<>();

        set.stream()
                .map(m -> m.getResource())
                .filter(pod -> "Running".equals(pod.getStatus().getPhase()))
                .forEach(pod -> root.add(encodePod(pod)));

        message.setBody(new AmqpSequence(root));
        return message;
    }

    private Map<String, Object> encodePod(Pod pod) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("ip", pod.getStatus().getPodIP());
        map.put("ports", encodePorts(pod.getSpec().getContainers()));
        return map;
    }

    private Map<Integer, String> encodePorts(List<Container> containers) {
        Map<Integer, String> ports = new LinkedHashMap<>();
        for (Container container : containers) {
            for (ContainerPort port : container.getPorts()) {
                ports.put(port.getContainerPort(), port.getName());
            }
        }
        return ports;
    }
}
