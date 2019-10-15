/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RouterManagement {
    private static final Logger log = LoggerFactory.getLogger(RouterManagement.class);
    private final Vertx vertx;
    private final String containerId;
    private final ProtonClientOptions clientOptions;
    private final Duration connectTimeout;
    private final Duration queryTimeout;

    private RouterManagement(Vertx vertx, String containerId, ProtonClientOptions clientOptions, Duration connectTimeout, Duration queryTimeout) {
        this.vertx = vertx;
        this.containerId = containerId;
        this.clientOptions = clientOptions;
        this.connectTimeout = connectTimeout;
        this.queryTimeout = queryTimeout;
    }

    public static RouterManagement withCertsInDir(Vertx vertx, String containerId, Duration connectTimeout, Duration queryTimeout, String certDir) {
        ProtonClientOptions clientOptions = new ProtonClientOptions()
                .setSsl(true)
                .addEnabledSaslMechanism("EXTERNAL")
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                        .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        return new RouterManagement(vertx, containerId, clientOptions, connectTimeout, queryTimeout);
    }

    public static RouterManagement withCerts(Vertx vertx, String containerId, Duration connectTimeout, Duration queryTimeout, byte[] caCert, byte[] clientCert, byte[] clientKey) {
        ProtonClientOptions clientOptions = new ProtonClientOptions()
                .setSsl(true)
                .addEnabledSaslMechanism("EXTERNAL")
                .setHostnameVerificationAlgorithm("")
                .setPemTrustOptions(new PemTrustOptions()
                        .addCertValue(Buffer.buffer(caCert)))
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .addCertValue(Buffer.buffer(clientCert))
                        .addKeyValue(Buffer.buffer(clientKey)));
        return new RouterManagement(vertx, containerId, clientOptions, connectTimeout, queryTimeout);
    }



    public Map<RouterEntity, List<List>> query(String host, int port, RouterEntity... entities) throws Exception {
        int attempt = 1;
        final int allowedAttempts = 3;
        Exception lastException;
        do {
            try {
                return doQuery(host, port, entities);
            } catch (Exception e) {
                log.error("Failed to collect router {} status (attempt {}/{}})", host, attempt, allowedAttempts, e);
                lastException = e;
                try {
                    Thread.sleep(5 * 1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
                attempt++;
            }
        } while (allowedAttempts >= attempt);

        throw lastException;
    }

    private Map<RouterEntity, List<List>> doQuery(String host, int port, RouterEntity... entities) throws Exception {
        log.debug("Checking router status of router : {}", host);
        try (ProtonRequestClient client = new ProtonRequestClient(vertx, containerId)) {
            CompletableFuture<Void> promise = new CompletableFuture<>();
            client.connect(host, port, clientOptions, "$management", promise);

            promise.get(connectTimeout.getSeconds(), TimeUnit.SECONDS);

            Map<RouterEntity, List<List>> resultMap = new HashMap<>();
            for (RouterEntity routerEntity : entities) {
                resultMap.put(routerEntity, collectRouter(client, routerEntity));
            }
            return resultMap;
        }
    }

    private List<List> collectRouter(SyncRequestClient client, RouterEntity routerEntity) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("operation", "QUERY");
        properties.put("entityType", routerEntity.getName());
        Map<String, Object> body = new LinkedHashMap<>();

        if (routerEntity.getAttributes() != null) {
            body.put("attributeNames", Arrays.asList(routerEntity.getAttributes()));
        }

        Message message = Proton.message();
        message.setApplicationProperties(new ApplicationProperties(properties));
        message.setBody(new AmqpValue(body));

        long timeoutSeconds = this.queryTimeout.getSeconds();
        Message response = client.request(message, timeoutSeconds, TimeUnit.SECONDS);
        if (response == null) {
            throw new IllegalArgumentException(String.format("No response received within timeout : %s(s)", timeoutSeconds));
        }
        AmqpValue value = (AmqpValue) response.getBody();
        if (value == null) {
            throw new IllegalArgumentException("Unexpected null body");
        }
        Map<?,?> values = (Map<?,?>) value.getValue();
        if (values == null) {
            throw new IllegalArgumentException("Unexpected null body value");
        }

        @SuppressWarnings("unchecked")
        List<List> results = (List<List>) values.get("results");
        if (results == null) {
            throw new IllegalArgumentException("Unexpected null results list");
        }
        return results;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getQueryTimeout() {
        return queryTimeout;
    }

}
