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

var AddressSource = require('../lib/internal_address_source.js');
var ConsoleServer = require('../lib/console_server.js');
var kubernetes = require('../lib/kubernetes.js');
var Ragent = require('../lib/ragent.js');
var tls_options = require('../lib/tls_options.js');

function bind_event(source, event, target, method) {
    source.on(event, target[method || event].bind(target));
}

function start(env) {
    kubernetes.get_messaging_route_hostname(env).then(function (result) {
        if (result !== undefined) env.MESSAGING_ROUTE_HOSTNAME = result;
        var address_source = new AddressSource(env.ADDRESS_SPACE);
        var console_server = new ConsoleServer(address_source);
        bind_event(address_source, 'addresses_defined', console_server.addresses);

        console_server.listen(env);

        if (env.ADDRESS_SPACE_TYPE === 'brokered') {
            var bc = require('../lib/broker_controller.js').create_agent(kubernetes.post_event);
            bind_event(bc, 'address_stats_retrieved', console_server.addresses, 'update_existing');
            bind_event(bc, 'connection_stats_retrieved', console_server.connections, 'set');
            bind_event(address_source, 'addresses_defined', bc);
            bind_event(bc, 'address_stats_retrieved', address_source, 'check_status');
            bc.connect(tls_options.get_client_options({host:env.BROKER_SERVICE_HOST, port:env.BROKER_SERVICE_PORT,username:'console'}));
        } else {
            //assume standard address space for now
            var StandardStats = require('../lib/standard_stats.js');
            var stats = new StandardStats();
            stats.init(console_server);

            var ragent = new Ragent();
            bind_event(address_source, 'addresses_ready', ragent, 'sync_addresses')
            ragent.start_listening(env);
        }
    });
}

if (require.main === module) {
    start(process.env);
} else {
    module.exports.start = start;
}
