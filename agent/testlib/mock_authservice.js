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
'use strict';

var rhea = require('rhea');

function is_valid_user(username, password) {
    return username === password.split().reverse().join('');
}

function MockAuthService(f, groups) {
    this.groups = groups;
    var self = this;
    this.container = rhea.create_container({id:'mock-auth-service'});
    this.container.sasl_server_mechanisms.enable_plain(f || is_valid_user);
    this.container.sasl_server_mechanisms.enable_anonymous();
    this.container.on('connection_open', function (context) {
        if (self.groups) {
            var properties = context.connection.local.open.properties || {};
            properties.groups = self.groups;
            context.connection.local.open.properties = properties;
        }
        context.connection.close();
    });
    this.container.on('disconnected', function (context) {});
}

MockAuthService.prototype.listen = function (options) {
    this.server = this.container.listen(options || {port:0});
    var self = this;
    this.server.on('listening', function () {
        self.port = self.server.address().port;
    });
    return this.server;
};

MockAuthService.prototype.close = function (callback) {
    if (this.server) this.server.close(callback);
};

module.exports = MockAuthService;
