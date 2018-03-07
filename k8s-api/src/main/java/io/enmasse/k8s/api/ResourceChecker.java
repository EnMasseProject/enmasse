/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResourceChecker<T> implements Watcher<T>, Runnable {
    private static final Logger log = LoggerFactory.getLogger(ResourceChecker.class.getName());
    private final Watcher<T> watcher;
    private final Duration recheckInterval;
    private final Object monitor = new Object();
    private Set<T> items = new HashSet<>();
    private volatile boolean running = false;

    private Thread thread;

    public ResourceChecker(Watcher<T> watcher, Duration recheckInterval) {
        this.watcher = watcher;
        this.recheckInterval = recheckInterval;
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();

    }

    @Override
    public void run() {
        while (running) {
            doWork();
        }
    }

    void doWork() {
        synchronized (monitor) {
            try {
                monitor.wait(recheckInterval.toMillis());
                watcher.onUpdate(items);
                log.debug("Woke up from monitor");
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (Exception e) {
                log.warn("Exception in checker task", e);
            }
        }
    }

    public void stop() {
        try {
            running = false;
            thread.interrupt();
            thread.join();
        } catch (InterruptedException ignored) {
            log.warn("Interrupted while stopping", ignored);
        }
    }

    @Override
    public void onUpdate(Set<T> items) {
        this.items = Collections.unmodifiableSet(new HashSet<>(items));
        synchronized (monitor) {
            monitor.notifyAll();
        }
    }
}
