/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class JdbcDeviceProperties {

    private JdbcProperties adapter;
    private JdbcProperties management;

    public JdbcProperties getAdapter() {
        return adapter;
    }

    public void setAdapter(JdbcProperties adapter) {
        this.adapter = adapter;
    }

    public JdbcProperties getManagement() {
        return management;
    }

    public void setManagement(JdbcProperties management) {
        this.management = management;
    }

}
