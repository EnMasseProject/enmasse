/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.admin.model.v1.DoneableAuthenticationService;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.vertx.core.json.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AuthServiceUtils {

    public static AuthenticationService createNoneAuthServiceObject(String name) {
        return createAuthService(name, AuthenticationServiceType.none).done();
    }

    public static AuthenticationService createStandardAuthServiceObject(String name, boolean persistent) {
        return createStandardAuthServiceObject(name, persistent, "5Gi", true, name);
    }

    public static AuthenticationService createStandardAuthServiceObject(String name, String host, int port, String type, String database, String credentialsSecret) {
        return new DoneableAuthenticationService(createStandardAuthServiceObject(name, false, "5Gi", true, name))
                .editSpec()
                .editStandard()
                .addToAdditionalProperties("datasource",
                        createDatasource(host, port, type, database, credentialsSecret))
                .endStandard()
                .endSpec()
                .done();
    }

    public static AuthenticationService createStandardAuthServiceObject(String name, boolean persistent, String volumeSize, boolean deleteClaim, String claimName) {
        return createAuthService(name, AuthenticationServiceType.standard)
                .editSpec()
                .withNewStandard()
                .addToAdditionalProperties("storage", createStorage(persistent, volumeSize, deleteClaim, claimName))
                .addToAdditionalProperties("resources", createResources("1Gi", "2Gi"))
                .endStandard()
                .endSpec()
                .done();
    }

    public static AuthenticationService createExternalAuthServiceObject(String name, String host, int port, String realm, SecretReference caCertSecret, SecretReference clientCertSecret) {
        return createAuthService(name, AuthenticationServiceType.external)
            .editSpec()
            .addToAdditionalProperties("realm", realm)
            .withNewExternal()
            .addToAdditionalProperties("allowOverride", true)
            .addToAdditionalProperties("host", host)
            .addToAdditionalProperties("port", port)
            .addToAdditionalProperties("caCertSecret", caCertSecret)
            .addToAdditionalProperties("clientCertSecret", clientCertSecret)
            .endExternal()
            .endSpec()
            .done();
    }

    private static DoneableAuthenticationService createAuthService(String name, AuthenticationServiceType type) {
        return new DoneableAuthenticationService(new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withType(type)
                .endSpec()
                .build());
    }

    private static Map<String, String> createStorage(boolean persistent, String size, boolean deleteClaim, String claimName) {
        Map<String, String> storage = new HashMap<>();
        storage.put("type", persistent ? "persistent-claim" : "ephemeral");
        if (persistent) {
            storage.put("delete-claim", Boolean.toString(deleteClaim));
            storage.put("claimName", claimName);
            storage.put("size", size);
        }
        return storage;
    }

    private static Map<String, Object> createResources(String requestMemory, String limitMemory) {
        Map<String, Object> resources = new HashMap<>();
        resources.put("requests", Collections.singletonMap("memory", requestMemory));
        resources.put("limits", Collections.singletonMap("memory", limitMemory));
        return resources;
    }

    private static Map<String, Object> createDatasource(String host, int port, String type, String database, String credentialsSecret) {
        Map<String, Object> datasource = new HashMap<>();
        datasource.put("type", type);
        datasource.put("host", host);
        datasource.put("port", port);
        datasource.put("database", database);
        datasource.put("credentialsSecret", Collections.singletonMap("name", credentialsSecret));
        return datasource;
    }

    public static JsonObject authenticationServiceToJson(AuthenticationService service) throws JsonProcessingException {
        return new JsonObject(new ObjectMapper().writeValueAsString(service));
    }
}
