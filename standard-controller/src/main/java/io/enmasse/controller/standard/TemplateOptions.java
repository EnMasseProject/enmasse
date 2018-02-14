/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.Map;

public class TemplateOptions {
    private final Map<String, String> env;

    public TemplateOptions(Map<String, String> env) {
        this.env = env;
    }

    public String getMessagingSecret() {
        return env.get("MESSAGING_SECRET");
    }

    public String getAuthenticationServiceHost() {
        return env.get("AUTHENTICATION_SERVICE_HOST");
    }

    public String getAuthenticationServicePort() {
        return env.get("AUTHENTICATION_SERVICE_PORT");
    }

    public String getAuthenticationServiceCaSecret() {
        return env.get("AUTHENTICATION_SERVICE_CA_SECRET");
    }

    public String getAuthenticationServiceClientSecret() {
        return env.get("AUTHENTICATION_SERVICE_CLIENT_SECRET");
    }

    public String getAuthenticationServiceSaslInitHost() {
        return env.get("AUTHENTICATION_SERVICE_SASL_INIT_HOST");
    }
}
