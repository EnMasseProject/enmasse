/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A verticle that handles watching a resource with the appropriate reconnect and retry logic,
 * which notifies a resource interface when things change.
 */
public class ResourceController<T> implements io.fabric8.kubernetes.client.Watcher, Runnable {
    private static final Logger log = LoggerFactory.getLogger(ResourceController.class.getName());
    private Watch watch;
    private final Resource<T> resource;
    private final Watcher<T> changeHandler;
    private Thread watcherThread;
    private final BlockingQueue<Action> events = new LinkedBlockingDeque<>();
    private volatile boolean running;
    private final Supplier<Long> resyncSupplier;
    private final boolean useEventLoop;

    ResourceController(Resource<T> resource, Watcher<T> changeHandler, Supplier<Long> resyncSupplier, boolean useEventLoop) {
        this.resource = resource;
        this.changeHandler = changeHandler;
        this.resyncSupplier = resyncSupplier;
        this.useEventLoop = useEventLoop;
    }

    public static <T> ResourceController<T> create(Resource<T> resource, Watcher<T> changeHandler) {
        Random random = new Random(System.currentTimeMillis());
        return new ResourceController<>(resource, changeHandler, () -> 10000 + Math.abs(random.nextLong()) % 5000, true);
    }

    public static <T> ResourceController<T> create(Resource<T> resource, Watcher<T> changeHandler, boolean useEventLoop) {
        Random random = new Random(System.currentTimeMillis());
        return new ResourceController<>(resource, changeHandler, () -> 10000 + Math.abs(random.nextLong()) % 5000, false);
    }

    public void start() {
        running = true;
        events.add(Action.ADDED);
        watcherThread = new Thread(this);
        watcherThread.start();
    }

    @Override
    public void run() {
        while (running) {
            try {
                if (useEventLoop) {
                    if (watch == null) {
                        watch = resource.watchResources(this);
                    }
                    events.poll(resyncSupplier.get(), TimeUnit.MILLISECONDS);
                } else {
                    Thread.sleep(resyncSupplier.get());
                }

                if (running) {
                    // TODO: Use action and resource instead of relisting
                    changeHandler.resourcesUpdated(resource.listResources());
                }
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (Exception e) {
                log.warn("Exception doing resource update", e);
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (useEventLoop) {
                log.debug("Putting poison pill event");
                if (watch != null) {
                    watch.close();
                }
                events.put(Action.ERROR);
            } else {
                watcherThread.interrupt();
            }

            watcherThread.join();
            watcherThread = null;
            watch = null;
        } catch (InterruptedException ignored) {
            log.warn("Interrupted while stopping", ignored);
        }
    }

    @Override
    public void eventReceived(Action action, Object obj) {
        if (action.equals(Action.ERROR)) {
            log.error("Got error event while watching resource " + obj);
            return;
        }

        switch (action) {
            case ADDED:
            case DELETED:
                try {
                    log.debug("Putting action {} on queue", action);
                    events.put(action);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while posting event", e);
                }
                break;
            case MODIFIED:
                break;
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for address space config resource", cause);
            stop();
            start();
        } else {
            log.info("Watch for address space configs force closed, stopping");
            watch = null;
            stop();
        }
    }
}
