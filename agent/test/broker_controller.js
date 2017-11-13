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
var MockBroker = require('../testlib/mock_broker.js');
var BrokerController = require('../lib/broker_controller.js');

function remove(list, predicate) {
    var removed = [];
    for (var i = 0; i < list.length; ) {
        if (predicate(list[i])) {
            removed.push(list.splice(i, 1)[0]);
        } else {
            i++;
        }
    }
    return removed;
}

function verify_queue(addresses, queues, name) {
    var results = remove(queues, function (o) { return o.name === name; });
    assert.equal(results.length, 1, util.format('queue %s not found', name));
    assert.equal(results[0].name, name);
    results = remove(addresses, function (o) { return o.name === name; });
    assert.equal(results.length, 1, util.format('address %s not found', name));
    assert.equal(results[0].name, name);
    assert.equal(results[0].routingTypesAsJSON[0], 'ANYCAST');
    assert.equal(results[0].queueNames[0], name);
    return results[0];
}

function verify_topic(addresses, name) {
    var results = remove(addresses, function (o) { return o.name === name; });
    assert.equal(results.length, 1, util.format('address %s not found', name));
    assert.equal(results[0].name, name);
    assert.equal(results[0].routingTypesAsJSON[0], 'MULTICAST');
    return results[0];
}

describe('broker controller', function() {
    var broker;
    var controller;

    beforeEach(function(done) {
        broker = new MockBroker('mybroker');
        broker.listen().on('listening', function () {
            controller = new BrokerController();
            controller.connect({port:broker.port});
            controller.on('ready', function () {
                done();
            });
        });
    });

    afterEach(function(done) {
        controller.close();
        broker.close(done);
    });

    it('creates a queue', function(done) {
        controller.addresses_defined([{address:'foo',type:'queue'}]).then(function () {
            var addresses = broker.list_addresses();
            var queues = broker.list_queues();
            verify_queue(addresses, queues, 'foo');
            assert.equal(addresses.length, 0);
            assert.equal(queues.length, 0);
            done();
        });
    });
    it('creates a topic', function(done) {
        controller.addresses_defined([{address:'bar',type:'topic'}]).then(function () {
            var addresses = broker.list_addresses();
            verify_topic(addresses, 'bar');
            assert.equal(addresses.length, 0);
            done();
        });
    });
    it('deletes a queue', function(done) {
        controller.addresses_defined([{address:'foo',type:'queue'}, {address:'bar',type:'topic'}]).then(function () {
            var addresses = broker.list_addresses();
            var queues = broker.list_queues();
            verify_queue(addresses, queues, 'foo');
            verify_topic(addresses, 'bar');
            assert.equal(addresses.length, 0);
            assert.equal(queues.length, 0);
            controller.addresses_defined([{address:'bar',type:'topic'}]).then(function () {
                var addresses = broker.list_addresses();
                var queues = broker.list_queues();
                verify_topic(addresses, 'bar');
                assert.equal(addresses.length, 0);
                assert.equal(queues.length, 0);
                done();
            });
        });
    });
    it('deletes a topic', function(done) {
        controller.addresses_defined([{address:'foo',type:'queue'}, {address:'bar',type:'topic'}]).then(function () {
            var addresses = broker.list_addresses();
            var queues = broker.list_queues();
            verify_queue(addresses, queues, 'foo');
            verify_topic(addresses, 'bar');
            assert.equal(addresses.length, 0);
            assert.equal(queues.length, 0);
            controller.addresses_defined([{address:'foo',type:'queue'}]).then(function () {
                var addresses = broker.list_addresses();
                var queues = broker.list_queues();
                verify_queue(addresses, queues, 'foo');
                assert.equal(addresses.length, 0);
                assert.equal(queues.length, 0);
                done();
            });
        });
    });
    it('retrieves topic stats', function(done) {
        broker.add_topic_address('foo', {
            's1':{durable:true, messageCount:10, consumerCount:9, messagesAdded:8, deliveringCount:7, messagesAcknowledged:6, messagesExpired:5, messagesKilled: 4},
            's2':{durable:false, messageCount:11, consumerCount:10, messagesAdded:9, deliveringCount:8, messagesAcknowledged:7, messagesExpired:6, messagesKilled: 5},
        }, 21);
        for (var i = 0; i < 5; i++) broker.add_connection({}, [{destination:'foo'}]);
        Promise.all([
            new Promise(function (resolve, reject) {
                controller.on('address_stats_retrieved', function (stats) {
                    assert.equal(stats.foo.propagated, 100);
                    assert.equal(stats.foo.messages_in, 17);
                    assert.equal(stats.foo.messages_out, 13);
                    assert.equal(stats.foo.receivers, 2);
                    assert.equal(stats.foo.senders, 5);
                    resolve();
                });
            }),
            controller.retrieve_stats()
        ]).then(function () {
            done();
        });
    });
});
