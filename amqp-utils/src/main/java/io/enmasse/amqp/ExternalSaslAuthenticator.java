/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.amqp;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import io.vertx.proton.sasl.impl.ProtonSaslExternalImpl;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;

public class ExternalSaslAuthenticator implements ProtonSaslAuthenticator {
    private Sasl sasl;
    private boolean succeeded;

    @Override
    public void init(final NetSocket socket,
                     final ProtonConnection protonConnection,
                     final Transport transport) {
        sasl = transport.sasl();
        sasl.server();
        sasl.allowSkip(false);
        sasl.setMechanisms(ProtonSaslExternalImpl.MECH_NAME);
        succeeded = false;
    }

    @Override
    public void process(final Handler<Boolean> completionHandler) {
        if (sasl == null) {
            throw new IllegalStateException("Init was not called with the associated transport");
        }
        // Note we do not record the identity with which the client authenticated, nor to we take any notice of
        // an alternative identity passed in the response
        boolean done = false;
        String[] remoteMechanisms = sasl.getRemoteMechanisms();
        if (remoteMechanisms.length > 0) {
            String chosen = remoteMechanisms[0];
            if (ProtonSaslExternalImpl.MECH_NAME.equals(chosen)) {
                // TODO - should handle the case of no initial response per the SASL spec, (i.e. send an empty challenge)
                // however this was causing errors in some clients
                // Missing initial response can be detected with: sasl.recv(new byte[0], 0, 0) == -1
                sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
                succeeded = true;
            } else {
                sasl.done(Sasl.SaslOutcome.PN_SASL_AUTH);
            }
            done = true;
        }
        completionHandler.handle(done);
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }
}
