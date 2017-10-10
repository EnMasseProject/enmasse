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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GlobalLogCollector {
    private final Map<String, LogCollector> collectorMap = new HashMap<>();
    private final OpenShift openShift;
    private final File logDir;

    public GlobalLogCollector(OpenShift openShift, File logDir) {
        this.openShift = openShift;
        this.logDir = logDir;
    }


    public synchronized void startCollecting(String namespace) {
        Logging.log.info("Start collecting logs for pods in namespace {}", namespace);
        collectorMap.put(namespace, new LogCollector(openShift, new File(logDir, namespace), namespace));
    }

    public synchronized void stopCollecting(String namespace) throws Exception {
        Logging.log.info("Stop collecting logs for pods in namespace {}", namespace);
        LogCollector collector = collectorMap.get(namespace);
        if (collector != null) {
            collector.close();
        }
        collectorMap.remove(namespace);
    }
}
