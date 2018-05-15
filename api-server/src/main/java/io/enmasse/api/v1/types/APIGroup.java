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
public class APIGroup
{
    @NotNull
    @JsonProperty("apiVersion")
    private String apiVersion = "v1";

    @NotNull
    @JsonProperty("kind")
    private String kind = "APIGroup";

    @JsonProperty("name")
    @Valid
    private String name;

    @JsonProperty("versions")
    @Valid
    private List<APIGroupVersion> versions;

    @JsonProperty("preferredVersion")
    @Valid
    private APIGroupVersion preferredVersion;

    @JsonProperty("serverAddressByClientCIDRs")
    @Valid
    private String serverAddressByClientCIDRs;

    public APIGroup(String name, List<APIGroupVersion> versions, APIGroupVersion preferredVersion, String serverAddressByClientCIDRs) {
        this.name = name;
        this.versions = versions;
        this.preferredVersion = preferredVersion;
        this.serverAddressByClientCIDRs = serverAddressByClientCIDRs;
    }

    @JsonProperty("apiVersion")
    public String getApiVersion() {
        return apiVersion;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("versions")
    private List<APIGroupVersion> getVersions() {
        return versions;
    }

    @JsonProperty("preferredVersion")
    private APIGroupVersion getPreferredVersion() {
        return preferredVersion;
    }

    @JsonProperty("serverAddressByClientCIDRs")
    private String getServerAddressByClientCIDRs() {
        return serverAddressByClientCIDRs;
    }
}
