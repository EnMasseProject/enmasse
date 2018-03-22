/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.keycloak.spi;

import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;
import org.keycloak.Config;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.LinkedHashMap;
import java.util.Map;

class SaslAuthenticator implements ProtonSaslAuthenticator
{

    static final Object USER_ATTACHMENT = new Object();

    private static Map<String, SaslServerMechanism> MECHANISMS = new LinkedHashMap<>();

    // TODO - load these dynamically
    static {
        final SaslServerMechanism plainSaslServerMechanism = new PlainSaslServerMechanism();
        final SaslServerMechanism scramSHA1SaslServerMechanism = new ScramSaslServerMechanism("HmacSHA1", "SCRAM-SHA-1", "SHA-1", ScramSha1PasswordHashProviderFactory.ID, Pbkdf2PasswordHashProviderFactory.ID);
        final SaslServerMechanism scramSha256SaslServerMechanism = new ScramSaslServerMechanism("HmacSHA256", "SCRAM-SHA-256", "SHA-256", ScramSha256PasswordHashProviderFactory.ID, Pbkdf2Sha256PasswordHashProviderFactory.ID);
        final SaslServerMechanism scramSha512SaslServerMechanism = new ScramSaslServerMechanism("HmacSHA512", "SCRAM-SHA-512", "SHA-512", ScramSha512PasswordHashProviderFactory.ID, Pbkdf2Sha512PasswordHashProviderFactory.ID);
        final SaslServerMechanism xoauth2SHA1SaslServerMechanism = new XOAUTH2SaslServerMechanism();

        MECHANISMS.put(plainSaslServerMechanism.getName(), plainSaslServerMechanism);
        MECHANISMS.put(scramSHA1SaslServerMechanism.getName(), scramSHA1SaslServerMechanism);
        MECHANISMS.put(scramSha256SaslServerMechanism.getName(), scramSha256SaslServerMechanism);
        MECHANISMS.put(scramSha512SaslServerMechanism.getName(), scramSha512SaslServerMechanism);
        MECHANISMS.put(xoauth2SHA1SaslServerMechanism.getName(), xoauth2SHA1SaslServerMechanism);
    }

    private final Config.Scope config;

    private KeycloakSessionFactory keycloakSessionFactory;
    private Sasl sasl;
    private boolean succeeded;
    private KeycloakSession keycloakSession;
    private SaslServerMechanism.Instance saslMechanism;
    private ProtonConnection connection;

    SaslAuthenticator(final KeycloakSessionFactory sessionFactory, final Config.Scope config, final boolean secure) {
        this.keycloakSessionFactory = sessionFactory;
        this.config = config;
    }

    @Override
    public void init(final NetSocket socket,
                     final ProtonConnection protonConnection,
                     final Transport transport) {
        this.sasl = transport.sasl();
        sasl.server();
        sasl.allowSkip(false);
        sasl.setMechanisms(MECHANISMS.keySet().toArray(new String[MECHANISMS.size()]));
        keycloakSession = keycloakSessionFactory.create();
        connection = protonConnection;
    }


    @Override
    public void process(final Handler<Boolean> completionHandler) {
        String[] remoteMechanisms = sasl.getRemoteMechanisms();
        boolean done = false;

        if(saslMechanism == null) {
            if (remoteMechanisms.length > 0) {
                String chosen = remoteMechanisms[0];
                SaslServerMechanism mechanismImpl = MECHANISMS.get(chosen);
                if (mechanismImpl != null) {
                    String saslHostname = sasl.getHostname();
                    if(saslHostname == null) {
                        saslHostname = config.get("defaultDomain","");
                    }
                    saslMechanism = mechanismImpl.newInstance(keycloakSession, saslHostname, config);

                } else {

                    sasl.done(Sasl.SaslOutcome.PN_SASL_SYS);
                    done = true;
                }
            }
        }
        if(!done && saslMechanism != null) {
            byte[] response;
            if(sasl.pending()>0) {
                response = new byte[sasl.pending()];
                sasl.recv(response, 0, response.length);
            } else {
                response = new byte[0];
            }
            try {
                byte[] challenge = saslMechanism.processResponse(response);
                if (!saslMechanism.isComplete() || (challenge != null && challenge.length > 0))
                {
                    sasl.send(challenge, 0, challenge.length);
                }
                else
                {
                    succeeded = saslMechanism.isAuthenticated();
                    done = true;
                    if (succeeded)
                    {
                        connection.attachments().set(USER_ATTACHMENT, UserData.class, saslMechanism.getAuthenticatedUser());
                        sasl.done(Sasl.SaslOutcome.PN_SASL_OK);
                    }
                    else
                    {
                        sasl.done(Sasl.SaslOutcome.PN_SASL_AUTH);
                    }
                }
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
                done = true;
                sasl.done(Sasl.SaslOutcome.PN_SASL_SYS);
            }
        }
        completionHandler.handle(done);
    }

    @Override
    public boolean succeeded() {
        return succeeded;
    }
}
