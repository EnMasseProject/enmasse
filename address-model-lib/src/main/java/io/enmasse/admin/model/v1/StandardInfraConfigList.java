/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListMeta;

import java.util.Collections;
import java.util.List;

@JsonDeserialize(
    using = JsonDeserializer.None.class
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigList implements KubernetesResource<StandardInfraConfig>, KubernetesResourceList<StandardInfraConfig> {
    private static final long serialVersionUID = 1L;

    private String apiVersion;
    private String kind;
    private ListMeta metadata;
    private final List<StandardInfraConfig> items;

    @JsonCreator
    public StandardInfraConfigList(@JsonProperty("items") List<StandardInfraConfig> items) {
        this.items = items;
    }

    @Override
    public ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<StandardInfraConfig> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setMetadata(ListMeta metadata) {
        this.metadata = metadata;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
