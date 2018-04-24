/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config;

public interface AnnotationKeys {
    String CLUSTER_ID = "cluster_id";
    String ADDRESS_SPACE = "addressSpace";
    String CERT_PROVIDER = "enmasse.io/cert-provider";
    String CERT_SECRET_NAME = "enmasse.io/cert-secret";
    String CERT_CN = "enmasse.io/cert-cn";
    String ENDPOINT_PORT = "io.enmasse.endpointPort";
    String SERVICE_NAME = "enmasse.io/service-name";
    String SERVICE_PORT_PREFIX = "enmasse.io/service-port.";
    String CREATED_BY_OLD = "io.enmasse.createdBy";
    String CREATED_BY = "enmasse.io/created-by";
    String CREATED_BY_UID = "enmasse.io/created-by-uid";
    String DEFINED_BY = "enmasse.io/defined-by";
    String BROKER_ID = "enmasse.io/broker-id";
    String NAMESPACE = "enmasse.io/namespace";
    String REALM_NAME = "enmasse.io/realm-name";
    String UUID = "enmasse.io/uuid";
}
