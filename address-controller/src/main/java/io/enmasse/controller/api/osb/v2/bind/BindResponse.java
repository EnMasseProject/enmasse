/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.bind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = BindResponse.Serializer.class)
public class BindResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    private Map<String, String> credentials;
    private String syslogDrainUrl;
    private String routeServiceUrl;
    private List<Object> volumeMounts = new ArrayList<>();

    public BindResponse(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public String getSyslogDrainUrl() {
        return syslogDrainUrl;
    }

    public void setSyslogDrainUrl(String syslogDrainUrl) {
        this.syslogDrainUrl = syslogDrainUrl;
    }

    public String getRouteServiceUrl() {
        return routeServiceUrl;
    }

    public void setRouteServiceUrl(String routeServiceUrl) {
        this.routeServiceUrl = routeServiceUrl;
    }

    public List<Object> getVolumeMounts() {
        return volumeMounts;
    }

    public void setVolumeMounts(List<Object> volumeMounts) {
        this.volumeMounts = volumeMounts;
    }

    protected static class Serializer extends JsonSerializer<BindResponse> {
        @Override
        public void serialize(BindResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            if (value.getSyslogDrainUrl() != null) {
                node.put("syslog_drain_url", value.getSyslogDrainUrl());
            }

            if (value.getRouteServiceUrl() != null) {
                node.put("route_service_url", value.getRouteServiceUrl());
            }

            // TODO volumeMounts

            ObjectNode credentialsNode = node.putObject("credentials");
            value.getCredentials().forEach(credentialsNode::put);

            mapper.writeValue(gen, node);
        }
    }
}
