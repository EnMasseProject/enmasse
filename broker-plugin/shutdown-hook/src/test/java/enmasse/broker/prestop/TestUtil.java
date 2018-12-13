/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import enmasse.discovery.Host;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestUtil {
    public static Host createHost(String hostname, int port) {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        portMap.put("amqp", port);
        portMap.put("core", port);
        return new Host(hostname, portMap);
    }
}
