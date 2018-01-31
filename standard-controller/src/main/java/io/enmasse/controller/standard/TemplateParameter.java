/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

/**
 * Template parameters that are dynamically set by the address controller.
 */
public interface TemplateParameter {
    String NAME = "NAME";
    String ADDRESS = "ADDRESS";
    String ADDRESS_SPACE = "ADDRESS_SPACE";
    String CLUSTER_ID = "CLUSTER_ID";

    String COLOCATED_ROUTER_SECRET = "COLOCATED_ROUTER_SECRET";

    String AUTHENTICATION_SERVICE_HOST = "AUTHENTICATION_SERVICE_HOST";
    String AUTHENTICATION_SERVICE_PORT = "AUTHENTICATION_SERVICE_PORT";
    String AUTHENTICATION_SERVICE_CA_SECRET = "AUTHENTICATION_SERVICE_CA_SECRET";
    String AUTHENTICATION_SERVICE_CLIENT_SECRET = "AUTHENTICATION_SERVICE_CLIENT_SECRET";
    String AUTHENTICATION_SERVICE_SASL_INIT_HOST = "AUTHENTICATION_SERVICE_SASL_INIT_HOST";
    String REPLICAS = "REPLICAS";
}
