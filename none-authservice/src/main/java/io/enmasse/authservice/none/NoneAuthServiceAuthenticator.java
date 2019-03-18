/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;

public class NoneAuthServiceAuthenticator implements ProtonSaslAuthenticator {
    private Sasl sasl;

    @Override
    public void init(NetSocket netSocket, ProtonConnection protonConnection, Transport transport) {

        transport.setInitialRemoteMaxFrameSize(1024*1024);
        this.sasl = transport.sasl();
        sasl.server();
        sasl.allowSkip(false);
        sasl.setMechanisms("ANONYMOUS", "PLAIN");
    }

    @Override
    public void process(Handler<Boolean> handler) {
        sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
        handler.handle(true);
    }

    @Override
    public boolean succeeded() {
        return true;
    }
}
