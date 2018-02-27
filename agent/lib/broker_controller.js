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

var log = require("./log.js").logger();
var util = require('util');
var events = require('events');
var rhea = require('rhea');
var artemis = require('./artemis.js');
var myevents = require('./events.js');

function BrokerController(event_sink) {
    events.EventEmitter.call(this);
    this.check_in_progress = false;
    this.post_event = event_sink || function (event) { log.info('event: %j', event); };
    this.serial_sync = require('./utils.js').serialize(this._sync_broker_addresses.bind(this));
    this.addresses_synchronized = false;
    this.busy_count = 0;
    this.retrieve_count = 0;
    this.last_retrieval = undefined;
};

util.inherits(BrokerController, events.EventEmitter);

BrokerController.prototype.start_polling = function (poll_frequency) {
    this.polling = setInterval(this.check_broker_addresses.bind(this), poll_frequency || 5000);//poll broker stats every 5 secs by default
};

BrokerController.prototype.connect = function (options) {
    var container = rhea.create_container();
    container.on('connection_open', this.on_connection_open.bind(this));
    return container.connect(options);
};

BrokerController.prototype.close = function () {
    if (this.polling) {
        clearInterval(this.polling);
        this.polling = undefined;
    }
    return this.broker.close();
};

BrokerController.prototype.on_connection_open = function (context) {
    this.broker = new artemis.Artemis(context.connection);
    this.id = context.connection.container_id;
    log.info('[%s] broker controller ready', this.id);
    this.check_broker_addresses();
    this.emit('ready');
};

BrokerController.prototype.set_connection = function (connection) {
    this.broker = new artemis.Artemis(connection);
    this.id = connection.container_id;
    log.info('[%s] broker controller ready', this.id);
};

BrokerController.prototype.addresses_defined = function (addresses) {
    this.addresses = addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {});
    return this.check_broker_addresses();
};

BrokerController.prototype.sync_addresses = function (addresses) {
    this.addresses = addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {});
    return this.serial_sync();
};

function transform_queue_stats(queue) {
    return {
        receivers: queue.consumers,
        senders: 0,/*add this later*/
        depth: queue.messages,
        messages_in: queue.enqueued,
        messages_out: (queue.acknowledged + queue.expired + queue.killed),
        propagated: 100,
        shards: [queue],
        outcomes: {
            egress: {
                links: [],
                accepted: queue.acknowledged,
                unsettled: queue.delivered,
                rejected: queue.killed
            },
            ingress: {
                links: [],
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
        senders: 0,/*add this later*/
        depth: topic.messages,
        //messages_in: topic.enqueued,//doesn't seem to work as expected
        /*this isn't really correct; what we want is the total number of messages sent to the topic (independent of the number of subscribers)*/
        messages_in: sum('enqueued', topic.subscriptions),
        messages_out: sum('acknowledged', topic.subscriptions),
        propagated: 100,
        shards: [topic],
        outcomes: {
            egress: {
                links: [],
                accepted: sum('acknowledged', topic.subscriptions),
                unsettled: sum('delivered', topic.subscriptions),
                rejected: sum('killed', topic.subscriptions)
            },
            ingress: {
                links: [],
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
        user: raw.sessions.length ? raw.sessions[0].principal : '',
        senders: [],
        receivers: []
    };
}

function transform_producer_stats(raw) {
    return {
        name: undefined,
        connection_id: raw.connectionID,
        address: raw.destination,
        deliveries: raw.msgSent,
        deliveryCount: raw.msgSent,
        lastUpdated: Date.now()
    };
}

function transform_consumer_stats(raw) {
    return {
        name: raw.consumerID,
        connection_id: raw.connectionID,
        address: raw.queueName,//mapped to address in later step
        deliveries: 0,//TODO: not yet available from broker
        lastUpdated: Date.now()
    };
}

function index_by(list, field) {
    return list.reduce(function (map, item) { map[item[field]] = item; return map; }, {});
}

function collect_by_connection(conns, links, name) {
    links.forEach(function (link) {
        var conn = conns[link.connection_id];
        if (conn) {
            conn[name].push(link);
        }
    });
}

function collect_by_address(addresses, links, name, count) {
    links.forEach(function (link) {
        var address = addresses[link.address];
        if (address) {
            address.outcomes[name].links.push(link);
            if (count) address[count] = address.outcomes[name].links.length;
        }
    });
}

function is_not_internal(conn) {
    return conn.user !== 'agent';//Can't get properties or anything else on which to base decision yet
}

BrokerController.prototype.retrieve_stats = function () {
    var self = this;
    if (this.broker !== undefined) {
        return Promise.all([
            this.broker.getAllQueuesAndTopics(),
            this.broker.listQueues(['address']),//for queue->address mapping
            this.broker.listConnectionsWithSessions(),
            this.broker.listProducers(),
            this.broker.listConsumers()]).then(function (results) {
                var address_stats = {};
                for (var name in results[0]) {
                    address_stats[name] = transform_address_stats(results[0][name]);
                }
                var connection_stats = index_by(results[2].map(transform_connection_stats).filter(is_not_internal), 'id');
                var senders = results[3].map(transform_producer_stats);
                var receivers = results[4].map(transform_consumer_stats);
                receivers.forEach(function (r) {
                    if (results[1][r.address]) {
                        r.address = results[1][r.address].address;
                    }
                });

                collect_by_connection(connection_stats, senders, 'senders');
                collect_by_connection(connection_stats, receivers, 'receivers');

                collect_by_address(address_stats, senders, 'ingress', 'senders');
                collect_by_address(address_stats, receivers, 'egress');

                for(var c in connection_stats) {
                    connection_stats[c].messages_in = connection_stats[c].senders.reduce(function (total, sender) { return total + sender.deliveries; }, 0);
                }

                self.retrieve_count++;
                var now = Date.now();
                if (self.last_retrieval === undefined) {
                    log.info('[%s] broker stats retrieved (%d)', self.id, self.retrieve_count);
                } else {
                    var interval = now - self.last_retrieval;
                    if (interval >= 60000) {
                        log.info('[%s] broker stats retrieved %d times in last %d secs', self.id, self.retrieve_count, Math.round(interval/1000));
                        self.retrieve_count = 0;
                    }
                }
                self.emit('address_stats_retrieved', address_stats);
                self.emit('connection_stats_retrieved', connection_stats);
            }).catch(function (error) {
                log.error('[%s] error retrieving stats: %s', self.id, error);
            });
    } else {
        log.info('Unable to retrieve stats, no broker object');
        return Promise.resolve();
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

function address_and_type(object) {
    return {address: object.address, type: object.type};
}

function values(map) {
    return Object.keys(map).map(function (key) { return map[key]; });
}

function excluded_addresses(address) {
    return address === 'DLQ' || address === 'ExpiryQueue';
}

function is_temp_queue(a) {
    return a.anycast && a.queues && a.queues.length === 1 && a.queues[0].temporary;
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
        if (is_temp_queue(a)) {
            log.debug('ignoring temp queue %s', a.name);
            continue;
        }
        if (a.name) {
            addresses_out[name] = {address:a.name, type: a.multicast ? 'topic' : 'queue'};
        } else {
            log.warn('Skipping address with no name: %j', a);
        }
    }
    return addresses_out;
}

BrokerController.prototype.delete_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) {
        if (a.type === 'queue') {
            log.info('[%s] Deleting queue "%s"...', self.id, a.address);
            return self.broker.destroyQueue(a.address).then(function () {
                log.info('[%s] Deleted queue "%s"', self.id, a.address);
                self.post_event(myevents.address_delete(a));
            }).catch(function (error) {
                log.error('[%s] Failed to delete queue %s: %s', self.id, a.address, error);
                self.broker.deleteAddress(a.address).then(function () {
                    log.info('[%s] Deleted anycast address %s', self.id, a.address);
                }).catch(function (error) {
                    log.error('[%s] Failed to delete queue address %s: %s', self.id, a.address, error);
                    self.post_event(myevents.address_failed_delete(a, error));
                });
            });
        } else {
            log.info('[%s] Deleting topic "%s"...', self.id, a.address);
            return self.broker.deleteAddressAndBindings(a.address).then(function () {
                log.info('[%s] Deleted topic "%s"', self.id, a.address);
                self.post_event(myevents.address_delete(a));
            }).catch(function (error) {
                log.error('[%s] Failed to delete topic %s: %s', self.id, a.address, error);
                self.post_event(myevents.address_failed_delete(a, error));
            });
        }
    }));
};

BrokerController.prototype.create_addresses = function (addresses) {
    var self = this;
    return Promise.all(addresses.map(function (a) {
        if (a.type === 'queue') {
            log.info('[%s] Creating queue "%s"...', self.id, a.address);
            return self.broker.createQueue(a.address).then(function () {
                log.info('[%s] Created queue "%s"', self.id, a.address);
                self.post_event(myevents.address_create(a));
            }).catch(function (error) {
                log.error('[%s] Failed to create queue "%s": %s', self.id, a.address, error);
                self.post_event(myevents.address_failed_create(a, error));
            });
        } else {
            log.info('[%s] Creating topic "%s"', self.id, a.address);
            return self.broker.createAddress(a.address, {multicast:true}).then(function () {
                log.info('[%s] Created topic "%s"', self.id, a.address);
                self.post_event(myevents.address_create(a));
            }).catch(function (error) {
                log.error('[%s] Failed to create topic "%s": %s', self.id, a.address, error);
                self.post_event(myevents.address_failed_create(a, error));
            });
        }
    }));
};

BrokerController.prototype.check_broker_addresses = function () {
    if (this.broker !== undefined && this.addresses !== undefined) {
        if (!this.check_in_progress) {
            this.busy_count = 0;
            this.check_in_progress = true;
            var self = this;
            return this._sync_broker_addresses().then(function () {
                return self.retrieve_stats().then(function () {
                    self.check_in_progress = false;
                }).catch( function (error) {
                    log.error('[%s] error retrieving stats: %s', self.id, error);
                    self.check_in_progress = false;
                });
            }).catch (function (error) {
                log.error('[%s] error syncing addresses: %s', self.id, error);
                self.check_in_progress = false;
            });
        } else {
            if (++(this.busy_count) % 10 === 0) {
                log.info('Unable to check broker, check already in progress (%d)', this.busy_count);
            }
            return Promise.resolve();
        }
    } else {
        log.info('Unable to check broker (broker %s defined, addresses %s defined)', this.broker === undefined ? 'is not' : 'is', this.addresses === undefined ? 'is not' : 'is');
        return Promise.resolve();
    }
};

BrokerController.prototype._sync_addresses = function (addresses) {
    this.addresses = addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {});
    return this._sync_broker_addresses();
};

BrokerController.prototype._set_sync_status = function (stale, missing) {
    var b = (stale === 0 && missing === 0);
    if (this.addresses_synchronized !== b) {
        if (b) log.info('[%s] addresses are synchronized', this.id);
        else log.info('[%s] addresses synchronizing (%d to delete, %d to create)', this.id, stale, missing);
    }
    this.addresses_synchronized = b;
}

BrokerController.prototype._sync_broker_addresses = function () {
    var self = this;
    return this.broker.listAddresses().then(function (results) {
        var actual = translate(results, excluded_addresses)
        var stale = values(difference(actual, self.addresses, same_address)).map(address_and_type);
        var missing = values(difference(self.addresses, actual, same_address)).map(address_and_type);
        log.debug('[%s] checking addresses, desired=%j, actual=%j => delete %j and create %j', self.id, values(self.addresses).map(address_and_type), values(actual), stale, missing);
        self._set_sync_status(stale.length, missing.length);
        return self.delete_addresses(stale).then(
            function () {
                return self.create_addresses(missing);
            });
    }).catch(function (e) {
        console.log('[%s] failed to retrieve addresses: %s', self.id, e);
        log.error('[%s] failed to retrieve addresses: %s', self.id, e);
    });
};

module.exports.create_controller = function (connection, event_sink) {
    var bc = new BrokerController(event_sink);
    bc.set_connection(connection);
    return bc;
};

module.exports.create_agent = function (event_sink, polling_frequency) {
    var bc = new BrokerController(event_sink);
    bc.start_polling(polling_frequency);
    return bc;
};
