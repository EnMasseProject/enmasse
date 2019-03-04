/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.keycloak;

import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.util.JULInitializingTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeycloakRealmParamsTest extends JULInitializingTest {

    @Test
    void testRequiredEnvironment() {
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_URL, "https://localhost:8443/auth")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_ID, "myclient")
                .addToAnnotations(AnnotationKeys.IDENTITY_PROVIDER_CLIENT_SECRET, "mysecret")
                .addToAnnotations(AnnotationKeys.BROWSER_SECURITY_HEADERS, "{\"mykey\":\"myvalue\"}")
                .endMetadata()
                .withNewStatus()
                .withHost("example.com")
                .withPort(5671)
                .endStatus()
                .build();

        KeycloakRealmParams params = KeycloakRealmParams.fromAuthenticationService(authenticationService);
        assertEquals("https://localhost:8443/auth", params.getIdentityProviderUrl());
        assertEquals("myclient", params.getIdentityProviderClientId());
        assertEquals("mysecret", params.getIdentityProviderClientSecret());
        assertEquals("myvalue", params.getBrowserSecurityHeaders().get("mykey"));
    }
}
