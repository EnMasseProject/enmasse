/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.models.*;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionCrossDCManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class XOAUTH2SaslServerMechanism implements SaslServerMechanism {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger LOG = Logger.getLogger(XOAUTH2SaslServerMechanism.class);

    private final TokenVerifier tokenVerifier;

    XOAUTH2SaslServerMechanism() {
        tokenVerifier = this::verifyRSAToken;
    }

    // Used only for testing purposes
    XOAUTH2SaslServerMechanism(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

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
            private UserDataImpl authenticatedUser;
            private boolean complete;
            private boolean authenticated;
            private RuntimeException error;

            @Override
            public byte[] processResponse(byte[] response) throws IllegalArgumentException
            {
                if (error != null) {
                    throw error;
                }

                final RealmModel realm = keycloakSession.realms().getRealmByName(hostname);
                if (realm == null) {
                    LOG.info("Realm " + hostname + " not found");
                    authenticated = false;
                    complete = true;
                    return null;
                }


                String tokenString = getTokenFromInitialResponse(response);

                AccessToken token = null;
                try {
                    URI baseUri = new URI(config.get("baseUri", "https://localhost:8443/auth"));

                    token = tokenVerifier.verifyTokenString(realm, tokenString, baseUri, keycloakSession);
                } catch (VerificationException e) {
                    LOG.debug("Token invalid: " + e.getMessage());
                    authenticated = false;
                    complete = true;
                    return null;
                } catch (URISyntaxException e)  {
                    error = new IllegalArgumentException("Invalid URI from SASL XOAUTH2 config: " + e.getMessage());
                    throw error;
                }

                ClientModel clientModel = realm.getClientByClientId(token.getIssuedFor());

                if(clientModel == null || !clientModel.isEnabled()) {
                    authenticated = false;
                } else {
                    UserSessionModel userSession = findValidSession(token, clientModel, realm, keycloakSession);
                    if(userSession != null) {
                        UserModel user = userSession.getUser();
                        if (user == null || !AuthenticationManager.isSessionValid(realm, userSession)) {
                            authenticated = false;
                        } else {
                            authenticated = true;
                            authenticatedUser = new UserDataImpl(user.getId(), user.getUsername(), user.getGroups().stream().map(GroupModel::getName).collect(Collectors.toSet()));
                        }
                    } else {
                        authenticated = false;
                    }
                }

                complete = true;

                return null;
            }

            private String getTokenFromInitialResponse(byte[] response) {
                String[] splitResponse = new String(response, StandardCharsets.US_ASCII).split("\1");
                String invalidFormatMessage = "Invalid XOAUTH2 encoding, the format must be of the form user={User}^Aauth=Bearer{Access Token}^A^A";
                String specifiedUser;
                String auth;

                if(splitResponse.length != 2) {
                    LOG.info(invalidFormatMessage);
                    error = new IllegalArgumentException(invalidFormatMessage);
                    throw error;
                }

                String[] userValue = splitResponse[0].split("=", 2);
                if (userValue.length == 2 && userValue[0].trim().equals("user")) {
                    specifiedUser = userValue[1].trim();
                } else {
                    LOG.info(invalidFormatMessage);
                    error = new IllegalArgumentException(invalidFormatMessage);
                    throw error;
                }

                String[] authValue = splitResponse[1].split("=", 2);
                if (authValue.length == 2 && authValue[0].trim().equals("auth")) {
                    auth = authValue[1].trim();
                } else {
                    LOG.info(invalidFormatMessage);
                    error = new IllegalArgumentException(invalidFormatMessage);
                    throw error;
                }

                if (!auth.startsWith(BEARER_PREFIX)) {
                    String msg = "Invalid XOAUTH2 encoding, the 'auth' part of response does not not begin with the expected prefix";
                    LOG.info(msg);
                    error = new IllegalArgumentException(msg);
                    throw error;
                }
                return auth.substring(BEARER_PREFIX.length()).trim();
            }

            @Override
            public boolean isComplete() {
                return complete;
            }

            @Override
            public boolean isAuthenticated() {
                return authenticated;
            }

            @Override
            public UserData getAuthenticatedUser() {
                return authenticatedUser;
            }

            private UserSessionModel findValidSession(AccessToken token, ClientModel client, RealmModel realm, KeycloakSession session) {
                UserSessionModel userSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, token.getSessionState(), false, client.getId());
                UserSessionModel offlineUserSession = null;
                if (AuthenticationManager.isSessionValid(realm, userSession)) {
                    return userSession;
                } else {
                    offlineUserSession = new UserSessionCrossDCManager(session).getUserSessionWithClient(realm, token.getSessionState(), true, client.getId());
                    if (AuthenticationManager.isOfflineSessionValid(realm, offlineUserSession)) {
                        return offlineUserSession;
                    }
                }

                if (userSession == null && offlineUserSession == null) {
                    LOG.debug("User session not found or doesn't have client attached on it");
                } else {
                    LOG.debug("Session expired");
                }
                return null;
            }

        };
    }

    interface TokenVerifier
    {
        AccessToken verifyTokenString(RealmModel realm, String tokenString, URI baseUri, KeycloakSession keycloakSession) throws VerificationException;
    }

    private AccessToken verifyRSAToken(RealmModel realm, String tokenString, URI baseUri, KeycloakSession keycloakSession) throws VerificationException {
        AccessToken token;
        RSATokenVerifier verifier = RSATokenVerifier.create(tokenString)
                                                    .realmUrl(Urls.realmIssuer(baseUri,
                                                                               realm.getName()));
        String kid = verifier.getHeader().getKeyId();
        verifier.publicKey(keycloakSession.keys().getRsaPublicKey(realm, kid));
        token = verifier.verify().getToken();
        return token;
    }
}
