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
var AddressList = require('../lib/address_list.js');
var AddressSource = require('../lib/address_source.js');
var ConsoleServer = require('../lib/console_server.js');

var address_list = new AddressList();
var address_source = new AddressSource();
address_source.on('addresses_defined', address_list.addresses_defined.bind(address_list));

var console_server = new ConsoleServer(address_list, address_ctrl.create());
console_server.listen();

if (process.env.ADDRESS_SPACE_TYPE === 'brokered') {
    var BrokerController = require('../lib/broker_controller.js');
    var bc = new BrokerController(console_server.address_list.update_stats.bind(console_server.address_list),
                                  console_server.connections.set.bind(console_server.connections));
    bc.connect({host:process.env.BROKER_SERVICE_HOST, port:process.env.BROKER_SERVICE_PORT});
    address_source.on('addresses_defined', bc.addresses_defined.bind(bc));
} else {
    //assume standard address space for now
    var StandardStats = require('../lib/standard_stats.js');
    var stats = new StandardStats();
    stats.init(console_server);
}
