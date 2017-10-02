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
var Promise = require('bluebird');
var artemis = require('./artemis.js');

function BrokerController(console_server) {
    this.console_server = console_server;
    this.container = rhea.create_container();
    this.container.on('connection_open', this.on_connection_open.bind(this));
    setInterval(this.retrieve_stats.bind(this), 5000);//poll broker stats every 5 secs
    var self = this;
    this.closed = function () { self.broker = undefined; };
};

BrokerController.prototype.listen = function (port) {
    //listen for broker connection
    this.server = this.container.listen(port || 55672);
    return this.server;
};

BrokerController.prototype.connect = function (options) {
    this.container.connect(options);
};

BrokerController.prototype.on_connection_open = function (context) {
    //assume its a broker? or check in some way?
    this.broker = new artemis.Artemis(context.connection);
    this.check_broker_addresses();
    context.connection.on('connection_close', this.closed);
    context.connection.on('disconnected', this.closed);
};

BrokerController.prototype.addresses_defined = function (addresses) {
    this.addresses = addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {});
    this.check_broker_addresses();
};

BrokerController.prototype.retrieve_stats = function () {
    var self = this;
    if (this.broker !== undefined) {
        this.broker.getAllQueuesAndTopics().then(function (results) {
            for (var name in results) {
                self.console_server.address_list.update_stats(name, results[name]);
            }
        });
        //retrieve connection stats
    }
};

function same_address(a, b) {
    return b !== undefined && a.address === b.address && a.type === b.type;
}

function difference(a, b, equivalent) {
    var diff = {};
    for (var k in a) {
	if (!equivalent(a[k], b[k])) {
	    diff[k] = a[k];
	}
    }
    return diff;
}

function values(map) {
    return Object.keys(map).map(function (key) { return map[key]; });
}

/**
 * Translate from the address details we get back from artemis to the
 * structure used for the definition, for easier comparison.
 */
function translate(addresses_in) {
    var addresses_out = {};
    for (var name in addresses_in) {
        var a = addresses_in[name];
        addresses_out[name] = {address:a.name, type: a.multicast ? 'topic' : 'queue'};
    }
    return addresses_out;
}

BrokerController.prototype.delete_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) { return self.broker.deleteAddress(a.address); } ));
};

BrokerController.prototype.create_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) { return self.broker.createAddress(a.address, a.type); } ));
};

BrokerController.prototype.check_broker_addresses = function () {
    var self = this;
    if (this.broker !== undefined && this.addresses !== undefined) {
        this.broker.listAddresses().then(function (results) {
            var actual = translate(results);
            var stale = values(difference(actual, self.addresses, same_address));
            var missing = values(difference(self.addresses, actual, same_address));
            self.delete_addresses(stale).then(
                function () {
                    return self.create_addresses(missing);
                }
            );
        });
    }
};

module.exports = BrokerController;
