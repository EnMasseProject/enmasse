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

var myutils = require('./utils.js');

function AuthorizationPolicy() {};

AuthorizationPolicy.prototype.has_permission = function (required, actual) {
    return actual && actual.indexOf(required) >= 0;
};

AuthorizationPolicy.prototype.set_authz_props = function (request, credentials, properties) {
    if (credentials) {
        request.authz_props = {groups:properties.groups, authid:credentials.name};
    }
};

AuthorizationPolicy.prototype.get_authz_props = function (request) {
    try {
        var credentials = myutils.basic_auth(request);
        if (credentials) {
            return request.authz_props;
        } else {
            return undefined;
        }
    } catch (error) {
        log.error('error retrieving authz properties for user: %s', error);
        return undefined;
    }
};

AuthorizationPolicy.prototype.address_filter = function (connection) {
    var groups = connection.options.groups;
    if (this.has_permission('address-space-admin', groups) || this.has_permission('address-space-monitor', groups)) {
        return undefined;
    } else {
        var self = this;
        return function (a) {
            //TODO: handle wildcards
            return self.has_permission('view_' + encodeURIComponent(a.address), groups);
        };
    }
};

AuthorizationPolicy.prototype.connection_filter = function (connection) {
    var groups = connection.options.groups;
    if (this.has_permission('address-space-admin', groups) || this.has_permission('address-space-monitor', groups)) {
        return undefined;
    } else {
        return function (c) {
            return connection.options.authid !== undefined && connection.options.authid === c.user;
        };
    }
};

AuthorizationPolicy.prototype.can_publish = function (sender, message) {
    var groups = sender.connection.options.groups;
    if (this.has_permission('address-space-admin', groups) || this.has_permission('address-space-monitor', groups)) {
        return true;
    } else if (message.subject === 'address' || message.subject === 'address_deleted') {
        //TODO: handle wildcards
        return this.has_permission('view_' + encodeURIComponent(message.body.address), groups);
    } else if (message.subject === 'connection' || message.subject === 'connection_deleted') {
        return sender.connection.options.authid === message.body.user;
    }
};

function NullPolicy() {};

NullPolicy.prototype.has_permission = function (required, actual) {
    return true;
};

NullPolicy.prototype.set_authz_props = function (request, credentials, properties) {};

NullPolicy.prototype.get_authz_props = function (request) {};

NullPolicy.prototype.address_filter = function (connection) {
    return undefined;
};

NullPolicy.prototype.connection_filter = function (connection) {
    return undefined;
};

NullPolicy.prototype.can_publish = function (sender, message) {
    return true;
};

module.exports.policy = function (env) {
    if (env.KEYCLOAK_GROUP_PERMISSIONS) {
        return new AuthorizationPolicy();
    } else {
        return new NullPolicy();
    }
}
