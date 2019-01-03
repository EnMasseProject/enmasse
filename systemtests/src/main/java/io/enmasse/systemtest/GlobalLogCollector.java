/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GlobalLogCollector {
    private static Logger log = CustomLogger.getLogger();
    private final Map<String, LogCollector> collectorMap = new HashMap<>();
    private final Kubernetes kubernetes;
    private final File logDir;
    private final String namespace;

    public GlobalLogCollector(Kubernetes kubernetes, File logDir) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
        this.namespace = kubernetes.getNamespace();
    }


    public synchronized void startCollecting(AddressSpace addressSpace) {
        log.info("Start collecting logs for address space {}", addressSpace);
        collectorMap.put(addressSpace.getInfraUuid(), new LogCollector(kubernetes, new File(logDir, addressSpace.getInfraUuid()), addressSpace.getInfraUuid()));
    }

    public synchronized void stopCollecting(String namespace) throws Exception {
        log.info("Stop collecting logs for pods in namespace {}", namespace);
        LogCollector collector = collectorMap.get(namespace);
        if (collector != null) {
            collector.close();
        }
        collectorMap.remove(namespace);
    }

    public void collectConfigMaps() {
        collectConfigMaps("global");
    }

    public void collectConfigMaps(String operation) {
        log.info("Collecting configmaps for namespace {}", namespace);
        kubernetes.getAllConfigMaps(namespace).getItems().forEach(configMap -> {
            try {
                Path path = Paths.get(logDir.getPath(), namespace);
                File confMapFile = new File(
                        Files.createDirectories(path).toFile(),
                        configMap.getMetadata().getName() + "." + operation + ".configmap");
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
    public void collectLogsTerminatedPods() {
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

    public void collectEvents() {
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

    public void collectRouterState(String operation) {
        log.info("Collecting router state in namespace {}", namespace);
        long timestamp = System.currentTimeMillis();
        kubernetes.listPods(Collections.singletonMap("capability", "router")).forEach(pod -> {
            collectRouterInfo(pod, "." + operation + ".autolinks." + timestamp, "qdmanage", "QUERY", "--type=autoLink");
            collectRouterInfo(pod, "." + operation + ".links." + timestamp, "qdmanage", "QUERY", "--type=link");
            collectRouterInfo(pod, "." + operation + ".connections." + timestamp, "qdmanage", "QUERY", "--type=connection");
            collectRouterInfo(pod, "." + operation + ".qdstat_a." + timestamp, "qdstat", "-a");
            collectRouterInfo(pod, "." + operation + ".qdstat_l." + timestamp, "qdstat", "-l");
            collectRouterInfo(pod, "." + operation + ".qdstat_n." + timestamp, "qdstat", "-n");
            collectRouterInfo(pod, "." + operation + ".qdstat_c." + timestamp, "qdstat", "-c");
        });
    }

    private void collectRouterInfo(Pod pod, String filesuffix, String... command) {
        String output = kubernetes.runOnPod(pod, "router", command);
        try {
            Path path = Paths.get(logDir.getPath(), namespace);
            File routerAutoLinks = new File(
                    Files.createDirectories(path).toFile(),
                    pod.getMetadata().getName() + filesuffix);
            if (!routerAutoLinks.exists()) {
                try (BufferedWriter bf = Files.newBufferedWriter(routerAutoLinks.toPath())) {
                    bf.write(output);
                }
            }
        } catch (IOException e) {
            log.warn("Error collecting router state: {}", e.getMessage());
        }
    }

    public void collectApiServerJmapLog() {
        log.info("Collecting jmap from api server");
        kubernetes.listPods(Collections.singletonMap("component", "api-server")).forEach(this::collectJmap);
    }

    private void collectJmap(Pod pod) {
        String output = kubernetes.runOnPod(pod, "api-server", "jmap", "-dump:live,format=b,file=/tmp/dump.bin", "1");
        try {
            Path path = Paths.get(logDir.getPath(), namespace);
            File jmapLog = new File(
                    Files.createDirectories(path).toFile(),
                    pod.getMetadata().getName() + ".dump." + Instant.now() + ".bin");
            KubeCMDClient.copyPodContent(pod.getMetadata().getName(), "/tmp/dump.bin", jmapLog.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Error collecting jmap state: {}", e.getMessage());
        }
    }
}
