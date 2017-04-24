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
import java.nio.file.Files;
import java.util.Map;

public final class ControllerOptions {

    private final String masterUrl;
    private final boolean isMultiinstance;
    private final boolean useTLS;
    private final String namespace;
    private final String token;
    private final File templateDir;

    private ControllerOptions(String masterUrl, boolean isMultiinstance, boolean useTLS, String namespace, String token, File templateDir) {
        this.masterUrl = masterUrl;
        this.isMultiinstance = isMultiinstance;
        this.useTLS = useTLS;
        this.namespace = namespace;
        this.token = token;
        this.templateDir = templateDir;
    }

    public String masterUrl() {
        return masterUrl;
    }

    public static ControllerOptions fromEnv(Map<String, String> env) throws IOException {
        String masterHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String masterPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");
        boolean isMultiinstance = Boolean.parseBoolean(env.get("MULTIINSTANCE"));
        boolean useTLS = Boolean.parseBoolean(env.get("TLS"));

        File namespaceFile = new File(SERVICEACCOUNT_PATH, "namespace");
        String namespace;
        if (namespaceFile.exists()) {
            namespace = readFile(namespaceFile);
        } else {
            namespace = getEnvOrThrow(env, "NAMESPACE");
        }

        String token;
        File tokenFile = new File(SERVICEACCOUNT_PATH, "token");
        if (tokenFile.exists()) {
            token = readFile(tokenFile);
        } else {
            token = getEnvOrThrow(env, "TOKEN");
        }

        File templateDir = new File("/templates");
        if (env.containsKey("TEMPLATE_DIR")) {
            templateDir = new File(env.get("TEMPLATE_DIR"));
        }

        return new ControllerOptions(String.format("https://%s:%s", masterHost, masterPort), isMultiinstance, useTLS, namespace, token, templateDir);
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    public String namespace() throws IOException {
        return namespace;
    }

    public String token() throws IOException {
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

    public boolean useTLS() {
        return useTLS;
    }

    public File templateDir() {
        return templateDir;
    }
}
