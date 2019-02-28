/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.tenant.config;

import static org.eclipse.hono.service.auth.AuthTokenHelperImpl.forValidating;

import org.eclipse.hono.connection.ConnectionFactory;
import org.eclipse.hono.connection.impl.ConnectionFactoryImpl;
import org.eclipse.hono.service.auth.AuthTokenHelper;
import org.eclipse.hono.service.auth.HonoSaslAuthenticatorFactory;
import org.eclipse.hono.service.auth.delegating.AuthenticationServerClientConfigProperties;
import org.eclipse.hono.service.auth.delegating.DelegatingAuthenticationService;
import org.eclipse.hono.util.AuthenticationConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vertx.core.Vertx;

@Configuration
public class AuthenticationConfiguration {

    @Bean
    public DelegatingAuthenticationService authenticationService() {
        return new DelegatingAuthenticationService();
    }

    @Bean
    @ConfigurationProperties(prefix = "enmasse.iot.auth")
    @Qualifier(AuthenticationConstants.QUALIFIER_AUTHENTICATION)
    public AuthenticationServerClientConfigProperties authenticationServiceClientProperties() {
        return new AuthenticationServerClientConfigProperties();
    }

    @Bean
    @Qualifier(AuthenticationConstants.QUALIFIER_AUTHENTICATION)
    public ConnectionFactory authenticationServiceConnectionFactory(final Vertx vertx) {
        return new ConnectionFactoryImpl(vertx, authenticationServiceClientProperties());
    }

    @Bean
    @Qualifier(AuthenticationConstants.QUALIFIER_AUTHENTICATION)
    public AuthTokenHelper tokenValidator(final Vertx vertx) {
        return forValidating(vertx, authenticationServiceClientProperties().getValidation());
    }

    @Bean
    public HonoSaslAuthenticatorFactory authenticatorFactory(
            @Autowired final Vertx vertx,
            @Qualifier(AuthenticationConstants.QUALIFIER_AUTHENTICATION) final AuthTokenHelper validator) {

        return new HonoSaslAuthenticatorFactory(vertx, validator);

    }
}
