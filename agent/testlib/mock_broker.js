/*
 * Copyright 2017 Red Hat Inc.
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

var assert = require('assert');
var events = require('events');
var util = require('util');
var rhea = require('rhea');
var myutils = require('../lib/utils.js');

function find(array, predicate) {
    var results = array.filter(predicate);
    if (results.length > 0) return results[0];
    else return undefined;
}

function match_source_address(link, address) {
    return link && link.local && link.local.attach && link.local.attach.source
        && link.local.attach.source.address === address;
}

function MockBroker (name) {
    this.name = name;
    this.objects = [];
    this.container = rhea.create_container({id:this.name});
    this.container.on('message', this.on_message.bind(this));
    var self = this;
    this.container.on('connection_open', function (context) {
        self.emit('connected', context);
    });
    this.container.on('connection_close', function (context) {
        self.emit('disconnected', context);
    });
    this.container.on('sender_open', function(context) {
        if (context.sender.source.dynamic) {
            var id = self.container.generate_uuid();
            context.sender.set_source({address:id});
        }
    });

    var self = this;
    this.objects.push({
        resource_id: 'broker',
        getAddressNames : function () {
            return self.list_addresses().map(function (a) { return a.name; });
        },
        getQueueNames : function () {
            return self.list_queues().map(function (a) { return a.name; });
        },
        createQueue : function (address, routingTypes, name, filter, durable, maxConsumers, purgeOnNoConsumers, autoCreateAddress) {
            assert.equal(routingTypes, 'ANYCAST');
            assert.equal(filter, null);
            assert.equal(purgeOnNoConsumers, false);
            assert.equal(autoCreateAddress, true);
            if (self.objects.some(function (o) { return o.type === 'queue' && o.name === name})) {
                throw new Error('queue ' + name + ' already exists!');
            } else {
                self.add_queue(name, {'durable':durable});
                if (!self.objects.some(function (o) { return o.type === 'address' && o.name === address; })) {
                    self.add_address(address, false, 0, [name]);
                }
            }
        },
        createAddress : function (name, routingTypes) {
            assert.equal(routingTypes, 'MULTICAST');
            if (self.objects.some(function (o) { return o.type === 'address' && o.name === name; })) {
                throw new Error('address ' + name + ' already exists!');
            } else {
                self.add_address(name, true);
            }
        },
        destroyQueue : function (name) {
            if (myutils.remove(self.objects, function (o) { return o.type === 'queue' && o.name === name; }) !== 1) {
                throw new Error('error deleting queue ' + name);
            } else {
                function is_queue_address(o) {
                    return o.type === 'address' && o.name === name
                        && o.routingTypesAsJSON[0] === 'ANYCAST'
                        && o.queueNames.length === 1
                        && o.queueNames[0] === name;
                }
                myutils.remove(self.objects, is_queue_address);
            }
        },
        deleteAddress : function (name) {
            if (myutils.remove(self.objects, function (o) { return o.type === 'address' && o.name === name; }) !== 1) {
                throw new Error('error deleting address ' + name);
            }
        },
        listConnectionsAsJSON : function () {
            return JSON.stringify(self.get('connection'));
        },
        listProducersInfoAsJSON  : function () {
            return JSON.stringify(self.get('producer'));
        },
        listAllConsumersAsJSON : function () {
            return JSON.stringify(self.get('consumer'));
        }

    });
}

util.inherits(MockBroker, events.EventEmitter);

MockBroker.prototype.listen = function (port) {
    this.server = this.container.listen({port:port || 0});
    var self = this;
    this.server.on('listening', function () {
        self.port = self.server.address().port;
    });
    return this.server;
};

MockBroker.prototype.close = function (callback) {
    if (this.server) this.server.close(callback);
};

MockBroker.prototype.on_message = function (context) {
    var request = context.message;
    var resource = context.message.application_properties._AMQ_ResourceName;
    var operation = context.message.application_properties._AMQ_OperationName;
    var params = request.body ? JSON.parse(request.body) : [];
    var target = find(this.objects, function (o) { return o.resource_id === resource; });
    var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, request.reply_to); });
    try {
        if (target) {
            if (target[operation]) {
                var result = target[operation].apply(target, params);
                //console.log('invocation of ' + operation + ' on ' + resource + ' returned ' + JSON.stringify(result));
                if (reply_link) {
                    reply_link.send({application_properties:{_AMQ_OperationSucceeded:true}, body:JSON.stringify([result])});
                }
            } else {
                throw new Error('no such operation: ' + operation + ' on' + resource);
            }
        } else {
            throw new Error('no such resource: ' + resource);
        }
    } catch (e) {
        console.log('invocation of ' + operation + ' on ' + resource + ' failed: ' + e);
        if (reply_link) {
            reply_link.send({application_properties:{_AMQ_OperationSucceeded:false}, body:util.format('%s', e)});
        }
    }
};

var prefixes = {
    'is': false,
    'get': 0
};

function Resource (name, type, accessors, properties) {
    this.name = name;
    this.type = type;
    this.resource_id = type + '.' + name;
    var initial_values = properties || {};
    var self = this;
    accessors.forEach(function (accessor) {
        for (var prefix in prefixes) {
            if (accessor.indexOf(prefix) === 0) {
                var property = accessor.charAt(prefix.length).toLowerCase() + accessor.substr(prefix.length + 1);
                self[property] = initial_values[property] || prefixes[prefix];
                self[accessor] = function () { return self[property]; };
            }
        }
    });
}

var queue_accessors = ['isTemporary', 'isDurable', 'getMessageCount', 'getConsumerCount', 'getMessagesAdded',
                       'getDeliveringCount', 'getMessagesAcknowledged', 'getMessagesExpired', 'getMessagesKilled'];
var address_accessors = ['getRoutingTypesAsJSON','getNumberOfMessages','getQueueNames', 'getMessageCount'];

MockBroker.prototype.add_queue = function (name, properties) {
    var queue = new Resource(name, 'queue', queue_accessors, properties);
    this.objects.push(queue);
    return queue;
};

MockBroker.prototype.add_address = function (name, is_multicast, messages, queue_names) {
    var address = new Resource(name, 'address', address_accessors, {
        queueNames: queue_names || [],
        numberOfMessages: messages || 0,
        routingTypesAsJSON: [is_multicast ? 'MULTICAST' : 'ANYCAST'],
        messageCount: messages || 0
    });
    this.objects.push(address);
    return address;
};

MockBroker.prototype.add_queue_address = function (name, properties) {
    this.add_queue(name, properties);
    var addr = this.add_address(name, false, 0, [name]);
    return addr;
};

MockBroker.prototype.add_topic_address = function (name, subscribers, messages) {
    for (var s in subscribers) {
        this.add_queue(s, subscribers[s]);
    }
    return this.add_address(name, true, messages, Object.keys(subscribers));
};

MockBroker.prototype.get = function (type) {
    return this.objects.filter(function (o) { return o.type === type; });
};

MockBroker.prototype.list_queues = function () {
    return this.get('queue');
};

MockBroker.prototype.list_addresses = function () {
    return this.get('address');
};


MockBroker.prototype.get_pod_descriptor = function () {
    return {
        ready : (this.port === undefined ? 'False' : 'True'),
        phase : 'Running',
        name : this.name,
        host : 'localhost',
        ports : {
            broker : {
                amqp: this.port
            }
        }
    };
};

module.exports = MockBroker;
