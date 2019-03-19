/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.authservice.none;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.amqp.security.SaslMechanisms;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoneAuthServiceAuthenticator implements ProtonSaslAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(NoneAuthServiceAuthenticator.class);
    private static final String MECH_ANONYMOUS = "ANONYMOUS";
    private static final String MECH_PLAIN = "PLAIN";

    private Sasl sasl;

    @Override
    public void init(NetSocket netSocket, ProtonConnection protonConnection, Transport transport) {

        transport.setInitialRemoteMaxFrameSize(1024*1024);
        this.sasl = transport.sasl();
        log.info("Init SASL handshake");
        sasl.server();
        sasl.allowSkip(false);
        sasl.setMechanisms(MECH_ANONYMOUS, MECH_PLAIN);
    }

    @Override
    public void process(Handler<Boolean> handler) {
        String[] remoteMechanisms = sasl.getRemoteMechanisms();
        if (remoteMechanisms.length > 0) {
            String chosen = remoteMechanisms[0];
            byte[] response;
            if (sasl.pending() > 0) {
                response = new byte[sasl.pending()];
                sasl.recv(response, 0, response.length);
            }
            if (MECH_ANONYMOUS.equals(chosen) || MECH_PLAIN.equals(chosen)) {
                sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
            } else {
                sasl.done(Sasl.SaslOutcome.PN_SASL_SYS);
            }
        }
        handler.handle(true);
    }

    @Override
    public boolean succeeded() {
        log.info("Check succeeded");
        return true;
    }
}
