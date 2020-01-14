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
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.credential.hash.Pbkdf2PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha256PasswordHashProviderFactory;
import org.keycloak.credential.hash.Pbkdf2Sha512PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

class SaslAuthenticator implements ProtonSaslAuthenticator
{

    static final Object USER_ATTACHMENT = new Object();
    private static final Logger LOG = Logger.getLogger(SaslAuthenticator.class);


    // TODO - load these dynamically
    private static final List<SaslServerMechanism> ALL_MECHANISMS =
        Collections.unmodifiableList(Arrays.asList(
            new PlainSaslServerMechanism(),
            new ScramSaslServerMechanism("HmacSHA1", "SCRAM-SHA-1", "SHA-1", ScramSha1PasswordHashProviderFactory.ID, Pbkdf2PasswordHashProviderFactory.ID, 50),
            new ScramSaslServerMechanism("HmacSHA256", "SCRAM-SHA-256", "SHA-256", ScramSha256PasswordHashProviderFactory.ID, Pbkdf2Sha256PasswordHashProviderFactory.ID, 60),
            new ScramSaslServerMechanism("HmacSHA512", "SCRAM-SHA-512", "SHA-512", ScramSha512PasswordHashProviderFactory.ID, Pbkdf2Sha512PasswordHashProviderFactory.ID, 70),
            new XOAUTH2SaslServerMechanism()));

    private final Config.Scope config;
    private final AmqpServer amqpServer;

    private KeycloakSessionFactory keycloakSessionFactory;
    private Sasl sasl;
    private boolean succeeded;
    private SaslServerMechanism.Instance saslMechanism;
    private ProtonConnection connection;

    SaslAuthenticator(final KeycloakSessionFactory sessionFactory,
                      final Config.Scope config,
                      final boolean secure,
                      AmqpServer amqpServer) {
        this.keycloakSessionFactory = sessionFactory;
        this.config = config;
        this.amqpServer = amqpServer;
    }

    @Override
    public void init(final NetSocket socket,
                     final ProtonConnection protonConnection,
                     final Transport transport) {
        // allow for frames bigger than 512 bytes to support mechanisms that send (for instance) tokens
        transport.setInitialRemoteMaxFrameSize(1024*1024);
        this.sasl = transport.sasl();
        sasl.server();
        sasl.allowSkip(false);
        sasl.setMechanisms(getValidMechanisms(getPasswordHashAlgorithms()));
        connection = protonConnection;
    }

    private String[] getValidMechanisms(Set<String> hashAlgos) {
        TreeSet<SaslServerMechanism> mechanisms = new TreeSet<>(Comparator.comparingInt(SaslServerMechanism::priority));
        for(SaslServerMechanism mech : ALL_MECHANISMS) {
            if(hashAlgos.isEmpty()) {
                if (mech.isSupported("")) {
                    mechanisms.add(mech);
                }
            } else {
                for (String hashAlgo : hashAlgos) {
                    if (mech.isSupported(hashAlgo)) {
                        mechanisms.add(mech);
                        break;
                    }
                }
            }
        }
        return mechanisms.stream().map(SaslServerMechanism::getName).toArray(String[]::new);
    }

    private Set<String> getPasswordHashAlgorithms() {
        Set<String> hashAlgos = new HashSet<>();
        boolean enmasseRealmsFound = false;
        KeycloakSession keycloakSession = keycloakSessionFactory.create();
        KeycloakTransactionManager transactionManager = keycloakSession.getTransactionManager();
        transactionManager.begin();
        try {
            List<RealmModel> realms = keycloakSession.realms().getRealms();
            for(RealmModel realm : realms) {
                if(realm.getAttribute("enmasse-realm",Boolean.FALSE)) {
                    enmasseRealmsFound = true;
                    hashAlgos.add(realm.getPasswordPolicy().getHashAlgorithm());
                }
            }
        } finally {
            transactionManager.commit();
            keycloakSession.close();
        }
        if(!enmasseRealmsFound) {
            LOG.warn("No realms with attribute \"enmasse-realm\" found, only universally accepted SASL mechanisms will be offered");
        }
        return hashAlgos;
    }

    @Override
    public void process(final Handler<Boolean> completionHandler) {
        String[] remoteMechanisms = sasl.getRemoteMechanisms();
        boolean done = false;

        if(saslMechanism == null) {
            if (remoteMechanisms.length > 0) {
                String chosen = remoteMechanisms[0];
                Optional<SaslServerMechanism> mechanismImpl = ALL_MECHANISMS.stream().filter(m -> m.getName().equals(chosen)).findFirst();
                if (mechanismImpl.isPresent()) {
                    String saslHostname = sasl.getHostname();
                    if(saslHostname == null) {
                        saslHostname = config.get("defaultDomain","");
                    }
                    saslMechanism = mechanismImpl.get().newInstance(keycloakSessionFactory, saslHostname, config, amqpServer);

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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                done = true;
                sasl.done(Sasl.SaslOutcome.PN_SASL_SYS);
            } catch (IllegalArgumentException e) {
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
