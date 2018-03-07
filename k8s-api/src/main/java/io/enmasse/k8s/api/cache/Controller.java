/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.enmasse.k8s.api.Watch;

public class Controller implements Watch {
    private final Reflector reflector;
    private volatile boolean running;
    private Thread thread;

    public Controller(Reflector reflector) {
        this.reflector = reflector;
    }

    public void start() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                reflector.run();
            }
        });
        thread.start();
    }

    public void stop() throws InterruptedException {
        running = false;
        reflector.shutdown();
        thread.interrupt();
        thread.join();
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
