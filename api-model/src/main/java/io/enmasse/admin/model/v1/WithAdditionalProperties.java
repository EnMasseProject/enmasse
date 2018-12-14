/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.admin.model.v1;

import java.util.Map;

public interface WithAdditionalProperties {

    Map<String, Object> getAdditionalProperties();
    void setAdditionalProperties(Map<String, Object> additionalProperties);
    void setAdditionalProperty(String name, Object value);

}