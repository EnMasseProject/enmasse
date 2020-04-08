/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import java.io.File;
import java.util.Map;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.proton.ProtonServerOptions;

public class NoneAuthService {
    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        String certDir = env.getOrDefault("CERT_DIR", "/opt/none-authservice/cert");
        int listenPort = Integer.parseInt(env.getOrDefault("LISTENPORT", "5671"));
        int healthPort = Integer.parseInt(env.getOrDefault("HEALTHPORT", "8080"));
        int serverInstances = Integer.parseInt(env.getOrDefault("ENMASSE_NUM_AMQPS_SERVER_INSTANCES", String.format("%s", Runtime.getRuntime().availableProcessors())));

        VertxOptions vertxOptions = new VertxOptions();
        if (serverInstances > vertxOptions.getWorkerPoolSize()) {
            vertxOptions.setWorkerPoolSize(serverInstances);
        }
        Vertx vertx = Vertx.vertx(vertxOptions);

        ProtonServerOptions protonServerOptions = new ProtonServerOptions()
                .setSsl(true)
                .setPort(listenPort)
                .setHost("0.0.0.0")
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                        .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));

        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setHost("0.0.0.0")
                .setPort(healthPort);
        HealthServer healthServer = new HealthServer(httpServerOptions);

        vertx.deployVerticle(() -> new SaslServer(protonServerOptions), new DeploymentOptions().setWorker(true).setInstances(serverInstances));
        vertx.deployVerticle(healthServer);
    }
}
