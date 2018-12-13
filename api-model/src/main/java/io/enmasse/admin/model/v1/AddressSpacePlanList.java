/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.enmasse.common.model.AbstractList;
import io.enmasse.common.model.DefaultCustomResource;

@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpacePlanList extends AbstractList<AddressSpacePlan> {

    public static final String KIND = "AddressSpacePlanList";
    public static final String VERSION = "v1alpha1";
    public static final String GROUP = "admin.enmasse.io";
    public static final String API_VERSION = GROUP + "/" + VERSION;

    @JsonCreator
    public AddressSpacePlanList() {
        super(KIND, API_VERSION);
    }
}
