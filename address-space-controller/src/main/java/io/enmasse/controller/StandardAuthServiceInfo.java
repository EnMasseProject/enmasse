/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

public class StandardAuthServiceInfo {
    private final String configMap;

    public StandardAuthServiceInfo(String configMap) {
        this.configMap = configMap;
    }

    public String getConfigMap() {
        return configMap;
    }
}
