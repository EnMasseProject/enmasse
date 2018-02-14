/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.keycloak.spi;

import org.keycloak.Config;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.credential.hash.PasswordHashProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ScramSha512PasswordHashProviderFactory implements PasswordHashProviderFactory {

    public static final String ID = "scramsha512";

    public static final int DEFAULT_ITERATIONS = 20000;

    @Override
    public PasswordHashProvider create(KeycloakSession session) {
        return new ScramPasswordHashProvider(ID, DEFAULT_ITERATIONS, "HmacSHA512", "SHA-512");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void close() {
    }
}
