/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String [] args) throws Exception {
        boolean debug = System.getenv("PRESTOP_DEBUG") != null;

        Optional<Runnable> debugFn = Optional.empty();
        Host localHost = localHost();
        Vertx vertx = Vertx.vertx();
        BrokerFactory brokerFactory = new ArtemisBrokerFactory(60_000L);

        String certDir = System.getenv("CERT_DIR");
        ProtonClientOptions clientOptions = createClientOptions(certDir);

        if (System.getenv("TOPIC_NAME") != null) {
            String clusterId = System.getenv("CLUSTER_ID");

            Map<String, String> labelFilter = new LinkedHashMap<>();
            labelFilter.put("role", "broker");
            Map<String, String> annotationFilter = new LinkedHashMap<>();
            annotationFilter.put("cluster_id", clusterId);

            Endpoint messagingEndpoint = new Endpoint(System.getenv("MESSAGING_SERVICE_HOST"), Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT_AMQPS_BROKER")));
            DiscoveryClient discoveryClient = new DiscoveryClient(labelFilter, annotationFilter, "broker");
            CompletableFuture<Set<Host>> peers = new CompletableFuture<>();
            discoveryClient.addListener(peers::complete);

            TopicMigrator migrator = new TopicMigrator(vertx, localHost, messagingEndpoint, brokerFactory, clientOptions);
            migrator.migrate(peers.get(60, TimeUnit.SECONDS));
        } else {
            Endpoint messagingEndpoint = new Endpoint(System.getenv("MESSAGING_SERVICE_HOST"), Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT_AMQPS_NORMAL")));
            String queueName = System.getenv("QUEUE_NAME");
            QueueDrainer client = new QueueDrainer(vertx, localHost, brokerFactory, clientOptions, debugFn);

            client.drainMessages(messagingEndpoint, queueName);
        }
    }

    private static Host localHost() throws UnknownHostException {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        portMap.put("amqp", 5673);
        portMap.put("core", 61616);

        String hostOverride = System.getenv("LOCAL_BROKER_HOSTNAME");
        if (hostOverride != null) {
            return new Host(hostOverride, portMap);
        } else {
            return new Host(Inet4Address.getLocalHost().getHostAddress(), portMap);
        }
    }

    private static ProtonClientOptions createClientOptions(String certDir)
    {
        ProtonClientOptions options = new ProtonClientOptions();

        if (certDir != null) {
            options.setSsl(true)
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        }
        return options;
    }

    private static final String serviceAccountPath = "/var/run/secrets/kubernetes.io/serviceaccount";
    private static String openshiftNamespace() throws IOException {
        return readFile(new File(serviceAccountPath, "namespace"));
    }

    private static String openshiftToken() throws IOException {
        return readFile(new File(serviceAccountPath, "token"));
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
