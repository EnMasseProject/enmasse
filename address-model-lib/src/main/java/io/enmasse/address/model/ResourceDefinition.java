/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

public class ResourceDefinition {
    private final String name;
    private final String templateName;
    private final Map<String, String> templateParameters;

    private ResourceDefinition(String name, String templateName, Map<String, String> templateParameters) {
        this.name = name;
        this.templateName = templateName;
        this.templateParameters = templateParameters;
    }

    public String getName() {
        return name;
    }

    public Optional<String> getTemplateName() {
        return Optional.ofNullable(templateName);
    }

    public Map<String, String> getTemplateParameters() {
        return Collections.unmodifiableMap(templateParameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceDefinition that = (ResourceDefinition) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public static class Builder {
        private String name;
        private String templateName;
        private Map<String, String> templateParameters = new HashMap<>();

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setTemplateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder setTemplateParameters(Map<String, String> templateParameters) {
            this.templateParameters = new HashMap<>(templateParameters);
            return this;
        }

        public ResourceDefinition build() {
            Objects.requireNonNull(name);
            return new ResourceDefinition(name, templateName, templateParameters);
        }
    }
}
