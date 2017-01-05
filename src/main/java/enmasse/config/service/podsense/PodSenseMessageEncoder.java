package enmasse.config.service.podsense;

import enmasse.config.service.openshift.MessageEncoder;
import io.fabric8.kubernetes.api.model.Pod;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.*;

/**
 * Encodes podsense responses
 */
public class PodSenseMessageEncoder implements MessageEncoder<PodResource> {

    @Override
    public Message encode(Set<PodResource> set) throws IOException {
        Message message = Message.Factory.create();
        List<Map<String, Object>> root = new ArrayList<>();

        set.forEach(pod -> root.add(encodePod(pod)));

        message.setBody(new AmqpValue(root));
        return message;
    }

    private Map<String, Object> encodePod(PodResource pod) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("host", pod.getHost());
        map.put("phase", pod.getPhase());
        map.put("ready", pod.getReady());
        map.put("ports", pod.getPortMap());
        return map;
    }


}
