/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@DefaultCustomResource
@SuppressWarnings("serial")
@RegisterForReflection
public class IoTConfigList extends AbstractList<IoTConfig> {

    public static final String KIND = "IoTConfigList";

    public IoTConfigList() {
        super(KIND, IoTCrd.API_VERSION);
    }
}
