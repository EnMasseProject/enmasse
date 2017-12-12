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
