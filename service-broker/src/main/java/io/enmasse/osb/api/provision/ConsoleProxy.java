/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.osb.api.provision;

import io.enmasse.address.model.AddressSpace;

public interface ConsoleProxy {
    String getConsoleUrl(AddressSpace addressSpace);
}
