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
var BrokerStats = require('../lib/broker_stats.js');
var MockBroker = require('../testlib/mock_broker.js');
var ResourceServer = require('../testlib/mock_resource_server.js').ResourceServer;

function PodServer () {
    this.resource_server = new ResourceServer('Pod', true, function (b) { return b.get_pod_definition(); });
}

PodServer.prototype.add_broker = function (name, port) {
    var broker = new MockBroker(name || 'broker-' + (this.resource_server.resources.length+1));
    broker.listen(port);
    this.resource_server.add_resource(broker);
    return broker;
};

PodServer.prototype.remove_broker = function (broker) {
    this.resource_server.remove_resource(broker);
};

PodServer.prototype.listen = function (port) {
    return this.resource_server.listen(port);
};

PodServer.prototype.close = function (callback) {
    this.resource_server.resources.forEach(function (b) { b.close(); });
    this.resource_server.close(callback);
};

describe('broker stats', function() {
    this.timeout(5000);
    var discovery;
    var broker;

    beforeEach(function(done) {
        discovery = new PodServer();
        broker = discovery.add_broker();
        var s = discovery.listen(0).on('listening', function () {
            process.env.CONFIGURATION_SERVICE_HOST = 'localhost';
            process.env.CONFIGURATION_SERVICE_PORT = s.address().port;
            done();
        });
    });

    afterEach(function(done) {
        discovery.close(function () {
            done();
        });
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
        var stats = new BrokerStats({port:discovery.resource_server.port, host:'localhost', namespace:'default', token: 'foo'});
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
                stats.watcher.close();
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
        var stats = new BrokerStats({port:discovery.resource_server.port, host:'localhost', namespace:'default', token: 'foo'});
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
                stats.watcher.close();
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
        var stats = new BrokerStats({port:discovery.resource_server.port, host:'localhost', namespace:'default', token: 'foo'});
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
                stats.watcher.close();
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
        var stats = new BrokerStats({port:discovery.resource_server.port, host:'localhost', namespace:'default', token: 'foo'});
        broker2.on('connected', function () {
            stats._retrieve().then(function (results) {
                assert.equal(results.myqueue.depth, 32);
                assert.equal(results.myqueue.shards.length, 2);
                discovery.remove_broker(broker2);
                broker2.on('disconnected', function () {
                    stats._retrieve().then(function (new_results) {
                        assert.equal(new_results.myqueue.depth, 10);
                        assert.equal(new_results.myqueue.shards.length, 1);
                        stats.watcher.close();
                        done();
                    });
                });
            });
        });
    });

});
