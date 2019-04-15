/*
 * Copyright 2016 Red Hat Inc.
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
'use strict';

function TrustAllPolicy() {};

// We no longer use derive permissions from keycloak groups.

TrustAllPolicy.prototype.has_permission = function (required, actual) {
    return true;
};

TrustAllPolicy.prototype.access_console = function (request) {
    return request.authz_props && request.authz_props.console;
};

TrustAllPolicy.prototype.is_admin = function (connection) {
    return connection.options && connection.options.admin;
};

TrustAllPolicy.prototype.set_authz_props = function (request, credentials, properties) {
    request.authz_props = {
        admin: properties.admin,
        console: properties.console,
        token: credentials.token,
        username: credentials.username,
    };
};

TrustAllPolicy.prototype.get_authz_props = function (request) {
    return request.authz_props;
};

TrustAllPolicy.prototype.address_filter = function (connection) {
    return undefined;
};

TrustAllPolicy.prototype.connection_filter = function (connection) {
    return undefined;
};

TrustAllPolicy.prototype.can_publish = function (sender, message) {
    return true;
};

TrustAllPolicy.prototype.get_access_token = function (connection) {
    return connection.options && connection.options.token ? connection.options.token.getAccessToken() : null;
};

TrustAllPolicy.prototype.get_user = function (connection) {
    return connection.options && connection.options.username ? connection.options.username : null;
};


module.exports.policy = function (env) {
    return new TrustAllPolicy();
};
