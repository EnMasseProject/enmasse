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
    private String clientCa;
    private String requestHeaderClientCa;
    private boolean enableRbac;

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

    public static ApiServerOptions fromEnv(Map<String, String> env) {

        ApiServerOptions options = new ApiServerOptions();

        options.setNamespace(getEnv(env, "NAMESPACE")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "namespace"))));

        options.setCertDir(getEnv(env, "CERT_DIR").orElse("/api-server-cert"));

        options.setClientCa(getEnv(env, "CLIENT_CA").orElse(null));
        options.setRequestHeaderClientCa(getEnv(env, "REQUEST_HEADER_CLIENT_CA").orElse(null));

        options.setResyncInterval(getEnv(env, "RESYNC_INTERVAL")
                .map(i -> Duration.ofSeconds(Long.parseLong(i)))
                .orElse(Duration.ofMinutes(5)));

        options.setEnableRbac(Boolean.parseBoolean(getEnv(env, "ENABLE_RBAC").orElse("false")));

        return options;
    }

    private static Optional<String> getEnv(Map<String, String> env, String envVar) {
        return Optional.ofNullable(env.get(envVar));
    }

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void setClientCa(String clientCa) {
        this.clientCa = clientCa;
    }

    public String getClientCa() {
        return clientCa;
    }

    public boolean isEnableRbac() {
        return enableRbac;
    }

    public void setEnableRbac(boolean enableRbac) {
        this.enableRbac = enableRbac;
    }

    public void setRequestHeaderClientCa(String clientCa) {
        this.requestHeaderClientCa = clientCa;
    }

    public String getRequestHeaderClientCa() {
        return requestHeaderClientCa;
    }
}
