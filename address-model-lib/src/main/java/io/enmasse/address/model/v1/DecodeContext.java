/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.Schema;

/**
 * An interface for decoding address space types names
 */
public interface DecodeContext {
    /**
     * Get the defeault authentication service type.
     *
     * @return The default {@link AuthenticationServiceType}.
     */
    AuthenticationServiceType getDefaultAuthenticationServiceType();
}
