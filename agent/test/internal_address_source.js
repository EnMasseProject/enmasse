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
var fs = require('fs');
var https = require('https');
var path = require('path');
var url = require('url');
var myutils = require('../lib/utils.js');

var AddressSource = require('../lib/internal_address_source');
var AddressServer = require('../testlib/mock_resource_server.js').AddressServer;

function broker_state(id) {
    return {clusterId: id, containerId: id, state: 'Active'};
}

describe('configmap backed address source', function() {
    var address_server;

    beforeEach(function(done) {
        address_server = new AddressServer();
        address_server.listen(0, done);
    });

    afterEach(function(done) {
        address_server.close(done);
    });

    it('retrieves all addresses', function(done) {
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
    it('indicates allocation to broker', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue'}, undefined, '1234', {'enmasse.io/broker-id':'broker-1'});
        address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234', {'enmasse.io/broker-id':'broker-2'});
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
    it('updates readiness', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue'}, undefined, '1234');
        address_server.add_address_definition({address:'bar', type:'topic'}, undefined, '1234');
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', INFRA_UUID: '1234'});
        source.start();
        source.watcher.close();
        source.on('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                var address = address_server.find_resource('addresses', 'foo');
                assert.equal(address.status.isReady, true);
                done();
            }).catch(done);
        });
    });
    it('updates readiness after recreation', function(done) {
        address_server.add_address_definition({address:'foo', type:'queue'});
        address_server.add_address_definition({address:'bar', type:'topic'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start();
        source.once('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                address_server.remove_resource_by_name('addresses', 'foo');
                source.once('addresses_defined', function (addresses) {
                    address_server.add_address_definition({address:'foo', type:'queue'});
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
    function remove_by_name (list, name) {
        function f(o) { return o.name === name}
        var results = list.filter(f);
        myutils.remove(list, f);
        return results[0];
    }
    function assert_equal_set(actual, expected) {
        assert.deepEqual(actual.sort(), expected.sort());
    }
    function plan_name (o) {
        return o.name;
    }
    it('retrieves address types from plans', function(done) {
        address_server.add_address_space_plan({plan_name:'space', address_plans:['small', 'medium', 'large', 'foo', 'bar', 'standard']});
        address_server.add_address_plan({plan_name:'small', address_type:'queue'});
        address_server.add_address_plan({plan_name:'medium', address_type:'queue'});
        address_server.add_address_plan({plan_name:'large', address_type:'queue'});
        address_server.add_address_plan({plan_name:'foo', address_type:'topic'});
        address_server.add_address_plan({plan_name:'bar', address_type:'topic'});
        address_server.add_address_plan({plan_name:'standard', address_type:'anycast', display_name:'display me', shortDescription:'abcdefg', longDescription:'hijklmn'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space'});
        source.start();
        source.watcher.close();
        source.get_address_types().then(function (types) {
            var queue = remove_by_name(types, 'queue');
            assert(queue);
            assert.equal(queue.plans.length, 3);
            assert_equal_set(queue.plans.map(plan_name), ['small', 'medium', 'large']);
            var topic = remove_by_name(types, 'topic');
            assert(topic);
            assert.equal(topic.plans.length, 2);
            assert_equal_set(topic.plans.map(plan_name), ['foo', 'bar']);
            var anycast = remove_by_name(types, 'anycast');
            assert(anycast);
            assert.equal(anycast.plans.length, 1);
            assert.equal(anycast.plans[0].name, 'standard');
            assert.equal(anycast.plans[0].displayName, 'display me');
            assert.equal(anycast.plans[0].shortDescription, 'abcdefg');
            assert.equal(anycast.plans[0].longDescription, 'hijklmn');
            assert.equal(types.length, 0);
            done();
        }).catch(function (error) {
            done(error);
        });
    });
    it('retrieves address types in order', function(done) {
        address_server.add_address_space_plan({plan_name:'space', address_plans:['bar', 'medium', 'foo', 'standard', 'non-standard', 'large', 'small']});
        address_server.add_address_plan({plan_name:'bar', address_type:'topic'});
        address_server.add_address_plan({plan_name:'medium', address_type:'queue', displayOrder:11});
        address_server.add_address_plan({plan_name:'foo', address_type:'topic', displayOrder:20});
        address_server.add_address_plan({plan_name:'standard', address_type:'anycast'});
        address_server.add_address_plan({plan_name:'non-standard', address_type:'anycast', displayOrder:30});
        address_server.add_address_plan({plan_name:'large', address_type:'queue', displayOrder:12});
        address_server.add_address_plan({plan_name:'small', address_type:'queue', displayOrder:10});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space'});
        source.start();
        source.watcher.close();
        source.get_address_types().then(function (types) {
            assert.equal(types[0].name, 'queue');
            assert.equal(types[0].plans[0].name, 'small');
            assert.equal(types[0].plans[1].name, 'medium');
            assert.equal(types[0].plans[2].name, 'large');
            assert.equal(types[1].name, 'topic');
            assert.equal(types[1].plans[0].name, 'foo');
            assert.equal(types[1].plans[1].name, 'bar');
            assert.equal(types[2].name, 'anycast');
            assert.equal(types[2].plans[0].name, 'non-standard');
            assert.equal(types[2].plans[1].name, 'standard');
            done();
        }).catch(function (error) {
            done(error);
        });
    });
    it('treats 0 correctly as value for displayOrder', function(done) {
        address_server.add_address_space_plan({plan_name:'space', address_plans:['bar', 'medium', 'foo', 'standard', 'non-standard', 'large', 'small']});
        address_server.add_address_plan({plan_name:'bar', address_type:'topic'});
        address_server.add_address_plan({plan_name:'medium', address_type:'queue', displayOrder:11});
        address_server.add_address_plan({plan_name:'foo', address_type:'topic', displayOrder:20});
        address_server.add_address_plan({plan_name:'standard', address_type:'anycast'});
        address_server.add_address_plan({plan_name:'non-standard', address_type:'anycast', displayOrder:30});
        address_server.add_address_plan({plan_name:'large', address_type:'queue', displayOrder:12});
        address_server.add_address_plan({plan_name:'small', address_type:'queue', displayOrder:0});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space'});
        source.start();
        source.watcher.close();
        source.get_address_types().then(function (types) {
            assert.equal(types[0].name, 'queue');
            assert.equal(types[0].plans[0].name, 'small');
            assert.equal(types[0].plans[1].name, 'medium');
            assert.equal(types[0].plans[2].name, 'large');
            assert.equal(types[1].name, 'topic');
            assert.equal(types[1].plans[0].name, 'foo');
            assert.equal(types[1].plans[1].name, 'bar');
            assert.equal(types[2].name, 'anycast');
            assert.equal(types[2].plans[0].name, 'non-standard');
            assert.equal(types[2].plans[1].name, 'standard');
            done();
        }).catch(function (error) {
            done(error);
        });
    });
    // internal_address_source now creates address objects, rather than the underlying configmap.
    // it('creates an address', function(done) {
    //     var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE: 'foo'});
    //     source.once('addresses_defined', function () {
    //         source.create_address({address:'myqueue', type:'queue', plan:'clever'}).then(
    //             function () {
    //                 source.once('addresses_defined', function (addresses) {
    //                     assert.equal(addresses.length, 1);
    //                     assert.equal(addresses[0].name.substring(0,4), 'foo.');
    //                     assert.equal(addresses[0].address, 'myqueue');
    //                     assert.equal(addresses[0].type, 'queue');
    //                     assert.equal(addresses[0].plan, 'clever');
    //                     source.watcher.close().then(function () {
    //                         done();
    //                     });
    //                 });
    //             }
    //         ).catch(done);
    //     });
    // });
    // it('deletes an address', function(done) {
    //     address_server.add_address_definition({address:'foo', type:'queue'}, 'address-config-foo');
    //     address_server.add_address_definition({address:'bar', type:'topic'}, 'address-config-bar');
    //     var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
    //     source.on('addresses_defined', function () {
    //         source.delete_address({address:'foo', type:'queue', name:'address-config-foo'}).then(
    //             function () {
    //                 source.on('addresses_defined', function (addresses) {
    //                     assert.equal(addresses.length, 1);
    //                     assert.equal(addresses[0].address, 'bar');
    //                     assert.equal(addresses[0].type, 'topic');
    //                     source.watcher.close().then(function () {
    //                         done();
    //                     });
    //                 });
    //             }
    //         ).catch(done);
    //     });
    // });
    //it('handles invalid address syntax', function(done) {
    //    address_server.add_address_definition({address:'foo', type:'queue'});
    //    address_server.add_config_map('baz', {type:'address-config'}, {'config.json': '{bad:x[!'});
    //    address_server.add_address_definition({address:'bar', type:'topic'});
    //    var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
    //    source.start();
    //    source.watcher.close();//prevents watching
    //    source.on('addresses_defined', function (addresses) {
    //        assert.equal(addresses.length, 2);
    //        assert.equal(addresses[0].address, 'bar');
    //        assert.equal(addresses[0].type, 'topic');
    //        assert.equal(addresses[1].address, 'foo');
    //        assert.equal(addresses[1].type, 'queue');
    //        done();
    //    });
    //});
    it('retrieves plans concurrently with addresses', function(done) {
        this.timeout(5000);
        address_server.add_address_space_plan({plan_name:'space', address_plans:['small', 'medium', 'large', 'foo', 'bar', 'standard']});
        address_server.add_address_plan({plan_name:'small', address_type:'queue'});
        address_server.add_address_plan({plan_name:'medium', address_type:'queue'});
        address_server.add_address_plan({plan_name:'large', address_type:'queue'});
        address_server.add_address_plan({plan_name:'foo', address_type:'topic'});
        address_server.add_address_plan({plan_name:'bar', address_type:'topic'});
        address_server.add_address_plan({plan_name:'standard', address_type:'anycast', display_name:'display me', shortDescription:'abcdefg', longDescription:'hijklmn'});
        var source = new AddressSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space'});
        source.start();
        var plans = source.get_address_types();
        source.once('addresses_defined', function (addresses) {
            address_server.add_address_definition({address:'foo', type:'queue'}, 'address-config-foo');
            source.once('addresses_defined', function (addresses) {
                plans.then(function (types) {
                    var queue = remove_by_name(types, 'queue');
                    assert(queue);
                    assert.equal(queue.plans.length, 3);
                    assert_equal_set(queue.plans.map(plan_name), ['small', 'medium', 'large']);
                    var topic = remove_by_name(types, 'topic');
                    assert(topic);
                    assert.equal(topic.plans.length, 2);
                    assert_equal_set(topic.plans.map(plan_name), ['foo', 'bar']);
                    var anycast = remove_by_name(types, 'anycast');
                    assert(anycast);
                    assert.equal(anycast.plans.length, 1);
                    assert.equal(anycast.plans[0].name, 'standard');
                    assert.equal(anycast.plans[0].displayName, 'display me');
                    assert.equal(anycast.plans[0].shortDescription, 'abcdefg');
                    assert.equal(anycast.plans[0].longDescription, 'hijklmn');
                    assert.equal(types.length, 0);

                    source.watcher.close().then(function () {
                        assert.equal(addresses.length, 1);
                        assert.equal(addresses[0].address, 'foo');
                        assert.equal(addresses[0].type, 'queue');
                        done();
                    }).catch(done);
                }).catch(function (error) {
                    done(error);
                });
            });
        });
    });
});
