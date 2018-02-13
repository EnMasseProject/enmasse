/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.bind;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static io.enmasse.controller.api.osb.v2.JsonSerializationUtils.getUuid;

@JsonDeserialize(using = BindRequest.Deserializer.class)
public class BindRequest {
    private static final ObjectMapper mapper = new ObjectMapper();

    private UUID serviceId;
    private UUID planId;
    private BindResource bindResource;
    private Map<String, String> parameters = new HashMap<>();

    public BindRequest(UUID serviceId, UUID planId) {
        this.serviceId = serviceId;
        this.planId = planId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    public void setServiceId(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(UUID planId) {
        this.planId = planId;
    }

    public BindResource getBindResource() {
        return bindResource;
    }

    public void setBindResource(BindResource bindResource) {
        this.bindResource = bindResource;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    protected static class Deserializer extends JsonDeserializer<BindRequest> {

        @Override
        public BindRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);

            BindRequest bindRequest = new BindRequest(
                    getUuid(node, "service_id"),
                    getUuid(node, "plan_id")
            );

            ObjectNode bindResourceNode = (ObjectNode) node.get("bind_resource");
            if (bindResourceNode != null) {
                JsonNode appGuid = bindResourceNode.get("app_guid");
                JsonNode route = bindResourceNode.get("route");
                bindRequest.setBindResource(new BindResource(
                        appGuid == null ? null : appGuid.asText(),
                        route == null ? null : route.asText()
                ));
            }

            ObjectNode parametersNode = (ObjectNode) node.get("parameters");
            if (parametersNode != null) {
                Iterator<Map.Entry<String, JsonNode>> iterator = parametersNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    bindRequest.getParameters().put(entry.getKey(), entry.getValue().asText());
                }
            }
            return bindRequest;
        }
    }

    public static class BindResource {
        private String appId;
        private String route;

        public BindResource(String appId, String route) {
            this.appId = appId;
            this.route = route;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getRoute() {
            return route;
        }

        public void setRoute(String route) {
            this.route = route;
        }
    }

}
