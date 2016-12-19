/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main entry point for topic forwarder.
 */
public class Main {

    public static void main(String [] args) throws IOException, InterruptedException {
        Map<String, String> env = System.getenv();
        Map<String, String> labelFilter = getLabelFilter(env);
        Host localHost = getLocalHost();
        String address = getAddress(env);
        Endpoint podsenseService = getPodsenseService(env);

        DiscoveryClient discoveryClient = new DiscoveryClient(podsenseService, labelFilter);
        ForwarderController replicator = new ForwarderController(localHost, address);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(replicator, result -> {
            if (result.succeeded()) {
                vertx.deployVerticle(discoveryClient);
            }
        });
        discoveryClient.addListener(replicator);

        discoveryClient.start();
    }

    private static Endpoint getPodsenseService(Map<String, String> env) {
        if (env.containsKey("ADMIN_SERVICE_HOST")) {
            return new Endpoint(getEnvOrThrow(env, "ADMIN_SERVICE_HOST"), Integer.parseInt(getEnvOrThrow(env, "ADMIN_SERVICE_PORT_CONFIGSERV")));
        } else {
            return new Endpoint(getEnvOrThrow(env, "CONFIGURATION_SERVICE_HOST"), Integer.parseInt(getEnvOrThrow(env, "CONFIGURATION_SERVICE_PORT")));
        }
    }

    private static String getAddress(Map<String, String> env) {
        return getEnvOrThrow(env, "TOPIC_NAME");
    }

    private static Host getLocalHost() throws UnknownHostException {
        return new Host(Inet4Address.getLocalHost().getHostAddress(), Collections.singletonMap("amqp", 5673));
    }

    private static Map<String,String> getLabelFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("role", "broker");
        labelMap.put("address", getAddress(env));
        return labelMap;
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }
}
