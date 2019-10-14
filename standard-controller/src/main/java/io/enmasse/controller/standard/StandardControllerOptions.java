/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class StandardControllerOptions {
    private File templateDir;
    private String certDir;
    private String addressSpace;
    private String addressSpaceNamespace;
    private String addressSpacePlanName;
    private String infraUuid;
    private Duration resyncInterval;
    private Duration recheckInterval;
    private Duration statusCheckInterval;
    private String version;
    private boolean enableEventLogger;
    private String authenticationServiceHost;
    private String authenticationServicePort;
    private String authenticationServiceCaSecret;
    private String authenticationServiceClientSecret;
    private String authenticationServiceSaslInitHost;
    private Duration managementQueryTimeout;
    private Duration managementConnectTimeout;

    public String getCertDir() {
        return certDir;
    }

    public void setCertDir(String certDir) {
        this.certDir = certDir;
    }

    public String getAddressSpace() {
        return addressSpace;
    }

    public void setAddressSpace(String addressSpace) {
        this.addressSpace = addressSpace;
    }

    public String getAddressSpacePlanName() {
        return addressSpacePlanName;
    }

    public void setAddressSpacePlanName(String addressSpacePlanName) {
        this.addressSpacePlanName = addressSpacePlanName;
    }

    public String getInfraUuid() {
        return infraUuid;
    }

    public void setInfraUuid(String infraUuid) {
        this.infraUuid = infraUuid;
    }

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public void setResyncInterval(Duration resyncInterval) {
        this.resyncInterval = resyncInterval;
    }

    public Duration getRecheckInterval() {
        return recheckInterval;
    }

    public void setRecheckInterval(Duration recheckInterval) {
        this.recheckInterval = recheckInterval;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isEnableEventLogger() {
        return enableEventLogger;
    }

    public void setEnableEventLogger(boolean enableEventLogger) {
        this.enableEventLogger = enableEventLogger;
    }

    public String getAuthenticationServiceHost() {
        return authenticationServiceHost;
    }

    public void setAuthenticationServiceHost(String authenticationServiceHost) {
        this.authenticationServiceHost = authenticationServiceHost;
    }

    public String getAuthenticationServicePort() {
        return authenticationServicePort;
    }

    public void setAuthenticationServicePort(String authenticationServicePort) {
        this.authenticationServicePort = authenticationServicePort;
    }

    public String getAuthenticationServiceCaSecret() {
        return authenticationServiceCaSecret;
    }

    public void setAuthenticationServiceCaSecret(String authenticationServiceCaSecret) {
        this.authenticationServiceCaSecret = authenticationServiceCaSecret;
    }

    public String getAuthenticationServiceClientSecret() {
        return authenticationServiceClientSecret;
    }

    public void setAuthenticationServiceClientSecret(String authenticationServiceClientSecret) {
        this.authenticationServiceClientSecret = authenticationServiceClientSecret;
    }

    public String getAuthenticationServiceSaslInitHost() {
        return authenticationServiceSaslInitHost;
    }

    public void setAuthenticationServiceSaslInitHost(String authenticationServiceSaslInitHost) {
        this.authenticationServiceSaslInitHost = authenticationServiceSaslInitHost;
    }

    public static StandardControllerOptions fromEnv(Map<String, String> env) {

        StandardControllerOptions options = new StandardControllerOptions();

        options.setAuthenticationServiceHost(env.get("AUTHENTICATION_SERVICE_HOST"));
        options.setAuthenticationServicePort(env.get("AUTHENTICATION_SERVICE_PORT"));
        options.setAuthenticationServiceCaSecret(env.get("AUTHENTICATION_SERVICE_CA_SECRET"));
        options.setAuthenticationServiceClientSecret(env.get("AUTHENTICATION_SERVICE_CLIENT_SECRET"));
        options.setAuthenticationServiceSaslInitHost(env.get("AUTHENTICATION_SERVICE_SASL_INIT_HOST"));

        File templateDir = new File(getEnvOrThrow(env, "TEMPLATE_DIR"));
        if (!templateDir.exists()) {
            throw new IllegalArgumentException("Template directory " + templateDir.getAbsolutePath() + " not found");
        }
        options.setTemplateDir(templateDir);

        options.setCertDir(getEnvOrThrow(env, "CERT_DIR"));
        options.setAddressSpace(getEnvOrThrow(env, "ADDRESS_SPACE"));
        options.setAddressSpaceNamespace(getEnvOrThrow(env, "ADDRESS_SPACE_NAMESPACE"));
        options.setAddressSpacePlanName(getEnvOrThrow(env, "ADDRESS_SPACE_PLAN"));
        options.setInfraUuid(getEnvOrThrow(env, "INFRA_UUID"));

        options.setResyncInterval(getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setRecheckInterval(getEnv(env, "CHECK_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setStatusCheckInterval(getEnv(env, "STATUS_CHECK_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        options.setVersion(getEnvOrThrow(env, "VERSION"));
        options.setEnableEventLogger(getEnv(env, "ENABLE_EVENT_LOGGER").map(Boolean::parseBoolean).orElse(false));

        options.setManagementQueryTimeout(getEnv(env, "MANAGEMENT_QUERY_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(60)));

        options.setManagementConnectTimeout(getEnv(env, "MANAGEMENT_CONNECT_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(30)));

        return options;
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    public File getTemplateDir() {
        return templateDir;
    }

    public void setTemplateDir(File templateDir) {
        this.templateDir = templateDir;
    }

    public String getAddressSpaceNamespace() {
        return addressSpaceNamespace;
    }

    @Override
    public String toString() {
        return "StandardControllerOptions{" +
                "templateDir=" + templateDir +
                ", certDir='" + certDir + '\'' +
                ", addressSpace='" + addressSpace + '\'' +
                ", addressSpaceNamespace='" + addressSpaceNamespace + '\'' +
                ", addressSpacePlanName='" + addressSpacePlanName + '\'' +
                ", infraUuid='" + infraUuid + '\'' +
                ", resyncInterval=" + resyncInterval +
                ", recheckInterval=" + recheckInterval +
                ", version='" + version + '\'' +
                ", enableEventLogger=" + enableEventLogger +
                ", authenticationServiceHost='" + authenticationServiceHost + '\'' +
                ", authenticationServicePort='" + authenticationServicePort + '\'' +
                ", authenticationServiceCaSecret='" + authenticationServiceCaSecret + '\'' +
                ", authenticationServiceClientSecret='" + authenticationServiceClientSecret + '\'' +
                ", authenticationServiceSaslInitHost='" + authenticationServiceSaslInitHost + '\'' +
                ", managementQueryTimeout='" + managementQueryTimeout + '\'' +
                ", managementConnectTimeout='" + managementConnectTimeout + '\'' +
                '}';
    }

    public void setAddressSpaceNamespace(String addressSpaceNamespace) {
        this.addressSpaceNamespace = addressSpaceNamespace;
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

    public Duration getStatusCheckInterval() {
        return statusCheckInterval;
    }

    public void setStatusCheckInterval(Duration statusCheckInterval) {
        this.statusCheckInterval = statusCheckInterval;
    }
}
