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
    String REALM_NAME = "enmasse.io/realm-name";
    String UUID = "enmasse.io/uuid";
    String ENDPOINT = "enmasse.io/endpoint";
    String ADDRESS = "enmasse.io/address";
    String INFRA_UUID = "enmasse.io/infra-uuid";
    String TEMPLATE_NAME = "enmasse.io/template-name";
    String QUEUE_TEMPLATE_NAME = "enmasse.io/queue-template-name";
    String TOPIC_TEMPLATE_NAME = "enmasse.io/topic-template-name";
    String WITH_MQTT = "enmasse.io/with-mqtt";
    String MQTT_TEMPLATE_NAME = "enmasse.io/mqtt-template-name";
    String APPLIED_INFRA_CONFIG = "enmasse.io/applied-infra-config";
    String OPENSHIFT_SERVING_CERT_SECRET_NAME = "service.alpha.openshift.io/serving-cert-secret-name";
    String APPLIED_PLAN = "enmasse.io/applied-plan";
    String OAUTH_URL = "enmasse.io/oauth-url";
    String IDENTITY_PROVIDER_URL = "enmasse.io/identity-provider-url";
    String IDENTITY_PROVIDER_CLIENT_ID = "enmasse.io/identity-provider-client-id";
    String IDENTITY_PROVIDER_CLIENT_SECRET = "enmasse.io/identity-provider-client-secret";
    String BROWSER_SECURITY_HEADERS = "enmasse.io/browser-security-headers";
}
