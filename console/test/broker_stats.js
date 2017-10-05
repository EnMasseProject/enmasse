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
var Promise = require('bluebird');
var events = require('events');
var util = require('util');
var rhea = require('rhea');
var BrokerStats = require('../lib/broker_stats.js');

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
            return self.objects.filter(function (o) { return o.type === 'address'}).map(function (a) { return a.name; });
        },
        getQueueNames : function () {
            return self.objects.filter(function (o) { return o.type === 'queue'}).map(function (a) { return a.name; });
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
    if (target) {
        if (target[operation]) {
            var result = target[operation].apply(target, params);
            //console.log('invocation of ' + operation + ' on ' + resource + ' returned ' + JSON.stringify(result));
            var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, request.reply_to); });
            if (reply_link) {
                reply_link.send({application_properties:{_AMQ_OperationSucceeded:true}, body:JSON.stringify([result])});
            }
        } else {
            console.log('invocation of ' + operation + ' on ' + resource + ' failed due to no matching operation being found');
            if (reply_link) {
                reply_link.send({application_properties:{_AMQ_OperationSucceeded:false}, body:'no such operation: ' + operation + ' on' + resource});
            }
        }
    } else {
        console.log('invocation of ' + operation + ' on ' + resource + ' failed due to no matching resource being found');
        if (reply_link) {
            reply_link.send({application_properties:{_AMQ_OperationSucceeded:false}, body:'no such resource: ' + resource});
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

function MockPodSense () {
    this.brokers = [];
    this.connections = {};
    this.container = rhea.create_container({id:'mock-pod-sense'});
    this.container.on('sender_open', this.on_subscribe.bind(this));
    this.container.on('connection_open', this.register.bind(this));
    this.container.on('connection_close', this.unregister.bind(this));
    this.container.on('disconnected', this.unregister.bind(this));
}

MockPodSense.prototype.listen = function (port) {
    this.port = port;
    this.server = this.container.listen({port:port});
    var self = this;
    this.server.on('listening', function () {
        self.port = self.server.address().port;
    });
    return this.server;
};

MockPodSense.prototype.close = function (callback) {
    this.brokers.forEach(function (b) { b.close(); });
    if (this.server) this.server.close(callback);
};

MockPodSense.prototype.notify = function (sender) {
    var pods = this.brokers.map(function (b) { return b.get_pod_descriptor(); });
    sender.send({body:pods});
};

MockPodSense.prototype.on_subscribe = function (context) {
    this.notify(context.sender);
};

MockPodSense.prototype.add_broker = function (name, port) {
    var broker = new MockBroker(name || 'broker-' + (this.brokers.length+1));
    broker.listen(port);
    this.brokers.push(broker);
    return broker;
};

function is_subscriber(link) {
    return link.is_sender();
}

MockPodSense.prototype.notify_all = function () {
    var self = this;
    for (var c in this.connections) {
        this.connections[c].each_link(function (link) {
            self.notify(link);
        }, is_subscriber);
    }
}

MockPodSense.prototype.register = function (context) {
    this.connections[context.connection.container_id] = context.connection;
};

MockPodSense.prototype.unregister = function (context) {
    delete this.connections[context.connection.container_id];
};

MockPodSense.prototype.remove_broker = function (broker) {
    var i = this.brokers.indexOf(broker);
    if (i >= 0) {
        this.brokers.splice(i, 1);
        this.notify_all();
    }
};

describe('broker stats', function() {
    this.timeout(5000);
    var discovery;
    var broker;

    beforeEach(function(done) {
        discovery = new MockPodSense();
        broker = discovery.add_broker();
        var s = discovery.listen(0).on('listening', function () {
            process.env.CONFIGURATION_SERVICE_HOST = 'localhost';
            process.env.CONFIGURATION_SERVICE_PORT = s.address().port;
            done();
        });
    });

    afterEach(function(done) {
        discovery.close(done);
        done();
    });

    it('retrieves queue stats from a single broker', function(done) {
        broker.add_queue_address('myqueue',
                                 {
                                     durable:true,
                                     messageCount:10,
                                     consumerCount:3,
                                     messagesAdded:44,
                                     deliveringCount:5,
                                     messagesAcknowledged:8,
                                     messagesExpired:4,
                                     messagesKilled: 2
                                 });
        var stats = new BrokerStats();
        broker.on('connected', function () {
            stats._retrieve().then(function (results) {
                assert.equal(results.myqueue.depth, 10);
                assert.equal(results.myqueue.shards.length, 1);
                assert.equal(results.myqueue.shards[0].durable, true);
                assert.equal(results.myqueue.shards[0].messages, 10);
                assert.equal(results.myqueue.shards[0].consumers, 3);
                assert.equal(results.myqueue.shards[0].enqueued, 44);
                assert.equal(results.myqueue.shards[0].delivering, 5);
                assert.equal(results.myqueue.shards[0].acknowledged, 8);
                assert.equal(results.myqueue.shards[0].expired, 4);
                assert.equal(results.myqueue.shards[0].killed, 2);
                done();
            });
        });
    });
    it('aggregates queue stats from multiple brokers', function(done) {
        broker.add_queue_address('myqueue',
                                 {
                                     durable:true,
                                     messageCount:10,
                                     consumerCount:3,
                                     messagesAdded:44,
                                     deliveringCount:5,
                                     messagesAcknowledged:8,
                                     messagesExpired:4,
                                     messagesKilled: 2
                                 });
        var broker2 = discovery.add_broker();
        broker2.add_queue_address('myqueue',
                                 {
                                     durable:true,
                                     messageCount:22,
                                     consumerCount:1,
                                     messagesAdded:88,
                                     deliveringCount:9,
                                     messagesAcknowledged:58,
                                     messagesExpired:7,
                                     messagesKilled: 3
                                 });
        var stats = new BrokerStats();
        broker2.on('connected', function () {
            stats._retrieve().then(function (results) {
                assert.equal(results.myqueue.depth, 32);
                assert.equal(results.myqueue.shards.length, 2);
                var b1, b2;
                if (results.myqueue.shards[0].name === 'broker-1') {
                    b1 = 0;
                    b2 = 1;
                } else {
                    b1 = 1;
                    b2 = 0;
                }
                assert.equal(results.myqueue.shards[b1].name, 'broker-1');
                assert.equal(results.myqueue.shards[b1].durable, true);
                assert.equal(results.myqueue.shards[b1].messages, 10);
                assert.equal(results.myqueue.shards[b1].consumers, 3);
                assert.equal(results.myqueue.shards[b1].enqueued, 44);
                assert.equal(results.myqueue.shards[b1].delivering, 5);
                assert.equal(results.myqueue.shards[b1].acknowledged, 8);
                assert.equal(results.myqueue.shards[b1].expired, 4);
                assert.equal(results.myqueue.shards[b1].killed, 2);

                assert.equal(results.myqueue.shards[b2].name, 'broker-2');
                assert.equal(results.myqueue.shards[b2].durable, true);
                assert.equal(results.myqueue.shards[b2].messages, 22);
                assert.equal(results.myqueue.shards[b2].consumers, 1);
                assert.equal(results.myqueue.shards[b2].enqueued, 88);
                assert.equal(results.myqueue.shards[b2].delivering, 9);
                assert.equal(results.myqueue.shards[b2].acknowledged, 58);
                assert.equal(results.myqueue.shards[b2].expired, 7);
                assert.equal(results.myqueue.shards[b2].killed, 3);
                done();
            });
        });
    });


    it('aggregates topic stats from multiple brokers', function(done) {
        broker.add_topic_address('mytopic',
                                 {
                                     'dsub1':{durable:true, messageCount:10, consumerCount:9, messagesAdded:8, deliveringCount:7, messagesAcknowledged:6, messagesExpired:5, messagesKilled: 4},
                                     'sub2':{durable:false, messageCount:11, consumerCount:10, messagesAdded:9, deliveringCount:8, messagesAcknowledged:7, messagesExpired:6, messagesKilled: 5},
                                 }, 21);
        var broker2 = discovery.add_broker();
        broker2.add_topic_address('mytopic',
                                 {
                                     'sub3':{durable:false, messageCount:9, consumerCount:8, messagesAdded:7, deliveringCount:6, messagesAcknowledged:5, messagesExpired:4, messagesKilled: 3}
                                 }, 9);
        var broker3 = discovery.add_broker();
        broker3.add_topic_address('mytopic',
                                 {
                                     'dsub4':{durable:true, messageCount:7, consumerCount:0, messagesAdded:7, deliveringCount:7, messagesAcknowledged:7, messagesExpired:7, messagesKilled: 7},
                                     'dsub5':{durable:true, messageCount:1, consumerCount:1, messagesAdded:1, deliveringCount:1, messagesAcknowledged:1, messagesExpired:1, messagesKilled: 1},
                                     'sub6':{durable:false, messageCount:2, consumerCount:2, messagesAdded:2, deliveringCount:2, messagesAcknowledged:2, messagesExpired:2, messagesKilled: 2}
                                 }, 14);
        var stats = new BrokerStats();
        broker3.on('connected', function () {
            stats._retrieve().then(function (results) {
                assert.equal(results.mytopic.depth, 44);
                assert.equal(results.mytopic.shards.length, 3);

                var b1, b2, b3;
                for (var i = 0; i < results.mytopic.shards.length; i++) {
                    if (results.mytopic.shards[i].name === 'broker-1') {
                        b1 = i;
                    } else if (results.mytopic.shards[i].name === 'broker-2') {
                        b2 = i;
                    } else if (results.mytopic.shards[i].name === 'broker-3') {
                        b3 = i;
                    }
                }

                assert.equal(results.mytopic.shards[b1].subscription_count, 2);
                assert.equal(results.mytopic.shards[b1].durable_subscription_count, 1);
                assert.equal(results.mytopic.shards[b1].inactive_durable_subscription_count, 0);
                assert.equal(results.mytopic.shards[b1].name, 'broker-1');
                assert.equal(results.mytopic.shards[b1].subscriptions.length, 2);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].durable, true);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].messages, 10);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].consumers, 9);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].enqueued, 8);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].delivering, 7);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].acknowledged, 6);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].expired, 5);
                assert.equal(results.mytopic.shards[b1].subscriptions[0].killed, 4);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].durable, false);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].messages, 11);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].consumers, 10);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].enqueued, 9);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].delivering, 8);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].acknowledged, 7);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].expired, 6);
                assert.equal(results.mytopic.shards[b1].subscriptions[1].killed, 5);

                assert.equal(results.mytopic.shards[b2].subscription_count, 1);
                assert.equal(results.mytopic.shards[b2].durable_subscription_count, 0);
                assert.equal(results.mytopic.shards[b2].inactive_durable_subscription_count, 0);
                assert.equal(results.mytopic.shards[b2].name, 'broker-2');
                assert.equal(results.mytopic.shards[b2].subscriptions.length, 1);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].durable, false);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].messages, 9);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].consumers, 8);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].enqueued, 7);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].delivering, 6);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].acknowledged, 5);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].expired, 4);
                assert.equal(results.mytopic.shards[b2].subscriptions[0].killed, 3);

                assert.equal(results.mytopic.shards[b3].subscription_count, 3);
                assert.equal(results.mytopic.shards[b3].durable_subscription_count, 2);
                assert.equal(results.mytopic.shards[b3].inactive_durable_subscription_count, 1);
                assert.equal(results.mytopic.shards[b3].name, 'broker-3');
                assert.equal(results.mytopic.shards[b3].subscriptions.length, 3);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].durable, true);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].messages, 7);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].consumers, 0);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].enqueued, 7);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].delivering, 7);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].acknowledged, 7);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].expired, 7);
                assert.equal(results.mytopic.shards[b3].subscriptions[0].killed, 7);

                for (var i = 1; i < 3; i++) {
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].durable, i == 1);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].messages, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].consumers, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].enqueued, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].delivering, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].acknowledged, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].expired, i);
                    assert.equal(results.mytopic.shards[b3].subscriptions[i].killed, i);
                }
                done();
            });
        });
    });

    it('handles removal of broker from list', function(done) {
        broker.add_queue_address('myqueue',
                                 {
                                     durable:true,
                                     messageCount:10,
                                     consumerCount:3,
                                     messagesAdded:44,
                                     deliveringCount:5,
                                     messagesAcknowledged:8,
                                     messagesExpired:4,
                                     messagesKilled: 2
                                 });
        var broker2 = discovery.add_broker();
        broker2.add_queue_address('myqueue',
                                 {
                                     durable:true,
                                     messageCount:22,
                                     consumerCount:1,
                                     messagesAdded:88,
                                     deliveringCount:9,
                                     messagesAcknowledged:58,
                                     messagesExpired:7,
                                     messagesKilled: 3
                                 });
        var stats = new BrokerStats();
        broker2.on('connected', function () {
            stats._retrieve().then(function (results) {
                assert.equal(results.myqueue.depth, 32);
                assert.equal(results.myqueue.shards.length, 2);
                discovery.remove_broker(broker2);
                broker2.on('disconnected', function () {
                    stats._retrieve().then(function (new_results) {
                        assert.equal(new_results.myqueue.depth, 10);
                        assert.equal(new_results.myqueue.shards.length, 1);
                        done();
                    });
                });
            });
        });
    });

});
