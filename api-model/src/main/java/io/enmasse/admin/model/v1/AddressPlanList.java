/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressPlanList extends AbstractList<AddressPlan> {

    public static final String KIND = "AddressPlanList";

    public AddressPlanList() {
        super(KIND, AdminCrd.API_VERSION);
    }

}
