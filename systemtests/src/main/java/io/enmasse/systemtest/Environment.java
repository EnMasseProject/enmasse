/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import org.slf4j.Logger;

import java.nio.file.Paths;

public class Environment {
    private static Logger log = CustomLogger.getLogger();
    private static Environment instance;

    public static final String useMinikubeEnv = "USE_MINIKUBE";
    public static final String ocpVersionEnv = "OC_VERSION";
    public static final String keycloakAdminPasswordEnv = "KEYCLOAK_ADMIN_PASSWORD";
    public static final String keycloakAdminUserEnv = "KEYCLOAK_ADMIN_USER";
    public static final String testLogDirEnv = "TEST_LOGDIR";
    public static final String namespaceEnv = "KUBERNETES_NAMESPACE";
    public static final String urlEnv = "KUBERNETES_API_URL";
    public static final String tokenEnv = "KUBERNETES_API_TOKEN";
    public static final String enmasseVersionProp = "enmasse.version";
    public static final String domain = "KUBERNETES_DOMAIN";
    public static final String upgradeTemplatesEnv = "UPGRADE_TEMPLATES";
    public static final String startTemplatesEnv = "START_TEMPLATES";
    public static final String skipCleanupEnv = "SKIP_CLEANUP";
    public static final String downstreamEnv = "DOWNSTREAM";

    private final String token = System.getenv(tokenEnv);
    private final String url = System.getenv(urlEnv);
    private final String namespace = System.getenv(namespaceEnv);
    private final String testLogDir = System.getenv().getOrDefault(testLogDirEnv, "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault(keycloakAdminUserEnv, "admin");
    private final String keycloakAdminPassword = System.getenv(keycloakAdminPasswordEnv);
    private final boolean useMinikube = Boolean.parseBoolean(System.getenv(useMinikubeEnv));
    private final String ocpVersion = System.getenv().getOrDefault(ocpVersionEnv, "3.11");
    private final String enmasseVersion = System.getProperty(enmasseVersionProp);
    private final String kubernetesDomain = System.getenv().getOrDefault(domain, "nip.io");
    private final boolean downstream = Boolean.parseBoolean(System.getenv().getOrDefault(downstreamEnv, "false"));
    private final String startTemplates = System.getenv().getOrDefault(startTemplatesEnv,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private final String upgradeTemplates = System.getenv().getOrDefault(upgradeTemplatesEnv,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-0.26.5").toString());

    private Environment() {
        String debugFormat = "{}:{}";
        log.info(debugFormat, useMinikubeEnv, useMinikube);
        log.info(debugFormat, keycloakAdminPasswordEnv, keycloakAdminPassword);
        log.info(debugFormat, keycloakAdminUserEnv, keycloakAdminUser);
        log.info(debugFormat, testLogDirEnv, testLogDir);
        log.info(debugFormat, namespaceEnv, namespace);
        log.info(debugFormat, urlEnv, url);
        log.info(debugFormat, tokenEnv, token);
        log.info(debugFormat, enmasseVersionProp, enmasseVersion);
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
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

    public String getGetOcpVersion() {
        return ocpVersion;
    }

    public String enmasseVersion() {
        return enmasseVersion;
    }

    public String kubernetesDomain() {
        return kubernetesDomain;
    }

    public String getUpgradeTemplates() {
        return upgradeTemplates;
    }

    public String getStartTemplates() {
        return startTemplates;
    }

    public boolean isDownstream() {
        return downstream;
    }
}
