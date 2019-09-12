/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.model.addressspace;

public enum AddressSpaceType {
    STANDARD, BROKERED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
