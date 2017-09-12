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

package enmasse.broker.prestop;

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.discovery.DiscoveryClient;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.enmasse.amqp.Artemis;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Main {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static void main(String [] args) throws Exception {
        boolean debug = System.getenv("PRESTOP_DEBUG") != null;

        Optional<Runnable> debugFn = Optional.empty();
        Host localHost = localHost();
        Vertx vertx = Vertx.vertx();
        BrokerFactory brokerFactory = new ArtemisBrokerFactory(60_000L);


        if (System.getenv("TOPIC_NAME") != null) {
            String clusterId = System.getenv("CLUSTER_ID");
            Endpoint messagingEndpoint = new Endpoint(System.getenv("MESSAGING_SERVICE_HOST"), Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT_AMQPS_BROKER")));

            Map<String, String> labelFilter = new LinkedHashMap<>();
            labelFilter.put("role", "broker");
            Map<String, String> annotationFilter = new LinkedHashMap<>();
            annotationFilter.put("cluster_id", clusterId);

            DiscoveryClient discoveryClient = new DiscoveryClient("podsense", labelFilter, annotationFilter, Optional.of("broker"));
            TopicMigrator migrator = new TopicMigrator(vertx, localHost, messagingEndpoint, brokerFactory);
            discoveryClient.addListener(migrator);
            vertx.deployVerticle(discoveryClient);
            migrator.migrate();
        } else {
            String queueName = System.getenv("QUEUE_NAME");
            String messagingHost = System.getenv("MESSAGING_SERVICE_HOST");
            int messagingPort = Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT"));
            Endpoint to = new Endpoint(messagingHost, messagingPort);

            QueueDrainer client = new QueueDrainer(vertx, localHost, brokerFactory, debugFn);

            client.drainMessages(to, queueName);
        }
    }

    private static Host localHost() throws UnknownHostException {
        Map<String, Integer> portMap = new LinkedHashMap<>();
        portMap.put("amqp", 5673);
        portMap.put("core", 61616);

        return new Host(Inet4Address.getLocalHost().getHostAddress(), portMap);
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
