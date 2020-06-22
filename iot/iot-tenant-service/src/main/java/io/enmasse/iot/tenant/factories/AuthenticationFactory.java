/*
 * Copyright 2020 EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.factories;

import static org.eclipse.hono.service.auth.AuthTokenHelperImpl.forValidating;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.hono.connection.ConnectionFactory;
import org.eclipse.hono.connection.impl.ConnectionFactoryImpl;
import org.eclipse.hono.service.auth.HonoSaslAuthenticatorFactory;
import org.eclipse.hono.service.auth.delegating.DelegatingAuthenticationService;

import io.enmasse.iot.tenant.config.compat.AuthenticationServerClientConfig;
import io.vertx.core.Vertx;

@Dependent
public class AuthenticationFactory {

    @Inject
    AuthenticationServerClientConfig configuration;

    @Inject
    Vertx vertx;

    @Singleton
    public DelegatingAuthenticationService authenticationService() {
        var result = new DelegatingAuthenticationService();
        result.setConfig(this.configuration.toHono());
        result.setConnectionFactory(authenticationServiceConnectionFactory());
        return result;
    }

    private ConnectionFactory authenticationServiceConnectionFactory() {
        return new ConnectionFactoryImpl(this.vertx, this.configuration.toHono());
    }

    @Singleton
    public HonoSaslAuthenticatorFactory authenticatorFactory() {
        return new HonoSaslAuthenticatorFactory(this.vertx, forValidating(this.vertx, this.configuration.toHono().getValidation()));
    }

}
