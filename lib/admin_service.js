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

function hostport(service_name, defaults) {
    var result = defaults || {host: 'localhost'};
    if (process.env.ADMIN_SERVICE_HOST) result.host = process.env.ADMIN_SERVICE_HOST;
    if (process.env['ADMIN_SERVICE_PORT_' + service_name]) result.port = process.env['ADMIN_SERVICE_PORT_' + service_name];
    if (process.env[service_name + '_SERVICE_HOST']) {
        result.host = process.env[service_name + '_SERVICE_HOST'];
        if (process.env[service_name + '_SERVICE_PORT']) {
            result.port = process.env[service_name + '_SERVICE_PORT'];
        }
    }
    return result;
};

var counter = 1;

function connect(container, service_name, defaults) {
    var options = hostport(service_name, defaults);
    if (options.host) {
        var self = path.basename(process.argv[1], '.js');
        if (options.properties === undefined) options.properties = {};
        if (options.properties.product === undefined) options.properties.product = self;
        if (options.container_id === undefined) options.container_id = process.env.HOSTNAME;
        if (options.username === undefined) options.username = self;
        if (options.id === undefined) options.id = self + '-' + counter++;
        console.log('Connecting to ' + service_name.toLowerCase() + ' service with ' + JSON.stringify(options));
        return container.connect(options);
    } else {
        return undefined;
    }
};

module.exports.hostport = hostport;
module.exports.connect = connect;

