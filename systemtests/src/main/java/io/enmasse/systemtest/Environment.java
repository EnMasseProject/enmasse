/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import org.slf4j.Logger;

import java.nio.file.Paths;

public class Environment {
    private static Logger log = CustomLogger.getLogger();
    private static Environment instance;

    public static final String USE_MINUKUBE_ENV = "USE_MINIKUBE";
    public static final String OCP_VERSION_ENV = "OC_VERSION";
    public static final String KEYCLOAK_ADMIN_PASSWORD_ENV = "KEYCLOAK_ADMIN_PASSWORD";
    public static final String KEYCLOAK_ADMIN_USER_ENV = "KEYCLOAK_ADMIN_USER";
    public static final String TEST_LOG_DIR_ENV = "TEST_LOGDIR";
    public static final String K8S_NAMESPACE_ENV = "KUBERNETES_NAMESPACE";
    public static final String K8S_API_URL_ENV = "KUBERNETES_API_URL";
    public static final String K8S_API_TOKEN_ENV = "KUBERNETES_API_TOKEN";
    public static final String ENMASSE_VERSION_SYSTEM_PROPERTY = "enmasse.version";
    public static final String K8S_DOMAIN_ENV = "KUBERNETES_DOMAIN";
    public static final String UPGRADE_TEPLATES_ENV = "UPGRADE_TEMPLATES";
    public static final String START_TEMPLATES_ENV = "START_TEMPLATES";
    public static final String SKIP_CLEANUP_ENV = "SKIP_CLEANUP";
    public static final String DOWNSTREAM_ENV = "DOWNSTREAM";
    public static final String STORE_SCREENSHOTS_ENV = "STORE_SCREENSHOTS";

    public static final String IS_OCP4_REGEXP = "4.*";

    private final String token = System.getenv(K8S_API_TOKEN_ENV);
    private final String url = System.getenv(K8S_API_URL_ENV);
    private final String namespace = System.getenv(K8S_NAMESPACE_ENV);
    private final String testLogDir = System.getenv().getOrDefault(TEST_LOG_DIR_ENV, "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault(KEYCLOAK_ADMIN_USER_ENV, "admin");
    private final String keycloakAdminPassword = System.getenv(KEYCLOAK_ADMIN_PASSWORD_ENV);
    private final boolean useMinikube = Boolean.parseBoolean(System.getenv(USE_MINUKUBE_ENV));
    private final String ocpVersion = System.getenv().getOrDefault(OCP_VERSION_ENV, "3.11");
    private final String enmasseVersion = System.getProperty(ENMASSE_VERSION_SYSTEM_PROPERTY);
    private final String kubernetesDomain = System.getenv().getOrDefault(K8S_DOMAIN_ENV, "nip.io");
    private final boolean downstream = Boolean.parseBoolean(System.getenv().getOrDefault(DOWNSTREAM_ENV, "false"));
    private final String startTemplates = System.getenv().getOrDefault(START_TEMPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private final String upgradeTemplates = System.getenv().getOrDefault(UPGRADE_TEPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-0.26.5").toString());

    private Environment() {
        String debugFormat = "{}:{}";
        log.info(debugFormat, USE_MINUKUBE_ENV, useMinikube);
        log.info(debugFormat, KEYCLOAK_ADMIN_PASSWORD_ENV, keycloakAdminPassword);
        log.info(debugFormat, KEYCLOAK_ADMIN_USER_ENV, keycloakAdminUser);
        log.info(debugFormat, TEST_LOG_DIR_ENV, testLogDir);
        log.info(debugFormat, K8S_NAMESPACE_ENV, namespace);
        log.info(debugFormat, K8S_API_URL_ENV, url);
        log.info(debugFormat, K8S_API_TOKEN_ENV, token);
        log.info(debugFormat, ENMASSE_VERSION_SYSTEM_PROPERTY, enmasseVersion);
        log.info(debugFormat, SKIP_CLEANUP_ENV, skipCleanup);
        log.info(debugFormat, K8S_DOMAIN_ENV, kubernetesDomain);
        if(!useMinikube) {
            log.info(debugFormat, OCP_VERSION_ENV, ocpVersion);
        }
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    /**
     * Skip removing address-spaces
     */
    private final boolean skipCleanup = Boolean.parseBoolean(System.getenv(SKIP_CLEANUP_ENV));

    /**
     * Store screenshots every time
     */
    private final boolean storeScreenshots = Boolean.parseBoolean(System.getenv(STORE_SCREENSHOTS_ENV));


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

    public boolean isOcp4() {
        return !useMinikube && ocpVersion.matches(IS_OCP4_REGEXP);
    }
}
