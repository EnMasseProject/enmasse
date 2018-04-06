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

    private final String keycloakUri;
    private final String adminUser;
    private final String adminPassword;
    private final KeyStore keyStore;

    public KeycloakParams(String keycloakUri, String adminUser, String adminPassword, KeyStore keyStore) {
        this.keycloakUri = keycloakUri;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.keyStore = keyStore;
    }

    public static KeycloakParams fromEnv(Map<String, String> env) throws Exception {
        String keycloakUri = getEnvOrThrow(env, "KEYCLOAK_URI");
        String adminUser = getEnvOrThrow(env, "KEYCLOAK_ADMIN_USER");
        String adminPassword = getEnvOrThrow(env, "KEYCLOAK_ADMIN_PASSWORD");
        KeyStore keyStore = createKeyStore(env);

        return new KeycloakParams(keycloakUri, adminUser, adminPassword, keyStore);
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

}
