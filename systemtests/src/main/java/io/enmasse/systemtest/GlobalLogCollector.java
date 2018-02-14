/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.fabric8.kubernetes.api.model.Event;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class GlobalLogCollector {
    private final Map<String, LogCollector> collectorMap = new HashMap<>();
    private final Kubernetes kubernetes;
    private final File logDir;
    private static Logger log = CustomLogger.getLogger();

    public GlobalLogCollector(Kubernetes kubernetes, File logDir) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
    }


    public synchronized void startCollecting(String namespace) {
        log.info("Start collecting logs for pods in namespace {}", namespace);
        collectorMap.put(namespace, new LogCollector(kubernetes, new File(logDir, namespace), namespace));
    }

    public synchronized void stopCollecting(String namespace) throws Exception {
        log.info("Stop collecting logs for pods in namespace {}", namespace);
        LogCollector collector = collectorMap.get(namespace);
        if (collector != null) {
            collector.close();
        }
        collectorMap.remove(namespace);
    }

    /**
     * Collect logs from terminated pods in namespace
     */
    public void collectLogsTerminatedPods(String namespace) {
        log.info("Store logs from all terminated pods in namespace '{}'", namespace);
        kubernetes.getLogsOfTerminatedPods(namespace).forEach((podName, podLogTerminated) -> {
            try {
                Path path = Paths.get(logDir.getPath(), namespace);
                File podLog = new File(
                        Files.createDirectories(path).toFile(),
                        namespace + "." + podName + ".terminated.log");
                log.info("log of terminated '{}' pod will be archived with path: '{}'",
                        podName,
                        path.toString());
                try (BufferedWriter bf = Files.newBufferedWriter(podLog.toPath())) {
                    bf.write(podLogTerminated);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void collectEvents(String namespace) {
        File eventLog = new File(logDir, namespace + ".events");
        try (FileWriter fileWriter = new FileWriter(eventLog)) {
            for (Event event : kubernetes.listEvents(namespace)) {
                fileWriter.write(event.toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
