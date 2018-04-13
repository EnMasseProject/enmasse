/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Map;

public class KeycloakParams {

    private static final Logger log = LoggerFactory.getLogger(KeycloakParams.class);

    private String keycloakUri;
    private String adminUser;
    private String adminPassword;
    private KeyStore keyStore;
    private String identityProviderUrl;
    private String identityProviderClientId;
    private String identityProviderClientSecret;

    public static KeycloakParams fromEnv(Map<String, String> env) throws Exception {
        KeycloakParams params = new KeycloakParams();
        params.setKeycloakUri(getEnvOrThrow(env, "KEYCLOAK_URI"));
        params.setAdminUser(getEnvOrThrow(env, "KEYCLOAK_ADMIN_USER"));
        params.setAdminPassword(getEnvOrThrow(env, "KEYCLOAK_ADMIN_PASSWORD"));

        params.setKeyStore(createKeyStore(env));

        params.setIdentityProviderUrl(env.get("OAUTH_IDENTITY_PROVIDER_URL"));
        params.setIdentityProviderClientId(env.get("OAUTH_IDENTITY_PROVIDER_CLIENT_ID"));
        params.setIdentityProviderClientSecret(env.get("OAUTH_IDENTITY_PROVIDER_CLIENT_SECRET"));

        return params;
    }

    private static KeyStore createKeyStore(Map<String, String> env) throws Exception {
        String authServiceCa = getEnvOrThrow(env, "KEYCLOAK_CERT");

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("keycloak",
                    cf.generateCertificate(new ByteArrayInputStream(authServiceCa.getBytes("UTF-8"))));

            return keyStore;
        } catch (Exception ignored) {
            log.warn("Error creating keystore for keycloak CA", ignored);
            throw ignored;
        }
    }

    private static String getEnvOrThrow(Map<String, String> envMap, String env) {
        String value = envMap.get(env);
        if (value == null) {
            throw new IllegalArgumentException("Required environment variable " + env + " is missing");
        }
        return value;
    }

    @Override
    public String toString() {
        return "{keycloakUrl=" + keycloakUri + "," +
                "adminUser=" + adminUser + "," +
                "identityProviderUrl=" + identityProviderUrl + "," +
                "identityProviderClientId=" + identityProviderClientId + "}";
    }

    public String getKeycloakUri() {
        return keycloakUri;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public String getIdentityProviderUrl() {
        return identityProviderUrl;
    }

    public String getIdentityProviderClientId() {
        return identityProviderClientId;
    }

    public String getIdentityProviderClientSecret() {
        return identityProviderClientSecret;
    }

    public void setKeycloakUri(String keycloakUri) {
        this.keycloakUri = keycloakUri;
    }

    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public void setIdentityProviderUrl(String identityProviderUrl) {
        this.identityProviderUrl = identityProviderUrl;
    }

    public void setIdentityProviderClientId(String identityProviderClientId) {
        this.identityProviderClientId = identityProviderClientId;
    }

    public void setIdentityProviderClientSecret(String identityProviderClientSecret) {
        this.identityProviderClientSecret = identityProviderClientSecret;
    }
}
