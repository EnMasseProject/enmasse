/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;

@DefaultCustomResource
@SuppressWarnings("serial")
public class StandardInfraConfigList extends CustomResourceList<StandardInfraConfig> {

    public static final String KIND = "StandardInfraConfigList";

}
