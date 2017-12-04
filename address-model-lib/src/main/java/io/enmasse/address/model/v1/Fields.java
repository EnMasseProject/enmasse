/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}
