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

var path = require('path');
var fs = require('fs');
var rhea = require('rhea');
var log = require("./log.js").logger();

function authenticate(user, options) {
    return new Promise(function(resolve, reject) {
        if( user && user.name ) {
            options.username = user.name;
            options.password = user.pass;
        } else {
            options.username = "anonymous";
        }

        var authServer = rhea.connect(options);
        var handled = false;
        authServer.on("connection_open", function (context) {
            handled = true;
            context.connection.close();
            resolve(context.connection.properties);
        });
        var handleFailure = function (context) {
            if (!handled) {
                handled = true;
                reject();
            }
        };
        authServer.on("connection_error", handleFailure);
        authServer.on("connection_close", handleFailure);
        authServer.on("disconnected", handleFailure);

    });
}

function default_options(env) {
    var options = {
        host: env.AUTHENTICATION_SERVICE_HOST,
        port: env.AUTHENTICATION_SERVICE_PORT,
        sasl_init_hostname: env.AUTHENTICATION_SERVICE_SASL_INIT_HOST
    };
    var ca_path = env.AUTH_SERVICE_CA || path.resolve('/opt/agent/authservice-ca', 'tls.crt');
    try {
        options.ca = [fs.readFileSync(ca_path)];
        options.transport = 'tls';
        options.rejectUnauthorized = false;
    } catch (error) {
        log.warn('CA cannot be loaded from ' + ca_path + ': ' + error);
    }
    return options;
}

module.exports.authenticate = authenticate;
module.exports.default_options = default_options;

