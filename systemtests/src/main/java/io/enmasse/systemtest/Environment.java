/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import io.enmasse.systemtest.logs.CustomLogger;
import io.fabric8.kubernetes.client.Config;
import org.slf4j.Logger;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

public class Environment {
    private static final String TEST_LOG_DIR_ENV = "TEST_LOGDIR";
    private static final String K8S_NAMESPACE_ENV = "KUBERNETES_NAMESPACE";
    private static final String K8S_API_URL_ENV = "KUBERNETES_API_URL";
    private static final String K8S_API_TOKEN_ENV = "KUBERNETES_API_TOKEN";
    private static final String ENMASSE_VERSION_SYSTEM_PROPERTY = "enmasse.version";
    private static final String K8S_DOMAIN_ENV = "KUBERNETES_DOMAIN";
    private static final String K8S_API_CONNECT_TIMEOUT = "KUBERNETES_API_CONNECT_TIMEOUT";
    private static final String K8S_API_READ_TIMEOUT = "KUBERNETES_API_READ_TIMEOUT";
    private static final String K8S_API_WRITE_TIMEOUT = "KUBERNETES_API_WRITE_TIMEOUT";
    private static final String UPGRADE_TEPLATES_ENV = "UPGRADE_TEMPLATES";
    private static final String START_TEMPLATES_ENV = "START_TEMPLATES";
    private static final String TEMPLATES_PATH = "TEMPLATES";
    private static final String SKIP_CLEANUP_ENV = "SKIP_CLEANUP";
    private static final String SKIP_UNNSTALL = "SKIP_UNINSTALL";
    private static final String DOWNSTREAM_ENV = "DOWNSTREAM";
    private static final String STORE_SCREENSHOTS_ENV = "STORE_SCREENSHOTS";
    private static final String MONITORING_NAMESPACE_ENV = "MONITORING_NAMESPACE";
    private static final String TAG_ENV = "TAG";
    private static final String APP_NAME_ENV = "APP_NAME";
    private static final String SKIP_SAVE_STATE = "SKIP_SAVE_STATE";
    private static final String SKIP_DEPLOY_INFINISPAN = "SKIP_DEPLOY_INFINISPAN";
    private static Logger LOGGER = CustomLogger.getLogger();
    private static Environment instance;
    private final String namespace = System.getenv().getOrDefault(K8S_NAMESPACE_ENV, "enmasse-infra");
    private final String testLogDir = System.getenv().getOrDefault(TEST_LOG_DIR_ENV, "/tmp/testlogs");
    private final String enmasseVersion = System.getProperty(ENMASSE_VERSION_SYSTEM_PROPERTY);
    private final String kubernetesDomain = System.getenv().getOrDefault(K8S_DOMAIN_ENV, "nip.io");
    private final boolean downstream = Boolean.parseBoolean(System.getenv().getOrDefault(DOWNSTREAM_ENV, "false"));
    private final String startTemplates = System.getenv().getOrDefault(START_TEMPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private final String upgradeTemplates = System.getenv().getOrDefault(UPGRADE_TEPLATES_ENV,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-0.26.5").toString());
    private final String monitoringNamespace = System.getenv().getOrDefault(MONITORING_NAMESPACE_ENV, "enmasse-monitoring");
    private final String tag = System.getenv().getOrDefault(TAG_ENV, "latest");
    private final String appName = System.getenv().getOrDefault(APP_NAME_ENV, "enmasse");
    private final boolean skipSaveState = Boolean.parseBoolean(System.getenv(SKIP_SAVE_STATE));
    private final boolean skipDeployInfinispan = Boolean.parseBoolean(System.getenv(SKIP_DEPLOY_INFINISPAN));
    private final Duration kubernetesApiConnectTimeout = Optional.ofNullable(System.getenv().get(K8S_API_CONNECT_TIMEOUT))
            .map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));
    private final Duration kubernetesApiReadTimeout = Optional.ofNullable(System.getenv().get(K8S_API_READ_TIMEOUT))
            .map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));
    private final Duration kubernetesApiWriteTimeout = Optional.ofNullable(System.getenv().get(K8S_API_WRITE_TIMEOUT))
            .map(i -> Duration.ofSeconds(Long.parseLong(i))).orElse(Duration.ofSeconds(60));

    private String templatesPath = System.getenv().getOrDefault(TEMPLATES_PATH,
            Paths.get(System.getProperty("user.dir"), "..", "templates", "build", "enmasse-latest").toString());
    private UserCredentials managementCredentials = new UserCredentials(null, null);
    protected UserCredentials defaultCredentials = new UserCredentials(null, null);
    private UserCredentials sharedManagementCredentials = new UserCredentials("artemis-admin", "artemis-admin");
    private UserCredentials sharedDefaultCredentials = new UserCredentials("test", "test");
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
        LOGGER.info(debugFormat, TEST_LOG_DIR_ENV, testLogDir);
        LOGGER.info(debugFormat, K8S_NAMESPACE_ENV, namespace);
        LOGGER.info(debugFormat, K8S_API_URL_ENV, url);
        LOGGER.info(debugFormat, K8S_API_TOKEN_ENV, token);
        LOGGER.info(debugFormat, ENMASSE_VERSION_SYSTEM_PROPERTY, enmasseVersion);
        LOGGER.info(debugFormat, SKIP_CLEANUP_ENV, skipCleanup);
        LOGGER.info(debugFormat, K8S_DOMAIN_ENV, kubernetesDomain);
        LOGGER.info(debugFormat, APP_NAME_ENV, appName);
        LOGGER.info(debugFormat, TEMPLATES_PATH, templatesPath);
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            instance = new Environment();
        }
        return instance;
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

    public String testLogDir() {
        return testLogDir;
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

    public boolean isDownstream() {
        return downstream;
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
