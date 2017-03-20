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

public final class AddressControllerOptions {

    private final String openshiftUrl;
    private final boolean isMultiinstance;
    private final boolean useTLS;

    private AddressControllerOptions(String openshiftUrl, boolean isMultiinstance, boolean useTLS) {
        this.openshiftUrl = openshiftUrl;
        this.isMultiinstance = isMultiinstance;
        this.useTLS = useTLS;
    }

    public String openshiftUrl() {
        return openshiftUrl;
    }

    public static AddressControllerOptions fromEnv(Map<String, String> env) {
        String openshiftHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String openshiftPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");
        boolean isMultiinstance = Boolean.parseBoolean(env.get("MULTIINSTANCE"));
        boolean useTLS = Boolean.parseBoolean(env.get("TLS"));

        return new AddressControllerOptions(String.format("https://%s:%s", openshiftHost, openshiftPort), isMultiinstance, useTLS);
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    public String openshiftNamespace() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
    }

    public String openshiftToken() throws IOException {
        return readFile(new File(SERVICEACCOUNT_PATH, "token"));
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
}
