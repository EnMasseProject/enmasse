/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonSerialize(using = Service.Serializer.class)
public class Service {
    private static final ObjectMapper mapper = new ObjectMapper();

    private UUID uuid;
    private String name;
    private String description;
    private List<String> tags = new ArrayList<>();
    private List<String> requires = new ArrayList<>();
    private boolean bindable;
    private boolean planUpdatable;
    private Map<String, String> metadata = new HashMap<>();
    private DashboardClient dashboardClient;
    private List<Plan> plans = new ArrayList<>();

    public Service() {
    }

    public Service(UUID uuid, String name, String description, boolean bindable) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.bindable = bindable;
    }


    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getRequires() {
        return requires;
    }

    public void setRequires(List<String> requires) {
        this.requires = requires;
    }

    public boolean isBindable() {
        return bindable;
    }

    public void setBindable(boolean bindable) {
        this.bindable = bindable;
    }

    public boolean isPlanUpdatable() {
        return planUpdatable;
    }

    public void setPlanUpdatable(boolean planUpdatable) {
        this.planUpdatable = planUpdatable;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public DashboardClient getDashboardClient() {
        return dashboardClient;
    }

    public void setDashboardClient(DashboardClient dashboardClient) {
        this.dashboardClient = dashboardClient;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    @Override
    public String toString() {
        return "Service{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                ", requires=" + requires +
                ", bindable=" + bindable +
                ", planUpdatable=" + planUpdatable +
                ", metadata=" + metadata +
                ", dashboardClient=" + dashboardClient +
                ", plans=" + plans +
                '}';
    }

    protected static class Serializer extends JsonSerializer<Service> {
        @Override
        public void serialize(Service service, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ObjectNode node = mapper.createObjectNode();

            node.put("id", service.getUuid().toString());
            node.put("name", service.getName());
            node.put("description", service.getDescription());
            node.put("bindable", service.isBindable());

            ArrayNode tagsNode = node.putArray("tags");
            service.getTags().forEach(tagsNode::add);

            ArrayNode requiresNode = node.putArray("requires");
            service.getRequires().forEach(requiresNode::add);

            ObjectNode metadataNode = node.putObject("metadata");
            service.getMetadata().forEach(metadataNode::put);

            if (service.getDashboardClient() != null) {
                node.set("dashboard_client", mapper.valueToTree(service.getDashboardClient()));
            }
            node.put("plan_updateable", service.isPlanUpdatable()); // The e in updateable is in the OSB API spec! Don't change.

            ArrayNode plansNode = node.putArray("plans");
            service.getPlans().forEach(plan -> {
                JsonNode jsonNode = mapper.valueToTree(plan);
                plansNode.add(jsonNode);
            });

            mapper.writeValue(gen, node);
        }
    }
}
