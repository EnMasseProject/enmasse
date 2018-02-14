/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

public class Environment {
    private final String user = System.getenv("OPENSHIFT_USER");
    private final String token = System.getenv("OPENSHIFT_TOKEN");
    private final String url = System.getenv("OPENSHIFT_URL");
    private final String namespace = System.getenv("OPENSHIFT_PROJECT");
    private final String useTls = System.getenv("OPENSHIFT_USE_TLS");
    private final String messagingCert = System.getenv("OPENSHIFT_SERVER_CERT");
    private final String testLogDir = System.getenv().getOrDefault("OPENSHIFT_TEST_LOGDIR", "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault("KEYCLOAK_ADMIN_USER", "admin");
    private final String keycloakAdminPassword = System.getenv("KEYCLOAK_ADMIN_PASSWORD");
    private final boolean useMinikube = Boolean.parseBoolean(System.getenv("USE_MINIKUBE"));

    public String openShiftUrl() {
        return url;
    }

    public String openShiftToken() {
        return token;
    }

    public String openShiftUser() {
        return user;
    }

    public boolean useTLS() {
        return (useTls != null && useTls.toLowerCase().equals("true"));
    }

    public String messagingCert() {
        return this.messagingCert;
    }

    public String namespace() {
        return namespace;
    }

    public String testLogDir() {
        return testLogDir;
    }

    public KeycloakCredentials keycloakCredentials() {
        if (keycloakAdminUser == null || keycloakAdminPassword == null) {
            return null;
        } else {
            return new KeycloakCredentials(keycloakAdminUser, keycloakAdminPassword);
        }
    }

    public boolean useMinikube() {
        return useMinikube;
    }
}
