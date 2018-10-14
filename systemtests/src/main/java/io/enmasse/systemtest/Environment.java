/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import org.slf4j.Logger;

public class Environment {
    private static Logger log = CustomLogger.getLogger();
    public static final String useMinikubeEnv = "USE_MINIKUBE";
    public static final String registerApiServerEnv = "REGISTER_API_SERVER";
    public static final String keycloakAdminPasswordEnv = "KEYCLOAK_ADMIN_PASSWORD";
    public static final String keycloakAdminUserEnv = "KEYCLOAK_ADMIN_USER";
    public static final String testLogDirEnv = "OPENSHIFT_TEST_LOGDIR";
    public static final String messagingCertEnv = "OPENSHIFT_SERVER_CERT";
    public static final String useTlsEnv = "OPENSHIFT_USE_TLS";
    public static final String namespaceEnv = "OPENSHIFT_PROJECT";
    public static final String urlEnv = "OPENSHIFT_URL";
    public static final String tokenEnv = "OPENSHIFT_TOKEN";
    public static final String userEnv = "OPENSHIFT_USER";
    public static final String upgradeEnv = "SYSTEMTESTS_UPGRADED";

    private final String user = System.getenv(userEnv);
    private final String token = System.getenv(tokenEnv);
    private final String url = System.getenv(urlEnv);
    private final String namespace = System.getenv(namespaceEnv);
    private final String useTls = System.getenv(useTlsEnv);
    private final String messagingCert = System.getenv(messagingCertEnv);
    private final String testLogDir = System.getenv().getOrDefault(testLogDirEnv, "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault(keycloakAdminUserEnv, "admin");
    private final String keycloakAdminPassword = System.getenv(keycloakAdminPasswordEnv);
    private final boolean registerApiServer = Boolean.parseBoolean(System.getenv(registerApiServerEnv));
    private final boolean useMinikube = Boolean.parseBoolean(System.getenv(useMinikubeEnv));
    private final boolean upgrade = Boolean.parseBoolean(System.getenv().getOrDefault(upgradeEnv, "false"));

    public Environment() {
        String debugFormat = "{}:{}";
        log.debug(debugFormat, useMinikubeEnv, useMinikube);
        log.debug(debugFormat, registerApiServerEnv, registerApiServer);
        log.debug(debugFormat, keycloakAdminPasswordEnv, keycloakAdminPassword);
        log.debug(debugFormat, keycloakAdminUserEnv, keycloakAdminUser);
        log.debug(debugFormat, testLogDirEnv, testLogDir);
        log.debug(debugFormat, messagingCertEnv, messagingCert);
        log.debug(debugFormat, useTlsEnv, useTls);
        log.debug(debugFormat, namespaceEnv, namespace);
        log.debug(debugFormat, urlEnv, url);
        log.debug(debugFormat, tokenEnv, token);
        log.debug(debugFormat, userEnv, user);
        log.debug(debugFormat, upgradeEnv, upgrade);
    }

    /**
     * Create dummy address in shared address-spaces due to faster deploy of next addresses
     */
    private final boolean useDummyAddress = Boolean.parseBoolean(System.getenv("USE_DUMMY_ADDRESS"));

    /**
     * Skip removing address-spaces
     */
    private final boolean skipCleanup = Boolean.parseBoolean(System.getenv("SKIP_CLEANUP"));

    /**
     * Store screenshots every time
     */
    private final boolean storeScreenshots = Boolean.parseBoolean(System.getenv("STORE_SCREENSHOTS"));

    public String openShiftUrl() {
        return url;
    }

    public String openShiftToken() {
        return token;
    }

    public String openShiftUser() {
        return user;
    }

    public boolean registerApiServer() {
        return registerApiServer;
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

    public UserCredentials keycloakCredentials() {
        if (keycloakAdminUser == null || keycloakAdminPassword == null) {
            return null;
        } else {
            return new UserCredentials(keycloakAdminUser, keycloakAdminPassword);
        }
    }

    public boolean useMinikube() {
        return useMinikube;
    }

    public boolean useDummyAddress() {
        return useDummyAddress;
    }

    public boolean skipCleanup() {
        return skipCleanup;
    }

    public boolean storeScreenshots() {
        return storeScreenshots;
    }

    public boolean isUpgraded() {
        return upgrade;
    }
}
