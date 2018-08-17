/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class ApiServerOptions {
    private static final Logger log = LoggerFactory.getLogger(ApiServerOptions.class);
    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";
    private String namespace;
    private String certDir;
    private Duration resyncInterval;
    private String clientCa;
    private String requestHeaderClientCa;
    private boolean enableRbac;
    private String keycloakUri;
    private String keycloakAdminUser;
    private String keycloakAdminPassword;
    private KeyStore keycloakTrustStore;

    public static ApiServerOptions fromEnv(Map<String, String> env) {

        ApiServerOptions options = new ApiServerOptions();

        options.setNamespace(getEnv(env, "NAMESPACE")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "namespace"))));

        options.setCertDir(getEnv(env, "CERT_DIR").orElse("/api-server-cert"));

        options.setClientCa(getEnv(env, "CLIENT_CA").orElse(null));
        options.setRequestHeaderClientCa(getEnv(env, "REQUEST_HEADER_CLIENT_CA").orElse(null));

        options.setResyncInterval(getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setEnableRbac(Boolean.parseBoolean(getEnv(env, "ENABLE_RBAC").orElse("false")));

        getEnv(env, "KEYCLOAK_URI").ifPresent(options::setKeycloakUri);
        getEnv(env, "KEYCLOAK_ADMIN_USER").ifPresent(options::setKeycloakAdminUser);
        getEnv(env, "KEYCLOAK_ADMIN_PASSWORD").ifPresent(options::setKeycloakAdminPassword);
        getEnv(env, "KEYCLOAK_URI").ifPresent(options::setKeycloakUri);
        getEnv(env, "KEYCLOAK_CERT").ifPresent(ca -> options.setKeycloakTrustStore(createKeyStore(ca)));

        return options;
    }

    private static KeyStore createKeyStore(String authServiceCa) {

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keyStore.setCertificateEntry("keycloak",
                    cf.generateCertificate(new ByteArrayInputStream(authServiceCa.getBytes("UTF-8"))));

            return keyStore;
        } catch (Exception ignored) {
            log.warn("Error creating keystore for keycloak CA. Ignoring", ignored);
            return null;
        }
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCertDir() {
        return certDir;
    }

    public void setCertDir(String certDir) {
        this.certDir = certDir;
    }

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public void setResyncInterval(Duration resyncInterval) {
        this.resyncInterval = resyncInterval;
    }

    private void setKeycloakUri(String keycloakUri) {
        this.keycloakUri = keycloakUri;
    }


    public void setClientCa(String clientCa) {
        this.clientCa = clientCa;
    }

    public String getClientCa() {
        return clientCa;
    }

    public boolean isEnableRbac() {
        return enableRbac;
    }

    public void setEnableRbac(boolean enableRbac) {
        this.enableRbac = enableRbac;
    }

    public void setRequestHeaderClientCa(String clientCa) {
        this.requestHeaderClientCa = clientCa;
    }

    public String getRequestHeaderClientCa() {
        return requestHeaderClientCa;
    }

    public void setKeycloakAdminUser(String keycloakAdminUser) {
        this.keycloakAdminUser = keycloakAdminUser;
    }

    public String getKeycloakUri() {
        return keycloakUri;
    }

    public String getKeycloakAdminUser() {
        return keycloakAdminUser;
    }

    public String getKeycloakAdminPassword() {
        return keycloakAdminPassword;
    }

    public void setKeycloakAdminPassword(String keycloakAdminPassword) {
        this.keycloakAdminPassword = keycloakAdminPassword;
    }

    public KeyStore getKeycloakTrustStore() {
        return keycloakTrustStore;
    }

    public void setKeycloakTrustStore(KeyStore keycloakTrustStore) {
        this.keycloakTrustStore = keycloakTrustStore;
    }
}
