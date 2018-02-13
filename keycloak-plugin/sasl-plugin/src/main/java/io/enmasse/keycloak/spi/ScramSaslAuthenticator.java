/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.keycloak.spi;

import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.PasswordCredentialProvider;
import org.keycloak.credential.UserCredentialStoreManager;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageManager;
import org.keycloak.storage.UserStorageProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class ScramSaslAuthenticator implements SaslServerMechanism.Instance
{
    private final KeycloakSession keycloakSession;
    private final String hostname;
    private boolean authenticated;
    private State state = State.INITIAL;
    private byte[] gs2Header;
    private String clientFirstMessageBare;
    private String username;
    private String nonce;
    private String serverFirstMessage;
    private UserData user;
    private final byte[] randomBytes;
    private final String digestName;
    private final String hmacName;
    private Map<String, Function<CredentialModel, StoredAndServerKey>> keyRetrievalFunctions;
    private CredentialModel credentialModel;

    interface StoredAndServerKey {
        byte[] getStoredKey();
        byte[] getServerKey();
    }

    public ScramSaslAuthenticator(final KeycloakSession keycloakSession,
                                  final String hostname,
                                  String digestName,
                                  String hmacName,
                                  Map<String, Function<CredentialModel, StoredAndServerKey>> keyRetrievalFunctions) {
        this.keycloakSession = keycloakSession;
        this.hostname = hostname;
        this.randomBytes = new byte[32];
        (new SecureRandom()).nextBytes(this.randomBytes);
        this.digestName = digestName;
        this.hmacName = hmacName;
        this.keyRetrievalFunctions = new HashMap<>(keyRetrievalFunctions);
    }


    enum State
    {
        INITIAL,
        SERVER_FIRST_MESSAGE_SENT,
        COMPLETE
    }


    private byte[] generateServerFirstMessage(final byte[] response) {
        String clientFirstMessage = new String(response, StandardCharsets.US_ASCII);
        if(!clientFirstMessage.startsWith("n")) {
            throw new IllegalArgumentException("Cannot parse gs2-header");
        }
        String[] parts = clientFirstMessage.split(",");
        if(parts.length < 4) {
            throw new IllegalArgumentException("Cannot parse client first message");
        }
        gs2Header = ("n,"+parts[1]+",").getBytes(StandardCharsets.US_ASCII);
        clientFirstMessageBare = clientFirstMessage.substring(gs2Header.length);
        if(!parts[2].startsWith("n=")) {
            throw new IllegalArgumentException("Cannot parse client first message");
        }
        username = decodeUsername(parts[2].substring(2));

        keycloakSession.getTransactionManager().begin();
        try {
            RealmModel realm = keycloakSession.realms().getRealmByName(hostname);
            if (realm != null) {
                UserModel userModel = keycloakSession.userStorageManager().getUserByUsername(username, realm);
                if (userModel != null) {
                    PasswordCredentialProvider passwordCredentialProvider = getPasswordCredentialProvider(realm, userModel);
                    credentialModel = passwordCredentialProvider.getPassword(realm, userModel);

                    user = new UserDataImpl(userModel.getId(), userModel.getUsername(), userModel.getGroups().stream().map(GroupModel::getName).collect(Collectors.toSet()));
                }
            }
        } finally {
            keycloakSession.getTransactionManager().commit();
        }


        if(!parts[3].startsWith("r=")) {
            throw new IllegalArgumentException("Cannot parse client first message");
        }
        nonce = parts[3].substring(2) + UUID.randomUUID().toString();

        serverFirstMessage = "r=" + nonce + ",s=" + Base64.encodeBytes(getSalt()) + ",i=" + getIterations();
        return serverFirstMessage.getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] getSalt() {
        if(credentialModel == null) {
            byte[] tmpSalt = new byte[16];
            byte[] hmac = computeHmac(this.hmacName, this.randomBytes, username);
            if(hmac.length >= tmpSalt.length) {
                System.arraycopy(hmac, 0, tmpSalt, 0, tmpSalt.length);
            } else {
                int offset = 0;
                String key = username;
                while(offset < tmpSalt.length) {
                    System.arraycopy(hmac, 0, tmpSalt, offset, Math.min(hmac.length, tmpSalt.length-offset));
                    key = key+"1";
                    hmac = computeHmac(this.hmacName, this.randomBytes, key);
                    offset+=hmac.length;
                }
            }
            return tmpSalt;
        } else {
            return credentialModel.getSalt();
        }
    }

    private int getIterations() {
        if (credentialModel == null) {
            // TODO - should come from the policy
            return 20000;
        } else {
            return credentialModel.getHashIterations();
        }
    }

    private String decodeUsername(String username) {
        if(username.contains("=")) {
            String check = username;
            while (check.contains("=")) {
                check = check.substring(check.indexOf('=') + 1);
                if (!(check.startsWith("2C") || check.startsWith("3D"))) {
                    throw new IllegalArgumentException("Invalid username");
                }
            }
            username = username.replace("=2C", ",");
            username = username.replace("=3D","=");
        }
        return username;
    }

     private static byte[] decodeBase64(String base64String) {
        base64String = base64String.replaceAll("\\s","");
        if(!base64String.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")) {
            throw new IllegalArgumentException("Cannot convert string '"+ base64String+ "'to a byte[] - it does not appear to be base64 data");
        }

        try {
            return Base64.decode(base64String);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private byte[] generateServerFinalMessage(final byte[] response) {
        try {
            String clientFinalMessage = new String(response, StandardCharsets.US_ASCII);
            String[] parts = clientFinalMessage.split(",");
            if (!parts[0].startsWith("c=")) {
                throw new IllegalArgumentException("Cannot parse client final message");
            }
            if (!Arrays.equals(gs2Header, decodeBase64(parts[0].substring(2)))) {
                throw new IllegalArgumentException("Client final message channel bind data invalid");
            }
            if (!parts[1].startsWith("r=")) {
                throw new IllegalArgumentException("Cannot parse client final message");
            }
            if (!parts[1].substring(2).equals(nonce)) {
                throw new IllegalArgumentException("Client final message has incorrect nonce value");
            }
            if (!parts[parts.length - 1].startsWith("p=")) {
                throw new IllegalArgumentException("Client final message does not have proof");
            }

            String clientFinalMessageWithoutProof =
                    clientFinalMessage.substring(0, clientFinalMessage.length() - (1 + parts[parts.length- 1].length()));
            byte[] proofBytes = decodeBase64(parts[parts.length - 1].substring(2));

            String authMessage =
                    clientFirstMessageBare + "," + serverFirstMessage + "," + clientFinalMessageWithoutProof;

            StoredAndServerKey keys;

            if (credentialModel == null) {
                keys = getStoredAndServerKeyFromPbkdf2Hash(null, hmacName, digestName);
            }  else {
                Function<CredentialModel, StoredAndServerKey> keyRetrieverFunction = this.keyRetrievalFunctions.get(credentialModel.getAlgorithm());
                if( keyRetrieverFunction != null) {
                    keys = keyRetrieverFunction.apply(credentialModel);
                } else {
                    throw new IllegalArgumentException("Unsupported algorithm: " + credentialModel.getAlgorithm());
                }
            }

            byte[] clientSignature = computeHmac(hmacName, keys.getStoredKey(), authMessage);

            for(int i = 0 ; i < proofBytes.length; i++) {
                proofBytes[i] ^= clientSignature[i];
            }


            final byte[] storedKeyFromClient = MessageDigest.getInstance(this.digestName).digest(proofBytes);

            if(user == null || !Arrays.equals(storedKeyFromClient, keys.getStoredKey())) {
                authenticated = false;
                state = State.COMPLETE;
                return null;
            } else {
                authenticated = true;
            }


            String finalResponse = "v=" + Base64.encodeBytes(computeHmac(this.hmacName, keys.getServerKey(), authMessage));

            return finalResponse.getBytes(StandardCharsets.US_ASCII);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    static StoredAndServerKey getStoredAndServerKeyFromScramHash(CredentialModel credentialModel, String hmacName, String digestName) {
        try {
            StoredAndServerKey keys;
            if (credentialModel == null) {
                return getStoredAndServerKeyFromPbkdf2Hash(credentialModel, hmacName, digestName);
            }
            String[] storedAndServerKeys = credentialModel.getValue().split("\\|", 2);
            byte[] storedKey = Base64.decode(storedAndServerKeys[0]);
            byte[] serverKey = Base64.decode(storedAndServerKeys[1]);
            keys = new StoredAndServerKey() {
                @Override
                public byte[] getStoredKey() {
                    return storedKey;
                }

                @Override
                public byte[] getServerKey() {
                    return serverKey;
                }
            };
            return keys;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    static StoredAndServerKey getStoredAndServerKeyFromPbkdf2Hash(CredentialModel credentialModel, String hamcName, String digestName) {
        try {
            StoredAndServerKey keys;
            byte[] saltedPassword = getSaltedPassword(credentialModel);
            byte[] clientKey = computeHmac(hamcName, saltedPassword, "Client Key");

            byte[] storedKey = MessageDigest.getInstance(digestName).digest(clientKey);
            byte[] serverKey = computeHmac(hamcName, saltedPassword, "Server Key");
            keys = new StoredAndServerKey() {
                @Override
                public byte[] getStoredKey() {
                    return storedKey;
                }

                @Override
                public byte[] getServerKey() {
                    return serverKey;
                }
            };
            return keys;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static byte[] getSaltedPassword(CredentialModel credentialModel) {
        if(credentialModel == null) {
            byte[] password = new byte[20];
            (new SecureRandom()).nextBytes(password);
            return password;
        } else {
            byte[] storedValue = decodeBase64(credentialModel.getValue());
            byte[] saltedPassword = new byte[20];
            System.arraycopy(storedValue, 0, saltedPassword, 0, 20);
            return saltedPassword;
        }

    }

    private PasswordCredentialProvider getPasswordCredentialProvider(final RealmModel realm, final UserModel user) {
        PasswordCredentialProvider passwordCredentialProvider = null;

        if (!StorageId.isLocalStorage(user)) {
            String providerId = StorageId.resolveProviderId(user);
            UserStorageProvider provider = UserStorageManager.getStorageProvider(keycloakSession, realm, providerId);
            if (provider instanceof PasswordCredentialProvider) {
                passwordCredentialProvider = (PasswordCredentialProvider)provider;
            }
        } else {
            if (user.getFederationLink() != null) {
                UserStorageProvider provider = UserStorageManager.getStorageProvider(keycloakSession, realm, user.getFederationLink());
                if (provider != null && provider instanceof PasswordCredentialProvider) {
                    passwordCredentialProvider = (PasswordCredentialProvider)provider;
                }
            }
        }
        if(passwordCredentialProvider == null) {
            List<CredentialInputValidator>
                    credentialProviders = UserCredentialStoreManager.getCredentialProviders(keycloakSession, realm, CredentialInputValidator.class);
            for (CredentialInputValidator validator : credentialProviders) {
                if (validator != null && validator instanceof PasswordCredentialProvider) {
                    passwordCredentialProvider = (PasswordCredentialProvider)validator;
                    break;
                }
            }
        }
        return passwordCredentialProvider;
    }


    @Override
    public boolean isComplete() {
        return state == State.COMPLETE;
    }


    @Override
    public byte[] processResponse(byte[] response) throws IllegalArgumentException {

        byte[] challenge;
        switch (state) {
            case INITIAL:
                challenge = generateServerFirstMessage(response);
                state = State.SERVER_FIRST_MESSAGE_SENT;
                break;
            case SERVER_FIRST_MESSAGE_SENT:
                challenge = generateServerFinalMessage(response);
                state = State.COMPLETE;
                break;
            case COMPLETE:
                if(response == null || response.length == 0) {
                    challenge = new byte[0];
                    break;
                }
            default:
                throw new IllegalArgumentException("No response expected in state " + state);

        }
        return challenge;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public UserData getAuthenticatedUser() {
        return user;
    }

    private static byte[] computeHmac(String hmacName, final byte[] key, final String string) {
        Mac mac = createShaHmac(hmacName, key);
        mac.update(string.getBytes(StandardCharsets.US_ASCII));
        return mac.doFinal();
    }


    private static Mac createShaHmac(String hmacName, final byte[] keyBytes) {
        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, hmacName);
            Mac mac = Mac.getInstance(hmacName);
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
