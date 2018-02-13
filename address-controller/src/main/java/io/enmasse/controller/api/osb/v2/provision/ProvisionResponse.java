/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.provision;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = ProvisionResponse.Serializer.class)
public class ProvisionResponse {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String dashboardUrl;
    private String operation;

    public ProvisionResponse() {
    }

    public ProvisionResponse(String dashboardUrl) {
        this(dashboardUrl, null);
    }

    public ProvisionResponse(String dashboardUrl, String operation) {
        this.dashboardUrl = dashboardUrl;
        this.operation = operation;
    }


    public String getDashboardUrl() {
        return dashboardUrl;
    }

    public void setDashboardUrl(String dashboardUrl) {
        this.dashboardUrl = dashboardUrl;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    protected static class Serializer extends JsonSerializer<ProvisionResponse> {
        @Override
        public void serialize(ProvisionResponse value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();
            if (value.getDashboardUrl() != null) {
                node.put("dashboard_url", value.getDashboardUrl());
            }
            if (value.getOperation() != null) {
                node.put("operation", value.getOperation());
            }
            mapper.writeValue(gen, node);
        }
    }
}
