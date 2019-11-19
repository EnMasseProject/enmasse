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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Collects logs from all EnMasse components and saves them to a file
 */
public class LogCollector implements Watcher<Pod>, AutoCloseable {
    private static Logger log = CustomLogger.getLogger();
    private final Path logDir;
    private final Kubernetes kubernetes;
    private final Map<String, LogWatch> logWatches = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final String uuid;
    private Watch watch;

    public LogCollector(Kubernetes kubernetes, Path logDir, String uuid) throws IOException {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
        this.uuid = uuid;
        Files.createDirectories(logDir);
        this.watch = kubernetes.watchPods(uuid, this);
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        switch (action) {
            case MODIFIED:
            case ADDED:
                executorService.execute(() -> collectLogs(pod));
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
            } catch (InterruptedException e) {

            }
            pod = kubernetes.getPod(pod.getMetadata().getName());
        }
        log.info("Collecting logs for pod {} with uuid {}", pod.getMetadata().getName(), uuid);
        for (Container container : pod.getSpec().getContainers()) {
            Path outputFile = logDir.resolve(pod.getMetadata().getName() + "." + container.getName());
            try {
                OutputStream outputFileStream = Files.newOutputStream(outputFile);

                synchronized (logWatches) {
                    logWatches.put(pod.getMetadata().getName(), kubernetes.watchPodLog(pod.getMetadata().getName(), container.getName(), outputFileStream));
                }
            } catch (Exception e) {
                log.info("Unable to save log for {}", outputFile, e);
            }
        }
    }

    @Override
    public void close() throws Exception {
        watch.close();
        executorService.shutdown();
        synchronized (logWatches) {
            for (LogWatch watch : logWatches.values()) {
                watch.close();
            }
        }
    }
}
