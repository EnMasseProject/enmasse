/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.scale.metrics;

import java.util.ArrayList;
import java.util.List;

public class MetricsMonitoringResult {

    private boolean result = true;
    private List<String> errors = new ArrayList<>();

    public MetricsMonitoringResult addError(String error) {
        result = false;
        errors.add(error);
        return this;
    }

    public boolean isError() {
        return !result;
    }

    public List<String> getErrors() {
        return errors;
    }

}
