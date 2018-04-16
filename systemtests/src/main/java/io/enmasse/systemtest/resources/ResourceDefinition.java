/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import java.util.List;

public class ResourceDefinition {

    List<ResourceParameter> parameters;
    private String name;
    private String template;

    public ResourceDefinition(String name, String template, List<ResourceParameter> parameters) {
        this.name = name;
        this.template = template;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

    public List<ResourceParameter> getParameters() {
        return parameters;
    }
}
