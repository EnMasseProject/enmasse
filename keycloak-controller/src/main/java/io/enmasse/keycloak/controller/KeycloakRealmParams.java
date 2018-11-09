/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.keycloak.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class KeycloakRealmParams {

    private static final Logger log = LoggerFactory.getLogger(KeycloakRealmParams.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final KeycloakRealmParams NULL_PARAMS = new KeycloakRealmParams(null, null, null, null);

    private final String identityProviderUrl;
    private final String identityProviderClientId;
    private final String identityProviderClientSecret;
    private final Map<String, String> browserSecurityHeaders;

    public KeycloakRealmParams(String identityProviderUrl, String identityProviderClientId, String identityProviderClientSecret, Map<String, String> browserSecurityHeaders) {
        this.identityProviderUrl = identityProviderUrl;
        this.identityProviderClientId = identityProviderClientId;
        this.identityProviderClientSecret = identityProviderClientSecret;
        this.browserSecurityHeaders = browserSecurityHeaders;
    }

    public static KeycloakRealmParams fromKube(KubernetesClient client, String configName) {

        ConfigMap config = client.configMaps().withName(configName).get();
        String identityProviderUrl = config.getData().get("identityProviderUrl");
        String identityProviderClientId = config.getData().get("identityProviderClientId");
        String identityProviderClientSecret = config.getData().get("identityProviderClientSecret");

        Map<String, String> browserSecurityHeaders = new HashMap<>();
        String browserSecurityHeadersString = config.getData().get("browserSecurityHeaders");
        if (browserSecurityHeadersString != null) {
            try {
                ObjectNode data = mapper.readValue(browserSecurityHeadersString, ObjectNode.class);
                Iterator<String> it = data.fieldNames();
                while (it.hasNext()) {
                    String key = it.next();
                    browserSecurityHeaders.put(key, data.get(key).asText());
                }
            } catch (IOException e) {
                log.warn("Error parsing browserSecurityHeaders, skipping");
            }
        }

        return new KeycloakRealmParams(identityProviderUrl, identityProviderClientId, identityProviderClientSecret, browserSecurityHeaders);
    }

    @Override
    public String toString() {
        return "{identityProviderUrl=" + identityProviderUrl + "," +
                "identityProviderClientId=" + identityProviderClientId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeycloakRealmParams that = (KeycloakRealmParams) o;
        return Objects.equals(identityProviderUrl, that.identityProviderUrl) &&
                Objects.equals(identityProviderClientId, that.identityProviderClientId) &&
                Objects.equals(identityProviderClientSecret, that.identityProviderClientSecret) &&
                Objects.equals(browserSecurityHeaders, that.browserSecurityHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityProviderUrl, identityProviderClientId, identityProviderClientSecret, browserSecurityHeaders);
    }

    public String getIdentityProviderUrl() {
        return identityProviderUrl;
    }

    public String getIdentityProviderClientId() {
        return identityProviderClientId;
    }

    public String getIdentityProviderClientSecret() {
        return identityProviderClientSecret;
    }

    public Map<String, String> getBrowserSecurityHeaders() {
        return Collections.unmodifiableMap(browserSecurityHeaders);
    }
}
