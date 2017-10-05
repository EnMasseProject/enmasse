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

var address_ctrl = require('../lib/address_ctrl.js');
var AddressSource = require('../lib/address_source.js');
var ConsoleServer = require('../lib/console_server.js');
var tls_options = require('../lib/tls_options.js');

function bind_event(source, event, target, method) {
    source.on(event, target[method || event].bind(target));
}

function start(env) {
    var address_source = new AddressSource();
    var console_server = new ConsoleServer(address_ctrl.create(env));
    bind_event(address_source, 'addresses_defined', console_server.addresses);

    console_server.listen(env);

    if (env.ADDRESS_SPACE_TYPE === 'brokered') {
        var BrokerController = require('../lib/broker_controller.js');
        var bc = new BrokerController();
        bind_event(bc, 'address_stats_retrieved', console_server.addresses, 'update_existing');
        bind_event(bc, 'connection_stats_retrieved', console_server.connections, 'set');
        bind_event(address_source, 'addresses_defined', bc);
        bc.connect(tls_options.get_client_options({host:env.BROKER_SERVICE_HOST, port:env.BROKER_SERVICE_PORT,username:'console'}));
    } else {
        //assume standard address space for now
        var StandardStats = require('../lib/standard_stats.js');
        var stats = new StandardStats();
        stats.init(console_server);
    }
}

if (require.main === module) {
    start(process.env);
} else {
    module.exports.start = start;
}
