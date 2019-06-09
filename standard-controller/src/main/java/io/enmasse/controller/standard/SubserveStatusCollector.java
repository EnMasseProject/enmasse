/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.admin.model.AddressPlan;
import io.enmasse.amqp.ProtonRequestClient;
import io.enmasse.amqp.SyncRequestClient;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SubserveStatusCollector {
    private static final Logger log = LoggerFactory.getLogger(RouterStatusCollector.class);
    private final Vertx vertx;
    private final String certDir;

    SubserveStatusCollector(Vertx vertx, String certDir) {
        this.vertx = vertx;
        this.certDir = certDir;
    }

    public Set<String> collect(Pod router) throws Exception {
        int port = 0;
        for (Container container : router.getSpec().getContainers()) {
            if (container.getName().equals("router")) {
                for (ContainerPort containerPort : container.getPorts()) {
                    if (containerPort.getName().equals("amqps-normal")) {
                        port = containerPort.getContainerPort();
                    }
                }
            }
        }

        if (port != 0) {
            log.debug("Checking status of subserv via router : {}" + router.getStatus().getPodIP());
            ProtonClientOptions clientOptions = new ProtonClientOptions()
                    .setSsl(true)
                    .addEnabledSaslMechanism("EXTERNAL")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));

            try (ProtonRequestClient client = new ProtonRequestClient(vertx, "standard-controller")) {
                CompletableFuture<Void> promise = new CompletableFuture<>();
                client.connect(router.getStatus().getPodIP(), port, clientOptions, "$subctrl", promise);

                promise.get(10, TimeUnit.SECONDS);

                Set<String> topics = collectRegisteredTopics(client);
                log.debug("Subserv registered topics : {}", topics);
                return topics;
            }
        } else {
            log.info("Unable to find appropriate router port, skipping subserv check");
            return Collections.emptySet();
        }
    }

    private Set<String> collectRegisteredTopics(SyncRequestClient client) throws Exception {
        Message message = Proton.message();
        message.setSubject("list_topics");

        Message response = client.request(message, 10, TimeUnit.SECONDS);
        AmqpValue value = (AmqpValue) response.getBody();

        if (value == null || value.getValue() == null) {
            throw new IllegalArgumentException("Unexpected null response body");
        } else if (!(value.getValue() instanceof List)) {
            throw new IllegalArgumentException(String.format("Unexpected response type : %s", value.getValue().getClass()));
        }
        return new HashSet<>(((List<String>) value.getValue()));
    }

    static void checkTopicRegistration(Set<String> subserveTopics, Address address, AddressPlan addressPlan) {
        if (addressPlan.getResources() != null && addressPlan.getResources().containsKey("broker")) {
            Double credit = addressPlan.getResources().get("broker");
            if (credit != null && credit >= 1) {
                String name = address.getSpec().getAddress();
                if (!subserveTopics.contains(name)) {
                    address.getStatus().setReady(false).appendMessage(String.format("Address %s is not registered with subserv", name));
                }

            }
        }
    }

}
