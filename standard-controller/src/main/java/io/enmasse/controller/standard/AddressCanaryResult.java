/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import java.util.Set;

public class AddressCanaryResult {
    private final String routerId;
    private final Set<String> passed;
    private final Set<String> failed;

    public AddressCanaryResult(String routerId, Set<String> passed, Set<String> failed) {
        this.routerId = routerId;
        this.passed = passed;
        this.failed = failed;
    }

    public String getRouterId() {
        return routerId;
    }

    public Set<String> getPassed() {
        return passed;
    }

    public Set<String> getFailed() {
        return failed;
    }
}
