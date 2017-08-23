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
var Promise = require('bluebird');

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
            resolve();
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

function default_options(ca_path) {
    var options = {
        host: process.env.AUTHENTICATION_SERVICE_HOST,
        port: process.env.AUTHENTICATION_SERVICE_PORT,
    };
    try {
        options.ca = [fs.readFileSync(ca_path)];
        options.transport = 'tls';
        options.rejectUnauthorized = false;
    } catch (error) {
        console.warn('CA cannot be loaded from ' + ca_path + ': ' + error);
    }
    return options;
}

module.exports.authenticate = authenticate;
module.exports.default_options = default_options;

