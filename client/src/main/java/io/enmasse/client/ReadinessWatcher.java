/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.client.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ReadinessWatcher<T extends HasMetadata> implements Watcher<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadinessWatcher.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicReference<T> reference = new AtomicReference<>();

    private final T resource;
    private final EnmasseReadyOperationsImpl ops;

    public ReadinessWatcher(EnmasseReadyOperationsImpl ops, T resource) {
        this.resource = resource;
        this.ops = ops;
    }

    @Override
    public void eventReceived(Action action, T resource) {
        LOGGER.debug("{} {}", action, resource.getMetadata().getName());
        switch (action) {
            case MODIFIED:
                if (ops.isReady(resource)) {
                    reference.set(resource);
                    latch.countDown();
                }
                break;
            default:
        }
    }

    @Override
    public void onClose(KubernetesClientException e) {

    }

    public T await(long amount, TimeUnit timeUnit) {
        try {
            if (latch.await(amount, timeUnit)) {
                return reference.get();
            }
            throw new KubernetesClientTimeoutException(resource, amount, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KubernetesClientTimeoutException(resource, amount, timeUnit);
        }
    }
}

