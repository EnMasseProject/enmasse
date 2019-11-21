/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResource
{
    @JsonProperty("name")
    private String name;

    @JsonProperty("singularname")
    private String singularname = "";

    @JsonProperty("namespaced")
    private boolean namespaced = false;

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("verbs")
    private List<String> verbs;

    public APIResource(String name, String singularname, boolean namespaced, String kind, List<String> verbs) {
        this.name = name;
        this.singularname = singularname;
        this.namespaced = namespaced;
        this.kind = kind;
        this.verbs = verbs;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("singularname")
    private String getSingularname() {
        return singularname;
    }

    @JsonProperty("namespaced")
    private boolean getNamespaced() {
        return namespaced;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("verbs")
    private List<String> getVerbs() {
        return verbs;
    }
}
