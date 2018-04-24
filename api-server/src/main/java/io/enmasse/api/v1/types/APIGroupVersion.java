/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIGroupVersion {
    @JsonProperty("groupVersion")
    @Valid
    private String groupVersion;


    @JsonProperty("version")
    @Valid
    private String version;

    public APIGroupVersion(String groupVersion, String version) {
        this.groupVersion = groupVersion;
        this.version = version;
    }

    @JsonProperty("groupVersion")
    public String getGroupVersion() {
        return groupVersion;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }
}
