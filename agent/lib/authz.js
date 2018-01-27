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

const admin_permission = 'manage';
const monitor_permission = 'monitor';
const access_permission = 'view_console';

function AuthorizationPolicy() {};

AuthorizationPolicy.prototype.get_permissions = function (connection) {
    return connection.options.groups;
};

AuthorizationPolicy.prototype.has_permission = function (required, actual) {
    return actual && actual.indexOf(required) >= 0;
};

function is_view_permission(s) {
    return s.indexOf('view_') === 0;
}

AuthorizationPolicy.prototype.access_console = function (properties) {
    return properties.groups
        && (this.has_permission(admin_permission, properties.groups)
            || this.has_permission(monitor_permission, properties.groups)
            || properties.groups.some(is_view_permission));
};

AuthorizationPolicy.prototype.is_admin = function (connection) {
    return this.has_permission(admin_permission, this.get_permissions(connection));
};

AuthorizationPolicy.prototype.set_authz_props = function (request, credentials, properties) {
    if (credentials) {
        request.authz_props = {groups:properties.groups, authid:credentials.name};
    } else {
        request.authz_props = {groups:properties.groups};
    }
};

AuthorizationPolicy.prototype.get_authz_props = function (request) {
    try {
        return request.authz_props;
    } catch (error) {
        log.error('error retrieving authz properties for user: %s', error);
        return undefined;
    }
};

AuthorizationPolicy.prototype.address_filter = function (connection) {
    var permissions = this.get_permissions(connection);
    if (this.has_permission(admin_permission, permissions) || this.has_permission(monitor_permission, permissions)) {
        return undefined;
    } else {
        var self = this;
        return function (a) {
            //TODO: handle wildcards
            return self.has_permission('view_' + encodeURIComponent(a.address), permissions);
        };
    }
};

AuthorizationPolicy.prototype.connection_filter = function (connection) {
    var permissions = this.get_permissions(connection);
    if (this.has_permission(admin_permission, permissions) || this.has_permission(monitor_permission, permissions)) {
        return undefined;
    } else {
        return function (c) {
            return connection.options.authid !== undefined && connection.options.authid === c.user;
        };
    }
};

AuthorizationPolicy.prototype.can_publish = function (sender, message) {
    var permissions = this.get_permissions(sender.connection);
    if (this.has_permission(admin_permission, permissions) || this.has_permission(monitor_permission, permissions)) {
        return true;
    } else if (message.subject === 'address' || message.subject === 'address_deleted') {
        //TODO: handle wildcards
        return this.has_permission('view_' + encodeURIComponent(message.body.address), permissions);
    } else if (message.subject === 'connection' || message.subject === 'connection_deleted') {
        return sender.connection.options.authid === message.body.user;
    }
};

function NullPolicy() {};

NullPolicy.prototype.has_permission = function (required, actual) {
    return true;
};

NullPolicy.prototype.access_console = function () {
    return true;
};

NullPolicy.prototype.is_admin = function () {
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
    if (env.DISABLE_AUTHORIZATION) {
        return new NullPolicy();
    } else {
        return new AuthorizationPolicy();
    }
}
