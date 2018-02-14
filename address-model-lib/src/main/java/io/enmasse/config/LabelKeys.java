/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config;

/**
 * Labels that are applied to a destination cluster.
 */
public interface LabelKeys {
    String TYPE = "type";

    String UUID = "uuid";
    String ADDRESS = "address";
    String APP = "app";
    String CAPABILITY = "capability";
    String ENVIRONMENT = "environment";
}
