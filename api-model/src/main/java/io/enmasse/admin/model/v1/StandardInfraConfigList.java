/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class StandardInfraConfigList extends AbstractList<StandardInfraConfig>{

    public static final String KIND = "StandardInfraConfigList";
    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public StandardInfraConfigList() {
        super(KIND, API_VERSION);
    }

}
