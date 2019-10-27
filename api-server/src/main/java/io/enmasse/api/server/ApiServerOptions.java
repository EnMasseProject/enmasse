/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.server;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public class ApiServerOptions {
    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";
    private String namespace;
    private String certDir;
    private Duration resyncInterval;
    private boolean enableRbac;
    private String standardAuthserviceConfigName;
    private String standardAuthserviceCredentialsSecretName;
    private String standardAuthserviceCertSecretName;
    private String apiserverClientCaConfigName;
    private String apiserverClientCaConfigNamespace;
    private String restapiRouteName;
    private Duration userApiTimeout;
    private int numWorkerThreads;
    private String version;

    public static ApiServerOptions fromEnv(Map<String, String> env) {

        ApiServerOptions options = new ApiServerOptions();

        options.setNamespace(getEnv(env, "NAMESPACE")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "namespace"))));

        options.setCertDir(getEnv(env, "CERT_DIR").orElse("/api-server-cert"));

        options.setResyncInterval(getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setUserApiTimeout(getEnv(env, "USER_API_TIMEOUT")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofSeconds(10)));

        options.setNumWorkerThreads(getEnv(env, "NUM_WORKER_THREADS")
                .map(Integer::parseInt)
                .orElse(Runtime.getRuntime().availableProcessors() * 4));
        options.setEnableRbac(Boolean.parseBoolean(getEnv(env, "ENABLE_RBAC").orElse("false")));

        options.setStandardAuthserviceConfigName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CONFIG_NAME"));
        options.setStandardAuthserviceCredentialsSecretName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CREDENTIALS_SECRET_NAME"));
        options.setStandardAuthserviceCertSecretName(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_CERT_SECRET_NAME"));

        options.setApiserverClientCaConfigName(getEnv(env, "APISERVER_CLIENT_CA_CONFIG_NAME").orElse(null));
        options.setApiserverClientCaConfigNamespace(getEnv(env, "APISERVER_CLIENT_CA_CONFIG_NAMESPACE").orElse(null));

        options.setRestapiRouteName(getEnv(env, "APISERVER_ROUTE_NAME").orElse(null));
        options.setVersion(getEnvOrThrow(env, "VERSION"));

        return options;
    }



    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        return getEnv(env, envVar).orElseThrow(() -> new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar)));
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCertDir() {
        return certDir;
    }

    public void setCertDir(String certDir) {
        this.certDir = certDir;
    }

    public Duration getResyncInterval() {
        return resyncInterval;
    }

    public void setResyncInterval(Duration resyncInterval) {
        this.resyncInterval = resyncInterval;
    }

    public boolean isEnableRbac() {
        return enableRbac;
    }

    public void setEnableRbac(boolean enableRbac) {
        this.enableRbac = enableRbac;
    }

    public String getStandardAuthserviceConfigName() {
        return standardAuthserviceConfigName;
    }

    public void setStandardAuthserviceConfigName(String standardAuthserviceConfigName) {
        this.standardAuthserviceConfigName = standardAuthserviceConfigName;
    }

    public String getStandardAuthserviceCredentialsSecretName() {
        return standardAuthserviceCredentialsSecretName;
    }

    public void setStandardAuthserviceCredentialsSecretName(String standardAuthserviceCredentialsSecretName) {
        this.standardAuthserviceCredentialsSecretName = standardAuthserviceCredentialsSecretName;
    }

    public String getStandardAuthserviceCertSecretName() {
        return standardAuthserviceCertSecretName;
    }

    public void setStandardAuthserviceCertSecretName(String standardAuthserviceCertSecretName) {
        this.standardAuthserviceCertSecretName = standardAuthserviceCertSecretName;
    }

    public String getApiserverClientCaConfigName() {
        return apiserverClientCaConfigName;
    }

    public void setApiserverClientCaConfigName(String apiserverClientCaConfigName) {
        this.apiserverClientCaConfigName = apiserverClientCaConfigName;
    }

    public String getApiserverClientCaConfigNamespace() {
        return apiserverClientCaConfigNamespace;
    }

    public void setApiserverClientCaConfigNamespace(String apiserverClientCaConfigNamespace) {
        this.apiserverClientCaConfigNamespace = apiserverClientCaConfigNamespace;
    }

    public String getRestapiRouteName() {
        return restapiRouteName;
    }

    public void setRestapiRouteName(String restapiRouteName) {
        this.restapiRouteName = restapiRouteName;
    }

    public Duration getUserApiTimeout() {
        return userApiTimeout;
    }

    public void setUserApiTimeout(Duration userApiTimeout) {
        this.userApiTimeout = userApiTimeout;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ApiServerOptions{" +
                "namespace='" + namespace + '\'' +
                ", certDir='" + certDir + '\'' +
                ", resyncInterval=" + resyncInterval +
                ", enableRbac=" + enableRbac +
                ", numWorkerThreads=" + numWorkerThreads +
                ", apiserverClientCaConfigName='" + apiserverClientCaConfigName + '\'' +
                ", apiserverClientCaConfigNamespace='" + apiserverClientCaConfigNamespace + '\'' +
                ", userApiTimeout=" + userApiTimeout +
                ", version='" + version + '\'' +
                '}';
    }

    public int getNumWorkerThreads() {
        return numWorkerThreads;
    }

    public void setNumWorkerThreads(int numWorkerThreads) {
        this.numWorkerThreads = numWorkerThreads;
    }
}
