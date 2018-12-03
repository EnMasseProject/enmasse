/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.forwarder;

import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Host;
import io.enmasse.k8s.api.ConfigMapAddressApi;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.Vertx;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TopicForwarder entry point for topic forwarder.
 */
public class TopicForwarder {

    public static void main(String [] args) throws IOException {
        Map<String, String> env = System.getenv();
        Map<String, String> labelFilter = getLabelFilter(env);
        Map<String, String> annotationFilter = getAnnotationFilter(env);
        Host localHost = getLocalHost();

        String certDir = env.get("CERT_DIR");
        String infraUuid = getEnvOrThrow(env, "INFRA_UUID");

        NamespacedOpenShiftClient openShiftClient = new DefaultOpenShiftClient();
        DiscoveryClient discoveryClient = new DiscoveryClient(openShiftClient, labelFilter, annotationFilter, "broker");
        ForwarderController replicator = new ForwarderController(localHost, certDir);
        ConfigMapAddressApi addressApi = new ConfigMapAddressApi(openShiftClient, infraUuid);

        addressApi.watchAddresses(replicator, Duration.ofSeconds(300));
        discoveryClient.addListener(replicator);

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(replicator, result -> {
            if (result.succeeded()) {
                discoveryClient.start();
            }
        });
    }

    private static Host getLocalHost() throws UnknownHostException {
        return new Host(Inet4Address.getLocalHost().getHostAddress(), Collections.singletonMap("amqp", 5673));
    }

    private static Map<String,String> getLabelFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("role", "broker");
        labelMap.put("infraUuid", getEnvOrThrow(env, "INFRA_UUID"));
        return labelMap;
    }

    private static Map<String,String> getAnnotationFilter(Map<String, String> env) {
        Map<String, String> labelMap = new LinkedHashMap<>();
        labelMap.put("cluster_id", getEnvOrThrow(env, "CLUSTER_ID"));
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
