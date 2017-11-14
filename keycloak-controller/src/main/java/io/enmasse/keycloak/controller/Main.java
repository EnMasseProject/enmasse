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

import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Security;
import java.util.Map;

public class Main {
    public static void main(String [] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        Map<String, String> env = System.getenv();

        String openshiftUri = String.format("https://%s:%s", getEnvOrThrow(env, "KUBERNETES_SERVICE_HOST"), getEnvOrThrow(env, "KUBERNETES_SERVICE_PORT"));

        Config config = new ConfigBuilder().withMasterUrl(openshiftUri).withOauthToken(getAuthenticationToken()).withNamespace(getNamespace()).build();
        OpenShiftClient openShiftClient = new DefaultOpenShiftClient(config);
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(openShiftClient);
        KeycloakManager keycloakManager = new KeycloakManager(new Keycloak(KeycloakParams.fromEnv(System.getenv())));

        addressSpaceApi.watchAddressSpaces(keycloakManager);
    }

    private static String getEnvOrThrow(Map<String, String> env, String envVar) {
        String var = env.get(envVar);
        if (var == null) {
            throw new IllegalArgumentException(String.format("Unable to find value for required environment var '%s'", envVar));
        }
        return var;
    }

    private static final String SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount";

    private static String getNamespace() throws IOException {
        String namespace = System.getenv("KUBERNETES_NAMESPACE");
        if (namespace == null) {
            return readFile(new File(SERVICEACCOUNT_PATH, "namespace"));
        } else {
            return namespace;
        }
    }

    private static String getAuthenticationToken() throws IOException {
        String token = System.getenv("KUBERNETES_TOKEN");
        if (token == null) {
            return readFile(new File(SERVICEACCOUNT_PATH, "token"));
        } else {
            return token;
        }
    }

    private static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
}
