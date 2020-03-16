/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.store.devcon;

import static java.util.Optional.empty;

import java.util.Objects;
import java.util.Optional;

public class DeviceState {

    private Optional<String> lastKnownGateway = empty();

    public DeviceState() {}

    public void setLastKnownGateway(final Optional<String> lastKnownGateway) {
        Objects.requireNonNull(lastKnownGateway);
        this.lastKnownGateway = lastKnownGateway;
    }

    public Optional<String> getLastKnownGateway() {
        return lastKnownGateway;
    }
}
