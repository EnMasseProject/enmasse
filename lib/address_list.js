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

function Addresses() {
    Registry.call(this);
}

util.inherits(Addresses, Registry);

Addresses.prototype.known_addresses = function (known) {
    this.set(known);
};

Addresses.prototype.update_stats = function (name, stats) {
    this.update_if_exists(name, stats);
};

var addresses = new Addresses();

var configserv = require('./admin_service.js').connect(rhea, 'CONFIGURATION');
configserv.open_receiver('maas').on('message', function (context) {
    try {
        var content = JSON.parse(context.message.body);
        for (var v in content) {
            if (content[v].address === undefined) {
                content[v].address = v;
            }
        }
        addresses.known_addresses(content);
    } catch (e) {
        console.log('ERROR: failed to parse addresses as JSON: ' + e + '; ' + context.message.body);
    }
});

module.exports = addresses;
