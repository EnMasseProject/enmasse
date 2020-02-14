/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class ServiceBrokerOptions {
    private Duration resyncInterval = Duration.ofMinutes(10);
    private String certDir = null;
    private boolean enableRbac = false;
    private String standardAuthserviceConfigName;
    private String standardAuthserviceCredentialsSecretName;
    private String standardAuthserviceCertSecretName;
    private String serviceCatalogCredentialsSecretName;
    private String consoleRouteName;
    private int listenPort = 8080;
    private String version;

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public ServiceBrokerOptions setResyncInterval(Duration resyncInterval) {
        this.resyncInterval = resyncInterval;
        return this;
    }

    public String getCertDir() {
        return certDir;
    }

    public boolean getEnableRbac() {
        return enableRbac;
    }

    public int getListenPort() {
        return listenPort;
    }

    private ServiceBrokerOptions setCertDir(String certDir) {
        this.certDir = certDir;
        return this;
    }

    private ServiceBrokerOptions setEnableRbac(boolean enableRbac) {
        this.enableRbac = enableRbac;
        return this;
    }

    private ServiceBrokerOptions setListenPort(int listenPort) {
        this.listenPort = listenPort;
        return this;
    }

    public String getStandardAuthserviceConfigName() {
        return standardAuthserviceConfigName;
    }

    public ServiceBrokerOptions setStandardAuthserviceConfigName(String standardAuthserviceConfigName) {
        this.standardAuthserviceConfigName = standardAuthserviceConfigName;
        return this;
    }

    public String getStandardAuthserviceCredentialsSecretName() {
        return standardAuthserviceCredentialsSecretName;
    }

    public ServiceBrokerOptions setStandardAuthserviceCredentialsSecretName(String standardAuthserviceCredentialsSecretName) {
        this.standardAuthserviceCredentialsSecretName = standardAuthserviceCredentialsSecretName;
        return this;
    }

    public String getStandardAuthserviceCertSecretName() {
        return standardAuthserviceCertSecretName;
    }

    public ServiceBrokerOptions setStandardAuthserviceCertSecretName(String standardAuthserviceCertSecretName) {
        this.standardAuthserviceCertSecretName = standardAuthserviceCertSecretName;
        return this;
    }

    public static ServiceBrokerOptions fromEnv(Map<String, String> env) {
        ServiceBrokerOptions options = new ServiceBrokerOptions();

        options.setStandardAuthserviceConfigName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CONFIG_NAME"));
        options.setStandardAuthserviceCredentialsSecretName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CREDENTIALS_SECRET_NAME"));
        options.setStandardAuthserviceCertSecretName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CERT_SECRET_NAME"));
        options.setConsoleRouteName(getEnv(env, "CONSOLE_ROUTE_NAME").orElse("console"));
        options.setServiceCatalogCredentialsSecretName(getEnvOrThrow(env, "SERVICE_CATALOG_CREDENTIALS_SECRET_NAME"));

        String resyncInterval = env.get("RESYNC_INTERVAL");
        if (resyncInterval != null) {
            options.setResyncInterval(Duration.ofSeconds(Long.parseLong(resyncInterval)));
        }

        String enableRbac = env.get("ENABLE_RBAC");
        if (enableRbac != null) {
            options.setEnableRbac(Boolean.parseBoolean(enableRbac));
        }

        String certDir = env.get("CERT_DIR");
        if (certDir != null) {
            options.setCertDir(certDir);
        }

        String listenPort = env.get("LISTEN_PORT");
        if (listenPort != null) {
            options.setListenPort(Integer.parseInt(listenPort));
        }

        options.setVersion(getEnvOrThrow(env, "VERSION"));

        return options;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        return getEnv(env, envVar).orElseThrow(() -> new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar)));
    }

    public String getConsoleRouteName() {
        return consoleRouteName;
    }

    public void setConsoleRouteName(String consoleRouteName) {
        this.consoleRouteName = consoleRouteName;
    }

    public String getServiceCatalogCredentialsSecretName() {
        return serviceCatalogCredentialsSecretName;
    }

    public ServiceBrokerOptions setServiceCatalogCredentialsSecretName(String serviceCatalogCredentialsSecretName) {
        this.serviceCatalogCredentialsSecretName = serviceCatalogCredentialsSecretName;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
