/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.kubernetes.client.Config;
import org.slf4j.Logger;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class Environment {
    private static final Logger LOGGER = CustomLogger.getLogger();
    private static JsonNode jsonEnv;
    private final List<Map.Entry<String, String>> values = new ArrayList<>();

    private final String templateRoot = Paths.get(System.getProperty("user.dir"), "..", "templates", "build").toString();

    // Env variables
    private static final String SCALE_CONFIG = "SCALE_CONFIG";
    private static final String CONFIG = "CONFIG";
    private static final String TEST_LOG_DIR_ENV = "TEST_LOGDIR";
    private static final String K8S_NAMESPACE_ENV = "KUBERNETES_NAMESPACE";
    private static final String K8S_API_URL_ENV = "KUBERNETES_API_URL";
    private static final String K8S_API_TOKEN_ENV = "KUBERNETES_API_TOKEN";
    private static final String ENMASSE_VERSION_SYSTEM_PROPERTY = "enmasse.version";
    private static final String ENMASSE_DOCS_SYSTEM_PROPERTY = "enmasse.docs";
    private static final String ENMASSE_OLM_REPLACES_SYSTEM_PROPERTY = "enmasse.olm.replaces";
    private static final String K8S_DOMAIN_ENV = "KUBERNETES_DOMAIN";
    private static final String K8S_API_CONNECT_TIMEOUT = "KUBERNETES_API_CONNECT_TIMEOUT";
    private static final String K8S_API_READ_TIMEOUT = "KUBERNETES_API_READ_TIMEOUT";
    private static final String K8S_API_WRITE_TIMEOUT = "KUBERNETES_API_WRITE_TIMEOUT";
    private static final String UPGRADE_TEPLATES_ENV = "UPGRADE_TEMPLATES";
    private static final String START_TEMPLATES_ENV = "START_TEMPLATES";
    private static final String TEMPLATES_PATH = "TEMPLATES";
    private static final String SKIP_CLEANUP_ENV = "SKIP_CLEANUP";
    private static final String SKIP_UNNSTALL = "SKIP_UNINSTALL";
    private static final String STORE_SCREENSHOTS_ENV = "STORE_SCREENSHOTS";
    private static final String MONITORING_NAMESPACE_ENV = "MONITORING_NAMESPACE";
    private static final String TAG_ENV = "TAG";
    private static final String PRODUCT_NAME_ENV = "PRODUCT_NAME";
    private static final String OPERATOR_NAME_ENV = "OPERATOR_NAME";
    private static final String OPERATOR_CHANNEL_ENV = "OPERATOR_CHANNEL";
    private static final String INSTALL_TYPE = "INSTALL_TYPE";
    private static final String OLM_INSTALL_TYPE = "OLM_INSTALL_TYPE";
    private static final String SKIP_SAVE_STATE = "SKIP_SAVE_STATE";
    private static final String OCP4_EXTERNAL_IMAGE_REGISTRY = "OCP4_EXTERNAL_IMAGE_REGISTRY";
    private static final String OCP4_INTERNAL_IMAGE_REGISTRY = "OCP4_INTERNAL_IMAGE_REGISTRY";
    private static final String OVERRIDE_CLUSTER_TYPE = "OVERRIDE_CLUSTER_TYPE";
    private static final String DOWNSTREAM = "DOWNSTREAM";
    private static final String PRODUCT_VERSION = "PRODUCT_VERSION";
    private static final String CATALOG_SOURCE = "CATALOG_SOURCE";

    //Config paths
    private static final String scaleConfig = System.getenv().getOrDefault(SCALE_CONFIG, Paths.get(System.getProperty("user.dir"), "scale-config.json").toAbsolutePath().toString());
    private static final String config = System.getenv().getOrDefault(CONFIG, Paths.get(System.getProperty("user.dir"), "config.json").toAbsolutePath().toString());

    //Collecting variables
    private static Environment instance;
    private final String namespace = getOrDefault(jsonEnv, K8S_NAMESPACE_ENV, "enmasse-infra");
    private final String testLogDir = getOrDefault(jsonEnv, TEST_LOG_DIR_ENV, "/tmp/testlogs");
    private final String overrideClusterType = getOrDefault(jsonEnv, OVERRIDE_CLUSTER_TYPE, "");
    private String token = getOrDefault(jsonEnv, K8S_API_TOKEN_ENV, "");
    private String url = getOrDefault(jsonEnv, K8S_API_URL_ENV, "");
    private String kubernetesDomain = getOrDefault(jsonEnv, K8S_DOMAIN_ENV, "");
    private final String templatesPath = getOrDefault(jsonEnv, TEMPLATES_PATH, null);
    private final String startTemplates = getOrDefault(jsonEnv, START_TEMPLATES_ENV, null);
    private final String upgradeTemplates = getOrDefault(jsonEnv, UPGRADE_TEPLATES_ENV, null);
    private final String monitoringNamespace = getOrDefault(jsonEnv, MONITORING_NAMESPACE_ENV, "enmasse-monitoring");
    private final String tag = getOrDefault(jsonEnv, TAG_ENV, "latest");
    private final String productName = getOrDefault(jsonEnv, PRODUCT_NAME_ENV, "enmasse");
    private final String operatorName = getOrDefault(jsonEnv, OPERATOR_NAME_ENV, "enmasse");
    private final String operatorChannel = getOrDefault(jsonEnv, OPERATOR_CHANNEL_ENV, "alpha");
    private final boolean skipSaveState = getOrDefault(jsonEnv, SKIP_SAVE_STATE, Boolean::parseBoolean, false);
    private final Duration kubernetesApiConnectTimeout = getOrDefault(jsonEnv, K8S_API_CONNECT_TIMEOUT, i -> Duration.ofSeconds(Long.parseLong(i)), Duration.ofSeconds(60));
    private final Duration kubernetesApiReadTimeout = getOrDefault(jsonEnv, K8S_API_READ_TIMEOUT, i -> Duration.ofSeconds(Long.parseLong(i)), Duration.ofSeconds(60));
    private final Duration kubernetesApiWriteTimeout = getOrDefault(jsonEnv, K8S_API_WRITE_TIMEOUT, i -> Duration.ofSeconds(Long.parseLong(i)), Duration.ofSeconds(60));
    private final EnmasseInstallType installType = getOrDefault(jsonEnv, INSTALL_TYPE, value -> EnmasseInstallType.valueOf(value.toUpperCase()), EnmasseInstallType.BUNDLE);
    private final OLMInstallationType olmInstallType = Optional.ofNullable(getOrDefault(jsonEnv, OLM_INSTALL_TYPE, "")).map(s -> s.isEmpty() ? OLMInstallationType.SPECIFIC.name() : s).map(value -> OLMInstallationType.valueOf(value.toUpperCase())).orElse(OLMInstallationType.SPECIFIC);
    private final String clusterExternalImageRegistry = getOrDefault(jsonEnv, OCP4_EXTERNAL_IMAGE_REGISTRY, "");
    private final String clusterInternalImageRegistry = getOrDefault(jsonEnv, OCP4_INTERNAL_IMAGE_REGISTRY, "");
    private final String isTestDownstream = getOrDefault(jsonEnv, DOWNSTREAM, value -> System.getenv("DOWNSTREAM"),"false");
    private final String productVersion = getOrDefault(jsonEnv, PRODUCT_VERSION, System.getenv("PRODUCT_VERSION"));
    private final String catalogSource = getOrDefault(jsonEnv, CATALOG_SOURCE, value -> System.getenv("CATALOG_SOURCE"),"enmasse-catalog");


    //Default values
    private final UserCredentials managementCredentials = new UserCredentials(null, null);
    private final UserCredentials defaultCredentials = new UserCredentials(null, null);
    private final UserCredentials sharedManagementCredentials = new UserCredentials("artemis-admin", "artemis-admin");
    private final UserCredentials sharedDefaultCredentials = new UserCredentials("test", "test");

    //Collectings properties
    private final String enmasseVersion = System.getProperty(ENMASSE_VERSION_SYSTEM_PROPERTY);
    private final String enmasseDocs = System.getProperty(ENMASSE_DOCS_SYSTEM_PROPERTY);
    private final String enmasseOlmDocsUrl = System.getProperty("enmasse.olm.docs.url");
    private final String enmasseOlmAboutName = System.getProperty("enmasse.olm.about.name");
    private final String enmasseOlmAboutUrl = System.getProperty("enmasse.olm.about.url");
    private final String enmasseOlmDocConfigUrl = System.getProperty("enmasse.olm.doc.configure.url");
    private final String enmasseOlmReplaces = System.getProperty(ENMASSE_OLM_REPLACES_SYSTEM_PROPERTY);

    /**
     * Skip removing address-spaces
     */
    private final boolean skipCleanup = Boolean.parseBoolean(System.getenv().getOrDefault(SKIP_CLEANUP_ENV, "false"));
    private final boolean skipUninstall = Boolean.parseBoolean(System.getenv().getOrDefault(SKIP_UNNSTALL, "false"));
    /**
     * Store screenshots every time
     */
    private final boolean storeScreenshots = Boolean.parseBoolean(System.getenv(STORE_SCREENSHOTS_ENV));


    // Constructor and getters
    private Environment() {
        if (Strings.isNullOrEmpty(token) || Strings.isNullOrEmpty(url)) {
            Config config = Config.autoConfigure(System.getenv()
                    .getOrDefault("TEST_CLUSTER_CONTEXT", null));
            token = config.getOauthToken();
            url = config.getMasterUrl();
        }
        String debugFormat = "{}:{}";
        LOGGER.info(debugFormat, INSTALL_TYPE, installType.name());
        if (installType == EnmasseInstallType.OLM) {
            LOGGER.info(debugFormat, OLM_INSTALL_TYPE, olmInstallType.name());
        }
        if (Strings.isNullOrEmpty(this.kubernetesDomain)) {
            if (url.equals("https://api.crc.testing:6443")) {
                this.kubernetesDomain = "apps-crc.testing";
            } else if (url.startsWith("https://api")) { //is api url for openshift4
                try {
                    this.kubernetesDomain = new URL(url).getHost().replaceFirst("api", "apps");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                this.kubernetesDomain = "nip.io";
            }
        }
        LOGGER.info(debugFormat, "CONFIG", config);
        LOGGER.info(debugFormat, "SCALE_CONFIG", scaleConfig);
        values.forEach(v -> LOGGER.info(debugFormat, v.getKey(), v.getValue()));
    }

    public static synchronized Environment getInstance() {
        if (instance == null) {
            jsonEnv = loadJsonEnv();
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

    public String enmasseDocs() {
        return enmasseDocs;
    }

    public String enmasseOlmDocsUrl() {
        return enmasseOlmDocsUrl;
    }

    public String enmasseOlmAboutName() {
        return enmasseOlmAboutName;
    }

    public String enmasseOlmAboutUrl() {
        return enmasseOlmAboutUrl;
    }

    public String enmasseOlmDocConfigUrl() {
        return enmasseOlmDocConfigUrl;
    }

    public String enmasseOlmReplaces() {
        return enmasseOlmReplaces;
    }

    public String kubernetesDomain() {
        return kubernetesDomain;
    }

    public String getMonitoringNamespace() {
        return monitoringNamespace;
    }

    public String getTag() {
        return tag;
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

    public String getTemplatesPath(Kubernetes version) {
        if (templatesPath == null) {
            return buildDefaultTemplatePath(version);
        } else {
            return templatesPath;
        }
    }

    public String getUpgradeTemplates(Kubernetes version) {
        if (upgradeTemplates == null) {
            return buildDefaultTemplatePath(version);
        } else {
            return upgradeTemplates;
        }
    }
    public String getStartTemplates(Kubernetes version) {
        if (startTemplates == null) {
            return buildDefaultTemplatePath(version);
        } else {
            return startTemplates;
        }
    }

    private String buildDefaultTemplatePath(Kubernetes version) {
        return Path.of(templateRoot, getVersionPart(version), "enmasse-latest").toString();
    }


    private String getVersionPart(Kubernetes version) {
        return version == null || version.getKubernetesVersion() > 1.15 ? "default" : "prekube1_16";
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

    public String getScaleConfig() {
        return scaleConfig;
    }

    public String getClusterExternalImageRegistry() {
        return clusterExternalImageRegistry;
    }

    public String getClusterInternalImageRegistry() {
        return clusterInternalImageRegistry;
    }

    public String getOverrideClusterType() {
        return overrideClusterType;
    }

    public String isTestDownstream() {
        return isTestDownstream;
    }

    public String getProductVersion(){ return productVersion; }

    public String getCatalogSource(){ return catalogSource; }

    private String getOrDefault(JsonNode jsonConfig, String varName, String defaultValue) {
        return getOrDefault(jsonConfig, varName, String::toString, defaultValue);
    }

    private <T> T getOrDefault(JsonNode jsonConfig, String var, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(var) != null ? System.getenv(var) : (jsonConfig.get(var) != null ? jsonConfig.get(var).asText() : null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        values.add(Map.entry(var, String.valueOf(returnValue)));
        return returnValue;
    }

    private static JsonNode loadJsonEnv() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File jsonFile = new File(config).getAbsoluteFile();
            return mapper.readTree(jsonFile);
        } catch (Exception e) {
            LOGGER.warn("Json configuration not provider or not exists");
            return mapper.createObjectNode();
        }
    }

    public String getOperatorChannel() {
        return operatorChannel;
    }

    public String getOperatorName() {
        return operatorName;
    }
}
