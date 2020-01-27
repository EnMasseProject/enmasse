/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import java.io.File;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.proton.ProtonServerOptions;

public class NoneAuthService {
    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        String certDir = env.getOrDefault("CERT_DIR", "/opt/none-authservice/cert");
        int listenPort = Integer.parseInt(env.getOrDefault("LISTENPORT", "5671"));
        int healthPort = Integer.parseInt(env.getOrDefault("HEALTHPORT", "8080"));

        Vertx vertx = Vertx.vertx();

        ProtonServerOptions protonServerOptions = new ProtonServerOptions()
                .setSsl(true)
                .setPort(listenPort)
                .setHost("0.0.0.0")
                .setPemKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                        .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        SaslServer saslServer = new SaslServer(protonServerOptions);

        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setHost("0.0.0.0")
                .setPort(healthPort);
        HealthServer healthServer = new HealthServer(httpServerOptions);

        vertx.deployVerticle(saslServer);
        vertx.deployVerticle(healthServer);
    }
}
