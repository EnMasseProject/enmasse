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

package enmasse.storage.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public final class StorageControllerOptions {

    private final String openshiftUrl;

    private StorageControllerOptions(String openshiftUrl) {
        this.openshiftUrl = openshiftUrl;
    }

    public String openshiftUrl() {
        return openshiftUrl;
    }

    public static StorageControllerOptions fromEnv(Map<String, String> env) {
        String openshiftHost = getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST");
        String openshiftPort = getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT");

        return new StorageControllerOptions(String.format("https://%s:%s", openshiftHost, openshiftPort));
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

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public int port() {
        return 55674;
    }
}
