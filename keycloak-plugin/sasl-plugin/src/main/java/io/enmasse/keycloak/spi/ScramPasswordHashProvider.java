/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.common.util.Base64;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserCredentialModel;

public class ScramPasswordHashProvider implements PasswordHashProvider
{

    private final String providerId;
    private final String hmacName;
    private final String digestName;
    private final int defaultIterations;

    private static final byte[] INT_1 = new byte[]{0, 0, 0, 1};

    public ScramPasswordHashProvider(String providerId,
                                     int defaultIterations,
                                     final String hmacName,
                                     final String digestName) {
        this.providerId = providerId;
        this.defaultIterations = defaultIterations;
        this.hmacName = hmacName;
        this.digestName = digestName;
    }

    @Override
    public boolean policyCheck(PasswordPolicy policy, CredentialModel credential) {
        return credential.getHashIterations() == policy.getHashIterations() && providerId.equals(credential.getAlgorithm());
    }

    @Override
    public void encode(String rawPassword, int iterations, CredentialModel credential) {
        if (iterations == -1) {
            iterations = defaultIterations;
        }

        byte[] salt = getSalt();
        String encodedPassword = encode(rawPassword, iterations, salt);

        credential.setAlgorithm(providerId);
        credential.setType(UserCredentialModel.PASSWORD);
        credential.setSalt(salt);
        credential.setHashIterations(iterations);
        credential.setValue(encodedPassword);
    }

    @Override
    public boolean verify(String rawPassword, CredentialModel credential) {

        String encodedPassword = encode(rawPassword, credential.getHashIterations(), credential.getSalt());

        String storedCredential = credential.getValue();
        return encodedPassword.equals(storedCredential);
    }

    @Override
    public void close() {
    }

    private byte[] createSaltedPassword(byte[] salt, String password, int iterationCount) {
        Mac mac = createShaHmac(password.getBytes(StandardCharsets.US_ASCII));

        mac.update(salt);
        mac.update(INT_1);
        byte[] result = mac.doFinal();

        byte[] previous = null;
        for(int i = 1; i < iterationCount; i++) {
            mac.update(previous != null? previous: result);
            previous = mac.doFinal();
            for(int x = 0; x < result.length; x++) {
                result[x] ^= previous[x];
            }
        }

        return result;

    }

    private byte[] computeHmac(final byte[] key, final String string) {
        Mac mac = createShaHmac(key);
        mac.update(string.getBytes(StandardCharsets.US_ASCII));
        return mac.doFinal();
    }


    private Mac createShaHmac(final byte[] keyBytes) {
        try {
            SecretKeySpec key = new SecretKeySpec(keyBytes, getHmacName());
            Mac mac = Mac.getInstance(getHmacName());
            mac.init(key);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private String getHmacName() {
        return hmacName;
    }

    private String getDigestName() {
        return digestName;
    }

    private String encode(String rawPassword, int iterations, byte[] salt) {
        try {
            byte[] saltedPassword = createSaltedPassword(salt, rawPassword, iterations);
            byte[] clientKey = computeHmac(saltedPassword, "Client Key");

            byte[] storedKey = MessageDigest.getInstance(getDigestName()).digest(clientKey);
            byte[] serverKey = computeHmac(saltedPassword, "Server Key");

            return Base64.encodeBytes(storedKey) + "|" + Base64.encodeBytes(serverKey);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getSalt() {
        byte[] buffer = new byte[16];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

}
