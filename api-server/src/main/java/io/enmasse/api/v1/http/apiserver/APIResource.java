/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1.http.apiserver;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResource
{
    @JsonProperty("name")
    @Valid
    private String name;

    @JsonProperty("singularname")
    @Valid
    private String singularname = "";

    @JsonProperty("namespaced")
    @Valid
    private boolean namespaced = false;

    @JsonProperty("kind")
    @Valid
    private String kind;

    @JsonProperty("verbs")
    @Valid
    private List<String> verbs;


    public APIResource() {
    }

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

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("singularname")
    private String getSingularname() {
        return singularname;
    }

    @JsonProperty("singularname")
    private void setSingularname(String singularname) {
        this.singularname = singularname;
    }


    @JsonProperty("namespaced")
    private boolean getNamespaced() {
        return namespaced;
    }

    @JsonProperty("namespaced")
    private void setNamespaced(boolean namespaced) {
        this.namespaced = namespaced;
    }

    @JsonProperty("kind")
    public String getKind() {
        return kind;
    }

    @JsonProperty("kind")
    public void setKind(String kind) {
        this.kind = kind;
    }


    @JsonProperty("verbs")
    private List<String> getVerbs() {
        return verbs;
    }

    @JsonProperty("verbs")
    private void setVerbs(List<String> verbs) {
        this.verbs = verbs;
    }
}
