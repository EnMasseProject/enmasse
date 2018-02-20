/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config;

public interface AnnotationKeys {
    String CLUSTER_ID = "cluster_id";
    String ADDRESS_SPACE = "addressSpace";
    String CERT_SECRET_NAME = "io.enmasse.certSecretName";
    String CERT_CN = "io.enmasse.certCn";
    String ENDPOINT_PORT = "io.enmasse.endpointPort";
    String SERVICE_NAME = "io.enmasse.serviceName";
    String CREATED_BY = "io.enmasse.createdBy";
    String DEFINED_BY = "enmasse.io/defined-by";
    String BROKER_ID = "enmasse.io/broker-id";
    String NAMESPACE = "enmasse.io/namespace";
    String UUID = "enmasse.io/uuid";
}
