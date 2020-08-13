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
var EventEmitter = require('events');

var AddressSource = require('../lib/internal_address_source');
var AddressServer = require('../testlib/mock_resource_server.js').AddressServer;

describe('address source', function() {
    var address_server;
    var address_plans_source;

    beforeEach(function(done) {
        address_plans_source = new EventEmitter();
        address_server = new AddressServer();
        address_server.listen(0, done);
    });

    afterEach(function(done) {
        address_server.close(done);
    });

    it('retrieves all addresses - event addresses_defined', function(done) {
        address_server.add_address_definition({address:'s1.foo', type:'queue'}, undefined, '1234');
        address_server.add_address_definition({address:'s1.bar', type:'topic'}, undefined, '1234');
        address_server.add_address_definition({address:'s2.baz', type:'queue'}, undefined, "4321");
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start();
        source.watcher.close();//prevents watching
        source.on('addresses_defined', function (addresses) {
            assert.equal(addresses.length, 2);
            //relies on sorted order (TODO: avoid relying on any order)
            assert.equal(addresses[0].address, 's1.bar');
            assert.equal(addresses[0].type, 'topic');
            assert.equal(addresses[1].address, 's1.foo');
            assert.equal(addresses[1].type, 'queue');
            done();
        });
    });
    it('retrieves all addresses - addresses_ready', function(done) {
        address_server.add_address_definition({address:'s1.foo', type:'queue'}, undefined, '1234');
        address_server.add_address_definition({address:'s1.bar', type:'topic'}, undefined, '1234');
        address_server.add_address_definition({address:'s1.pending', type:'anycast'}, undefined, '1234', {}, {phase: 'Pending'});
        address_server.add_address_definition({address:'s1.terminating', type:'anycast'}, undefined, '1234', {}, {phase: 'Terminating'});
        address_server.add_address_definition({address:'s2.baz', type:'queue'}, undefined, "4321");
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start();
        source.watcher.close();//prevents watching
        source.on('addresses_ready', function (addresses) {
            assert.equal(addresses.length, 2);
            //relies on sorted order (TODO: avoid relying on any order)
            assert.equal(addresses[0].address, 's1.bar');
            assert.equal(addresses[0].type, 'topic');
            assert.equal(addresses[1].address, 's1.foo');
            assert.equal(addresses[1].type, 'queue');
            done();
        });
    });
    it('indicates allocation to broker', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue'}, undefined, '1234', undefined, {brokerStatuses:[{containerId: 'broker-1'}]});
        address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234', undefined, {brokerStatuses:[{containerId: 'broker-2'}]});
        address_server.add_address_definition({address:'baz', type:'anycast'}, undefined, '1234');
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start();
        source.watcher.close();//prevents watching
        source.on('addresses_defined', function (addresses) {
            assert.equal(addresses.length, 3);
            //relies on sorted order (TODO: avoid relying on any order)
            assert.equal(addresses[0].address, 'bar');
            assert.equal(addresses[0].type, 'topic');
            assert.equal(addresses[0].allocated_to[0].containerId, 'broker-2');
            assert.equal(addresses[1].address, 'baz');
            assert.equal(addresses[1].type, 'anycast');
            assert.equal(addresses[1].allocated_to, undefined);
            assert.equal(addresses[2].address, 'foo');
            assert.equal(addresses[2].type, 'queue');
            assert.equal(addresses[2].allocated_to[0].containerId, 'broker-1');
            done();
        });
    });
    it('watches for changes', function(done) {
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start();
        source.once('addresses_defined', function () {
            setTimeout(function () {
                address_server.add_address_definition({address:'foo', type:'queue'}, undefined, '1234');
                address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234');
                var addresses;
                source.on('addresses_defined', function (latest) {
                    addresses = latest;
                });
                setTimeout(function () {
                    assert.equal(addresses.length, 2);
                    //relies on sorted order (TODO: avoid relying on any order)
                    assert.equal(addresses[0].address, 'bar');
                    assert.equal(addresses[0].type, 'topic');
                    assert.equal(addresses[1].address, 'foo');
                    assert.equal(addresses[1].type, 'queue');
                    source.watcher.close().then(function () {
                        done();
                    });
                }, 200);
            }, 200);
        });
    });
    it('watches for changes - address updated', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan1' }, undefined, '1234');
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start();
        source.once('addresses_defined', (addresses) => {
            assert.equal(addresses.length, 1);
            process.nextTick(() => {
                source.on('addresses_defined', (updates) => {
                    assert.equal(updates.length, 1);
                    assert.equal(updates[0].address, 'foo');
                    assert.equal(updates[0].type, 'queue');
                    assert.equal(updates[0].plan, 'myplan2');
                    source.watcher.close().then(() => {
                        done();
                    });
                });
                address_server.update_address_definition({address:'foo', type:'queue', plan: 'myplan2' }, undefined, '1234');
            });
        });
    });
    it('watches for changes even on error', function(done) {
        var count = 0;
        address_server.failure_injector = {
            match: function (request) {
                return count++ === 0 && request.watch === false && request.type === 'address_server';
            },
            code: function (request) {
                return 500;
            }
        };
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start();
        source.once('addresses_defined', function () {
            setTimeout(function () {
                address_server.add_address_definition({address:'foo', type:'queue'}, undefined, '1234');
                address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234');
                var addresses;
                source.on('addresses_defined', function (latest) {
                    addresses = latest;
                });
                setTimeout(function () {
                    assert.equal(addresses.length, 2);
                    //relies on sorted order (TODO: avoid relying on any order)
                    assert.equal(addresses[0].address, 'bar');
                    assert.equal(addresses[0].type, 'topic');
                    assert.equal(addresses[1].address, 'foo');
                    assert.equal(addresses[1].type, 'queue');
                    source.watcher.close().then(function () {
                        done();
                    });
                }, 200);
            }, 200);
        });
    });
    it('updates status readiness', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'}, undefined, '1234');
        address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234');
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {addressType: 'queue'}
        }]);
        source.watcher.close();
        source.on('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.phase, 'Active');

                done();
            }).catch(done);
        });
    });
    it('updates readiness after recreation', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'});
        address_server.add_address_definition({address:'bar', type:'topic'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {addressType: 'queue'}
        }]);
        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                address_server.remove_resource_by_name('addresses', 'foo');
                source.once('addresses_defined', function (addresses) {
                    address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'});
                    source.once('addresses_defined', function (addresses) {
                        source.watcher.close();
                        source.check_status({foo:{propagated:100}}).then(function () {
                            var address = address_server.find_resource('addresses', 'foo');
                            assert.equal(address.status.isReady, true);
                            done();
                        }).catch(done);
                    });
                });
            });
        });
    });
    it('updates status - plan not found', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'not found'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {addressType: 'queue'}
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, false);
                assert.equal(address.status.messages.length, 1);
                assert.equal(address.status.messages[0], "Unknown address plan 'not found'");
                done();
            });
        });
    });
    it('updates status - deadletter address not found', function(done) {
        address_server.add_address_definition({address:'mydla', type:'queue', plan: 'myplan'});
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan', deadLetterAddress: 'mydla'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {addressType: 'queue'}
        }]);
        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, false);
                assert.equal(address.status.messages.length, 1);
                assert.equal(address.status.messages[0], "Address 'foo' (resource name 'foo') references a deadletter address 'mydla' (resource name 'mydla') that is not of expected type 'deadletter' (found type 'queue' instead).");
                done();
            });
        });
    });
    it('updates status - expiry address not found', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan', expiryAddress: 'notfound'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {addressType: 'queue'}
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, false);
                assert.equal(address.status.messages.length, 1);
                assert.equal(address.status.messages[0], "Address 'foo' (resource name 'foo') references an expiry address 'notfound' that does not exist.");
                done();
            });
        });
    });
    it('updates status - plan status', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue',
                resources: {
                    broker: 0.01
                }
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messages.length, 0);
                assert.equal(address.status.messages.length, 0);
                assert.deepEqual(address.status.planStatus, {name: 'myplan',
                partitions: 1,
                resources: {
                    broker: 0.01
                }});
                done();
            });
        });
    });
    it('updates status - ttl from plan', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue',
                messageTtl: {
                    minimum: 1000,
                    maximum: 2000
                },
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messageTtl.minimum, 1000);
                assert.equal(address.status.messageTtl.maximum, 2000);
                done();
            });
        });
    });
    it('updates status - ttl from address', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan', messageTtl: {minimum: 1000, maximum: 2000}});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue'
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messageTtl.minimum, 1000);
                assert.equal(address.status.messageTtl.maximum, 2000);
                done();
            });
        });
    });
    it('updates status - ttl address overrides plan', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan', messageTtl: {minimum: 500, maximum: 750}});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue',
                messageTtl: {
                    minimum: 600,
                    maximum: 2000
                },
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messageTtl.minimum, 600);
                assert.equal(address.status.messageTtl.maximum, 750);
                done();
            });
        });
    });
    it('updates status - messageRedelivery from plan', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue',
                messageRedelivery: {
                    maximumDeliveryAttempts: 2,
                },
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messageRedelivery.maximumDeliveryAttempts, 2);
                done();
            });
        });
    });
    it('updates status - messageRedelivery address overrides plan', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue', plan: 'myplan', messageRedelivery: {maximumDeliveryAttempts: 3, }});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_plans_source);
        address_plans_source.emit("addressplans_defined", [{
            kind: 'AddressPlan',
            metadata: {name: 'myplan'},
            spec: {
                addressType: 'queue',
                messageRedelivery: {
                    maximumDeliveryAttempts: 2,
                    redeliveryDelay: 1000,
                },
            }
        }]);

        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function (add) {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                assert.equal(address.status.messageRedelivery.maximumDeliveryAttempts, 3);
                assert.equal(address.status.messageRedelivery.redeliveryDelay, 1000);
                done();
            });
        });
    });

    function equal_properties (a, b) {
        for (let k in a) {
            if (a[k] !== b[k]) return false;
        }
        for (let k in b) {
            if (b[k] !== a[k]) return false;
        }
        return true;
    }

    it('compares the name of the address', function(done) {
        let source = new AddressSource({});
        source.last = {};

        // Populate the initial
        source.get_changes('foo', [{name:'ab',address:'AB'}], equal_properties)

        // Add the 'aa' address
        let c = source.get_changes('foo', [{name:'aa',address:'aa'},{name:'ab',address:'AB'}], equal_properties)
        assert(c !== undefined);
        assert.equal(c.added.length, 1);
        assert.equal(c.added[0].name, 'aa');
        assert.equal(c.removed.length, 0);
        assert.equal(c.modified.length, 0);
        done();
    });
});
