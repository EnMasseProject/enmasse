/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.client.Config;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

public class Environment {
    public static final String KEYCLOAK_ADMIN_PASSWORD_ENV = "KEYCLOAK_ADMIN_PASSWORD";
    public static final String KEYCLOAK_ADMIN_USER_ENV = "KEYCLOAK_ADMIN_USER";
    public static final String TEST_LOG_DIR_ENV = "TEST_LOGDIR";
    public static final String K8S_NAMESPACE_ENV = "KUBERNETES_NAMESPACE";
    public static final String K8S_API_URL_ENV = "KUBERNETES_API_URL";
    public static final String K8S_API_TOKEN_ENV = "KUBERNETES_API_TOKEN";
    public static final String ENMASSE_VERSION_SYSTEM_PROPERTY = "enmasse.version";
    public static final String K8S_DOMAIN_ENV = "KUBERNETES_DOMAIN";
    public static final String K8S_API_CONNECT_TIMEOUT = "KUBERNETES_API_CONNECT_TIMEOUT";
    public static final String K8S_API_READ_TIMEOUT = "KUBERNETES_API_READ_TIMEOUT";
    public static final String K8S_API_WRITE_TIMEOUT = "KUBERNETES_API_WRITE_TIMEOUT";
    public static final String UPGRADE_TEPLATES_ENV = "UPGRADE_TEMPLATES";
    public static final String START_TEMPLATES_ENV = "START_TEMPLATES";
    public static final String TEMPLATES_PATH = "TEMPLATES";
    public static final String SKIP_CLEANUP_ENV = "SKIP_CLEANUP";
    public static final String SKIP_UNNSTALL = "SKIP_UNINSTALL";
    public static final String STORE_SCREENSHOTS_ENV = "STORE_SCREENSHOTS";
    public static final String MONITORING_NAMESPACE_ENV = "MONITORING_NAMESPACE";
    public static final String TAG_ENV = "TAG";
    public static final String PRODUCT_NAME_ENV = "PRODUCT_NAME";
    public static final String APP_NAME_ENV = "APP_NAME";
    public static final String INSTALL_TYPE = "INSTALL_TYPE";
    public static final String OLM_INSTALL_TYPE = "OLM_INSTALL_TYPE";
    private static final String SKIP_SAVE_STATE = "SKIP_SAVE_STATE";
    private static final String SKIP_DEPLOY_INFINISPAN = "SKIP_DEPLOY_INFINISPAN";
    private static Logger log = CustomLogger.getLogger();
    private static Environment instance;
    private final String namespace = System.getenv().getOrDefault(K8S_NAMESPACE_ENV, "enmasse-infra");
    private final String testLogDir = System.getenv().getOrDefault(TEST_LOG_DIR_ENV, "/tmp/testlogs");
    private final String keycloakAdminUser = System.getenv().getOrDefault(KEYCLOAK_ADMIN_USER_ENV, "admin");
    private final String keycloakAdminPassword = System.getenv(KEYCLOAK_ADMIN_PASSWORD_ENV);
    private final String enmasseVersion = System.getProperty(ENMASSE_VERSION_SYSTEM_PROPERTY);
    private final String kubernetesDomain = System.getenv().getOrDefault(K8S_DOMAIN_ENV, "nip.io");
    private final String startTemplates = System.getenv().getOrDefault(START_TEMPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private final String upgradeTemplates = System.getenv().getOrDefault(UPGRADE_TEPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private final String monitoringNamespace = System.getenv().getOrDefault(MONITORING_NAMESPACE_ENV, "enmasse-monitoring");
    private final String tag = System.getenv().getOrDefault(TAG_ENV, "latest");
    private final String appName = System.getenv().getOrDefault(APP_NAME_ENV, "enmasse");
    private final String productName = System.getenv().getOrDefault(PRODUCT_NAME_ENV, "enmasse");
    private final boolean skipSaveState = Boolean.parseBoolean(System.getenv(SKIP_SAVE_STATE));
    private final boolean skipDeployInfinispan = Boolean.parseBoolean(System.getenv(SKIP_DEPLOY_INFINISPAN));
    private final Duration kubernetesApiConnectTimeout = Optional.ofNullable(System.getenv().get(K8S_API_CONNECT_TIMEOUT)).map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));
    private final Duration kubernetesApiReadTimeout = Optional.ofNullable(System.getenv().get(K8S_API_READ_TIMEOUT)).map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));
    private final Duration kubernetesApiWriteTimeout = Optional.ofNullable(System.getenv().get(K8S_API_WRITE_TIMEOUT)).map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));
    private final EnmasseInstallType installType = Optional.ofNullable(System.getenv().get(INSTALL_TYPE)).map(EnmasseInstallType::valueOf).orElse(EnmasseInstallType.BUNDLE);
    private final OLMInstallationType olmInstallType = Optional.ofNullable(System.getenv().get(OLM_INSTALL_TYPE)).map(s->s.isEmpty() ? OLMInstallationType.SPECIFIC.name() : s)
            .map(OLMInstallationType::valueOf).orElse(OLMInstallationType.SPECIFIC);
    protected String templatesPath = System.getenv().getOrDefault(TEMPLATES_PATH,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    protected UserCredentials managementCredentials = new UserCredentials(null, null);
    protected UserCredentials defaultCredentials = new UserCredentials(null, null);
    protected UserCredentials sharedManagementCredentials = new UserCredentials("artemis-admin", "artemis-admin");
    protected UserCredentials sharedDefaultCredentials = new UserCredentials("test", "test");
    /**
     * Skip removing address-spaces
     */
    private final boolean skipCleanup = Boolean.parseBoolean(System.getenv().getOrDefault(SKIP_CLEANUP_ENV, "false"));
    private final boolean skipUninstall = Boolean.parseBoolean(System.getenv().getOrDefault(SKIP_UNNSTALL, "false"));
    /**
     * Store screenshots every time
     */
    private final boolean storeScreenshots = Boolean.parseBoolean(System.getenv(STORE_SCREENSHOTS_ENV));
    private String token = System.getenv(K8S_API_TOKEN_ENV);
    private String url = System.getenv(K8S_API_URL_ENV);

    private Environment() {
        if (token == null || url == null) {
            Config config = Config.autoConfigure(System.getenv()
                    .getOrDefault("TEST_CLUSTER_CONTEXT", null));
            token = config.getOauthToken();
            url = config.getMasterUrl();
        }
        String debugFormat = "{}:{}";
        log.info(debugFormat, INSTALL_TYPE, installType.name());
        if (installType == EnmasseInstallType.OLM) {
            log.info(debugFormat, OLM_INSTALL_TYPE, olmInstallType.name());
        }
        log.info(debugFormat, TEST_LOG_DIR_ENV, testLogDir);
        log.info(debugFormat, K8S_NAMESPACE_ENV, namespace);
        log.info(debugFormat, K8S_API_URL_ENV, url);
        log.info(debugFormat, K8S_API_TOKEN_ENV, token);
        log.info(debugFormat, ENMASSE_VERSION_SYSTEM_PROPERTY, enmasseVersion);
        log.info(debugFormat, SKIP_CLEANUP_ENV, skipCleanup);
        log.info(debugFormat, K8S_DOMAIN_ENV, kubernetesDomain);
        log.info(debugFormat, APP_NAME_ENV, appName);
        log.info(debugFormat, TEMPLATES_PATH, templatesPath);
        log.info(debugFormat, PRODUCT_NAME_ENV, productName);
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
    }

    public EnmasseInstallType installType() {
        return installType;
    }

    public OLMInstallationType olmInstallType() {
        return olmInstallType;
    }

    public String getApiUrl() {
        return url;
    }

    public String getApiToken() {
        return token;
    }

    public String namespace() {
        return namespace;
    }

    public Path testLogDir() {
        return Paths.get(testLogDir);
    }

    public UserCredentials keycloakCredentials() {
        if (keycloakAdminUser == null || keycloakAdminPassword == null) {
            return null;
        } else {
            return new UserCredentials(keycloakAdminUser, keycloakAdminPassword);
        }
    }

    public boolean skipCleanup() {
        return skipCleanup;
    }

    public boolean skipUninstall() {
        return skipUninstall;
    }

    public boolean storeScreenshots() {
        return storeScreenshots;
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

    public String getMonitoringNamespace() {
        return monitoringNamespace;
    }

    public String getTag() {
        return tag;
    }

    public String getAppName() {
        return appName;
    }

    public String getProductName() {
        return productName;
    }

    public UserCredentials getManagementCredentials() {
        return managementCredentials;
    }

    public UserCredentials getDefaultCredentials() {
        return defaultCredentials;
    }

    public UserCredentials getSharedManagementCredentials() {
        return sharedManagementCredentials;
    }

    public UserCredentials getSharedDefaultCredentials() {
        return sharedDefaultCredentials;
    }

    public boolean isSkipSaveState() {
        return this.skipSaveState;
    }

    public boolean isSkipDeployInfinispan() {
        return this.skipDeployInfinispan;
    }

    public String getTemplatesPath() {
        return templatesPath;
    }

    public Duration getKubernetesApiConnectTimeout() {
        return kubernetesApiConnectTimeout;
    }

    public Duration getKubernetesApiReadTimeout() {
        return kubernetesApiReadTimeout;
    }

    public Duration getKubernetesApiWriteTimeout() {
        return kubernetesApiWriteTimeout;
    }
}
