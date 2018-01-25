/*
 * Copyright 2018 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
