/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.podsense;

import io.enmasse.config.service.kubernetes.MessageEncoder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.io.IOException;
import java.util.*;

/**
 * Encodes podsense responses
 */
public class PodSenseMessageEncoder implements MessageEncoder<Pod> {

    @Override
    public Message encode(Set<Pod> set) throws IOException {
        Message message = Message.Factory.create();
        List<Map<String, Object>> root = new ArrayList<>();

        set.forEach(pod -> root.add(encodePod(pod)));

        message.setBody(new AmqpValue(root));
        return message;
    }

    private Map<String, Object> encodePod(Pod pod) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", pod.getName());
        map.put("host", pod.getHost());
        map.put("phase", pod.getPhase());
        map.put("ready", pod.getReady());
        map.put("ports", pod.getPortMap());
        return map;
    }


}
