/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.ExternalSaslAuthenticator;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String [] args) {
        Vertx vertx = Vertx.vertx();
        String certDir = System.getenv("CERT_DIR");
        int listenPort = Integer.parseInt(getEnvOrThrow("LISTEN_PORT"));
        String requestTimeoutStr = System.getenv("DEFAULT_BROKER_REQUEST_TIMEOUT");
        Long requestTimeout = requestTimeoutStr != null ? Long.parseLong(requestTimeoutStr) : null;

        QueueScheduler scheduler = new QueueScheduler(
                connection -> {
                    Future<Broker> broker = Future.future();
                    Future<Artemis> artemis = Artemis.createFromConnection(vertx, connection);
                    artemis.setHandler(result -> {
                        if (result.succeeded()) {
                            Artemis a = result.result();
                            if (requestTimeout != null) {
                                a.setRequestTimeout(requestTimeout, TimeUnit.SECONDS);
                            }
                            broker.complete(new ArtemisAdapter(a));
                        } else {
                            broker.fail(result.cause());
                        }
                    });
                    return broker;
                },
                new SchedulerState(),
                listenPort,
                certDir);


        if (certDir != null) {
            scheduler.setProtonSaslAuthenticatorFactory(ExternalSaslAuthenticator::new);
        } else {
            scheduler.setProtonSaslAuthenticatorFactory(new DummySaslAuthenticatorFactory());
        }


        KubernetesClient kubernetesClient = new DefaultKubernetesClient();
        ConfigServiceClient configServiceClient = new ConfigServiceClient(scheduler, kubernetesClient, getEnvOrThrow("NAMESPACE"));

        vertx.deployVerticle(configServiceClient);
        vertx.deployVerticle(scheduler);
    }

    private static String getEnvOrThrow(String env) {
        String value = System.getenv(env);
        if (value == null) {
            throw new IllegalArgumentException("No environment " + env + " defined");
        }
        return value;
    }
}
