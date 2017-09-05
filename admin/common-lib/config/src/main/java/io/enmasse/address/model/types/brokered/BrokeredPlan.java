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
package io.enmasse.address.model.types.brokered;

import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.TemplateConfig;

import java.util.Optional;

public class BrokeredPlan implements Plan {
    private final String name;
    private final String displayName;
    private final String description;
    private final String uuid;

    public BrokeredPlan(String name, String displayName, String description, String uuid) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.uuid = uuid;
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
    public Optional<TemplateConfig> getTemplateConfig() {
        return Optional.empty();
    }
}
