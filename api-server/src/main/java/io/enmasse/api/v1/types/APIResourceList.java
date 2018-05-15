/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "apiVersion",
        "kind"
})
public class APIResourceList {
    @NotNull
    @JsonProperty("apiVersion")
    private String apiVersion = "v1";

    @NotNull
    @JsonProperty("kind")
    private String kind = "APIResourceList";

    @JsonProperty("groupVersion")
    private String groupVersion;

    @JsonProperty("resources")
    @Valid
    private List<APIResource> resources;

    public APIResourceList(String groupVersion, List<APIResource> resources) {
        this.groupVersion = groupVersion;
        this.resources = resources;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("groupVersion")
    public String getGroupVersion() {
        return groupVersion;
    }

    @JsonProperty("resources")
    private List<APIResource> getResources() {
        return resources;
    }
}
