/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.RouterManagement;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.enmasse.controller.standard.ControllerKind.AddressSpace;
import static io.enmasse.controller.standard.ControllerReason.RouterCheckFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

public class RouterStatusCache implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusCache.class);
    private final RouterManagement routerManagement;
    private final Kubernetes kubernetes;
    private final EventLogger eventLogger;
    private final String addressSpace;

    private volatile boolean running = false;
    private Thread thread;
    private final Duration checkInterval;
    private final Object monitor = new Object();
    private boolean needCheck = false;

    private AtomicInteger routerCheckFailures = new AtomicInteger(0);
    private volatile boolean checkRouterLinks = false;
    private volatile List<RouterStatus> latestResult = Collections.emptyList();

    RouterStatusCache(RouterManagement routerManagement, Kubernetes kubernetes, EventLogger eventLogger, String addressSpace, Duration checkInterval)
    {
        this.routerManagement = routerManagement;
        this.kubernetes = kubernetes;
        this.eventLogger = eventLogger;
        this.addressSpace = addressSpace;
        this.checkInterval = checkInterval;
    }

    List<RouterStatus> getLatestResults() {
        return latestResult;
    }

    void checkRouterStatus() {
        RouterStatusCollector routerStatusCollector = new RouterStatusCollector(routerManagement, checkRouterLinks);
        List<Pod> routers = kubernetes.listRouters().stream()
                .filter(Readiness::isPodReady)
                .collect(Collectors.toList());

        log.info("Collecting status from {} routers", routers.size());

        ExecutorCompletionService<RouterStatus> service = new ExecutorCompletionService<>(ForkJoinPool.commonPool());
        for (Pod router : routers) {
            service.submit(() -> routerStatusCollector.collect(router));
        }
        List<RouterStatus> routerStatusList = new ArrayList<>(routers.size());
        for (int i = 0; i < routers.size(); i++) {
            try {
                RouterStatus status = service.take().get();
                if (status != null) {
                    routerStatusList.add(status);
                }
            } catch (Exception e) {
                log.info("Error requesting router status. Ignoring", e);
                eventLogger.log(RouterCheckFailed, e.getMessage(), Warning, AddressSpace, addressSpace);
                routerCheckFailures.incrementAndGet();
            }
        }
        this.latestResult = routerStatusList;
    }

    public void setCheckRouterLinks(boolean checkRouterLinks) {
        this.checkRouterLinks = checkRouterLinks;
    }

    public int getRouterCheckFailures() {
        return routerCheckFailures.get();
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.setName("router-status-collector");
        thread.setDaemon(true);
        thread.start();
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
    public void run() {
        while (running) {
            try {
                checkRouterStatus();
                synchronized (monitor) {
                    if (!needCheck) {
                        monitor.wait(checkInterval.toMillis());
                    }
                    needCheck = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("Exception in collector task", e);
            }
        }
    }

    public void wakeup() {
        synchronized (monitor) {
            needCheck = true;
            monitor.notifyAll();
        }
    }
}
