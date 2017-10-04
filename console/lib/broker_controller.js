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

function BrokerController(update_address_stats, update_connection_stats) {
    this.update_address_stats= update_address_stats;
    this.update_connection_stats= update_connection_stats;
    this.container = rhea.create_container();
    this.container.on('connection_open', this.on_connection_open.bind(this));
    setInterval(this.retrieve_stats.bind(this), 5000);//poll broker stats every 5 secs
    var self = this;
    this.closed = function () { self.broker = undefined; };
};

BrokerController.prototype.connect = function (options) {
    this.container.connect(options);
};

BrokerController.prototype.on_connection_open = function (context) {
    this.broker = new artemis.Artemis(context.connection);
    console.log('connected to %s', context.connection.container_id);
    this.retrieve_stats();
    this.check_broker_addresses();
    context.connection.on('connection_close', this.closed);
    context.connection.on('disconnected', this.closed);
};

BrokerController.prototype.addresses_defined = function (addresses) {
    this.addresses = addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {});
    this.check_broker_addresses();
};

function transform_queue_stats(queue) {
    return {
        receivers: queue.consumers,
        senders: undefined,/*can't get this info yet?*/
        depth: queue.messages,
        messages_in: queue.enqueued,
        messages_out: (queue.acknowledged + queue.expired + queue.killed),
        propagated: 100,
        outcomes: {
            egress: {
                //links: listConsumersAsJSON()
                accepted: queue.acknowledged,
                unsettled: queue.delivered,
                rejected: queue.killed
            },
            ingress: {
                accepted: queue.enqueued
            }
        }
    };
}

function sum(field, list) {
    return util.isArray(list) ? list.map(function (o) { return o[field] || 0; }).reduce(function (a, b) { return a + b; }, 0) : 0;
}

function transform_topic_stats(topic) {
    return {
        receivers: topic.subscription_count,
        senders: undefined,/*can't get this info yet?*/
        depth: topic.messages,
        //messages_in: topic.enqueued,//doesn't seem to work as expected
        /*this isn't really correct; what we want is the total number of messages sent to the topic (independent of the number of subscribers)*/
        messages_in: sum('enqueued', topic.subscriptions),
        messages_out: util.isArray(topic.subscriptions) ? topic.subscriptions.map(function (queue) { return queue.acknowledged + queue.expired + queue.killed; }).reduce(function (a, b) { return a + b; }, 0) : 0,
        propagated: 100,
        outcomes: {
            egress: {
                //links: listConsumersAsJSON() for each topic
                accepted: sum('acknowledged', topic.subscriptions),
                unsettled: sum('delivered', topic.subscriptions),
                rejected: sum('killed', topic.subscriptions)
            },
            ingress: {
                accepted: topic.enqueued
            }
        }
    };
}

function transform_address_stats(address) {
    return address.is_queue ? transform_queue_stats(address) : transform_topic_stats(address);
}

function transform_connection_stats(raw) {
    return {
        id: raw.connectionID,
        host: raw.clientAddress,
        container: 'not available',
        senders: [],
        receivers: []
    };
}

function transform_producer_stats(raw) {
    return {
        connection_id: raw.connectionID,
        address: raw.destination,
        deliveries: raw.msgSent
    };
}

function transform_consumer_stats(raw) {
    return {
        name: raw.queueName,
        connection_id: raw.connectionID,
        address: raw.queueName,//TODO: need to map this back to topic address
        deliveries: 0//TODO: need to retrive this
    };
}

function index_by(list, field) {
    return list.reduce(function (map, item) { map[item[field]] = item; return map; }, {});
}

function collect_as(conns, links, name) {
    links.forEach(function (link) {
        var conn = conns[link.connection_id];
        if (conn) {
            conn[name].push(link);
        }
    });
}


BrokerController.prototype.retrieve_stats = function () {
    var self = this;
    if (this.broker !== undefined) {
        this.broker.getAllQueuesAndTopics().then(function (results) {
            for (var name in results) {
                var stats = transform_address_stats(results[name]);
                self.update_address_stats(name, stats);
            }
        });
        Promise.all([this.broker.listConnections(), this.broker.listProducers(), this.broker.listConsumers()]).then(function (results) {
            var conn_stats = index_by(results[0].map(transform_connection_stats), 'id');
            collect_as(conn_stats, results[1].map(transform_producer_stats), 'senders');
            collect_as(conn_stats, results[2].map(transform_consumer_stats), 'receivers');
            self.update_connection_stats(conn_stats);
        });
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

function excluded_addresses(address) {
    return address === 'DLQ' || address === 'ExpiryQueue';
}

/**
 * Translate from the address details we get back from artemis to the
 * structure used for the definition, for easier comparison.
 */
function translate(addresses_in, exclude) {
    var addresses_out = {};
    for (var name in addresses_in) {
        if (exclude && exclude(name)) continue;
        var a = addresses_in[name];
        addresses_out[name] = {address:a.name, type: a.multicast ? 'topic' : 'queue'};
    }
    return addresses_out;
}

BrokerController.prototype.delete_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) {
        if (a.type === 'queue') {
            return self.broker.destroyQueue(a.address);
        } else {
            return self.broker.deleteAddress(a.address);
        }
    }));
};

BrokerController.prototype.create_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) {
        if (a.type === 'queue') {
            return self.broker.createQueue(a.address);
        } else {
            return self.broker.createAddress(a.address, {multicast:true});
        }
    }));
};

BrokerController.prototype.check_broker_addresses = function () {
    var self = this;
    if (this.broker !== undefined && this.addresses !== undefined) {
        this.broker.listAddresses().then(function (results) {
            var actual = translate(results, excluded_addresses);
            var stale = values(difference(actual, self.addresses, same_address));
            var missing = values(difference(self.addresses, actual, same_address));
            console.log('checking addresses, desired=%j, actual=%j => delete %j and create %j', values(self.addresses), values(actual), stale, missing);
            self.delete_addresses(stale).then(
                function () {
                    return self.create_addresses(missing);
                }
            );
        });
    }
};

module.exports = BrokerController;
