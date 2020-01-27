/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.proton.amqp.Symbol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;

public class SaslServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(SaslServer.class);
    private static final Symbol AUTHENTICATED_IDENTITY = Symbol.getSymbol("authenticated-identity");
    private static final Symbol GROUPS = Symbol.getSymbol("groups");
    private final ProtonServerOptions options;
    public SaslServer(ProtonServerOptions options) {
        this.options  = options;
    }

    @Override
    public void start(Promise<Void> startPromise) {

        ProtonServer server = ProtonServer.create(vertx, options);
        server.connectHandler(SaslServer::connectHandler);
        server.saslAuthenticatorFactory(NoneAuthServiceAuthenticator::new);
        server.listen(result -> {
            if (result.succeeded()) {
                log.info("SaslServer started");
                startPromise.complete();
            } else {
                log.error("Error starting SaslServer");
                startPromise.fail(result.cause());
            }
        });
    }

    private static void connectHandler(ProtonConnection connection) {
        connection.setContainer("none-authservice");
        connection.openHandler(conn -> {
            Map<Symbol, Object> properties = new HashMap<>();

            properties.put(AUTHENTICATED_IDENTITY, Collections.singletonMap("sub", "anonymous"));
            properties.put(GROUPS, Collections.singletonList("manage"));

            connection.setProperties(properties);
            connection.open();
            connection.close();

        }).closeHandler(conn -> {
            connection.close();
            connection.disconnect();
        }).disconnectHandler(protonConnection -> {
            connection.disconnect();
        });
    }
}
