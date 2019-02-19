/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.iot.model.v1;

import static io.enmasse.iot.model.v1.Version.API_VERSION;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class IoTProjectList extends AbstractList<IoTProject> {

    public static final String KIND = "IoTProjectList";

    public IoTProjectList() {
        super(KIND, API_VERSION);
    }
}
