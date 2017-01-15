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

package enmasse.address.controller.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A @{link Flavor} represents a fixed set of configuration parameters for a template.
 */
public class Flavor {
    private final String templateName;
    private final Map<String, String> templateParameters;

    private Flavor(String templateName,
                   Map<String, String> templateParameters) {
        this.templateName = templateName;
        this.templateParameters = templateParameters;
    }

    public String templateName() {
        return templateName;
    }

    public Map<String, String> templateParameters() {
        return templateParameters;
    }

    public static class Builder {
        private String templateName;
        private Map<String, String> templateParameters = new LinkedHashMap<>();

        public Builder templateParameter(String key, String value) {
            templateParameters.put(key, value);
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Flavor build() {
            if (templateName == null) {
                throw new IllegalArgumentException("Cannot instantiate flavor without template name");
            }
            return new Flavor(templateName, templateParameters);
        }
    }
}
