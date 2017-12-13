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
package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.LogWatch;

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
    private final File logDir;
    private final Kubernetes kubernetes;
    private Watch watch;
    private final Map<String, LogWatch> logWatches = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final String namespace;

    public LogCollector(Kubernetes kubernetes, File logDir, String namespace) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
        this.namespace = namespace;
        logDir.mkdirs();
        this.watch = kubernetes.watchPods(namespace, this);
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
            Logging.log.info("LogCollector closed with message: " + cause.getMessage() + ", reconnecting");
            watch = kubernetes.watchPods(namespace, this);
        }
    }

    private void collectLogs(Pod pod) {
        if (logWatches.containsKey(pod.getMetadata().getName())) {
            return;
        }
        Logging.log.info("Waiting for pod {} to start", pod.getMetadata().getName());
        while (!"Running".equals(pod.getStatus().getPhase())) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            pod = kubernetes.getPod(namespace, pod.getMetadata().getName());
        }
        Logging.log.info("Collecting logs for pod {} in namespace {}", pod.getMetadata().getName(), namespace);
        for (Container container : pod.getSpec().getContainers()) {
            try {
                File outputFile = new File(logDir, pod.getMetadata().getName() + "." + container.getName());
                FileOutputStream outputFileStream = new FileOutputStream(outputFile);

                synchronized (logWatches) {
                    Logging.log.info("Starting watcher for container {} in pod {} and writing to {}", container.getName(), pod.getMetadata().getName(), outputFile.getAbsolutePath());
                    logWatches.put(pod.getMetadata().getName(), kubernetes.watchPodLog(namespace, pod.getMetadata().getName(), container.getName(), outputFileStream));
                }
            } catch (Exception e) {
                Logging.log.info("Unable to save log for " + pod.getMetadata().getName() + "." + container.getName());
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
