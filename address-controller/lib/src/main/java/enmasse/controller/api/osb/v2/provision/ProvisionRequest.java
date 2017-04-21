package enmasse.controller.api.osb.v2.provision;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static enmasse.controller.api.osb.v2.JsonSerializationUtils.getRequiredField;
import static enmasse.controller.api.osb.v2.JsonSerializationUtils.getUuid;

@JsonDeserialize(using = ProvisionRequest.Deserializer.class)
public class ProvisionRequest {
    private static final ObjectMapper mapper = new ObjectMapper();

    private String organizationId;
    private String spaceId;
    private UUID serviceId;
    private UUID planId;
    private Map<String, String> parameters = new HashMap<>();
    private boolean acceptsIncomplete;

    public ProvisionRequest() {
    }

    public ProvisionRequest(UUID serviceId, UUID planId, boolean acceptsIncomplete, String organizationId, String spaceId) {
        this.serviceId = serviceId;
        this.planId = planId;
        this.acceptsIncomplete = acceptsIncomplete;
        this.organizationId = organizationId;
        this.spaceId = spaceId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
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

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public boolean isAcceptsIncomplete() {
        return acceptsIncomplete;
    }

    public void setAcceptsIncomplete(boolean acceptsIncomplete) {
        this.acceptsIncomplete = acceptsIncomplete;
    }

    public Optional<String> getParameter(String name) {
        return Optional.ofNullable(parameters.get(name));
    }

    protected static class Deserializer extends JsonDeserializer<ProvisionRequest> {

        @Override
        public ProvisionRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectNode node = mapper.readValue(p, ObjectNode.class);
            JsonNode acceptsIncomplete = node.get("accepts_incomplete");

            ProvisionRequest provisionRequest = new ProvisionRequest(
                    getUuid(node, "service_id"),
                    getUuid(node, "plan_id"),
                    acceptsIncomplete != null && acceptsIncomplete.asBoolean(),
                    getRequiredField(node, "organization_guid").asText(),
                    getRequiredField(node, "space_guid").asText());

            ObjectNode parametersNode = (ObjectNode) node.get("parameters");
            if (parametersNode != null) {
                Iterator<Map.Entry<String, JsonNode>> iterator = parametersNode.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    provisionRequest.getParameters().put(entry.getKey(), entry.getValue().asText());
                }
            }

            return provisionRequest;
        }

    }
}
