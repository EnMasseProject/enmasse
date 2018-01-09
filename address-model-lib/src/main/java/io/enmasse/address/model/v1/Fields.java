/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model.v1;

/**
 * Constants used for field definitions
 */
interface Fields {
    String API_VERSION = "apiVersion";
    String KIND = "kind";
    String METADATA = "metadata";
    String NAME = "name";
    String NAMESPACE = "namespace";
    String ENDPOINTS = "endpoints";
    String HOST = "host";
    String SERVICE = "service";
    String CERT_PROVIDER = "certProvider";
    String SECRET_NAME = "secretName";

    String SPEC = "spec";
    String STATUS = "status";
    String IS_READY = "isReady";
    String MESSAGES = "messages";
    String ITEMS = "items";
    String TYPE = "type";
    String PLAN = "plan";
    String ADDRESS = "address";
    String ADDRESS_SPACE = "addressSpace";
    String UUID = "uuid";
    String ADDRESS_SPACE_TYPES = "addressSpaceTypes";
    String ADDRESS_TYPES = "addressTypes";
    String AUTHENTICATION_SERVICE = "authenticationService";
    String DESCRIPTION = "description";
    String PLANS = "plans";
    String DETAILS = "details";
    String PORT = "port";
    String UID = "uid";
    String CREATED_BY = "createdBy";
    String DISPLAY_NAME = "displayName";
    String DISPLAY_ORDER = "displayOrder";
    String SHORT_DESCRIPTION = "shortDescription";
    String LONG_DESCRIPTION = "longDescription";
    String RESOURCES = "resources";
    String REQUIRED_RESOURCES = "requiredResources";
    String ADDRESS_PLANS = "addressPlans";
    String ADDRESS_TYPE = "addressType";
    String CLUSTER_NAME = "clusterName";
    String TEMPLATE = "template";
    String PARAMETERS = "parameters";
    String MIN = "min";
    String MAX = "max";
    String VALUE = "value";
    String CREDIT = "credit";
    String ANNOTATIONS = "annotations";
    String ADDRESS_SPACE_TYPE = "addressSpaceType";
    String PHASE = "phase";
}
