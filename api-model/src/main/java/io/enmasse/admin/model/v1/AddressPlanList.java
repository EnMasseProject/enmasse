/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
public class AddressPlanList extends AbstractList<AddressPlan> {

    private static final long serialVersionUID = 1L;

    public static final String KIND = "AddressPlanList";
    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    public AddressPlanList() {
        super(KIND, API_VERSION);
    }

}
