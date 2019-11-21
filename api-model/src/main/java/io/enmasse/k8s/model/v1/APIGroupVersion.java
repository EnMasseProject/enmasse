/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIGroupVersion {
    @JsonProperty("groupVersion")
    private String groupVersion;

    @JsonProperty("version")
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
