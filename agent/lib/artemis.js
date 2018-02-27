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

Artemis.prototype.listQueues = function (attribute_list) {
    var attributes = attribute_list || Object.keys(queue_attributes);
    var agent = this;
    return new Promise(function (resolve, reject) {
        agent.getQueueNames().then(function (results) {
            if (results && !util.isArray(results)) {
                log.info('[%s] unexpected result for queue names: %j', agent.id, results);
            }
            var allnames = util.isArray(results) ? results : [];
            var names = allnames.filter(function (n) { return n !== agent.address; } );
            Promise.all(
                names.map(function (name) {
                    return Promise.all(
                        attributes.map(function (attribute) {
                            var method_name = queue_attributes[attribute] || extra_queue_attributes[attribute];
                            if (method_name) {
                                return agent[method_name](name);
                            } else {
                                return Promise.reject('Invalid attribute for queue: ' + attribute);
                            }
                        })
                    );
                })
            ).then(function (results) {
                var queues = {};
                for (var i = 0; i < results.length; i++) {
                    var q = {name: names[i]};
                    if (results[i]) {
                        for (var j = 0; j < results[i].length; j++) {
                            q[attributes[j]] = results[i][j];
                        }
                    }
                    queues[q.name] = q;
                }
                resolve(queues);
            }).catch(function (e) {
                reject(e);
            });
        });
    });
}

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

Artemis.prototype.getAddressNames = function () {
    return this._request('broker', 'getAddressNames', []);
}

var address_attributes = {delivery_modes: 'getRoutingTypesAsJSON',
                          messages: 'getNumberOfMessages',
                          queues: 'getQueueNames',
                          enqueued: 'getMessageCount'};

Artemis.prototype.listAddresses = function () {
    var attributes = Object.keys(address_attributes);
    var agent = this;
    return agent.getAddressNames().then(function (allnames) {
        var names = allnames.filter(function (n) { return n && n !== agent.address; } );
        return Promise.all(
            names.map(function (name) {
                return Promise.all(
                    attributes.map(function (attribute) {
                        return agent._request('address.'+name, address_attributes[attribute], []);
                    })
                ).then(function (results) {
                    var a = {'name': name};
                    for (var i = 0; i < results.length; i++) {
                        if (attributes[i] === 'delivery_modes') {
                            if (results[i]) {
                                if (results[i].indexOf('MULTICAST') >= 0) {
                                    a.multicast = true;
                                }
                                if (results[i].indexOf('ANYCAST') >= 0) {
                                    a.anycast = true;
                                }
                            } else {
                                log.info('unexpected result for delivery_modes: %j', results[i]);
                            }
                        } else {
                            a[attributes[i]] = results[i];
                        }
                    }
                    return a;
                });
            })
        ).then(function (results) {
            return Promise.all(results.map(function (address) {
                return Promise.all(address.queues.map(function (queue) {
                    return agent.getQueueDetails(queue);
                })).then(function (results) {
                    address.queues = results;
                    return address;
                });
            })).then(function (results) {
                var address_map = {};
                for (var i = 0; i < results.length; i++) {
                    address_map[results[i].name] = results[i];
                }
                return address_map;
            });
        });
    });
};

Artemis.prototype.createAddress = function (name, type) {
    var routing_types = [];
    if (type.anycast || type.queue) routing_types.push('ANYCAST');
    if (type.multicast || type.topic) routing_types.push('MULTICAST');
    return this._request('broker', 'createAddress', [name, routing_types.join(',')]);
};

Artemis.prototype.deleteAddress = function (name) {
    return this._request('broker', 'deleteAddress', [name]);
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

Artemis.prototype.getAllQueuesAndTopics = function () {
    return this.listAddresses().then(function (addresses) {
        for (var name in addresses) {
            var address = addresses[name];
            if (address.anycast && address.queues.length === 1 && address.name === address.queues[0].name) {
                addresses[name] = address.queues[0];
                addresses[name].is_queue = true;
            } else if (address.multicast) {
                address.is_topic = true;
                address.subscription_count = address.queues.length;
                address.durable_subscription_count = address.queues.filter(function (q) { return q.durable; }).length;
                address.inactive_durable_subscription_count = address.queues.filter(function (q) { return q.durable && q.consumers === 0; }).length;
                address.subscriptions = address.queues;
                delete address['queues'];
                delete address['multicast'];
            }
        }
        return addresses;
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
