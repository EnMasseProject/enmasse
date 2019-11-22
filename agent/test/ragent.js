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
var child_process = require('child_process');
var events = require('events');
var path = require('path');
var util = require('util');
var http = require('http');

var rhea = require('rhea');

var Ragent = require('../lib/ragent.js');
var BrokerAddressSettings = require('../lib/broker_address_settings.js');
var tls_options = require('../lib/tls_options.js');
var MockBroker = require('../testlib/mock_broker.js');
var MockRouter = require('../testlib/mock_router.js');
var match_source_address = require('../lib/utils.js').match_source_address;
var mock_resource_server = require('../testlib/mock_resource_server.js');
var AddressServer = mock_resource_server.AddressServer;
var ResourceServer = mock_resource_server.ResourceServer;
var myutils = require('../lib/utils.js');

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

function broker_state(id) {
    return {clusterId: id, containerId: id + "-0", state: 'Active'};
}

function verify_subscription(name, topic, all_linkroutes, allocated_to) {
    var prefix = topic + '::' + name;
    var linkroutes = remove(all_linkroutes, function (o) { return o.prefix === prefix; });
    assert.equal(linkroutes.length, 1, 'no link route found for subscription ' + name + ' on ' + topic);
    assert.equal(linkroutes[0].prefix, prefix);
    assert.equal(linkroutes[0].direction, 'out');
    if (allocated_to) {
        var containerId = allocated_to[0].containerId;
        assert.equal(linkroutes[0].containerId, containerId + '-out');
    }
}

function verify_topic(name, all_linkroutes, allocated_to) {
    var linkroutes = remove(all_linkroutes, function (o) { return o.prefix === name; });
    assert.equal(linkroutes.length, 2, 'no link routes found for topic ' + name);
    assert.equal(linkroutes[0].prefix, name);
    assert.equal(linkroutes[1].prefix, name);
    if (linkroutes[0].direction === 'in') {
        assert.equal(linkroutes[1].direction, 'out');
        if (allocated_to) {
            var containerId = allocated_to[0].containerId;
            assert.equal(linkroutes[0].containerId, containerId + '-in');
            assert.equal(linkroutes[1].containerId, containerId + '-out');
        }
    } else {
        assert.equal(linkroutes[0].direction, 'out');
        assert.equal(linkroutes[1].direction, 'in');
        if (allocated_to) {
            var containerId = allocated_to[0].containerId;
            assert.equal(linkroutes[0].containerId, containerId + '-out');
            assert.equal(linkroutes[1].containerId, containerId + '-in');
        }
    }
}

function verify_queue(name, all_addresses, all_autolinks, allocated_to) {
    var addresses = remove(all_addresses, function (o) { return o.prefix === name; });
    assert.equal(addresses.length, 1, 'did not find queue ' + name);
    assert.equal(addresses[0].prefix, name);
    assert.equal(addresses[0].distribution, 'balanced');
    assert.equal(addresses[0].waypoint, true);

    var autolinks = remove(all_autolinks, function (o) { return o.address === name; });
    if (allocated_to !== undefined) {
        if (allocated_to[0].state === 'Active') {
            assert.equal(autolinks.length, 2, 'did not find required autolinks for queue ' + name);
            assert.equal(autolinks[0].address, name);
            assert.equal(autolinks[1].address, name);
            if (autolinks[0].direction === 'in') {
                assert.equal(autolinks[1].direction, 'out');
                assert.equal(autolinks[0].containerId, util.format('%s-in', allocated_to[0].containerId));
                assert.equal(autolinks[1].containerId, util.format('%s-out', allocated_to[0].containerId));
            } else {
                assert.equal(autolinks[0].containerId, util.format('%s-out', allocated_to[0].containerId));
                assert.equal(autolinks[1].containerId, util.format('%s-in', allocated_to[0].containerId));
                assert.equal(autolinks[0].direction, 'out');
                assert.equal(autolinks[1].direction, 'in');
            }
        } else {
            assert.equal(1, autolinks.length);
            assert.equal(autolinks[0].containerId, util.format('%s-in', allocated_to[0].containerId || name));
            assert.equal(autolinks[0].direction, 'in');
        }
    } else {
        assert.equal(autolinks.length, 2, 'did not find required autolinks for queue ' + name);
        assert.equal(autolinks[0].address, name);
        assert.equal(autolinks[1].address, name);
        if (autolinks[0].direction === 'in') {
            assert.equal(autolinks[1].direction, 'out');
            assert.equal(autolinks[0].containerId, util.format('%s-in', name));
            assert.equal(autolinks[1].containerId, util.format('%s-out', name));
        } else {
            assert.equal(autolinks[0].containerId, util.format('%s-out', name));
            assert.equal(autolinks[1].containerId, util.format('%s-in', name));
            assert.equal(autolinks[0].direction, 'out');
            assert.equal(autolinks[1].direction, 'in');
        }
    }
}

function verify_anycast(name, all_addresses) {
    var addresses = remove(all_addresses, function (o) { return o.prefix === name; });
    assert.equal(addresses.length, 1);
    assert.equal(addresses[0].prefix, name);
    assert.equal(addresses[0].distribution, 'balanced');
    assert.equal(addresses[0].waypoint, false);
}

function verify_multicast(name, all_addresses) {
    var addresses = remove(all_addresses, function (o) { return o.prefix === name; });
    assert.equal(addresses.length, 1);
    assert.equal(addresses[0].prefix, name);
    assert.equal(addresses[0].distribution, 'multicast');
    assert.equal(addresses[0].waypoint, false);
}

function verify_addresses(inputs, router, verify_extra) {
    if (util.isArray(router)) {
        router.forEach(function (r) { verify_addresses(inputs, r, verify_extra); });
    } else {
        var addresses = router.list_objects('org.apache.qpid.dispatch.router.config.address');
        var linkroutes = router.list_objects('org.apache.qpid.dispatch.router.config.linkRoute');
        var autolinks = router.list_objects('org.apache.qpid.dispatch.router.config.autoLink');
        for (var i = 0; i < inputs.length; i++) {
            var a = inputs[i];
            if (a.type === 'queue') {
                verify_queue(a.address, addresses, autolinks, a.allocated_to);
            } else if (a.type === 'topic') {
                verify_topic(a.address, linkroutes, a.allocated_to);
            } else if (a.type === 'subscription') {
                verify_subscription(a.address, a.topic, linkroutes, a.allocated_to);
            } else if (a.type === 'anycast') {
                verify_anycast(a.address, addresses);
            } else if (a.type === 'multicast') {
                verify_multicast(a.address, addresses);
            } else {
                console.warn('Cannot verify address of type: ' + a.type);
            }
        }
        if (verify_extra) verify_extra({'addresses':addresses, 'linkroutes':linkroutes,'autolinks':autolinks});
        if (addresses.length) console.warn('Unexpected addresses found: ' + JSON.stringify(addresses));
        if (autolinks.length) console.warn('Unexpected auto links found: ' + JSON.stringify(autolinks));
        if (linkroutes.length) console.warn('Unexpected link routes found: ' + JSON.stringify(linkroutes));
        assert.equal(addresses.length, 0);
        assert.equal(linkroutes.length, 0);
        assert.equal(autolinks.length, 0);
    }
}

function get_neighbours(name, connectivity) {
    var neighbours = connectivity[name];
    if (neighbours === undefined) {
        neighbours = [];
        connectivity[name] = neighbours;
    }
    return neighbours;
}

function are_connected(router_a, router_b, n) {
    var from_a = router_a.list_objects('connector').filter(function (c) { return c.host === router_b.name; });
    var from_b = router_b.list_objects('connector').filter(function (c) { return c.host === router_a.name; });
    if (n) {
        if (from_a.length + from_b.length < n) {
            console.warn('insufficient connectivity between ' + router_a.name + ' and ' + router_b.name + ' expected ' + n + ' got ' +
                         (from_a.length + from_b.length) + ': ' + JSON.stringify(from_a.concat(from_b)));
        }
        return from_a.length + from_b.length === n;
    } else {
        if (from_a.length + from_b.length > 1) {
            console.warn('unexpected connectivity between ' + router_a.name + ' and ' + router_b.name + ': ' + JSON.stringify(from_a.concat(from_b)));
        }
    }
    return from_a.length || from_b.length;
}

function verify_full_mesh(routers) {
    for (var i = 0; i < routers.length; i++) {
        for (var j = i+1; j < routers.length; j++) {
            assert.ok(are_connected(routers[i], routers[j]), 'routers not connected: ' + routers[i].name + ' and ' + routers[j].name);
        }
    }
}

function verify_full_mesh_n(routers, n) {
    for (var i = 0; i < routers.length; i++) {
        for (var j = i+1; j < routers.length; j++) {
            assert.ok(are_connected(routers[i], routers[j], n), 'routers not connected: ' + routers[i].name + ' and ' + routers[j].name);
        }
    }
}

function RouterList(port, basename) {
    this.counter = 1;
    this.routers = [];
    this.port = port;
    this.basename = basename || 'router';
}

RouterList.prototype.new_router = function (name, opts) {
    var r = new MockRouter(name || this.new_name(), this.port || 55672, opts);
    this.routers.push(r);
    return r;
};

RouterList.prototype.new_name = function () {
    return this.basename + '-' + this.counter++;
};

RouterList.prototype.close = function () {
    return Promise.all(this.routers.map(function (r) { return r.close(); }));
}

function HealthChecker(conn) {
    this.connection = conn;
    this.address = undefined;
    this.receiver = conn.open_receiver({source:{dynamic:true}});
    this.sender = conn.open_sender('health-check');
    this.counter = 0;
    this.handlers = {};
    this.requests = [];
    this.receiver.on('message', this.incoming.bind(this));
    this.receiver.on('receiver_open', this.ready.bind(this));
    this.connection.on('connection_close', this.closed.bind(this));
    this.connection.on('disconnected', this.disconnected.bind(this));
}

HealthChecker.prototype.check = function (input) {
    var id = this.counter.toString();
    this.counter++;
    var request = {correlation_id:id, subject:'health-check', body:JSON.stringify(input)};
    if (this.address) {
        this._send_pending_requests();
        this._send_request(request);
    } else {
        this.requests.push(request);
    }
    var handlers = this.handlers;
    return new Promise(function (resolve, reject) {
        handlers[id] = function (response) {
            if (response.body !== undefined) {
                resolve(response.body);
            } else {
                reject('failed: ' + response);
            }
        };
    });
}

HealthChecker.prototype.incoming = function (context) {
    var message = context.message;
    var handler = this.handlers[message.correlation_id];
    if (handler) {
        handler(message);
        delete this.handlers[message.correlation_id];
    }
};

HealthChecker.prototype._abort_requests = function (error) {
    for (var h in this.handlers) {
        this.handlers[h](error);
        delete this.handlers[h];
    }
    while (this.requests.length > 0) { this.requests.shift(); };
};

HealthChecker.prototype.disconnected = function (context) {
    this.address = undefined;
    this._abort_requests('disconnected');
};

HealthChecker.prototype.closed = function (context) {
    this.connection = undefined;
    this.sender = undefined;
    this.address = undefined;
    this._abort_requests('closed '+ context.connection.error);
};

HealthChecker.prototype.ready = function (context) {
    this.address = context.receiver.source.address;
    this._send_pending_requests();
};

HealthChecker.prototype._send_pending_requests = function () {
    for (var i = 0; i < this.requests.length; i++) {
        this._send_request(this.requests[i]);
    }
    this.requests = [];
};

HealthChecker.prototype._send_request = function (request) {
    if (this.sender) {
        request.reply_to = this.address;
        this.sender.send(request);
    }
};

function generate_address_list(count, allowed_types) {
    var types = allowed_types || ['anycast', 'multicast', 'queue', 'topic'];
    var list = [];
    for (var i = 0; i < count; i++) {
        list.push({address:util.format('address-%s', (i+1)), type:types[i % types.length]});
    }
    return list;
}

describe('basic router configuration', function() {
    this.timeout(10000);
    var ragent;
    var routers;

    beforeEach(function(done) {
        ragent = new Ragent();
        ragent.listen({port:0}).on('listening', function (){
            routers = new RouterList(ragent.server.address().port);
            done();
        });
    });

    afterEach(function(done) {
        routers.close().then(function () {
            ragent.server.close();
            done();
        });
    });

    function new_router(name) {
        return routers.new_router(name);
    }

    function multi_router_address_test(count, address_list, verification, initial_config, propagation_wait) {
        return function(done) {
            var routers = [];
            for (var i = 0; i < count; i++) {
                routers.push(new_router());
            }
            if (initial_config) initial_config(routers);
            ragent.sync_addresses(address_list);
            ragent.wait_for_stable(address_list.length, routers.length).then(function () {
                verification ? verification(routers, address_list) : verify_addresses(address_list, routers);
                done();
            }).catch(function (error) {
                console.error('Failed: %s', error);
            });
        };
    }

    function simple_address_test(address_list, verification, initial_config, propagation_wait) {
        return multi_router_address_test(1, address_list, function (routers, address_list) {
            verification ? verification(routers[0], address_list) : verify_addresses(address_list, routers[0]);
        }, function (routers) {
            if (initial_config) initial_config(routers[0]);
        }, propagation_wait);
    }

    it('configures a single anycast address', simple_address_test([{address:'foo',type:'anycast'}]));
    it('configures a single multicast address', simple_address_test([{address:'foo',type:'multicast'}]));
    it('configures a single queue address', simple_address_test([{address:'foo',type:'queue'}]));
    it('configures a single topic address', simple_address_test([{address:'foo',type:'topic'}]));
    it('configures a topic and subscription', simple_address_test([{address:'foo',type:'topic'}, {address:'sub',type:'subscription',topic:'foo','allocated_to':[broker_state('broker-0')]}]));
    it('configures multiple anycast addresses', simple_address_test([{address:'a',type:'anycast'}, {address:'b',type:'anycast'}, {address:'c',type:'anycast'}]));
    it('configures multiple multicast addresses', simple_address_test([{address:'a',type:'multicast'}, {address:'b',type:'multicast'}, {address:'c',type:'multicast'}]));
    it('configures multiple topics', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'topic'}, {address:'c',type:'topic'}]));
    it('configures multiple topics and subscriptions', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'topic'}, {address:'c',type:'topic'},
                                                                            {address:'sub-a',type:'subscription',topic:'a','allocated_to':[broker_state('broker-0')]},
                                                                            {address:'sub-b',type:'subscription',topic:'b','allocated_to':[broker_state('broker-1')]},
                                                                            {address:'sub-b2',type:'subscription',topic:'b','allocated_to':[broker_state('broker-2')]},
                                                                            {address:'sub-c',type:'subscription',topic:'c','allocated_to':[broker_state('broker-3')]}]));
    it('configures multiple queues', simple_address_test([{address:'a',type:'queue'}, {address:'b',type:'queue'}, {address:'c',type:'queue'}]));
    it('configures autolinks based on drain state', simple_address_test([{address:'c',type:'queue', allocated_to:[{clusterId: 'broker-1', containerId: 'broker-1-0', state: 'Draining'}]}]));
    it('configures queues based on allocation', simple_address_test([{address:'a',type:'queue'}, {address:'b',type:'queue', allocated_to:[broker_state('broker-1')]}, {address:'c',type:'queue', allocated_to:[broker_state('broker-1')]}]));
    it('configures topic based on allocation', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'topic', allocated_to:[broker_state('broker-1')]}, {address:'c',type:'topic', allocated_to:[broker_state('broker-1')]}]));
    it('configures multiple different types of address', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}]));
    it('removes unwanted address config', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'ragent-foo', {prefix:'foo', distribution:'closest', 'waypoint':false});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'ragent-bar', {prefix:'bar', direction:'in'});
           router.create_object('org.apache.qpid.dispatch.router.config.autoLink', 'ragent-baz', {address:'baz', direction:'out', containerId: 'baz'});
       }));
    it('removes or updates address config', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'ragent-a', {prefix:'a', distribution:'closest', 'waypoint':false});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'ragent-b-in', {prefix:'b', direction:'in'});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'ragent-b-out', {prefix:'b', direction:'out'});
           router.create_object('org.apache.qpid.dispatch.router.config.autoLink', 'ragent-baz', {address:'baz', direction:'out', containerId: 'baz'});
       }));
    it('configures addresses on multiple routers', multi_router_address_test(3, [{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}]));
    it('configures multiple routers into a full mesh', multi_router_address_test(6, [], function (routers) {
        verify_full_mesh(routers);
    }));
    it('configures routers correctly whenever they connect', function(done) {
        ragent.sync_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
        var routers = [];
        for (var i = 0; i < 2; i++) {
            routers.push(new_router());
        }
        ragent.wait_for_stable(2, routers.length).then(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
            verify_full_mesh(routers);
            //update addresses
            ragent.sync_addresses([{address:'a',type:'queue'}, {address:'c',type:'topic'}, {address:'d',type:'multicast'}]);
            //add another couple of routers
            for (var i = 0; i < 2; i++) {
                routers.push(new_router());
            }
            ragent.wait_for_stable(3, routers.length).then(function () {
                verify_addresses([{address:'a',type:'queue'}, {address:'c',type:'topic'}, {address:'d',type:'multicast'}], routers);
                verify_full_mesh(routers);
                done();
            }).catch(done);
        }).catch(done);
    });
    it('handles health-check requests', function(done) {
        var routers = [];
        for (var i = 0; i < 2; i++) {
            routers.push(new_router());
        }
        routers[1].on('connected', function () {
            var client = rhea.connect({port:ragent.server.address().port});
            var checker = new HealthChecker(client);
            checker.check([{name:'x'}, {name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                assert.equal(result, false);
                ragent.sync_addresses([{address:'a',type:'topic'}]);
                ragent.wait_for_stable(1, routers.length).then(function () {
                    checker.check([{name:'x'}, {name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                        assert.equal(result, false);
                        ragent.sync_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
                        ragent.wait_for_stable(2, routers.length).then(function () {
                            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
                            verify_full_mesh(routers);
                            checker.check([{name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                                assert.equal(result, true);
                                client.close();
                                client.on('connection_close', function () { done(); } );
                            }).catch(done);
                        }).catch(done);
                    }).catch(done);
                }).catch(done);
            }).catch(done);
        });
    });
    it('handles disconnection of router', function(done) {
        var routers = [];
        for (var i = 0; i < 6; i++) {
            routers.push(new_router());
        }
        ragent.sync_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
        ragent.wait_for_stable(2, routers.length).then(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
            verify_full_mesh(routers);
            //make sure we close a router which some remaining router still has a connector to
            var r;
            for (var i = 0; i < routers.length; i++) {
                if (routers[i].check_connector_from(routers)) {
                    r = routers[i];
                    routers.splice(i, 1);
                    break;
                }
            }
            r.close_with_error({name:'amqp:internal-error', description:'just a test'}).then(function () {
                routers.push(new_router());
                ragent.wait_for_stable(2, routers.length).then(function () {
                    verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
                    verify_full_mesh(routers);
                    done();
                }, 500);
            });
        }).catch(done);
    });
    it('handles invalid messages', function(done) {
        var routers = [];
        for (var i = 0; i < 2; i++) {
            routers.push(new_router());
        }
        var client = rhea.connect({port:ragent.server.address().port});
        var sender = client.open_sender();
        sender.send({subject:'enmasse.io/v1/AddressList'});
        sender.send({subject:'enmasse.io/v1/AddressList', body:'foo'});
        sender.send({subject:'enmasse.io/v1/AddressList', body:'{{{'});
        sender.send({subject:'enmasse.io/v1/AddressList', body:'{"items":101}'});
        sender.send({subject:'health-check'});
        sender.send({subject:'random-nonsense'});
        ragent.sync_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
        ragent.wait_for_stable(2, routers.length).then(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
            verify_full_mesh(routers);
            done();
        }).catch(done);
    });
    it('handles address query error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('QUERY','org.apache.qpid.dispatch.router.config.address');
       }));
    it('handles address auto-link error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('QUERY','org.apache.qpid.dispatch.router.config.autoLink');
       }));
    it('handles address link-route error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('QUERY','org.apache.qpid.dispatch.router.config.linkRoute');
       }));
    it('handles connector query error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('QUERY','connector');
       }));
    it('handles listener query error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('QUERY','listener');
       }));
    it('handles address create error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('CREATE','org.apache.qpid.dispatch.router.config.address');
       }));
    it('handles address create error 2', simple_address_test([{address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.set_onetime_error_response('CREATE','org.apache.qpid.dispatch.router.config.address');
       }));
    it('handles address delete error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'ragent-foo', {prefix:'foo', distribution:'closest', 'waypoint':false});
           router.set_onetime_error_response('DELETE','org.apache.qpid.dispatch.router.config.address');
       }));
    it('handles connector create error', multi_router_address_test(2, [{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (routers) {
           routers[0].set_onetime_error_response('CREATE','connector');
           routers[1].set_onetime_error_response('CREATE','connector');
       }));
    it('establishes multiple connectors between routers', function(done) {
        process.env.ROUTER_NUM_CONNECTORS=2;
        var routers = [];
        for (var i = 0; i < 3; i++) {
            routers.push(new_router());
        }
        var address_list = [{address:'a',type:'topic'}, {address:'b',type:'queue'}];
        ragent.sync_addresses(address_list);
        ragent.wait_for_stable(address_list.length, routers.length).then(function () {
            verify_full_mesh_n(routers, 2);
            delete process.env.ROUTER_NUM_CONNECTORS;
            done();
        }).catch(done);
    });
    it('ignores link route override', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}],
       function (routers, address_list) {
           verify_addresses(address_list, routers, function (objects) {
               var extra_linkroutes = remove(objects.linkroutes, function (o) { return o.prefix === 'foo' && o.direction === 'in'; });
               assert.equal(extra_linkroutes.length, 1);
               assert.equal(extra_linkroutes[0].name, 'override-foo');
           });
       },
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'override-foo', {prefix:'foo', direction:'in'});
       }));

    it('handles message with no correlation', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}], undefined,
        function (router) {
            var f = function (request, response, context) {
                var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, request.reply_to); });
                if (reply_link) {
                    reply_link.send({to:request.reply_to, subject:'fake message with no correlation'});
                }
                delete this.special;
                return false;
            };
            router.special = f.bind(router);
        }));
    it('handles query response with no body', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}], undefined, function (router) {
        var f = function (request, response) {
            if (request.application_properties.operation === 'QUERY'
                && request.application_properties.entityType === 'org.apache.qpid.dispatch.router.config.address') {

                response.application_properties = {};
                response.application_properties.statusCode = 200;
                response.correlation_id = request.correlation_id;
                delete this.special;
                return true;
            } else {
                return false;
            }
        };
        router.special = f.bind(router);
    }));
    it('configures large number of anycast addresses', simple_address_test(generate_address_list(2000, ['anycast']), undefined, undefined, 6000));
    it('configures large number of multicast addresses', simple_address_test(generate_address_list(2000, ['multicast']), undefined, undefined, 6000));
    it('configures large number of queues', simple_address_test(generate_address_list(2000, ['queue']), undefined, undefined, 6000));
    it('configures large number of topics', simple_address_test(generate_address_list(2000, ['topic']), undefined, undefined, 6000));
    it('configures large number of mixed addresses', simple_address_test(generate_address_list(2000), undefined, undefined, 6000));
});

function localpath(name) {
    return path.resolve(__dirname, name);
}

describe('configuration from configmaps', function() {
    this.timeout(5000);
    var ragent;
    var address_source;
    var routers;
    var watcher;

    beforeEach(function(done) {
        address_source = new AddressServer();
        address_source.listen(0).on('listening', function () {
            ragent = new Ragent();
            watcher = ragent.subscribe_to_addresses({token:'foo', namespace:'default', host: 'localhost', port:address_source.port});
            ragent.listen({port:0}).on('listening', function (){
                routers = new RouterList(ragent.server.address().port);
                done();
            });
        });
    });

    afterEach(function(done) {
        watcher.close().then(function () {
            routers.close().then(function () {
                ragent.server.close();
                address_source.close();
                done();
            });
        });
    });

    it('retrieves initial list of addresses', function (done) {
        var router = routers.new_router();
        address_source.add_address_definition({address:'a', type:'topic'}, undefined, '1234');
        address_source.add_address_definition({address:'b', type:'queue'}, undefined, '1234');
        ragent.wait_for_stable(2).then(function () {
            verify_addresses([{address: 'a', type: 'topic'}, {address: 'b', type: 'queue'}], router);
            done();
        }).catch(done);
    });
    it('watches for new addresses', function (done) {
        var router = routers.new_router();
        address_source.add_address_definition({address:'a', type:'topic'}, undefined, '1234');
        address_source.add_address_definition({address:'b', type:'queue'}, undefined, '1234');
        ragent.wait_for_stable(2).then(function () {
            address_source.add_address_definition({address:'c', type:'anycast'}, undefined, '1234');
            address_source.add_address_definition({address:'d', type:'queue'}, undefined, '1234');
            //address_source.remove_resource_by_name('b');
            ragent.wait_for_stable(4).then(function () {
                verify_addresses([{address:'a', type:'topic'}, {address:'b', type:'queue'}, {address:'c', type:'anycast'}, {address:'d', type:'queue'}], router);
                done();
            }).catch(done);
        }).catch(done);
    });
    it('watches for deleted addresses', function (done) {
        var router = routers.new_router();
        address_source.add_address_definition({address:'a', type:'topic'}, undefined, '1234');
        address_source.add_address_definition({address:'b', type:'queue'}, undefined, '1234');
        ragent.wait_for_stable(2).then(function () {
            address_source.add_address_definition({address:'c', type:'anycast'}, undefined, '1234');
            address_source.add_address_definition({address:'d', type:'queue'}, undefined, '1234');
            address_source.remove_resource_by_name('addresses', 'b');
            ragent.wait_for_stable(3).then(function () {
                verify_addresses([{address:'a', type:'topic'}, {address:'c', type:'anycast'}, {address:'d', type:'queue'}], router);
                done();
            }).catch(done);
        }).catch(done);
    });
    it('ignores pending and terminating addresses', function (done) {
        var router = routers.new_router();
        address_source.add_address_definition({address:'a', type:'queue'}, undefined, '1234');
        address_source.add_address_definition({address:'b', type:'queue'}, undefined, '1234', undefined, {phase:'Pending'});
        address_source.add_address_definition({address:'c', type:'topic'}, undefined, '1234', undefined, {phase:'Terminating'});
        address_source.add_address_definition({address:'d', type:'topic'}, undefined, '1234');
        ragent.wait_for_stable(2).then(function () {
            verify_addresses([{address:'a', type:'queue'}, {address:'d', type:'topic'}], router);
            done();
        }).catch(done);
    });
});

function RouterGroup (name) {
    this.name = name;
    this.ragent = new Ragent();
    this.routers = undefined;
    this.port = undefined;
}

RouterGroup.prototype.listen = function () {
    var self = this;
    return new Promise(function(resolve, reject) {
        self.ragent.listen({port:0}).on('listening', resolve);
    }).then(function () {
        self.port = self.ragent.server.address().port;
        self.routers = new RouterList(self.port, self.name);
    });
};

RouterGroup.prototype.close = function () {
    var self = this;
    return this.routers.close().then(function () {
        if (self.ragent.watcher) self.ragent.watcher.close();
        self.ragent.server.close();
    });
};

RouterGroup.prototype.pod = function () {
    return {
        ready : (this.port === undefined ? 'False' : 'True'),
        phase : 'Running',
        name : this.name,
        host : 'localhost',
        port : this.port
    };
};

RouterGroup.prototype.get_pod_definition = function () {
    return {
        metadata: {
            name: this.name,
            labels: {
                name: 'admin'
            }
        },
        spec: {
            containers: [
                {
                    name: 'ragent',
                    ports: [
                        {
                            name: 'amqp',
                            containerPort: this.port
                        }
                    ]
                }
            ]
        },
        status: {
            podIP: '127.0.0.1',
            phase: 'Running',
            conditions : [
                { type: 'Initialized', status: 'True' },
                { type: 'Ready', status: (this.port === undefined ? 'False' : 'True') }
            ]
        }
    };
};

function fill(n, f) {
    return Array.apply(null, Array(n)).map(function (v, i) { return f(i); });
}

describe('cooperating ragent group', function() {
    this.timeout(5000);
    var counter;
    var groups;
    var podserver;

    beforeEach(function(done) {
        counter = 0;
        groups = fill(4, function (i) { return new RouterGroup('ragent-' + (i+1)); });
        podserver = new ResourceServer(true);
        Promise.all(groups.map(function (g) { return g.listen(); })).then(function (){
            podserver.listen(0).on('listening', function () {
                done();
            });
        });
    });

    afterEach(function(done) {
        Promise.all(groups.map(function (g) {return g.close()})).then(function () {
            podserver.close();
            done();
        });
    });

    function new_router() {
        return groups[counter++ % groups.length].routers.new_router();
    }

    it('establishes full mesh', function (done) {
        //connect some routers
        var routers = [];
        routers = routers.concat(fill(3, new_router));
        //inform ragent instances of each other
        groups.forEach(function (g) {
            g.ragent.sync_addresses([]);
            podserver.add_resource('pods', g.get_pod_definition());
        });
        groups.forEach(function(g) {
            g.ragent.watch_pods({port:podserver.port, token:'foo', namespace:'default'});
        });
        //connect some more routers
        routers = routers.concat(fill(3, new_router));
        //wait for some time for things to settle, then verify we have a full mesh
        setTimeout(function () {
            verify_full_mesh(routers);
            done();
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
    it('handles removal of other ragents', function (done) {
        //connect some routers
        var routers = groups.map(function (g) {
            return new Array(g.routers.new_router(), g.routers.new_router());
        });
        //inform ragent instances of each other
        groups.forEach(function (g) {
            g.ragent.sync_addresses([]);
            podserver.add_resource('pods', g.get_pod_definition());
        });
        groups.forEach(function(g) {
            g.ragent.watch_pods({port:podserver.port, token:'foo', namespace:'default'});
        });
        //wait for some time for things to settle, then verify we have a full mesh
        setTimeout(function () {
            verify_full_mesh([].concat.apply([], routers));
            var leaving = groups.pop();
            podserver.remove_resource_by_name('pods', leaving.name);
            //shutdown one of the ragents
            routers.pop();
            leaving.close().then(function() {
                setTimeout(function () {
                    verify_full_mesh([].concat.apply([], routers));
                    done();
                }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
});

describe('health support', function() {
    var ragent, server, port;

    beforeEach(function(done) {
        ragent = new Ragent();
        server = ragent.listen_health({HEALTH_PORT:0});
        server.on('listening', function () {
            port = server.address().port;
            done();
        });
    });

    afterEach(function(done) {
        server.close();
        done();
    });

    it('responds to health request', function (done) {
        http.get('http://localhost:' + port, function (response) {
            assert.equal(response.statusCode, 200);
            done();
        });
    });
});

function merge(a, b) {
    var result = {};
    for (var ka in a) {
        result[ka] = a[ka];
    }
    for (var kb in b) {
        result[kb] = b[kb];
    }
    return result;
}

function mock_address_source(addresses) {
    var server = new AddressServer();
    for (var i = 0; i < addresses.length; i++) {
        server.add_address_definition(addresses[i], undefined, '1234');
    }
    return server;
}

describe('forked process', function() {
    this.timeout(5000);
    var ragent;
    var address_source = mock_address_source([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;

    beforeEach(function(done) {
        address_source.listen(0).on('listening', function () {
            var env =  merge(process.env, {'LOGLEVEL':'info', 'DEBUG':'ragent*', 'AMQP_PORT':0, 'INFRA_UUID': '1234', token:'foo', host: 'localhost', 'port':address_source.port, namespace:'default'});
            ragent = child_process.fork(path.resolve(__dirname, '../lib/ragent.js'), [], {silent:true, 'env':env});
            ragent.stderr.on('data', function (data) {
                if (data.toString().match('Router agent listening on')) {
                    var matches = data.toString().match(/on (\d+)/);
                    var port = matches[1];
                    routers = new RouterList(port);
                    done();
                }
            });
        });
    });

    afterEach(function(done) {
        ragent.kill();
        address_source.close();
        done();
    });

    it('subscribes to configuration service', function (done) {
        var router = routers.new_router();
        setTimeout(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], router);
            router.close().then(function () {
                done();
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
});


describe('run method', function() {
    this.timeout(5000);
    var ragent;
    var address_source = mock_address_source([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;
    var watcher;

    beforeEach(function(done) {
        address_source.listen(0).on('listening', function() {
            var env =  {'AMQP_PORT':0, 'port':address_source.port, token:'foo', namespace:'default'};
            ragent = new Ragent();
            watcher = ragent.run(env, function (port) {
                routers = new RouterList(port);
                done();
            });
        });
    });

    afterEach(function(done) {
        watcher.close();
        ragent.server.close();
        address_source.close();
        done();
    });

    it('subscribes to configuration service', function (done) {
        var router = routers.new_router();
        ragent.wait_for_stable(2).then(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], router);
            router.close().then(function () {
                done();
            });
        }).catch(done);
    });
});

function delay(f, time) {
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
            f();
            resolve();
        }, time);
    });
}


describe('changing router configuration', function() {
    this.timeout(5000);
    var ragent;
    var routers;

    beforeEach(function(done) {
        ragent = new Ragent();
        ragent.listen({port:0}).on('listening', function (){
            routers = new RouterList(ragent.server.address().port);
            done();
        });
    });

    afterEach(function(done) {
        routers.close().then(function () {
            ragent.server.close();
            done();
        });
    });

    function new_router(name) {
        return routers.new_router(name);
    }

    it('deletes and recreates a topic', function (done) {
        var router = new_router();
        var client = rhea.connect({port:ragent.server.address().port});
        client.on('connection_close', function () { done(); } );

        ragent.sync_addresses([{address:'mytopic',type:'topic'}]);
        delay(function () {
            ragent.sync_addresses([]);
        }, 200).then(function () {
            delay(function () {
                verify_addresses([], router);
                ragent.sync_addresses([{address:'mytopic',type:'topic'}]);
            }, 200).then(function () {
                ragent.wait_for_stable(1).then(function () {
                    verify_addresses([{address:'mytopic',type:'topic'}], router);
                    client.close();
                }).catch(done);
            });
        });
    });
});

describe('broker configuration', function() {
    this.timeout(6000);
    var ragent;
    var address_source;
    var routers;
    var port;
    var connections;
    var watcher;

    beforeEach(function(done) {
        address_source = new AddressServer();
        connections = [];
        address_source.listen(0).on('listening', function () {
            ragent = new Ragent();
            watcher = ragent.subscribe_to_addresses({token:'foo', namespace:'default', host: 'localhost', port:address_source.port});
            ragent.listen({port:0}).on('listening', function (){
                port = ragent.server.address().port;
                routers = new RouterList(ragent.server.address().port);
                done();
            });
        });
    });

    function close(connection) {
        if (connection.is_open()) {
            return new Promise(function (resolve) {
                try {
                    connection.close();
                } catch (error) {
                    console.error(error);
                    resolve();
                }
                connection.on('connection_close', resolve);
                connection.on('connection_error', resolve);
            });
        } else {
            return Promise.resolve();
        }
    }

    afterEach(function(done) {
        watcher.close().then(function () {
            setTimeout(function () {
                Promise.all(connections.map(close)).then(function () {
                    routers.close().then(function () {
                        try {
                            ragent.server.close();
                            address_source.close();
                        } catch (error) {
                            console.error(error);
                        }
                        done();
                    }).catch(done);
                }).catch(done);
            }, 500);
        }).catch(done);
    });

    function connect_broker(broker) {
        var conn = broker.connect(port);
        connections.push(conn);
        return new Promise(function (resolve, reject) {
            conn.once('connection_open', function () {
                resolve();
            });
        });
    }

    it('creates queues on associated brokers', function (done) {
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a-0');
        var broker_b = new MockBroker('broker_b-0');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            address_source.add_address_definition({address:'a', type:'queue'}, undefined, '1234', {'cluster_id': 'broker_a', 'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'b', type:'queue'}, undefined, '1234', {'cluster_id': 'broker_b', 'enmasse.io/broker-id':'broker_b-0'});
            ragent.wait_for_stable(2, 1, 2).then(function () {
                verify_addresses([{address:'a', type:'queue', allocated_to:[broker_state('broker_a')]}, {address:'b', type:'queue', allocated_to:[broker_state('broker_b')]}], router);
                //verify queues on respective brokers:
                broker_a.verify_addresses([{address:'a', type:'queue'}]);
                broker_b.verify_addresses([{address:'b', type:'queue'}]);
                done();
            }).catch(done);
        }).catch(done);
    });

    it('deletes queues from associated brokers', function (done) {
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a-0');
        var broker_b = new MockBroker('broker_b-0');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            address_source.add_address_definition({address:'a', type:'queue'}, 'address-config-a', '1234', {'cluster_id': 'broker_a', 'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'b', type:'queue'}, 'address-config-b', '1234', {'cluster_id': 'broker_b', 'enmasse.io/broker-id':'broker_b-0'});
            address_source.add_address_definition({address:'c', type:'queue'}, 'address-config-c', '1234', {'cluster_id': 'broker_a', 'enmasse.io/broker-id':'broker_a-0'});
            ragent.wait_for_stable(3, 1, 2).then(function () {
                verify_addresses([{address:'a', type:'queue', allocated_to:[broker_state('broker_a')]}, {address:'b', type:'queue', allocated_to:[broker_state('broker_b')]}, {address:'c', type:'queue', allocated_to:[broker_state('broker_a')]}], router);
                //verify queues on respective brokers:
                broker_a.verify_addresses([{address:'a', type:'queue'}, {address:'c', type:'queue'}]);
                broker_b.verify_addresses([{address:'b', type:'queue'}]);
                //delete configmap
                address_source.remove_resource_by_name('addresses', 'address-config-a');
                ragent.wait_for_stable(2, 1, 2).then(function () {
                    verify_addresses([{address:'b', type:'queue', allocated_to:[broker_state('broker_b')]}, {address:'c', type:'queue', allocated_to:[broker_state('broker_a')]}], router);
                    broker_a.verify_addresses([{address:'c', type:'queue'}]);
                    broker_b.verify_addresses([{address:'b', type:'queue'}]);
                    done();
                }).catch(done);
            }).catch(done);
        });
    });

    it('creates subscriptions on associated brokers', function (done) {
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a-0');
        var broker_b = new MockBroker('broker_b-0');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            address_source.add_address_definition({address:'a', type:'topic'}, undefined, '1234', {'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'b', type:'topic'}, undefined, '1234', {'enmasse.io/broker-id':'broker_b-0'});
            address_source.add_address_definition({address:'sub-a', type:'subscription', topic:'a'}, undefined, '1234', {'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'sub-b', type:'subscription', topic:'b'}, undefined, '1234', {'enmasse.io/broker-id':'broker_b-0'});
            ragent.wait_for_stable(4, 1, 2).then(function () {
                verify_addresses([{address:'a', type:'topic', allocated_to:[broker_state('broker_a')]}, {address:'b', type:'topic', allocated_to:[broker_state('broker_b')]},
                                  {address:'sub-a', type:'subscription', topic:'a', allocated_to:[broker_state('broker_a')]},
                                  {address:'sub-b', type:'subscription', topic:'b', allocated_to:[broker_state('broker_b')]}], router);
                //verify queues on respective brokers:
                broker_a.verify_addresses([{address:'a', type:'topic'}, {address:'sub-a', type:'subscription', topic:'a'}]);
                broker_b.verify_addresses([{address:'b', type:'topic'}, {address:'sub-b', type:'subscription', topic:'b'}]);
                done();
            }).catch(done);
        }).catch(done);
    });

    it('handles subscriptions and topics in correct order', function (done) {
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a-0');
        var broker_b = new MockBroker('broker_b-0');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            address_source.add_address_definition({address:'sub-a', type:'subscription', topic:'topic-a'}, undefined, '1234', {'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'sub-b', type:'subscription', topic:'topic-b'}, undefined, '1234', {'enmasse.io/broker-id':'broker_b-0'});
            address_source.add_address_definition({address:'topic-a', type:'topic'}, undefined, '1234', {'enmasse.io/broker-id':'broker_a-0'});
            address_source.add_address_definition({address:'topic-b', type:'topic'}, undefined, '1234', {'enmasse.io/broker-id':'broker_b-0'});
            ragent.wait_for_stable(4, 1, 2).then(function () {
                verify_addresses([{address:'topic-a', type:'topic', allocated_to:[broker_state('broker_a')]}, {address:'topic-b', type:'topic', allocated_to:[broker_state('broker_b')]},
                                  {address:'sub-a', type:'subscription', topic:'topic-a', allocated_to:[broker_state('broker_a')]},
                                  {address:'sub-b', type:'subscription', topic:'topic-b', allocated_to:[broker_state('broker_b')]}], router);
                //verify queues on respective brokers:
                broker_a.verify_addresses([{address:'topic-a', type:'topic'}, {address:'sub-a', type:'subscription', topic:'topic-a'}]);
                broker_b.verify_addresses([{address:'topic-b', type:'topic'}, {address:'sub-b', type:'subscription', topic:'topic-b'}]);
                done();
            }).catch(done);
        }).catch(done);
    });

    it('handles broker disconnection', function (done) {
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a');
        var broker_b = new MockBroker('broker_b');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            assert(ragent.connected_brokers['broker_a'] !== undefined);
            assert(ragent.connected_brokers['broker_b'] !== undefined);
            ragent.wait_for_stable(0, 1, 2).then(function () {
                connections[1].close();
                ragent.wait_for_stable(0, 1, 1).then(function () {
                    assert(ragent.connected_brokers['broker_a'] !== undefined, 'broker-a SHOULD be in connected_broker map');
                    assert(ragent.connected_brokers['broker_b'] === undefined, 'broker-b should NOT be in connected_broker map');
                    done();
                }).catch(done);
            }).catch(done);
        });
    });

    it('creates lots of queues on associated brokers', function (done) {
        this.timeout(60000);
        var router = routers.new_router();
        var broker_a = new MockBroker('broker_a-0');
        var broker_b = new MockBroker('broker_b-0');
        Promise.all([connect_broker(broker_a), connect_broker(broker_b)]).then(function () {
            var desired = generate_address_list(2000, ['queue']);
            desired.forEach(function (a, i) {
                var allocated_to = i % 2 ? 'broker_a' : 'broker_b';
                a.allocated_to = [broker_state(allocated_to)];
                address_source.add_address_definition(a, undefined, '1234', {'cluster_id': allocated_to, 'enmasse.io/broker-id': allocated_to + "-0"});
            });
            ragent.wait_for_stable(2000, 1, 2).then(function () {
                verify_addresses(desired, router);
                //verify queues on respective brokers:
                broker_a.verify_addresses(desired.filter(function (a) { return a.allocated_to[0].containerId === 'broker_a-0'; }));
                broker_b.verify_addresses(desired.filter(function (a) { return a.allocated_to[0].containerId === 'broker_b-0'; }));
                done();
            }).catch(done);
        }).catch(done);
    });
});
