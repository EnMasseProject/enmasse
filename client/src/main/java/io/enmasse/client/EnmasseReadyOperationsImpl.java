/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.client;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;

import java.util.concurrent.TimeUnit;

/**
 * Encapsulate the overriding of readiness-related methods
 *
 * @param <T> The resource type
 * @param <L> The resource list type
 * @param <D> The doneable type
 * @param <R> The resource operations
 */
public abstract class EnmasseReadyOperationsImpl<
        T extends HasMetadata,
        L extends KubernetesResourceList,
        D extends Doneable<T>,
        R extends Resource<T, D>>
        extends HasMetadataOperation<T, L, D, R>
        implements Resource<T, D> {

    public EnmasseReadyOperationsImpl(OperationContext ctx) {
        super(ctx.withCascading(true));
    }

    protected abstract boolean isReady(T resource);

    @Override
    public Boolean isReady() {
        return isReady(get());
    }

    @Override
    protected T periodicWatchUntilReady(int i, long started, long interval, long amount) {
        T item = fromServer().get();
        if (isReady(item)) {
            return item;
        }

        ReadinessWatcher<T> watcher = new ReadinessWatcher<>(this, item);
        try (Watch watch = watch(item.getMetadata().getResourceVersion(), watcher)) {
            try {
                return watcher.await(interval, TimeUnit.NANOSECONDS);
            } catch (KubernetesClientTimeoutException e) {
                if (i <= 0) {
                    throw e;
                }
            }

            long remaining = (started + amount) - System.nanoTime();
            long next = Math.max(0, Math.min(remaining, interval));
            return periodicWatchUntilReady(i - 1, started, next, amount);
        }
    }

    @Override
    public T waitUntilReady(long amount, TimeUnit timeUnit) throws InterruptedException {

        long startTime = System.currentTimeMillis();
        long deadline = startTime + timeUnit.toMillis(amount);
        T item = null;
        do {
            item = get();
            if (item != null) {
                break;
            }

            // in the future, this should probably be more intelligent
            Thread.sleep(1000);
        } while (System.currentTimeMillis() < deadline);
        if (item == null) {
            throw new KubernetesClientException("");
        }
        if (isReady(item)) {
            return item;
        }
        ReadinessWatcher<T> watcher = new ReadinessWatcher<>(this, item);
        try (Watch watch = watch(watcher)) {

            long taken = System.currentTimeMillis() - startTime;
            long remaining = timeUnit.toMillis(amount) - taken;
            T await = watcher.await(remaining, TimeUnit.MILLISECONDS);
            if (await != null) {
                return await;
            } else {
                throw new KubernetesClientTimeoutException(item, remaining, TimeUnit.NANOSECONDS);
            }
        }

    }
}
