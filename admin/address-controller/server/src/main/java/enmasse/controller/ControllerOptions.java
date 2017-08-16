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

package enmasse.controller;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

public final class ControllerOptions {

    private final String masterUrl;
    private final boolean isMultiinstance;
    private final String namespace;
    private final String token;
    private final String certDir;
    private final Optional<File> templateDir;
    private final Optional<String> messagingHost;
    private final Optional<String> mqttHost;
    private final Optional<String> consoleHost;
    private final Optional<String> certSecret;
    private final Optional<PasswordAuthentication> osbAuth;
    private final Optional<AuthServiceInfo> noneAuthService;
    private final Optional<AuthServiceInfo> standardAuthService;

    private ControllerOptions(String masterUrl, boolean isMultiinstance, String namespace, String token,
                              Optional<File> templateDir, Optional<String> messagingHost, Optional<String> mqttHost,
                              Optional<String> consoleHost, Optional<String> certSecret, String certDir,
                              Optional<PasswordAuthentication> osbAuth, Optional<AuthServiceInfo> noneAuthService, Optional<AuthServiceInfo> standardAuthService) {
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

    public static ControllerOptions fromEnv(Map<String, String> env) throws IOException {
        String masterHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String masterPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");
        boolean isMultiinstance = Boolean.parseBoolean(env.get("MULTIINSTANCE"));

        File namespaceFile = new File(SERVICEACCOUNT_PATH, "namespace");
        String namespace;
        if (namespaceFile.exists()) {
            namespace = readFile(namespaceFile);
        } else {
            namespace = getEnv(env, "NAMESPACE").orElse(null);
            if (namespace == null) {
                throw new IllegalStateException("Unable to find namespace from " + namespaceFile.getAbsolutePath() + " or NAMSPACE environment variable");
            }
        }

        String token;
        File tokenFile = new File(SERVICEACCOUNT_PATH, "token");
        if (tokenFile.exists()) {
            token = readFile(tokenFile);
        } else {
            token = getEnvOrThrow(env, "TOKEN");
        }

        File templateDir = new File("/enmasse-templates");
        // Fall back to path used in 0.11.0
        if (!templateDir.exists()) {
            templateDir = new File("/templates");
        }
        if (env.containsKey("TEMPLATE_DIR")) {
            templateDir = new File(env.get("TEMPLATE_DIR"));
        }

        Optional<String> messagingHost = getEnv(env, "INSTANCE_MESSAGING_HOST");
        Optional<String> mqttHost = getEnv(env, "INSTANCE_MQTT_HOST");
        Optional<String> consoleHost = getEnv(env, "INSTANCE_CONSOLE_HOST");
        Optional<String> certSecret = getEnv(env, "INSTANCE_CERT_SECRET");
        Optional<File> maybeDir = Optional.ofNullable(templateDir.exists() ? templateDir : null);

        Optional<String> osbAuthUser = getEnv(env, "OSB_AUTH_USERNAME");
        Optional<PasswordAuthentication> osbAuth = osbAuthUser.isPresent()
                ? Optional.of(new PasswordAuthentication(osbAuthUser.get(), getEnvOrThrow(env, "OSB_AUTH_PASSWORD").toCharArray()))
                : Optional.empty();

        Optional<AuthServiceInfo> noneAuthService = getAuthService(env, "NONE_AUTHSERVICE_SERVICE_HOST", "NONE_AUTHSERVICE_SERVICE_PORT");
        Optional<AuthServiceInfo> standardAuthService = getAuthService(env, "STANDARD_AUTHSERVICE_SERVICE_HOST", "STANDARD_AUTHSERVICE_SERVICE_PORT");

        String certDir = getEnv(env, "CERT_PATH").orElse("/ssl-certs");
        return new ControllerOptions(String.format("https://%s:%s", masterHost, masterPort), isMultiinstance, namespace,
                token, maybeDir, messagingHost, mqttHost, consoleHost, certSecret, certDir, osbAuth, noneAuthService, standardAuthService);
    }

    private static Optional<AuthServiceInfo> getAuthService(Map<String, String> env, String hostEnv, String portEnv) {
        Optional<String> host = getEnv(env, hostEnv);
        Optional<String> port = getEnv(env, portEnv);
        if (host.isPresent() && port.isPresent()) {
            return Optional.of(new AuthServiceInfo(host.get(), Integer.parseInt(port.get())));
        }
        return Optional.empty();
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

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    public String namespace() {
        return namespace;
    }

    public String token() {
        return token;
    }

    public boolean isMultiinstance() {
        return isMultiinstance;
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public int port() {
        return 5672;
    }

    public Optional<File> templateDir() {
        return templateDir;
    }

    public Optional<String> messagingHost() {
        return messagingHost;
    }

    public Optional<String> mqttHost() {
        return mqttHost;

    }

    public Optional<String> consoleHost() {
        return consoleHost;
    }

    public Optional<String> certSecret() {
        return certSecret;
    }

    public String certDir() {
        return certDir;
    }

    public Optional<PasswordAuthentication> osbAuth() {
        return osbAuth;
    }

    public Optional<AuthServiceInfo> getNoneAuthService() {
        return noneAuthService;
    }

    public Optional<AuthServiceInfo> getStandardAuthService() {
        return standardAuthService;
    }
}
