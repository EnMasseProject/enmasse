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

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * A verticle that handles watching a resource with the appropriate reconnect and retry logic,
 * which notifies a resource interface when things change.
 */
public class WatcherVerticle<T> extends AbstractVerticle implements io.fabric8.kubernetes.client.Watcher, Runnable {
    private static final Logger log = LoggerFactory.getLogger(WatcherVerticle.class.getName());
    private final Random random;
    private Watch watch;
    private final Resource<T> resource;
    private final Watcher<T> changeHandler;
    private final Thread watcherThread;
    private final BlockingQueue<Action> events = new LinkedBlockingDeque<>();
    private volatile boolean running;

    public WatcherVerticle(Resource<T> resource, Watcher<T> changeHandler) {
        this.random = new Random(System.currentTimeMillis());
        this.resource = resource;
        this.changeHandler = changeHandler;
        this.watcherThread = new Thread(this);
    }

    private long resyncInterval() {
        return 30000 + Math.abs(random.nextLong()) % 30000;
    }

    @Override
    public void start() {
        running = true;
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(resource.watchResources(this));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                log.debug("Watcher created, setting result and starting timer + watcher thread");
                watch = result.result();
                scheduleResync();
                watcherThread.start();
            } else {
                log.warn("Error starting watcher", result.cause());
                vertx.setTimer(10000, id -> start());
            }
        });
    }

    private void scheduleResync() {
        if (!running) {
            return;
        }
        vertx.executeBlocking(promise -> {
            try {
                log.debug("Putting event on queue");
                events.put(Action.ADDED);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }

        }, result -> {
            if (result.failed()) {
                log.warn("Error posting resync event", result.cause());
            }
            vertx.setTimer(resyncInterval(), id -> scheduleResync());
        });
    }

    @Override
    public void run() {
        while (running) {
            try {
                Action action = events.take();
                if (action != null) {
                    // TODO: Use action and resource instead of relisting
                    changeHandler.resourcesUpdated(resource.listResources());
                }
            } catch (Exception e) {
                log.warn("Exception doing resource update", e);
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        if (watch != null) {
            watch.close();
        }
        try {
            log.debug("Putting poison pill event");
            events.put(null);
        } catch (InterruptedException ignored) {
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
            log.info("Received onClose for addressspace config resource", cause);
            stop();
            start();
        } else {
            log.info("Watch for addressspace configs force closed, stopping");
            watch = null;
            stop();
        }
    }
}
