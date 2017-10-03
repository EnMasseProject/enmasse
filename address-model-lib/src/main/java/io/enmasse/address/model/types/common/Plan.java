/*
 * Copyright 2017 Red Hat Inc.
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
package io.enmasse.address.model.types.common;

import java.util.Optional;

/**
 * A plan type that contains common behavior for all address space types.
 */
public class Plan implements io.enmasse.address.model.types.Plan {

    private final String name;
    private final String displayName;
    private final String description;
    private final String uuid;
    private final TemplateConfig templateConfig;

    public Plan(String name, String displayName, String description, String uuid, TemplateConfig templateConfig) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.uuid = uuid;
        this.templateConfig = templateConfig;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUuid() {
        return uuid;
    }
    @Override
    public Optional<io.enmasse.address.model.types.TemplateConfig> getTemplateConfig() {
        return Optional.ofNullable(templateConfig);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{name=").append(name).append(",");
        sb.append("uuid=").append(uuid).append("}");
        return sb.toString();
    }
}
