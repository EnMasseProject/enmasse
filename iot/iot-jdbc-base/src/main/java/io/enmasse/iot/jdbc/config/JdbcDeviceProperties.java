/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.config;

import io.enmasse.iot.model.v1.Mode;

public class JdbcDeviceProperties extends JdbcProperties {

    private Mode mode = Mode.JSON_TREE;

    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

}
