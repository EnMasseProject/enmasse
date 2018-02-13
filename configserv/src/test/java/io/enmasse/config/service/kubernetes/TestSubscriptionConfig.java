/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.TestResource;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.k8s.api.Resource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestSubscriptionConfig implements SubscriptionConfig<TestResource> {

    private final TestWatch testWatch;
    private final BlockingQueue<Set<TestResource>> stream;

    public TestSubscriptionConfig(TestWatch testWatch, BlockingQueue<Set<TestResource>> stream) {
        this.testWatch = testWatch;
        this.stream = stream;
    }

    @Override
    public MessageEncoder<TestResource> getMessageEncoder() {
        return set -> {
            Message message = Message.Factory.create();

            message.setBody(new AmqpValue(set.stream().map(TestResource::getValue).collect(Collectors.toList())));
            return message;
        };
    }

    @Override
    public Resource<TestResource> getResource(ObserverKey observerKey, KubernetesClient client) {
        return new Resource<TestResource>() {
            @Override
            public Watch watchResources(Watcher watcher) {
                return testWatch;
            }

            @Override
            public Set<TestResource> listResources() {
                try {
                    return stream.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public Predicate<TestResource> getResourceFilter() {
        return TestResource -> true;
    }

    public static class TestWatch implements Watch {
        public volatile boolean isClosed = false;

        @Override
        public void close() {
            isClosed = true;
        }
    }
}
