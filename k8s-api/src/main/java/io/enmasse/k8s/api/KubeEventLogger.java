/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.k8s.api;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;

public class KubeEventLogger implements EventLogger {
    private static final Logger log = LoggerFactory.getLogger(KubeEventLogger.class);
    private final KubernetesClient kubeClient;
    private final String namespace;
    private final Clock clock;
    private final String componentName;

    public KubeEventLogger(KubernetesClient kubeClient, String namespace, Clock clock, String componentName) {
        this.kubeClient = kubeClient;
        this.namespace = namespace;
        this.clock = clock;
        this.componentName = componentName;
    }

    @Override
    public void log(Reason reason, String message, Type type, ObjectKind objectKind, String objectName) {
        String eventName = componentName + "." + (reason + message + type + objectKind + objectName).hashCode();
        Event existing = kubeClient.events().inNamespace(namespace).withName(eventName).get();
        String timestamp = Instant.now(clock).toString();
        try {
            if (existing != null && existing.getType().equals(type.name()) && existing.getReason().equals(reason.name()) && existing.getInvolvedObject().getName().equals(objectName) && existing.getInvolvedObject().getKind().equals(objectKind.name())) {
                existing.setCount(existing.getCount() + 1);
                existing.setLastTimestamp(timestamp);
                kubeClient.events().inNamespace(namespace).withName(eventName).replace(existing);
            } else {
                Event newEvent = new EventBuilder()
                        .withNewMetadata()
                        .withName(eventName)
                        .endMetadata()
                        .withCount(1)
                        .withReason(reason.name())
                        .withMessage(message)
                        .withType(type.name())
                        .withNewInvolvedObject()
                        .withNamespace(namespace)
                        .withKind(objectKind.name())
                        .withName(objectName)
                        .endInvolvedObject()
                        .withFirstTimestamp(timestamp)
                        .withLastTimestamp(timestamp)
                        .withNewSource()
                        .withComponent(componentName)
                        .endSource()
                        .build();
                kubeClient.events().inNamespace(namespace).withName(eventName).create(newEvent);
            }
        } catch (KubernetesClientException e) {
            log.warn("Error reporting event", e);
        }
    }
}
