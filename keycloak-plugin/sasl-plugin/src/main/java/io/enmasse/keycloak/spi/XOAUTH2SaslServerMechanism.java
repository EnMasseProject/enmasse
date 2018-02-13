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

public abstract class XOAUTH2SaslServerMechanism implements SaslServerMechanism {
/*
    private static final String BEARER_PREFIX = "Bearer ";
    private TokenManager tokenManager = new TokenManager();


    @Override
    public String getName() {
        return "XOAUTH2";
    }

    @Override
    public Instance newInstance(final KeycloakSession keycloakSession,
                                final String hostname,
                                final Config.Scope config)
    {
        return new Instance()
        {
            private boolean complete;
            private boolean authenticated;
            private RuntimeException error;

            @Override
            public byte[] processResponse(byte[] response) throws IllegalArgumentException
            {
                if (error != null) {
                    throw error;
                }

                Map<String, String> responsePairs = splitResponse(response);

                String auth = responsePairs.get("auth");
                if (auth == null) {
                    error = new IllegalArgumentException("Invalid XOAUTH2 encoding, the mandatory 'auth' part of the response was absent");
                    throw error;
                }

                if (!auth.startsWith(BEARER_PREFIX)) {
                    error = new IllegalArgumentException("Invalid XOAUTH2 encoding, the 'auth' part of response does not not begin with the expected prefix");
                    throw error;
                }


                final RealmModel realm = keycloakSession.realms().getRealmByName(hostname);

                String tokenString = auth.substring(BEARER_PREFIX.length()).trim();


                AccessToken token = null;
                try {
                    URI baseUri = new URI(config.get("baseUri", "https://localhost:8443/auth"));

                    RSATokenVerifier verifier = RSATokenVerifier.create(tokenString)
                                                                .realmUrl(Urls.realmIssuer(baseUri,
                                                                                           realm.getName()));
                    String kid = verifier.getHeader().getKeyId();
                    verifier.publicKey(keycloakSession.keys().getRsaPublicKey(realm, kid));
                    token = verifier.verify().getToken();
                } catch (VerificationException e) {
                    error = new IllegalArgumentException("Token invalid: " + e.getMessage());
                    throw error;
                } catch (URISyntaxException e)  {
                    error = new IllegalArgumentException("Invalid URI from SASL XOAUTH2 config: " + e.getMessage());
                    throw error;
                }

                UserSessionModel userSession =
                        keycloakSession.sessions().getUserSession(realm, token.getSessionState());

                ClientSessionModel clientSession =
                        keycloakSession.sessions().getClientSession(token.getClientSession());

                if (userSession == null) {
                    authenticated = false;
                } else {

                    UserModel userModel = userSession.getUser();
                    if (userModel == null || clientSession == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
                        authenticated = false;
                    } else {

                        ClientModel clientModel = realm.getClientByClientId(token.getIssuedFor());
                        if (clientModel == null || !clientModel.isEnabled()) {
                            authenticated = false;
                        } else {
                            authenticated = true;
                            AccessToken userInfo = new AccessToken();
                            tokenManager.transformUserInfoAccessToken(keycloakSession,
                                                                      userInfo,
                                                                      realm,
                                                                      clientModel,
                                                                      userModel,
                                                                      userSession,
                                                                      clientSession);

                        }
                    }
                }
                complete = true;

                return null;
            }

            @Override
            public boolean isComplete() {
                return complete;
            }

            @Override
            public boolean isAuthenticated() {
                return authenticated;
            }

            private Map<String, String> splitResponse(final byte[] response) {
                String[] splitResponse = new String(response, StandardCharsets.US_ASCII).split("\1");
                Map<String, String> responseItems = new HashMap<>(splitResponse.length);
                for(String nameValue : splitResponse) {
                    if (nameValue.length() > 0) {
                        String[] nameValueSplit = nameValue.split("=", 2);
                        if (nameValueSplit.length == 2) {
                            responseItems.put(nameValueSplit[0], nameValueSplit[1]);
                        }
                    }
                }
                return responseItems;
            }

        };
    }*/
}
