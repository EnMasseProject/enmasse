/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.logs;

import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Collects logs from all EnMasse components and saves them to a file
 */
public class LogCollector implements Watcher<Pod>, AutoCloseable {
    private static Logger log = CustomLogger.getLogger();
    private final File logDir;
    private final Kubernetes kubernetes;
    private final Map<String, LogWatch> logWatches = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final String uuid;
    private Watch watch;

    LogCollector(Kubernetes kubernetes, File logDir, String uuid) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
        this.uuid = uuid;
        logDir.mkdirs();
        this.watch = kubernetes.watchPods(uuid, this);
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        switch (action) {
            case MODIFIED:
            case ADDED:
                executorService.execute(() -> collectLogs(pod));
                break;
            default:
                CustomLogger.getLogger().error("Not supported action");
                break;
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("LogCollector closed with message: " + cause.getMessage() + ", reconnecting");
            watch = kubernetes.watchPods(uuid, this);
        }
    }

    private void collectLogs(Pod pod) {
        if (logWatches.containsKey(pod.getMetadata().getName())) {
            return;
        }
        while (!"Running".equals(pod.getStatus().getPhase())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {

            }
            pod = kubernetes.getPod(pod.getMetadata().getName());
        }
        log.info("Collecting logs for pod {} with uuid {}", pod.getMetadata().getName(), uuid);
        for (Container container : pod.getSpec().getContainers()) {
            try {
                File outputFile = new File(logDir, pod.getMetadata().getName() + "." + container.getName());
                FileOutputStream outputFileStream = new FileOutputStream(outputFile);

                synchronized (logWatches) {
                    logWatches.put(pod.getMetadata().getName(), kubernetes.watchPodLog(
                            pod.getMetadata().getName(), container.getName(), outputFileStream));
                }
            } catch (Exception e) {
                log.info("Unable to save log for " + pod.getMetadata().getName() + "." + container.getName());
            }
        }
    }

    @Override
    public void close() {
        watch.close();
        executorService.shutdown();
        synchronized (logWatches) {
            for (LogWatch watch : logWatches.values()) {
                watch.close();
            }
        }
    }
}
