/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Schema;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;

import java.util.List;
import java.util.Optional;

/**
 * Interface for resolving different authentication service
 */
public class SchemaAuthenticationServiceRegistry implements AuthenticationServiceRegistry {

    private final SchemaProvider schemaProvider;

    public SchemaAuthenticationServiceRegistry(SchemaProvider schemaProvider) {
        this.schemaProvider = schemaProvider;
    }

    @Override
    public Optional<AuthenticationService> findAuthenticationService(io.enmasse.address.model.AuthenticationService authenticationService) {
        if (authenticationService == null) {
            return resolveDefaultAuthenticationService();
        }
        if (authenticationService.getName() == null) {
            return findAuthenticationServiceByType(authenticationService.getType().toAdminType()).stream().findFirst();
        } else {
            return findAuthenticationServiceByName(authenticationService.getName());
        }
    }

    public Optional<AuthenticationService> findAuthenticationServiceByName(String name) {
        Schema schema = schemaProvider.getSchema();
        return schema.findAuthenticationService(name);
    }

    @Override
    public List<AuthenticationService> findAuthenticationServiceByType(AuthenticationServiceType type) {
        Schema schema = schemaProvider.getSchema();
        return schema.findAuthenticationServiceType(type);
    }

    @Override
    public Optional<AuthenticationService> resolveDefaultAuthenticationService() {
        Schema schema = schemaProvider.getSchema();
        List<AuthenticationService> standards = schema.findAuthenticationServiceType(AuthenticationServiceType.standard);
        if (standards.isEmpty()) {
            return schema.findAuthenticationServiceType(AuthenticationServiceType.none).stream()
                    .findFirst();
        } else {
            return standards.stream().findFirst();
        }
    }

    @Override
    public List<AuthenticationService> listAuthenticationServices() {
        return schemaProvider.getSchema().getAuthenticationServices();
    }
}
