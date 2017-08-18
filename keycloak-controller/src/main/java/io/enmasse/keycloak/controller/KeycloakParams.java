/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.keycloak.controller;

import java.util.Map;

public class KeycloakParams {

    private final String host;
    private final int httpPort;
    private final String adminUser;
    private final String adminPassword;

    public KeycloakParams(String host, int httpPort, String adminUser, String adminPassword) {
        this.host = host;
        this.httpPort = httpPort;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    public static KeycloakParams fromEnv(Map<String, String> env) {
        String host = getEnvOrThrow(env, "STANDARD_AUTHSERVICE_SERVICE_HOST");
        int httpPort = Integer.parseInt(getEnvOrThrow(env, "STANDARD_AUTHSERVICE_SERVICE_PORT_HTTP"));
        String adminUser = getEnvOrThrow(env, "STANDARD_AUTHSERVICE_ADMIN_USER");
        String adminPassword = getEnvOrThrow(env, "STANDARD_AUTHSERVICE_ADMIN_PASSWORD");
        return new KeycloakParams(host, httpPort, adminUser, adminPassword);
    }

    private static String getEnvOrThrow(Map<String, String> envMap, String env) {
        String value = envMap.get(env);
        if (value == null) {
            throw new IllegalArgumentException("Required environment variable " + env + " is missing");
        }
        return value;
    }

    public String getHost() {
        return host;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public String getAdminUser() {
        return adminUser;
    }

    public String getAdminPassword() {
        return adminPassword;
    }
}
