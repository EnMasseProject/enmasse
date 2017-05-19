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

var log = require('log4js').getLogger("config_service");

module.exports.connect = function(container, id) {
    var config_host = process.env.ADMIN_SERVICE_HOST;
    var config_port = process.env.ADMIN_SERVICE_PORT_CONFIGURATION;
    if (process.env.CONFIGURATION_SERVICE_HOST && process.env.CONFIGURATION_SERVICE_PORT) {
        config_host = process.env.CONFIGURATION_SERVICE_HOST;
        config_port = process.env.CONFIGURATION_SERVICE_PORT;
    }

    if (config_host && config_port) {
        log.info("Connecting to config service " + config_host + ":" + config_port + " with id=" + id);
        var connection_properties = {product:'subserv', container_id:process.env.HOSTNAME};
        container.options.username = 'subserv';
        return container.connect({host:config_host, port:config_port, properties:connection_properties, id:id});
    }
    return null;
}
