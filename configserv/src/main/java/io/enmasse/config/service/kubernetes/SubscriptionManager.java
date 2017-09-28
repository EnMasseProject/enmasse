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

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.model.Resource;
import io.enmasse.config.service.model.Subscriber;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages subscribers for a given set of OpenShift resources.
 */
public class SubscriptionManager<T extends Resource> {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class.getName());

    private final List<Subscriber> subscriberList = new ArrayList<>();
    private final Set<T> resources = new LinkedHashSet<>();
    private final MessageEncoder<T> messageEncoder;
    private final Predicate<T> resourceFilter;

    public SubscriptionManager(MessageEncoder<T> messageEncoder, Predicate<T> resourceFilter) {
        this.messageEncoder = messageEncoder;
        this.resourceFilter = resourceFilter;
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
        log.info("Notifying subscribers with updated resources: " + resources);
        Optional<Message> message = encodeAndLog();
        message.ifPresent(m -> subscriberList.forEach(s -> s.resourcesUpdated(m)));
    }

    private Optional<Message> encodeAndLog() {
        Set<T> set = Collections.unmodifiableSet(resources);
        try {
            return Optional.of(messageEncoder.encode(set));
        } catch (IOException e) {
            log.warn("Error encoding message", e);
            return Optional.empty();
        }
    }

    public synchronized void resourcesUpdated(Set<T> updated) {
        Set<T> filtered = updated.stream()
                .filter(resourceFilter)
                .collect(Collectors.toSet());

        if (!filtered.equals(resources)) {
            resources.clear();
            resources.addAll(filtered);
            notifySubscribers();
        }
    }
}
