/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.v1.InfraConfig;

import java.io.IOException;
import java.util.Map;

public interface InfraConfigDeserializer {
   InfraConfig fromJson(String json) throws IOException;
}
