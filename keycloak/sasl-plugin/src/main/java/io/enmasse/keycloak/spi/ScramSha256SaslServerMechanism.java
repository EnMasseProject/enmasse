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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import java.security.SecureRandom;

public class ScramSha256SaslServerMechanism implements SaslServerMechanism {

    private static final String HMAC_NAME = "HmacSHA256";

    private static final String MECHANISM = "SCRAM-SHA-256";
    private static final String DIGEST_NAME = "SHA-256";

    private static final byte[] RANDOM_BYTES = new byte[32];
    static {
        (new SecureRandom()).nextBytes(RANDOM_BYTES);
    }
    
    @Override
    public String getName() {
        return MECHANISM;
    }

    @Override
    public Instance newInstance(final KeycloakSession keycloakSession,
                                final String hostname,
                                final Config.Scope config) {
        return new ScramSaslAuthenticator(keycloakSession, hostname, RANDOM_BYTES, DIGEST_NAME, HMAC_NAME);
    }

}
