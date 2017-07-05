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

var rhea = require('rhea');

function MockRouter (name) {
    this.name = name;
    events.EventEmitter.call(this);
    this.connection = rhea.connect({port:55672, container_id:name, properties:{product:'qpid-dispatch-router'}});
    this.connection.on('message', this.on_message.bind(this));
    var self = this;
    this.connection.on('connection_open', function () { self.emit('connected', self); });
    this.objects = {};
    this.create_object('listener', 'default', {name:'default', addr:name, port:55672, role:'inter-router'});
}

util.inherits(MockRouter, events.EventEmitter);

function match_source_address(link, address) {
    return link && link.local && link.local.attach && link.local.attach.source
        && link.local.attach.source.address === address;
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
    this.connection.close();
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
    if (request.correlation_id) {
        response.correlation_id = request.correlation_id;
    }
    response.application_properties = {};
    response.application_properties.statusCode = 200;

    if (request.application_properties.operation === 'CREATE') {
        this.create_object(request.application_properties.type, request.application_properties.name, request.body);
    } else if (request.application_properties.operation === 'DELETE') {
        this.delete_object(request.application_properties.type, request.application_properties.name);
    } else if (request.application_properties.operation === 'QUERY') {
        var results = this.list_objects(request.application_properties.entityType);
        var attributes = request.body.attributeNames;
        if (!attributes || attributes.length === 0) {
            attributes = get_attribute_names(results);
        }
        response.body = query_result(attributes, results);
    }

    var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, reply_to); });
    if (reply_link) {
        reply_link.send(response);
    }
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

function verify_addresses(inputs, router) {
    if (util.isArray(router)) {
        router.forEach(verify_addresses.bind(null, inputs));
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

function are_connected(router_a, router_b) {
    var from_a = router_a.list_objects('connector').filter(function (c) { return c.addr === router_b.name; });
    var from_b = router_b.list_objects('connector').filter(function (c) { return c.addr === router_a.name; });
    if (from_a.length + from_b.length > 1) {
        console.warn('unexpected connectivity between ' + router_a.name + ' and ' + router_b.name + ': ' + JSON.stringify(from_a.concat(from_b)));
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

describe('basic router configuration', function() {
    this.timeout(20000);
    var counter = 1;
    var ragent;
    var routers = [];

    beforeEach(function(done) {
        ragent = child_process.fork(path.resolve(__dirname, '../ragent.js'), [], {silent:true, env:{}});
        ragent.stderr.on('data', function (data) {
            if (data.toString().match('Router agent listening on')) {
                done();
            }
        });
    });

    afterEach(function(done) {
        //TODO: return promise from MockRouter.close()
        for (var i = 0; i < routers.length; i++) {
            routers[i].close();
        }
        setTimeout(function () {
            ragent.kill();
            done();
        }, 500);
    });

    function new_name() {
        return 'router-' + counter++;
    }

    function new_router(name) {
        var r = new MockRouter(name || new_name());
        routers.push(r);
        return r;
    }

    function multi_router_address_test(count, address_list, verification, initial_config) {
        return function(done) {
            var routers = [];
            for (var i = 0; i < count; i++) {
                routers.push(new_router());
            }
            if (initial_config) initial_config(routers);
            var client = rhea.connect({port:55672});
            var content = JSON.stringify(address_list.map(function (a) { return {spec:a}; }));
            client.open_sender().send({subject:'enmasse.io/v1/AddressList', body:content});
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
});
