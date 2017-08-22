/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.controller;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public final class ControllerOptions {

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private final String masterUrl;
    private final boolean isMultiinstance;
    private final String namespace;
    private final String token;
    private final String certDir;
    private final File templateDir;
    private final String messagingHost;
    private final String mqttHost;
    private final String consoleHost;
    private final String certSecret;
    private final PasswordAuthentication osbAuth;
    private final AuthServiceInfo noneAuthService;
    private final AuthServiceInfo standardAuthService;

    private ControllerOptions(String masterUrl, boolean isMultiinstance, String namespace, String token,
                              File templateDir, String messagingHost, String mqttHost,
                              String consoleHost, String certSecret, String certDir,
                              PasswordAuthentication osbAuth, AuthServiceInfo noneAuthService, AuthServiceInfo standardAuthService) {
        this.masterUrl = masterUrl;
        this.isMultiinstance = isMultiinstance;
        this.namespace = namespace;
        this.token = token;
        this.templateDir = templateDir;
        this.messagingHost = messagingHost;
        this.mqttHost = mqttHost;
        this.consoleHost = consoleHost;
        this.certSecret = certSecret;
        this.certDir = certDir;
        this.osbAuth = osbAuth;
        this.noneAuthService = noneAuthService;
        this.standardAuthService = standardAuthService;
    }

    public String masterUrl() {
        return masterUrl;
    }

    public String namespace() {
        return namespace;
    }

    public String token() {
        return token;
    }

    public boolean isMultiinstance() {
        return isMultiinstance;
    }

    public Optional<File> templateDir() {
        return Optional.ofNullable(templateDir);
    }

    public Optional<String> messagingHost() {
        return Optional.ofNullable(messagingHost);
    }

    public Optional<String> mqttHost() {
        return Optional.ofNullable(mqttHost);

    }

    public Optional<String> consoleHost() {
        return Optional.ofNullable(consoleHost);
    }

    public Optional<String> certSecret() {
        return Optional.ofNullable(certSecret);
    }

    public String certDir() {
        return certDir;
    }

    public Optional<PasswordAuthentication> osbAuth() {
        return Optional.ofNullable(osbAuth);
    }

    public Optional<AuthServiceInfo> getNoneAuthService() {
        return Optional.ofNullable(noneAuthService);
    }

    public Optional<AuthServiceInfo> getStandardAuthService() {
        return Optional.ofNullable(standardAuthService);
    }

    public static ControllerOptions fromEnv(Map<String, String> env) throws IOException {

        String masterHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String masterPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");
        boolean isMultiinstance = Boolean.parseBoolean(env.get("MULTIINSTANCE"));

        String namespace = getEnv(env, "NAMESPACE")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "namespace")));

        String token = getEnv(env, "TOKEN")
                .orElseGet(() -> readFile(new File(SERVICEACCOUNT_PATH, "token")));

        File templateDir = getEnv(env, "TEMPLATE_DIR")
                .map(File::new)
                .orElse(new File("/enmasse-templates"));

        if (!templateDir.exists()) {
            templateDir = null;
        }

        PasswordAuthentication osbAuth = getEnv(env, "OSB_AUTH_USERNAME")
                .map(user -> new PasswordAuthentication(user, getEnvOrThrow(env, "OSB_AUTH_PASSWORD").toCharArray()))
                .orElse(null);

        AuthServiceInfo noneAuthService = getAuthService(env, "NONE_AUTHSERVICE_SERVICE_HOST", "NONE_AUTHSERVICE_SERVICE_PORT").orElse(null);
        AuthServiceInfo standardAuthService = getAuthService(env, "STANDARD_AUTHSERVICE_SERVICE_HOST", "STANDARD_AUTHSERVICE_SERVICE_PORT_AMQP").orElse(null);

        String certDir = getEnv(env, "CERT_PATH").orElse("/address-controller-cert");

        String messagingHost = getEnv(env, "MESSAGING_ENDPOINT_HOST").orElse(null);
        String mqttHost = getEnv(env, "MQTT_ENDPOINT_HOST").orElse(null);
        String consoleHost = getEnv(env, "CONSOLE_ENDPOINT_HOST").orElse(null);
        String certSecret = getEnv(env, "MESSAGING_CERT_SECRET").orElse(null);

        return new ControllerOptions(String.format("https://%s:%s", masterHost, masterPort),
                isMultiinstance,
                namespace,
                token,
                templateDir,
                messagingHost,
                mqttHost,
                consoleHost,
                certSecret,
                certDir,
                osbAuth,
                noneAuthService,
                standardAuthService);
    }


    private static Optional<AuthServiceInfo> getAuthService(Map<String, String> env, String hostEnv, String portEnv) {

        return getEnv(env, hostEnv)
                .map(host -> new AuthServiceInfo(host, Integer.parseInt(getEnvOrThrow(env, portEnv))));
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

    private static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
