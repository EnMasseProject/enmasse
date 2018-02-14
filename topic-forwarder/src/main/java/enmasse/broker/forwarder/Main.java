/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryClient;
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
        Map<String, String> annotationFilter = getAnnotationFilter(env);
        Host localHost = getLocalHost();
        String address = getAddress(env);

        String certDir = System.getenv("CERT_DIR");

        DiscoveryClient discoveryClient = new DiscoveryClient( labelFilter, annotationFilter, "broker");
        ForwarderController replicator = new ForwarderController(localHost, address, certDir);
        discoveryClient.addListener(replicator);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(replicator, result -> {
            if (result.succeeded()) {
                discoveryClient.start();
            }
        });
    }

    private static String getAddress(Map<String, String> env) {
        return getEnvOrThrow(env, "TOPIC_NAME");
    }

    private static String getBrokerName(Map<String, String> env) {
        return getEnvOrThrow(env, "CLUSTER_ID");
    }

    private static Host getLocalHost() throws UnknownHostException {
        return new Host(Inet4Address.getLocalHost().getHostAddress(), Collections.singletonMap("amqp", 5673));
    }

    private static Map<String,String> getLabelFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("role", "broker");
        return labelMap;
    }

    private static Map<String,String> getAnnotationFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("cluster_id", getBrokerName(env));
        return labelMap;
    }

    private static String
    getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }
}
