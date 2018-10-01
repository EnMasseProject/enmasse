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
import io.enmasse.address.model.Address;
import io.fabric8.kubernetes.api.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonDeserialize(
    using = JsonDeserializer.None.class
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpacePlanList implements KubernetesResource<AddressSpacePlan>, KubernetesResourceList<AddressSpacePlan> {
    private static final long serialVersionUID = 1L;

    private String apiVersion;
    private String kind;
    private ListMeta metadata;
    private List<AddressSpacePlan> items;

    @Override
    public ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<AddressSpacePlan> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<AddressSpacePlan> items) {
        this.items = items;
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
