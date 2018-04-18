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
var log = require('./log.js').logger();
var myutils = require('./utils.js');

var Artemis = function (connection) {
    this.connection = connection;
    this.sender = connection.open_sender('activemq.management');
    this.sender.on('sendable', this._send_pending_requests.bind(this));
    connection.open_receiver({source:{dynamic:true}});
    connection.on('receiver_open', this.ready.bind(this));
    connection.on('message', this.incoming.bind(this));
    connection.on('receiver_error', this.on_receiver_error.bind(this));
    connection.on('sender_error', this.on_sender_error.bind(this));
    var self = this;
    connection.on('connection_open', function (context) {
        let previous = self.id;
        self.id = context.connection.container_id;
        if (previous !== undefined && previous !== self.id) {
            log.info('[%s] connection opened (was %s)', self.id, previous);
        } else {
            log.info('[%s] connection opened', self.id);
        }
    });
    connection.on('connection_error', this.on_connection_error.bind(this));
    connection.on('connection_close', this.on_connection_close.bind(this));
    connection.on('disconnected', this.disconnected.bind(this));
    this.handlers = [];
    this.requests = [];
    this.pushed = 0;
    this.popped = 0;
};

Artemis.prototype.log_info = function (context) {
    if (this.id) {
        log.info('[%s] artemis requests pending: %d, made:%d, completed: %d, ready: %d', this.id, this.handlers.length, this.pushed, this.popped, this.address !== undefined);
    }
};

Artemis.prototype.ready = function (context) {
    log.info('[%s] ready to send requests', this.connection.container_id);
    this.address = context.receiver.remote.attach.source.address;
    this._send_pending_requests();
};

function as_handler(resolve, reject) {
    return function (context) {
        var message = context.message || context;
        if (message.application_properties && message.application_properties._AMQ_OperationSucceeded) {
            try {
                if (message.body) resolve(JSON.parse(message.body)[0]);
                else resolve(true);
            } catch (e) {
                log.info('[%s] Error parsing message body: %s: %s' + this.connection.container_id, message, e);
            }
        } else {
            reject(message.body);
        }
    };
}

Artemis.prototype.incoming = function (context) {
    var message = context.message;
    log.debug('[%s] recv: ', this.id, message);
    var handler = this.handlers.shift();
    if (handler) {
        this.popped++;
        handler(context);
    }
};

Artemis.prototype.disconnected = function (context) {
    log.info('[%s] disconnected', this.id || context.connection.container_id);
    this.address = undefined;
    this.abort_requests('disconnected');
};

Artemis.prototype.abort_requests = function (error) {
    while (this.handlers.length > 0) {
        var handler = this.handlers.shift();
        if (handler) {
            this.popped++;
            handler(error);
        }
    }
}

Artemis.prototype.on_sender_error = function (context) {
    var error = this.connection.container_id + ' sender error ' + JSON.stringify(context.sender.error);
    log.info('[' + this.connection.container_id + '] ' + error);
    this.abort_requests(error);
};

Artemis.prototype.on_receiver_error = function (context) {
    var error = this.connection.container_id + ' receiver error ' + JSON.stringify(context.receiver.error);
    log.info('[' + this.connection.container_id + '] ' + error);
    this.abort_requests(error);
};

Artemis.prototype.on_connection_error = function (context) {
    var error = this.connection.container_id + ' connection error ' + JSON.stringify(context.connection.error);
    log.info('[' + this.connection.container_id + '] connection error: ' + JSON.stringify(context.connection.error));
    this.abort_requests(error);
};

Artemis.prototype.on_connection_close = function (context) {
    var error = this.connection.container_id + ' connection closed';
    log.info('[' + this.connection.container_id + '] connection closed');
    this.abort_requests(error);
};

Artemis.prototype._send_pending_requests = function () {
    if (this.address === undefined) return false;

    var i = 0;
    while (i < this.requests.length && this.sender.sendable()) {
        this._send_request(this.requests[i++]);
    }
    this.requests.splice(0, i);
    return this.requests.length === 0 && this.sender.sendable();
}

Artemis.prototype._send_request = function (request) {
    request.application_properties.JMSReplyTo = this.address;
    request.reply_to = this.address;
    this.sender.send(request);
    log.debug('[%s] sent: %j', this.id, request);
}

Artemis.prototype._request = function (resource, operation, parameters) {
    var request = {application_properties:{'_AMQ_ResourceName':resource, '_AMQ_OperationName':operation}};
    request.body = JSON.stringify(parameters);

    if (this._send_pending_requests()) {
        this._send_request(request);
    } else {
        this.requests.push(request);
    }
    var stack = this.handlers;
    var self = this;
    return new Promise(function (resolve, reject) {
        self.pushed++;
        stack.push(as_handler(resolve, reject));
    });
}

Artemis.prototype.createQueue = function (name) {
    return this._request('broker', 'createQueue', [name/*address*/, 'ANYCAST', name/*queue name*/, null/*filter*/, true/*durable*/,
                                                   -1/*max consumers*/, false/*purgeOnNoConsumers*/, true/*autoCreateAddress*/]);
}

Artemis.prototype.deployQueue = function (name, durable) {
    var is_durable = durable === undefined ? true : durable;
    return this._request('broker', 'deployQueue', [name, name, null, is_durable]);
}

Artemis.prototype.destroyQueue = function (name) {
    return this._request('broker', 'destroyQueue', [name, true, true]);
}

Artemis.prototype.getQueueNames = function () {
    return this._request('broker', 'getQueueNames', []);
}

var queue_attributes = {
    temporary: 'isTemporary',
    durable: 'isDurable',
    messages: 'getMessageCount',
    consumers: 'getConsumerCount',
    enqueued: 'getMessagesAdded',
    delivering: 'getDeliveringCount',
    acknowledged: 'getMessagesAcknowledged',
    expired: 'getMessagesExpired',
    killed: 'getMessagesKilled'
};

function add_queue_method(name) {
    Artemis.prototype[name] = function (queue) {
        return this._request('queue.'+queue, name, []);
    };
}

for (var key in queue_attributes) {
    add_queue_method(queue_attributes[key]);
}

var extra_queue_attributes = {
    address: 'getAddress',
    routing_type: 'getRoutingType'
}

for (var key in extra_queue_attributes) {
    add_queue_method(extra_queue_attributes[key]);
}

var queue_attribute_aliases = {
    'isTemporary': 'temporary',
    'isDurable': 'durable',
    'messageCount': 'messages',
    'consumerCount': 'consumers',
    'messagesAdded': 'enqueued',
    'deliveringCount': 'delivering',
    'messagesAcked': 'acknowledged',
    'messagesExpired': 'expired',
    'messagesKilled': 'killed'
};

function correct_type(o) {
    var i = Number(o);
    if (!Number.isNaN(i)) return i;
    else if (o === 'true') return true;
    else if (o === 'false') return false;
    else if (o === null) return undefined;
    else return o;
}

function set_queue_aliases(q) {
    for (var f in queue_attribute_aliases) {
        var a = queue_attribute_aliases[f];
        if (q[f] !== undefined) {
            q[a] = q[f];
            delete q[f];
        }
    }
}

function fix_types(o) {
    for (var k in o) {
        o[k] = correct_type(o[k]);
    }
}

function process_queue_stats(q) {
    fix_types(q);
    set_queue_aliases(q);
}

Artemis.prototype.listQueues = function () {
    return this._request('broker', 'listQueues', ['{"field":"","operation":"","value":"","sortOrder":"","sortBy":"","sortColumn":""}', 1, 2147483647/*MAX_INT*/]).then(function (result) {
        var queues = JSON.parse(result).data;
        queues.forEach(process_queue_stats);
        return queues;
    });
};

function routing_type_to_type(t) {
    if (t === 'MULTICAST') return 'topic';
    if (t === 'ANYCAST') return 'queue';
    return t;
}

function routing_types_to_type(t) {
    if (t.indexOf('MULTICAST') >= 0) {
        return 'topic';
    } else if (t.indexOf('ANYCAST') >= 0) {
        return 'queue';
    } else {
        return undefined;
    }
}

function address_to_queue_or_topic(a) {
    return {
        name: a.name,
        type: routing_types_to_type(a.routingTypes)
    };
}

Artemis.prototype.getAddresses = function () {
    return this._request('broker', 'listAddresses', ['{"field":"","operation":"","value":"","sortOrder":"","sortBy":"","sortColumn":""}', 1, 2147483647/*MAX_INT*/]).then(function (result) {
        return JSON.parse(result).data.map(address_to_queue_or_topic);
    });
};

Artemis.prototype.getQueueDetails = function (name, attribute_list) {
    var attributes = attribute_list || Object.keys(queue_attributes);
    var agent = this;
    return Promise.all(
        attributes.map(function (attribute) {
            var method_name = queue_attributes[attribute];
            if (method_name) {
                return agent[method_name](name);
            }
        })
    ).then(function (results) {
        var q = {'name':name};
        for (var i = 0; i < results.length; i++) {
            q[attributes[i]] = results[i];
        }
        return q;
    });
}

function initialise_topic_stats(a) {
    a.enqueued = 0;
    a.messages = 0;
    a.subscriptions = [];
    a.subscription_count = 0;
    a.durable_subscription_count = 0;
    a.inactive_durable_subscription_count = 0;
}

function update_topic_stats(a, q) {
    a.enqueued += q.enqueued;
    a.messages += q.messages;
    a.subscriptions.push(q);
    a.subscription_count++;
    if (q.durable) {
        a.durable_subscription_count++;
        if (q.consumers === 0) {
            a.inactive_durable_subscription_count++;
        }
    }
}

function queues_to_addresses(addresses, queues, include_topic_stats, include_reverse_index) {
    var index = {};
    var reverse_index = include_reverse_index ? {} : undefined;
    for (var i = 0; i < addresses.length; i++) {
        var a = addresses[i];
        var b = index[a.name];
        if (b === undefined) {
            index[a.name] = a;
            if (include_topic_stats && a.type === 'topic') {
                initialise_topic_stats(a);
            }
        } else {
            log.warn('Duplicate address: %s (%s)', a.name, a.type);
        }
    }
    for (var i = 0; i < queues.length; i++) {
        var q = queues[i];
        if (reverse_index) {
            reverse_index[q.name] = q.address;
        }
        var a = index[q.address];
        if (q.routingType === 'MULTICAST') {
            if (a === undefined) {
                log.warn('Missing address %s for topic queue %s', q.address, q.name);
                a = {
                    name: q.address,
                    type: 'topic'
                };
                index[q.address] = a;
                if (include_topic_stats) {
                    initialise_topic_stats(a);
                }
            } else if (a.type !== 'topic') {
                log.warn('Unexpected address type: queue %s has type %s, address %s has type %s', q.name, q.routingType, q.address, a.type);
            }
            if (include_topic_stats) {
                update_topic_stats(a, q);
            }
        } else if (q.routingType === 'ANYCAST') {
            if (a === undefined) {
                a = {
                    name: q.address,
                    type: 'queue'
                };
                index[q.address] = a;
                log.warn('Missing address %s for queue %s', q.address, q.name);
            } else if (q.name !== q.address) {
                log.warn('Mismatched address %s for queue %s', q.address, q.name);
            } else if (a.routingType !== undefined) {
                log.warn('Duplicate queue for address %s: %j %j', q.address, a, q);
            }
            myutils.merge(a, q);
        } else {
            log.error('Unknown routingType: %s', q.routingType);
        }
    }
    return {
        addresses: addresses,
        queues: queues,
        index: index,
        reverse_index: reverse_index
    };
}

Artemis.prototype._get_address_data = function (include_topic_stats, include_reverse_index) {
    return Promise.all([this.getAddresses(), this.listQueues()]).then(function (results) {
        return queues_to_addresses(results[0], results[1], include_topic_stats, include_reverse_index);
    });
};

Artemis.prototype.listAddresses = function () {
    return this._get_address_data().then(function (data) {
        return data.index;
    });
};

Artemis.prototype.getAllAddressData = function () {
    return this._get_address_data(true, true).then(function (data) {
        return data;
    });
};

Artemis.prototype.getAllQueuesAndTopics = function () {
    return this._get_address_data(true, false).then(function (data) {
        return data.index;
    });
};

Artemis.prototype.getAddressNames = function () {
    return this._request('broker', 'getAddressNames', []);
}

Artemis.prototype.createAddress = function (name, type) {
    var routing_types = [];
    if (type.anycast || type.queue) routing_types.push('ANYCAST');
    if (type.multicast || type.topic) routing_types.push('MULTICAST');
    return this._request('broker', 'createAddress', [name, routing_types.join(',')]);
};

Artemis.prototype.deleteAddress = function (name) {
    return this._request('broker', 'deleteAddress', [name]);
};

var address_settings_fields = {
    'DLA':'',
    'expiryAddress':'',
    'expiryDelay':-1,
    'lastValueQueue':false,
    'deliveryAttempts':-1,
    'maxSizeBytes':-1,
    'pageSizeBytes':-1,
    'pageMaxCacheSize':-1,
    'redeliveryDelay':-1,
    'redeliveryMultiplier':-1,
    'maxRedeliveryDelay':-1,
    'redistributionDelay':-1,
    'sendToDLAOnNoRoute':false,
    'addressFullMessagePolicy': 'FAIL',
    'slowConsumerThreshold':-1,
    'slowConsumerCheckPeriod':-1,
    'slowConsumerPolicy':'DROP',
    'autoCreateJmsQueues':false,
    'autoDeleteJmsQueues':false,
    'autoCreateJmsTopics':false,
    'autoDeleteJmsTopics':false,
    'autoCreateQueues':false,
    'autoDeleteQueues':false,
    'autoCreateAddresses':false,
    'autoDeleteAddresses':false
};

Artemis.prototype.addAddressSettings = function (match, settings) {
    var args = [match];
    for (var name in address_settings_fields) {
        var v = settings[name] || address_settings_fields[name];
        args.push(v);
    }
    return this._request('broker', 'addAddressSettings', args);
};

Artemis.prototype.removeAddressSettings = function (match) {
    return this._request('broker', 'removeAddressSettings', [match]);
};

Artemis.prototype.deleteAddressAndBindings = function (address) {
    var self = this;
    return this.deleteBindingsFor(address).then(function () {
        return self.deleteAddress(address);
    });
};

Artemis.prototype.deleteBindingsFor = function (address) {
    var self = this;
    return this.getBoundQueues(address).then(function (results) {
        return Promise.all(results.map(function (q) { return self.destroyQueue(q); }));
    });
};

Artemis.prototype.getBoundQueues = function (address) {
    return this._request('address.'+address, 'getQueueNames', []);
};

Artemis.prototype.createDivert = function (name, source, target) {
    return this._request('broker', 'createDivert', [name, name, source, target, false, null, null]);
}

Artemis.prototype.destroyDivert = function (name) {
    return this._request('broker', 'destroyDivert', [name]);
}

Artemis.prototype.getDivertNames = function () {
    return this._request('broker', 'getDivertNames', []);
}

/**
 * Create divert if one does not already exist.
 */
Artemis.prototype.ensureDivert = function (name, source, target) {
    var broker = this;
    return broker.findDivert(name).then(
        function (found) {
            if (!found) {
                return broker.createDivert(name, source, target);
            }
        }
    );
};

Artemis.prototype.findDivert = function (name) {
    return this.getDivertNames().then(
        function (results) {
            return results.indexOf(name) >= 0;
        }
    );
};

Artemis.prototype.createConnectorService = function (name, source, target) {
    var parameters = {
        "host": process.env.MESSAGING_SERVICE_HOST,
        "port": process.env.MESSAGING_SERVICE_PORT_AMQPS_BROKER,
        "containerId": name,
        "clusterId": name,
        "clientAddress": target,
        "sourceAddress": source
    };
    return this._request('broker', 'createConnectorService', [name, "org.apache.activemq.artemis.integration.amqp.AMQPConnectorServiceFactory", parameters]);
}


Artemis.prototype.destroyConnectorService = function (name) {
    return this._request('broker', 'destroyConnectorService', [name]);
}

Artemis.prototype.getConnectorServices = function () {
    return this._request('broker', 'getConnectorServices', []);
}

Artemis.prototype.listConnections = function () {
    return this._request('broker', 'listConnectionsAsJSON', []).then(function (result) {
        return JSON.parse(result);
    });
}

Artemis.prototype.listSessionsForConnection = function (connection_id) {
    return this._request('broker', 'listSessionsAsJSON', [connection_id]).then(function (result) {
        return JSON.parse(result);
    });
}

Artemis.prototype.listConnectionsWithSessions = function () {
    var self = this;
    return this.listConnections().then(function (conns) {
        return Promise.all(conns.map(function (c) { return self.listSessionsForConnection(c.connectionID)})).then(function (sessions) {
            for (var i in conns) {
                conns[i].sessions = sessions[i];
            }
            return conns;
        });
    });
}

Artemis.prototype.listConsumers = function () {
    return this._request('broker', 'listAllConsumersAsJSON', []).then(function (result) {
        return JSON.parse(result);
    });
}

Artemis.prototype.listProducers = function () {
    return this._request('broker', 'listProducersInfoAsJSON', []).then(function (result) {
        return JSON.parse(result);
    });
}

Artemis.prototype.getGlobalMaxSize = function ()
{
    return this._request('broker', 'getGlobalMaxSize', []);
}

/**
 * Create connector service if one does not already exist.
 */
Artemis.prototype.ensureConnectorService = function (name, source, target) {
    var broker = this;
    return broker.findConnectorService(name).then(
        function (found) {
            if (!found) {
                return broker.createConnectorService(name, source, target);
            }
        }
    );
};

Artemis.prototype.findConnectorService = function (name) {
    return this.getConnectorServices().then(
        function (results) {
            return results.indexOf(name) >= 0;
        }
    );
};

Artemis.prototype.close = function () {
    if (this.connection) {
        this.connection.close();
    }
}

var amqp = require('rhea').create_container();
module.exports.Artemis = Artemis;
module.exports.connect = function (options) {
    return new Artemis(amqp.connect(options));
}
