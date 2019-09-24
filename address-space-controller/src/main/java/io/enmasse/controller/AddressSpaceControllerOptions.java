/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public final class AddressSpaceControllerOptions {

    private File templateDir;
    private File resourcesDir;
    private boolean enableEventLogger;
    private boolean exposeEndpointsByDefault;
    private boolean installDefaultResources;

    private String environment;

    private String wildcardCertSecret;

    private Duration resyncInterval;
    private Duration recheckInterval;

    private String version;

    private Duration managementQueryTimeout;
    private Duration managementConnectTimeout;

    private Duration kubernetesApiConnectTimeout;
    private Duration kubernetesApiReadTimeout;
    private Duration kubernetesApiWriteTimeout;

    public File getTemplateDir() {
        return templateDir;
    }

    public boolean isEnableEventLogger() {
        return enableEventLogger;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getWildcardCertSecret() {
        return wildcardCertSecret;
    }

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public Duration getRecheckInterval() {
        return recheckInterval;
    }

    public String getVersion() {
        return version;
    }

    public static AddressSpaceControllerOptions fromEnv(Map<String, String> env) throws IOException {

        AddressSpaceControllerOptions options = new AddressSpaceControllerOptions();

        File templateDir = new File(getEnvOrThrow(env, "TEMPLATE_DIR"));
        if (!templateDir.exists()) {
            throw new IllegalArgumentException("Template directory " + templateDir.getAbsolutePath() + " not found");
        }
        options.setTemplateDir(templateDir);

        File resourcesDir = new File(getEnvOrThrow(env, "RESOURCES_DIR"));
        if (!resourcesDir.exists()) {
            throw new IllegalArgumentException("Resources directory " + resourcesDir.getAbsolutePath() + " not found");
        }
        options.setResourcesDir(resourcesDir);

        options.setEnableEventLogger(getEnv(env, "ENABLE_EVENT_LOGGER").map(Boolean::parseBoolean).orElse(false));

        options.setExposeEndpointsByDefault(getEnv(env, "EXPOSE_ENDPOINTS_BY_DEFAULT").map(Boolean::parseBoolean).orElse(true));

        options.setEnvironment(getEnv(env, "ENVIRONMENT").orElse("development"));

        options.setInstallDefaultResources(getEnv(env, "INSTALL_DEFAULT_RESOURCES").map(Boolean::parseBoolean).orElse(true));

        options.setWildcardCertSecret(getEnv(env, "WILDCARD_ENDPOINT_CERT_SECRET").orElse(null));

        options.setResyncInterval(getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setRecheckInterval(getEnv(env, "CHECK_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setManagementQueryTimeout(getEnv(env, "MANAGEMENT_QUERY_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(60)));

        options.setManagementConnectTimeout(getEnv(env, "MANAGEMENT_CONNECT_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setKubernetesApiConnectTimeout(getEnv(env, "KUBERNETES_API_CONNECT_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setKubernetesApiReadTimeout(getEnv(env, "KUBERNETES_API_READ_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setKubernetesApiWriteTimeout(getEnv(env, "KUBERNETES_API_WRITE_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));


        options.setVersion(getEnvOrThrow(env, "VERSION"));
        return options;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    public boolean isExposeEndpointsByDefault() {
        return exposeEndpointsByDefault;
    }

    public void setTemplateDir(File templateDir) {
        this.templateDir = templateDir;
    }

    public void setEnableEventLogger(boolean enableEventLogger) {
        this.enableEventLogger = enableEventLogger;
    }

    public void setExposeEndpointsByDefault(boolean exposeEndpointsByDefault) {
        this.exposeEndpointsByDefault = exposeEndpointsByDefault;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setWildcardCertSecret(String wildcardCertSecret) {
        this.wildcardCertSecret = wildcardCertSecret;
    }

    public void setResyncInterval(Duration resyncInterval) {
        this.resyncInterval = resyncInterval;
    }

    public void setRecheckInterval(Duration recheckInterval) {
        this.recheckInterval = recheckInterval;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public File getResourcesDir() {
        return resourcesDir;
    }

    public void setResourcesDir(File resourcesDir) {
        this.resourcesDir = resourcesDir;
    }

    @Override
    public String toString() {
        return "AddressSpaceControllerOptions{" +
                "templateDir=" + templateDir +
                ", resourcesDir=" + resourcesDir +
                ", enableEventLogger=" + enableEventLogger +
                ", exposeEndpointsByDefault=" + exposeEndpointsByDefault +
                ", environment='" + environment + '\'' +
                ", wildcardCertSecret='" + wildcardCertSecret + '\'' +
                ", resyncInterval=" + resyncInterval +
                ", recheckInterval=" + recheckInterval +
                ", version='" + version + '\'' +
                ", managementQueryTimeout='" + managementQueryTimeout + '\'' +
                ", managementConnectTimeout='" + managementConnectTimeout + '\'' +
                ", kubernetesApiConnectTimeout='" + kubernetesApiConnectTimeout + '\'' +
                ", kubernetesApiReadTimeout='" + kubernetesApiReadTimeout + '\'' +
                ", kubernetesApiWriteTimeout='" + kubernetesApiWriteTimeout + '\'' +
                '}';
    }

    public boolean isInstallDefaultResources() {
        return installDefaultResources;
    }

    public void setInstallDefaultResources(boolean installDefaultResources) {
        this.installDefaultResources = installDefaultResources;
    }

    public Duration getManagementQueryTimeout() {
        return managementQueryTimeout;
    }

    public void setManagementQueryTimeout(Duration managementQueryTimeout) {
        this.managementQueryTimeout = managementQueryTimeout;
    }

    public Duration getManagementConnectTimeout() {
        return managementConnectTimeout;
    }

    public void setManagementConnectTimeout(Duration managementConnectTimeout) {
        this.managementConnectTimeout = managementConnectTimeout;
    }

    public Duration getKubernetesApiConnectTimeout() {
        return kubernetesApiConnectTimeout;
    }

    public void setKubernetesApiConnectTimeout(Duration kubernetesApiConnectTimeout) {
        this.kubernetesApiConnectTimeout = kubernetesApiConnectTimeout;
    }

    public Duration getKubernetesApiReadTimeout() {
        return kubernetesApiReadTimeout;
    }

    public void setKubernetesApiReadTimeout(Duration kubernetesApiReadTimeout) {
        this.kubernetesApiReadTimeout = kubernetesApiReadTimeout;
    }

    public Duration getKubernetesApiWriteTimeout() {
        return kubernetesApiWriteTimeout;
    }

    public void setKubernetesApiWriteTimeout(Duration kubernetesApiWriteTimeout) {
        this.kubernetesApiWriteTimeout = kubernetesApiWriteTimeout;
    }

}
