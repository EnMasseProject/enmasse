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

package enmasse.config.service.openshift;

import enmasse.config.service.model.Resource;
import enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Manages subscribers for a given set of OpenShift resources.
 */
public class OpenshiftResourceListener {
    private static final Logger log = LoggerFactory.getLogger(OpenshiftResourceListener.class.getName());

    private final List<Subscriber> subscriberList = new ArrayList<>();
    private final Set<Resource<? extends HasMetadata>> resources = new LinkedHashSet<>();
    private final MessageEncoder messageEncoder;

    public OpenshiftResourceListener(MessageEncoder messageEncoder) {
        this.messageEncoder = messageEncoder;
    }

    /**
     * Subscribe for updates.
     *
     * @param subscriber The subscriber handle.
     */
    public synchronized void subscribe(Subscriber subscriber) {
        subscriberList.add(subscriber);
        // Notify only when we have values
        if (!resources.isEmpty()) {
            Optional<Message> message = encodeAndLog();
            message.ifPresent(subscriber::resourcesUpdated);
        }
    }

    /**
     * Notify subscribers that the set of configs has been updated.
     */
    private void notifySubscribers() {
        Optional<Message> message = encodeAndLog();
        message.ifPresent(m -> subscriberList.forEach(s -> s.resourcesUpdated(m)));
    }

    private Optional<Message> encodeAndLog() {
        Set<Resource<? extends HasMetadata>> set = Collections.unmodifiableSet(resources);
        try {
            return Optional.of(messageEncoder.encode(set));
        } catch (IOException e) {
            log.warn("Error encoding message", e);
            return Optional.empty();
        }
    }

    public synchronized void resourcesUpdated(Set<Resource<? extends HasMetadata>> updated) {
        if (!updated.equals(resources)) {
            resources.clear();
            resources.addAll(updated);
            notifySubscribers();
        }
    }
}
