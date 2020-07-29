/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class IoTInfrastructureList extends AbstractList<IoTInfrastructure> {

    public static final String KIND = "IoTInfrastructureList";

    public IoTInfrastructureList() {
        super(KIND, IoTCrd.API_VERSION);
    }
}
