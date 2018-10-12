/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class AddressPlanList implements KubernetesResource<AddressPlan>, KubernetesResourceList<AddressPlan> {
    private static final long serialVersionUID = 1L;

    private String apiVersion;
    private String kind;
    private ListMeta metadata;
    private List<AddressPlan> items;

    @Override
    public ListMeta getMetadata() {
        return metadata;
    }

    @Override
    public List<AddressPlan> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<AddressPlan> items) {
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
