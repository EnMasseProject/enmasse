/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;

public class StatusInitializer implements Controller {

    public AddressSpace reconcileActive(AddressSpace addressSpace) {
            addressSpace.getStatus().setReady(true);
            addressSpace.getStatus().clearMessages();
            return addressSpace;
    }

    @Override
    public String toString() {
        return "StatusInitializer";
    }
}
