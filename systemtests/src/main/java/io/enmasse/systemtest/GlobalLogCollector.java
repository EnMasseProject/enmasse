/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GlobalLogCollector {
    private static Logger log = CustomLogger.getLogger();
    private final Map<String, LogCollector> collectorMap = new HashMap<>();
    private final Kubernetes kubernetes;
    private final File logDir;

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

    public void collectConfigMaps(String namespace) {
        kubernetes.getAllConfigMaps(namespace).getItems().forEach(configMap -> {
            try {
                Path path = Paths.get(logDir.getPath(), namespace);
                File confMapFile = new File(
                        Files.createDirectories(path).toFile(),
                        configMap.getMetadata().getName() + ".configmap");
                if (!confMapFile.exists()) {
                    try (BufferedWriter bf = Files.newBufferedWriter(confMapFile.toPath())) {
                        bf.write(configMap.toString());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
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
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("kubectl", "get", "events", "-n", namespace);
        processBuilder.redirectErrorStream(true);
        Process process;
        log.info("Collecting events in {}", namespace);

        File eventLog = new File(logDir, namespace + ".events");
        try (FileWriter fileWriter = new FileWriter(eventLog)) {
            process = processBuilder.start();
            InputStream stdout = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String line = null;
            while ((line = reader.readLine()) != null) {
                fileWriter.write(line);
                fileWriter.write("\n");
            }
            reader.close();
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                throw new RuntimeException("Command timed out");
            }
        } catch (Exception e) {
            log.error("Error collecting events for {}", namespace, e);
        }
    }
}
