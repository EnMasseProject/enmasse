/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.controller.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A @{link Flavor} represents a fixed set of configuration parameters for a template.
 */
public class Flavor {
    private final String templateName;
    private final Map<String, String> templateParameters;
    private final String name;
    private final String type;
    private final String description;
    private final Optional<String> uuid;

    private Flavor(String name,
                   String type,
                   String description,
                   String templateName,
                   Map<String, String> templateParameters,
                   Optional<String> uuid) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.templateName = templateName;
        this.templateParameters = templateParameters;
        this.uuid = uuid;
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public String description() {
        return description;
    }

    public String templateName() {
        return templateName;
    }

    public Map<String, String> templateParameters() {
        return templateParameters;
    }

    public Optional<String> uuid() {
        return uuid;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{name=").append(name).append(",")
                .append("templateName=").append(templateName).append(",")
                .append("templateParameters=").append(templateParameters).append("}");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flavor flavor = (Flavor) o;

        if (!name.equals(flavor.name)) return false;
        if (!templateName.equals(flavor.templateName)) return false;
        return templateParameters.equals(flavor.templateParameters);
    }

    @Override
    public int hashCode() {
        int result = templateName.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + templateParameters.hashCode();
        return result;
    }

    public static class Builder {
        private String name;
        private String type;
        private String description;
        private String templateName;
        private Map<String, String> templateParameters = new LinkedHashMap<>();
        private Optional<String> uuid = Optional.empty();

        public Builder(String name, String templateName) {
            this.name = name;
            this.templateName = templateName;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder templateParameter(String key, String value) {
            templateParameters.put(key, value);
            return this;
        }

        public Builder uuid(Optional<String> uuid) {
            this.uuid = uuid;
            return this;
        }

        public Flavor build() {
            return new Flavor(name, type, description, templateName, templateParameters, uuid);
        }
    }
}
