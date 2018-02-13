/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.ProtonSaslAuthenticatorFactory;
import org.apache.qpid.proton.engine.Transport;

/**
 * Authenticator factory which disables sasl handshake. This is temporary until Artemis supports sasl
 */
public class DummySaslAuthenticatorFactory implements ProtonSaslAuthenticatorFactory {
    @Override
    public ProtonSaslAuthenticator create() {
        return new ProtonSaslAuthenticator() {
            @Override
            public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
            }

            @Override
            public void process(Handler<Boolean> completionHandler) {
                completionHandler.handle(true);
            }

            @Override
            public boolean succeeded() {
                return true;
            }
        };
    }
}
