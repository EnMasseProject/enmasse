/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.enmasse.admin.model.v1.AbstractWithAdditionalProperties;
import io.sundr.builder.annotations.Buildable;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * A class containing a name and optional description.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteServicePortDescription extends AbstractWithAdditionalProperties {

    @NotNull
    private String name;
    private String displayName;
    private String description;
    private List<TlsTermination> routeTlsTerminations = new ArrayList<>();

    public RouteServicePortDescription() {
    }

    public RouteServicePortDescription(final String name, final String displayName, final String description, List<TlsTermination> routeTlsTerminations) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.routeTlsTerminations = routeTlsTerminations;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TlsTermination> getRouteTlsTerminations() {
        return Collections.unmodifiableList(routeTlsTerminations);
    }

    public void setRouteTlsTerminations(List<TlsTermination> routeTlsTerminations) {
        this.routeTlsTerminations = routeTlsTerminations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteServicePortDescription that = (RouteServicePortDescription) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(description, that.description) &&
                Objects.equals(routeTlsTerminations, that.routeTlsTerminations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, displayName, description, routeTlsTerminations);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("name=").append(this.name);
        sb.append(", displayName=").append(this.displayName);
        sb.append(", description=").append(this.description);
        sb.append(", routeTlsTerminations=").append(this.routeTlsTerminations);
        return sb.append("}").toString();
    }

}
