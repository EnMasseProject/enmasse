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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class PlainSaslServerMechanism implements SaslServerMechanism {

    private static final Logger LOG = Logger.getLogger(PlainSaslServerMechanism.class);

    @Override
    public String getName() {
        return "PLAIN";
    }

    @Override
    public Instance newInstance(final KeycloakSession keycloakSession,
                                final String hostname,
                                final Config.Scope config)
    {
        return new Instance()
        {
            public UserData authenticatedUser;
            private boolean complete;
            private boolean authenticated;
            private RuntimeException error;

            @Override
            public byte[] processResponse(byte[] response) throws IllegalArgumentException
            {
                if(error != null) {
                    throw error;
                }

                int authzidNullPosition = findNullPosition(response, 0);
                if (authzidNullPosition < 0) {
                    error = new IllegalArgumentException("Invalid PLAIN encoding, authzid null terminator not found");
                    throw error;
                }

                int authcidNullPosition = findNullPosition(response, authzidNullPosition + 1);
                if (authcidNullPosition < 0) {
                    error = new IllegalArgumentException("Invalid PLAIN encoding, authcid null terminator not found");
                    throw error;
                }

                String username = new String(response, authzidNullPosition + 1, authcidNullPosition - authzidNullPosition - 1, StandardCharsets.UTF_8);
                int passwordLen = response.length - authcidNullPosition - 1;
                String password = new String(response, authcidNullPosition + 1, passwordLen, StandardCharsets.UTF_8);

                LOG.info("SASL hostname: " + hostname);
                KeycloakTransactionManager transactionManager = keycloakSession.getTransactionManager();
                transactionManager.begin();
                try {
                    final RealmModel realm = keycloakSession.realms().getRealmByName(hostname);
                    if (realm == null) {
                        LOG.info("Realm " + hostname + " not found");
                        authenticated = false;
                        complete = true;
                        return null;
                    }

                    final UserModel user = keycloakSession.userStorageManager().getUserByUsername(username, realm);
                    if (user != null && keycloakSession.userCredentialManager().isValid(realm, user, UserCredentialModel.password(password))) {

                        authenticatedUser = new UserDataImpl(user.getId(), user.getUsername(), user.getGroups().stream().map(GroupModel::getName).collect(Collectors.toSet()));
                        authenticated = true;
                        complete = true;
                        return null;
                    } else {
                        LOG.info("Invalid password for " + username + " in realm " + hostname);
                        authenticated = false;
                        complete = true;
                        return null;
                    }
                } finally {
                    transactionManager.commit();
                }
            }

            @Override
            public boolean isComplete()
            {
                return complete;
            }

            @Override
            public boolean isAuthenticated()
            {
                return authenticated;
            }

            @Override
            public UserData getAuthenticatedUser() {
                return authenticatedUser;
            }

            private int findNullPosition(byte[] response, int startPosition) {
                int position = startPosition;
                while (position < response.length) {
                    if (response[position] == (byte) 0) {
                        return position;
                    }
                    position++;
                }
                return -1;
            }

        };
    }

}
