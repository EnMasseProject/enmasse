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

var Promise = require('bluebird');
var rhea = require('rhea');

var Ragent = require('../ragent.js');
var tls_options = require('../tls_options.js');

function MockRouter (name, port, opts) {
    this.name = name;
    events.EventEmitter.call(this);
    var options = {'port':port, container_id:name, properties:{product:'qpid-dispatch-router'}};
    for (var o in opts) {
        options[o] = opts[o];
    }
    this.connection = rhea.connect(options);
    this.connection.on('message', this.on_message.bind(this));
    var self = this;
    this.connection.on('connection_open', function () { self.emit('connected', self); });
    this.connection.on('sender_open', function (context) {
        if (context.sender.source.dynamic) {
            var id = rhea.generate_uuid();
            context.sender.set_source({address:id});
        }
    });
    this.objects = {};
    this.create_object('listener', 'default', {name:'default', host:name, 'port':port, role:'inter-router'});
}

util.inherits(MockRouter, events.EventEmitter);

function match_source_address(link, address) {
    return link && link.local && link.local.attach && link.local.attach.source
        && link.local.attach.source.value[0].toString() === address;
}

MockRouter.prototype.create_object = function (type, name, attributes)
{
    if (this.objects[type] === undefined) {
        this.objects[type] = {};
    }
    this.objects[type][name] = (attributes === undefined) ? {} : attributes;
    this.objects[type][name].name = name;
    this.objects[type][name].id = name;
};

MockRouter.prototype.delete_object = function (type, name)
{
    delete this.objects[type][name];
};

MockRouter.prototype.list_objects = function (type)
{
    var results = [];
    for (var key in this.objects[type]) {
        results.push(this.objects[type][key]);
    }
    return results;
};

MockRouter.prototype.close = function ()
{
    if (this.connection && !this.connection.is_closed()) {
        this.connection.close();
        var conn = this.connection;
        return new Promise(function(resolve, reject) {
            conn.on('connection_close', resolve);
        });
    } else {
        return Promise.resolve();
    }
};

MockRouter.prototype.close_with_error = function (error)
{
    if (this.connection && !this.connection.is_closed()) {
        this.connection.local.close.error = error;
    }
    return this.close();
};

MockRouter.prototype.has_connector_to = function (other) {
    return this.list_objects('connector').some(function (c) { return c.host === other.name; });
};

MockRouter.prototype.check_connector_from = function (others) {
    var self = this;
    return others.some(function (router) { return router !== self && router.has_connector_to(self); });
};

function get_attribute_names(objects) {
    var names = {};
    for (var i in objects) {
        for (var f in objects[i]) {
            names[f] = f;
        }
    }
    return Object.keys(names);
}

function query_result(names, objects) {
    var results = [];
    for (var j in objects) {
        var record = [];
        for (var i = 0; i < names.length; i++) {
            record.push(objects[j][names[i]]);
        }
        results.push(record);
    }
    return {attributeNames:names, 'results':results};
}

MockRouter.prototype.on_message = function (context)
{
    var request = context.message;
    var reply_to = request.reply_to;
    var response = {to: reply_to};
    if (!(this.special && this.special(request, response, context))) {// the 'special' method when defined lets errors be injected
        if (request.correlation_id) {
            response.correlation_id = request.correlation_id;
        }
        response.application_properties = {};

        if (request.application_properties.operation === 'CREATE') {
            response.application_properties.statusCode = 201;
            this.create_object(request.application_properties.type, request.application_properties.name, request.body);
        } else if (request.application_properties.operation === 'DELETE') {
            response.application_properties.statusCode = 204;
            this.delete_object(request.application_properties.type, request.application_properties.name);
        } else if (request.application_properties.operation === 'QUERY') {
            response.application_properties.statusCode = 200;
            var results = this.list_objects(request.application_properties.entityType);
            var attributes = request.body.attributeNames;
            if (!attributes || attributes.length === 0) {
                attributes = get_attribute_names(results);
            }
            response.body = query_result(attributes, results);
        }
    }

    var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, reply_to); });
    if (reply_link) {
        reply_link.send(response);
    }
};

MockRouter.prototype.set_onetime_error_response = function (operation, type, name) {
    var f = function (request, response) {
        if ((operation === undefined || request.application_properties.operation === operation)
            && (type === undefined || (operation === 'QUERY' && request.application_properties.entityType === type) || request.application_properties.type === type)
            && (name === undefined || request.application_properties.name === name)) {
            response.application_properties = {};
            response.application_properties.statusCode = 500;
            response.application_properties.statusDescription = 'error simulation';
            response.correlation_id = request.correlation_id;
            delete this.special;
            return true;
        } else {
            return false;
        }
    };
    this.special = f.bind(this);
};

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

function verify_topic(name, all_linkroutes) {
    var linkroutes = remove(all_linkroutes, function (o) { return o.prefix === name; });
    assert.equal(linkroutes.length, 2);
    assert.equal(linkroutes[0].prefix, name);
    assert.equal(linkroutes[1].prefix, name);
    if (linkroutes[0].dir === 'in') {
        assert.equal(linkroutes[1].dir, 'out');
    } else {
        assert.equal(linkroutes[0].dir, 'out');
        assert.equal(linkroutes[1].dir, 'in');
    }
}

function verify_queue(name, all_addresses, all_autolinks) {
    var addresses = remove(all_addresses, function (o) { return o.prefix === name; });
    assert.equal(addresses.length, 1);
    assert.equal(addresses[0].prefix, name);
    assert.equal(addresses[0].distribution, 'balanced');
    assert.equal(addresses[0].waypoint, true);

    var autolinks = remove(all_autolinks, function (o) { return o.addr === name; });
    assert.equal(autolinks.length, 2);
    assert.equal(autolinks[0].addr, name);
    assert.equal(autolinks[1].addr, name);
    if (autolinks[0].dir === 'in') {
        assert.equal(autolinks[1].dir, 'out');
    } else {
        assert.equal(autolinks[0].dir, 'out');
        assert.equal(autolinks[1].dir, 'in');
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
                verify_queue(a.address, addresses, autolinks);
            } else if (a.type === 'topic') {
                verify_topic(a.address, linkroutes);
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

function get_address_list_message(address_list) {
    var content = JSON.stringify({items:address_list.map(function (a) { return {spec:a}; })});
    return {subject:'enmasse.io/v1/AddressList', body:content};
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

function get_source_address_filter(address) {
    return function (link) {
        return link && link.remote && link.remote.attach && link.remote.attach.source
            && link.remote.attach.source.address === address;
    }
}

function MockAddressSource(address_list, pod_list) {
    this.address_list = address_list || [];
    this.pod_list = pod_list || [];
    this.connections = {};
    this.source_address = 'v1/addresses';
    this.pod_watch_address = 'podsense';
    this.container = rhea.create_container();
    this.container.on('sender_open', this.subscribe.bind(this));
    var self = this;
    this.cleanup = function (context) {
        delete self.connections[context.connection.container_id];
    };
    this.notify_addresses = this.send_address_list.bind(this);
    this.address_subscription_filter = get_source_address_filter(this.source_address);
    this.notify_pods = this.send_pod_list.bind(this);
    this.podwatch_subscription_filter = get_source_address_filter(this.pod_watch_address);
}

MockAddressSource.prototype.listen = function (port) {
    this.listener = this.container.listen({'port':port});
    return this.listener;
};

MockAddressSource.prototype.listen_tls = function (options) {
    this.listener = this.container.listen(options);
    return this.listener;
};

MockAddressSource.prototype.close = function () {
    if (this.listener) this.listener.close();
}

MockAddressSource.prototype.get_port = function () {
    return this.listener ? this.listener.address().port : 0;
}

MockAddressSource.prototype.send_address_list = function (sender) {
    sender.send(get_address_list_message(this.address_list));
};

MockAddressSource.prototype.update_address_list = function (address_list) {
    this.address_list = address_list;
    for (var c in this.connections) {
        this.connections[c].each_link(this.notify_addresses, this.address_subscription_filter);
    }
};

MockAddressSource.prototype.send_pod_list = function (sender) {
    sender.send({body:this.pod_list});
};

MockAddressSource.prototype.update_pod_list = function (pod_list) {
    this.pod_list = pod_list;
    this.foreach_podwatch_subscription(this.notify_pods);
};

MockAddressSource.prototype.foreach_podwatch_subscription = function (f) {
    for (var c in this.connections) {
        this.connections[c].each_link(f, this.podwatch_subscription_filter);
    }
}

MockAddressSource.prototype.register = function (connection) {
    this.connections[connection.container_id] = connection;
    connection.on('connection_close', this.cleanup);
    connection.on('disconnected', this.cleanup);
};

MockAddressSource.prototype.subscribe = function (context) {
    if (context.sender.remote.attach.source && context.sender.remote.attach.source.address === this.source_address) {
        context.sender.set_source({address:this.source_address});
        this.register(context.connection);
        this.send_address_list(context.sender);
    } else if (context.sender.remote.attach.source && context.sender.remote.attach.source.address === this.pod_watch_address) {
        context.sender.set_source({address:this.pod_watch_address});
        this.register(context.connection);
        this.send_pod_list(context.sender);
    } else {
        context.sender.close({condition:'amqp:not-found', description:'Unrecognised source'});
    }
};

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

describe('basic router configuration', function() {
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

    function multi_router_address_test(count, address_list, verification, initial_config) {
        return function(done) {
            var routers = [];
            for (var i = 0; i < count; i++) {
                routers.push(new_router());
            }
            if (initial_config) initial_config(routers);
            var client = rhea.connect({port:ragent.server.address().port});
            client.open_sender().send(get_address_list_message(address_list));
            client.on('sender_open', function () {
                setTimeout(function () {
                    verification ? verification(routers, address_list) : verify_addresses(address_list, routers);
                    client.close();
                    client.on('connection_close', function () { done(); } );
                }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
            });
        };
    }

    function simple_address_test(address_list, verification, initial_config) {
        return multi_router_address_test(1, address_list, function (routers, address_list) {
            verification ? verification(routers[0], address_list) : verify_addresses(address_list, routers[0]);
        }, function (routers) {
            if (initial_config) initial_config(routers[0]);
        });
    }

    it('configures a single anycast address', simple_address_test([{address:'foo',type:'anycast'}]));
    it('configures a single multicast address', simple_address_test([{address:'foo',type:'multicast'}]));
    it('configures a single queue address', simple_address_test([{address:'foo',type:'queue'}]));
    it('configures a single topic address', simple_address_test([{address:'foo',type:'topic'}]));
    it('configures multiple anycast addresses', simple_address_test([{address:'a',type:'anycast'}, {address:'b',type:'anycast'}, {address:'c',type:'anycast'}]));
    it('configures multiple multicast addresses', simple_address_test([{address:'a',type:'multicast'}, {address:'b',type:'multicast'}, {address:'c',type:'multicast'}]));
    it('configures multiple topics', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'topic'}, {address:'c',type:'topic'}]));
    it('configures multiple queues', simple_address_test([{address:'a',type:'queue'}, {address:'b',type:'queue'}, {address:'c',type:'queue'}]));
    it('configures multiple different types of address', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}]));
    it('removes unwanted address config', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'foo', {prefix:'foo', distribution:'closest', 'waypoint':false});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'bar', {prefix:'bar', dir:'in'});
           router.create_object('org.apache.qpid.dispatch.router.config.autolink', 'baz', {addr:'baz', dir:'out'});
       }));
    it('removes or updates address config', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'a', {prefix:'a', distribution:'closest', 'waypoint':false});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'b-in', {prefix:'b', dir:'in'});
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'b-out', {prefix:'b', dir:'out'});
           router.create_object('org.apache.qpid.dispatch.router.config.autolink', 'baz', {addr:'baz', dir:'out'});
       }));
    it('configures addresses on multiple routers', multi_router_address_test(3, [{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}]));
    it('configures multiple routers into a full mesh', multi_router_address_test(6, [], function (routers) {
        verify_full_mesh(routers);
    }));
    it('configures routers correctly whenever they connect', function(done) {
        var client = rhea.connect({port:ragent.server.address().port});
        var sender = client.open_sender();
        sender.send(get_address_list_message([{address:'a',type:'topic'}, {address:'b',type:'queue'}]));
        client.on('sender_open', function () {
            var routers = [];
            for (var i = 0; i < 2; i++) {
                routers.push(new_router());
            }
            setTimeout(function () {
                verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
                verify_full_mesh(routers);
                //update addresses
                sender.send(get_address_list_message([{address:'a',type:'queue'}, {address:'c',type:'topic'}, {address:'d',type:'multicast'}]));
                //add another couple of routers
                for (var i = 0; i < 2; i++) {
                    routers.push(new_router());
                }
                setTimeout(function () {
                    verify_addresses([{address:'a',type:'queue'}, {address:'c',type:'topic'}, {address:'d',type:'multicast'}], routers);
                    verify_full_mesh(routers);
                    client.close();
                    client.on('connection_close', function () { done(); } );
                }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
            }, 500);
        });
    });
    it('handles health-check requests', function(done) {
        var routers = [];
        for (var i = 0; i < 2; i++) {
            routers.push(new_router());
        }
        routers[1].on('connected', function () {
            var client = rhea.connect({port:ragent.server.address().port});
            var checker = new HealthChecker(client);
            var sender = client.open_sender();
            checker.check([{name:'x'}, {name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                assert.equal(result, false);
                sender.send(get_address_list_message([{address:'a',type:'topic'}]));
                setTimeout(function () {
                    checker.check([{name:'x'}, {name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                        assert.equal(result, false);
                        sender.send(get_address_list_message([{address:'a',type:'topic'}, {address:'b',type:'queue'}]));
                        setTimeout(function () {
                            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
                            verify_full_mesh(routers);
                            checker.check([{name:'b', store_and_forward:true, multicast:false}]).then(function (result) {
                                assert.equal(result, true);
                                client.close();
                                client.on('connection_close', function () { done(); } );
                            });
                        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
                    });
                }, 200);
            });
        });
    });
    it('handles disconnection of router', function(done) {
        var routers = [];
        for (var i = 0; i < 6; i++) {
            routers.push(new_router());
        }
        var client = rhea.connect({port:ragent.server.address().port});
        var sender = client.open_sender();
        sender.send(get_address_list_message([{address:'a',type:'topic'}, {address:'b',type:'queue'}]));
        setTimeout(function () {
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
                setTimeout(function () {
                    verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
                    verify_full_mesh(routers);
                    done();
                }, 500);
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
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
        sender.send(get_address_list_message([{address:'a',type:'topic'}, {address:'b',type:'queue'}]));
        setTimeout(function () {
            verify_addresses([{address:'a',type:'topic'}, {address:'b',type:'queue'}], routers);
            verify_full_mesh(routers);
            done();
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
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
    it('handles address delete error', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}, {address:'c',type:'anycast'}, {address:'d',type:'multicast'}], undefined,
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.address', 'foo', {prefix:'foo', distribution:'closest', 'waypoint':false});
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
        var client = rhea.connect({port:ragent.server.address().port});
        client.open_sender().send(get_address_list_message(address_list));
        client.on('sender_open', function () {
            setTimeout(function () {
                verify_full_mesh_n(routers, 2);
                client.close();
                client.on('connection_close', function () {
                    delete process.env.ROUTER_NUM_CONNECTORS;
                    done();
                });
            }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
        });
    });
    it('ignores link route override', simple_address_test([{address:'a',type:'topic'}, {address:'b',type:'queue'}],
       function (routers, address_list) {
           verify_addresses(address_list, routers, function (objects) {
               var extra_linkroutes = remove(objects.linkroutes, function (o) { return o.prefix === 'foo' && o.dir === 'in'; });
               assert.equal(extra_linkroutes.length, 1);
               assert.equal(extra_linkroutes[0].name, 'override-foo');
           });
       },
       function (router) {
           router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', 'override-foo', {prefix:'foo', dir:'in'});
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
});

function localpath(name) {
    return path.resolve(__dirname, name);
}

describe('address source subscription', function() {
    this.timeout(5000);
    var ragent;
    var address_source = new MockAddressSource([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;

    beforeEach(function(done) {
        address_source.listen(0).on('listening', function () {
            done();
        });
    });

    function with_ragent(env, done) {
        ragent = new Ragent();
        ragent.listen({port:0}).on('listening', function (){
            routers = new RouterList(ragent.server.address().port);
            done();
        });
        ragent.subscribe_to_addresses(env);
    }

    afterEach(function(done) {
        routers.close().then(function () {
            ragent.server.close();
            address_source.close();
            done();
        });
    });

    function simple_subscribe_env_test(get_env) {
        return function (done) {
            with_ragent(get_env(), function () {
                var router = routers.new_router();
                setTimeout(function () {
                    verify_addresses(address_source.address_list, router);
                    done();
                }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
            });
        }
    }

    it('subscribes to configuration service', simple_subscribe_env_test(function () { return {'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':address_source.get_port()} }));
    it('subscribes to admin service on configuration port', simple_subscribe_env_test(function () { return {'ADMIN_SERVICE_HOST': 'localhost', 'ADMIN_SERVICE_PORT_CONFIGURATION':address_source.get_port()} }));
    it('prefers configuration service over admin service', simple_subscribe_env_test(function () { return {'ADMIN_SERVICE_HOST': 'foo', 'ADMIN_SERVICE_PORT_CONFIGURATION':7777,
                                                                                                           'CONFIGURATION_SERVICE_HOST': 'localhost',
                                                                                                           'CONFIGURATION_SERVICE_PORT':address_source.get_port()} }));
    it('will not subscribe if no host and port provided', function (done) {
        with_ragent({}, function () {
            var router = routers.new_router();
            setTimeout(function () {
                verify_addresses([], router);
                done();
            }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
        });
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

function fill(n, f) {
    return Array.apply(null, Array(n)).map(function (v, i) { return f(i); });
}

describe('cooperating ragent group', function() {
    this.timeout(5000);
    var counter = 0;
    var groups = fill(4, function (i) { return new RouterGroup('ragent-' + (i+1)); });
    var configserv = new MockAddressSource();

    beforeEach(function(done) {
        Promise.all(groups.map(function (g) { return g.listen(); })).then(function (){
            configserv.listen(0).on('listening', function () {
                done();
            });
        });
    });

    afterEach(function(done) {
        Promise.all(groups.map(function (g) {return g.close()})).then(function () {
            configserv.close();
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
        configserv.update_pod_list(groups.map(function (g) { return g.pod(); }));
        groups.forEach(function(g) {
            g.ragent.subscribe_to_addresses({'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':configserv.get_port()});
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
        configserv.update_pod_list(groups.map(function (g) { return g.pod(); }));
        groups.forEach(function(g) {
            g.ragent.subscribe_to_addresses({'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':configserv.get_port()});
        });
        //wait for some time for things to settle, then verify we have a full mesh
        setTimeout(function () {
            verify_full_mesh([].concat.apply([], routers));
            var leaving = groups.pop();
            configserv.update_pod_list(groups.map(function (g) { return g.pod(); }));
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

describe('probe support', function() {
    var ragent, server, port;

    beforeEach(function(done) {
        ragent = new Ragent();
        server = ragent.listen_probe({PROBE_PORT:0});
        server.on('listening', function () {
            port = server.address().port;
            done();
        });
    });

    afterEach(function(done) {
        server.close();
        done();
    });

    it('responds to probe request', function (done) {
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

describe('forked process', function() {
    this.timeout(5000);
    var ragent;
    var address_source = new MockAddressSource([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;

    beforeEach(function(done) {
        address_source.listen(0).on('listening', function () {
            var env =  merge(process.env, {'LOGLEVEL':'info', 'AMQP_PORT':0, 'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':address_source.get_port(), 'DEBUG':''});
            ragent = child_process.fork(path.resolve(__dirname, '../ragent.js'), [], {silent:true, 'env':env});
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
            verify_addresses(address_source.address_list, router);
            router.close().then(function () {
                done();
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
});


describe('run method', function() {
    this.timeout(5000);
    var ragent;
    var address_source = new MockAddressSource([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;

    beforeEach(function(done) {
        address_source.listen(0).on('listening', function() {
            var env =  {'AMQP_PORT':0, 'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':address_source.get_port()};
            ragent = new Ragent();
            ragent.run(env, function (port) {
                routers = new RouterList(port);
                done();
            });
        });
    });

    afterEach(function(done) {
        ragent.server.close();
        address_source.close();
        done();
    });

    it('subscribes to configuration service', function (done) {
        var router = routers.new_router();
        setTimeout(function () {
            verify_addresses(address_source.address_list, router);
            router.close().then(function () {
                done();
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
});

describe('run method also', function() {
    this.timeout(5000);
    var ragent;
    var address_source = new MockAddressSource([{address:'a',type:'topic'}, {address:'b',type:'queue'}]);
    var routers;
    var tls_env =  {'CA_PATH': localpath('ca-cert.pem'), 'CERT_PATH': localpath('server-cert.pem'),'KEY_PATH': localpath('server-key.pem')};

    beforeEach(function(done) {
        address_source.listen_tls(tls_options.get_server_options({port:0}, tls_env)).on('listening', function() {
            var env =  {'AMQP_PORT':0, 'CONFIGURATION_SERVICE_HOST': 'localhost', 'CONFIGURATION_SERVICE_PORT':address_source.get_port(),
                        'CA_PATH': localpath('ca-cert.pem'), 'CERT_PATH': localpath('server-cert.pem'),
                        'KEY_PATH': localpath('server-key.pem')};
            ragent = new Ragent();
            ragent.run(env, function (port) {
                routers = new RouterList(port);
                done();
            });
        });
    });

    afterEach(function(done) {
        ragent.server.close();
        address_source.close();
        done();
    });

    it('uses TLS', function (done) {
        var router = routers.new_router(undefined, tls_options.get_client_options({}, tls_env));
        setTimeout(function () {
            verify_addresses(address_source.address_list, router);
            router.close().then(function () {
                done();
            });
        }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
    });
});

var podwatch = require('../podwatch.js');
//testing pod watch here for convenience since the necessary mocks are
//here, can move it out into separate file if move mocks into a test
//module:
describe('pod watch', function() {
    this.timeout(5000);
    var configserv;

    beforeEach(function(done) {
        configserv = new MockAddressSource([], []);
        configserv.listen(0).on('listening', function () {
            done();
        });
    });

    afterEach(function(done) {
        configserv.close();
        done();
    });

    it('does its thing', function (done) {
        configserv.update_pod_list([{
            ready : 'True',
            phase : 'Running',
            name : 'foo',
            host : 'localhost',
            port : 1234
        }]);
        var conn = rhea.connect({port:configserv.get_port()});
        var sub = podwatch.watch_pods(conn);
        sub.on('added', function (added) {
            assert.equal(added.foo.ready, 'True');
            assert.equal(added.foo.port, 1234);
            assert.equal(added.foo.host, 'localhost');
            sub.close();
            done();
        });
    });
    it('handles empty bodied message', function (done) {
        var conn = rhea.connect({port:configserv.get_port()}).on('receiver_open', function () {
            configserv.foreach_podwatch_subscription(function (s) {
                s.send({'subject':'ignore-me'});
            });
            configserv.update_pod_list([{
                ready : 'True',
                phase : 'Running',
                name : 'foo',
                host : 'localhost',
                port : 1234
            }]);
        });
        var sub = podwatch.watch_pods(conn);
        sub.on('added', function (added) {
            assert.equal(added.foo.ready, 'True');
            assert.equal(added.foo.port, 1234);
            assert.equal(added.foo.host, 'localhost');
            done();
            sub.close();
        });
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

        var sender = client.open_sender();
        sender.send(get_address_list_message([{address:'mytopic',type:'topic'}]));
        delay(function () {
            sender.send({subject:'enmasse.io/v1/AddressList', body:JSON.stringify({items:[]})});
        }, 500).then(function () {
            delay(function () {
                verify_addresses([], router);
                sender.send(get_address_list_message([{address:'mytopic',type:'topic'}]));
            }, 500).then(function () {
                setTimeout(function () {
                    verify_addresses([{address:'mytopic',type:'topic'}], router);
                    client.close();
                }, 1000/*1 second wait for propagation*/);//TODO: add ability to be notified of propagation in some way
            });
        });
    });
});
