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

import io.fabric8.kubernetes.api.model.Event;

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

    public GlobalLogCollector(Kubernetes kubernetes, File logDir) {
        this.kubernetes = kubernetes;
        this.logDir = logDir;
    }


    public synchronized void startCollecting(String namespace) {
        Logging.log.info("Start collecting logs for pods in namespace {}", namespace);
        collectorMap.put(namespace, new LogCollector(kubernetes, new File(logDir, namespace), namespace));
    }

    public synchronized void stopCollecting(String namespace) throws Exception {
        Logging.log.info("Stop collecting logs for pods in namespace {}", namespace);
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
        Logging.log.info("Store logs from all terminated pods in namespace '{}'", namespace);
        kubernetes.getLogsOfTerminatedPods(namespace).forEach((podName, podLogTerminated) -> {
            try {
                Path path = Paths.get(logDir.getPath(), namespace);
                File podLog = new File(
                        Files.createDirectories(path).toFile(),
                        namespace + "." + podName + ".terminated.log");
                Logging.log.info("log of terminated '{}' pod will be archived with path: '{}'",
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
