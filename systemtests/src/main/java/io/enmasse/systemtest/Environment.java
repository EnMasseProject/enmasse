/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import org.slf4j.Logger;

public class Environment {
    private static Logger log = CustomLogger.getLogger();
    public static final String useMinikubeEnv = "USE_MINIKUBE";
    public static final String keycloakAdminPasswordEnv = "KEYCLOAK_ADMIN_PASSWORD";
    public static final String keycloakAdminUserEnv = "KEYCLOAK_ADMIN_USER";
    public static final String testLogDirEnv = "TEST_LOGDIR";
    public static final String namespaceEnv = "KUBERNETES_NAMESPACE";
    public static final String urlEnv = "KUBERNETES_API_URL";
    public static final String tokenEnv = "KUBERNETES_API_TOKEN";
    public static final String upgradeEnv = "SYSTEMTESTS_UPGRADED";

    private final String token = System.getenv(tokenEnv);
    private final String url = System.getenv(urlEnv);
    private final String namespace = System.getenv(namespaceEnv);
    private final String testLogDir = System.getenv().getOrDefault(testLogDirEnv, "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault(keycloakAdminUserEnv, "admin");
    private final String keycloakAdminPassword = System.getenv(keycloakAdminPasswordEnv);
    private final boolean useMinikube = Boolean.parseBoolean(System.getenv(useMinikubeEnv));
    private final boolean upgrade = Boolean.parseBoolean(System.getenv().getOrDefault(upgradeEnv, "false"));

    public Environment() {
        String debugFormat = "{}:{}";
        log.info(debugFormat, useMinikubeEnv, useMinikube);
        log.info(debugFormat, keycloakAdminPasswordEnv, keycloakAdminPassword);
        log.info(debugFormat, keycloakAdminUserEnv, keycloakAdminUser);
        log.info(debugFormat, testLogDirEnv, testLogDir);
        log.info(debugFormat, namespaceEnv, namespace);
        log.info(debugFormat, urlEnv, url);
        log.info(debugFormat, tokenEnv, token);
        log.info(debugFormat, upgradeEnv, upgrade);
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

    public String getApiUrl() {
        return url;
    }

    public String getApiToken() {
        return token;
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
