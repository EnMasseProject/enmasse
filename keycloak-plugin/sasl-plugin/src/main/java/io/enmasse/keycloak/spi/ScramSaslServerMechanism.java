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
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.keycloak.spi;

import org.keycloak.Config;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.enmasse.keycloak.spi.ScramSaslAuthenticator.StoredAndServerKey;

class ScramSaslServerMechanism implements SaslServerMechanism {

    private final String hmacName;

    private final String mechanism;
    private final String digestName;
    private final Map<String, Function<CredentialModel, StoredAndServerKey>> keyRetrievalFunctions;
    private final int priority;

    ScramSaslServerMechanism(String hmacName,
                             String mechanism,
                             String digestName,
                             String scramCredAlgo,
                             String pbkdf2CredAlgo,
                             int priority) {
        this(hmacName, mechanism, digestName, createKeyRetrievalFunctionMap(scramCredAlgo, pbkdf2CredAlgo, hmacName, digestName),priority);
    }

    private static Map<String, Function<CredentialModel, StoredAndServerKey>> createKeyRetrievalFunctionMap(String scramCredAlgo,
                                                                                                            String pbkdf2CredAlgo,
                                                                                                            String hmacName,
                                                                                                            String digestName) {

        Map<String, Function<CredentialModel, StoredAndServerKey>> functions = new HashMap<>();
        if(scramCredAlgo != null) {
            functions.put(scramCredAlgo, m -> ScramSaslAuthenticator.getStoredAndServerKeyFromScramHash(m, hmacName, digestName));
        }
        if(pbkdf2CredAlgo != null) {
            functions.put(pbkdf2CredAlgo, m -> ScramSaslAuthenticator.getStoredAndServerKeyFromPbkdf2Hash(m, hmacName, digestName));
        }
        return functions;
    }

    private ScramSaslServerMechanism(String hmacName,
                                     String mechanism,
                                     String digestName,
                                     Map<String, Function<CredentialModel, StoredAndServerKey>> keyRetrievalFunctions,
                                     int priority) {
        this.hmacName = hmacName;
        this.mechanism = mechanism;
        this.digestName = digestName;
        this.keyRetrievalFunctions = new HashMap<>(keyRetrievalFunctions);
        this.priority = priority;
    }

    @Override
    public boolean isSupported(String passwordHashAlgo)
    {
        return this.keyRetrievalFunctions.containsKey(passwordHashAlgo);
    }

    @Override
    public final String getName() {
        return mechanism;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public final Instance newInstance(KeycloakSessionFactory keycloakSessionFactory, String hostname, Config.Scope config) {
        return new ScramSaslAuthenticator(keycloakSessionFactory, hostname, digestName, hmacName, keyRetrievalFunctions);
    }

}
