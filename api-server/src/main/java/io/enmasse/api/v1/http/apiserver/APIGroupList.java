/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http.apiserver;

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
public class APIGroupList {
    @NotNull
    @JsonProperty("apiVersion")
    private String apiVersion = "v1";

    @NotNull
    @JsonProperty("kind")
    private String kind = "APIGroupList";

    @JsonProperty("groups")
    @Valid
    private List<APIGroup> groups;

    public APIGroupList() {}

    public APIGroupList(String apiVersion, String kind, List<APIGroup> groups) {
        this.apiVersion = apiVersion;
        this.kind = kind;
        this.groups = groups;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("apiVersion")
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }

    @JsonProperty("groups")
    public List<APIGroup> getGroups() {
        return groups;
    }

    @JsonProperty("groups")
    public void setGroups(List<APIGroup> groups) {
        this.groups = groups;
    }
}
