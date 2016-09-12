package enmasse.storage.controller.model;

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
