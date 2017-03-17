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
var events = require('events');
var rhea = require('rhea');

function Addresses() {
    events.EventEmitter.call(this);
    this.addresses = {};
}

util.inherits(Addresses, events.EventEmitter);

Addresses.prototype.add_address = function (address) {
    this.addresses[address.address] = address;
    this.emit('updated', address);
};

Addresses.prototype.delete_address = function (address) {
    var a = this.addresses[address]
    delete this.addresses[address];
    this.emit('deleted', a);
};

Addresses.prototype.known_addresses = function (known) {
    for (var address in known) {
        if (this.addresses[address] === undefined) {
            this.add_address(known[address]);
        }
    }
    for (var address in this.addresses) {
        if (known[address] === undefined) {
            this.delete_address(address);
        }
    }
};

function equals(a, b) {
    return JSON.stringify(a) === JSON.stringify(b);
}

Addresses.prototype.update_stats = function (name, stats) {
    var address = this.addresses[name];
    if (address) {
        var changed = false;
        for (var s in stats) {
            if (!equals(address[s], stats[s])) {
                console.log('changing ' + s + ' on ' + name + ' from ' + JSON.stringify(address[s]) + ' to ' + JSON.stringify(stats[s]));
                address[s] = stats[s];
                changed = true;
            }
        }
        if (changed) {
            this.emit('updated', address);
        }
    }
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
