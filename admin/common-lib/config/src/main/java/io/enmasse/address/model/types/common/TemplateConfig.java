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

import java.util.Collections;
import java.util.Map;

/**
 * Represents a template and its parameters.
 */
public class TemplateConfig implements io.enmasse.address.model.types.TemplateConfig {
    private final String name;
    private final Map<String, String> parameters;

    public TemplateConfig(String name, Map<String, String> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
