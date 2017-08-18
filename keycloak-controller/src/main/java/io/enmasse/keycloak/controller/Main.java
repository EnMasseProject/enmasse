package io.enmasse.keycloak.controller;

import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class Main {
    public static void main(String [] args) throws Exception {
        String host = getEnvOrThrow("STANDARD_AUTHSERVICE_SERVICE_HOST");
        int httpPort = Integer.parseInt(getEnvOrThrow("STANDARD_AUTHSERVICE_SERVICE_PORT_HTTP"));
        String adminUser = getEnvOrThrow("STANDARD_AUTHSERVICE_ADMIN_USER");
        String adminPassword = getEnvOrThrow("STANDARD_AUTHSERVICE_ADMIN_PASSWORD");

        OpenShiftClient openShiftClient = new DefaultOpenShiftClient();
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(openShiftClient);
        KeycloakManager keycloakManager = new KeycloakManager(host, httpPort, adminUser, adminPassword);

        addressSpaceApi.watchAddressSpaces(keycloakManager);
    }

    private static String getEnvOrThrow(String env) {
        String value = System.getenv(env);
        if (value == null) {
            throw new IllegalArgumentException("Required environment variable " + env + " is missing");
        }
        return value;
    }
}
