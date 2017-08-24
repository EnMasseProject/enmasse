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

var util = require('util');
var rhea = require('rhea');
var Registry =require('./registry.js');

function Addresses(connection) {
    Registry.call(this);
    var configserv = connection || require('./admin_service.js').connect_service(rhea, 'CONFIGURATION');
    var addresses = this;
    configserv.open_receiver('v1/addresses').on('message', function (context) {
        if (context.message.subject === 'enmasse.io/v1/AddressList') {
            if (context.message.body && context.message.body.length) {
                try {
                    var content = JSON.parse(context.message.body);
                    var defs = content.items ? content.items.map(function (address) { return address.spec; }) : [];
                    addresses.known_addresses(defs.reduce(function (map, a) { map[a.address] = a; return map; }, {}));
                } catch (e) {
                    console.log('ERROR: failed to parse addresses: ' + e + '; ' + context.message.body);
                }
            } else {
                addresses.known_addresses({});
            }
        } else {
            console.log('WARN: unexpected subject: ' + context.message.subject);
        }
    });
}

util.inherits(Addresses, Registry);

Addresses.prototype.known_addresses = function (known) {
    this.set(known);
};

Addresses.prototype.update_stats = function (name, stats) {
    this.update_if_exists(name, stats);
};

module.exports = Addresses;
