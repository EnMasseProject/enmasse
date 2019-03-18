/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;
import org.apache.qpid.proton.amqp.Symbol;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NoneAuthService {
    private static final Symbol AUTHENTICATED_IDENTITY = Symbol.getSymbol("authenticated-identity");
    private static final Symbol GROUPS = Symbol.getSymbol("groups");

    public static void main(String[] args) {
        Map<String, String> env = System.getenv();
        String certDir = env.getOrDefault("CERT_DIR", "/opt/none-authservice/cert");
        int listenPort = Integer.parseInt(env.getOrDefault("LISTENPORT", "5671"));

        Vertx vertx = Vertx.vertx();

        ProtonServerOptions options = new ProtonServerOptions();
        options.setSsl(true);
        options.setPemKeyCertOptions(new PemKeyCertOptions()
                .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));

        ProtonServer server = ProtonServer.create(vertx, options);
        server.connectHandler(NoneAuthService::connectHandler);
        server.saslAuthenticatorFactory(NoneAuthServiceAuthenticator::new);
        server.listen(listenPort);
    }

    private static void connectHandler(ProtonConnection protonConnection) {
        protonConnection.setContainer("none-authservice");
        protonConnection.openHandler(result -> {
            ProtonConnection connection = result.result();
            Map<Symbol, Object> properties = new HashMap<>();
            properties.put(AUTHENTICATED_IDENTITY, Collections.singletonMap("sub", "anonymous"));
            properties.put(GROUPS, Collections.singletonList("manage"));
            connection.setProperties(properties);
            protonConnection.open();
            protonConnection.close();
        });
    }
}
