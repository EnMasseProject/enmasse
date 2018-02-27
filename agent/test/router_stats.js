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
var RouterStats = require('../lib/router_stats.js');
var AddressList = require('../lib/address_list.js');
var Registry = require('../lib/registry.js');

function NameGenerator (base, separator) {
    this.base = base;
    this.separator = separator || '_';
    this.counter = 1;
}

NameGenerator.prototype.next = function(base) {
    return this.base + this.separator + this.counter++;
};

function DummyConn (name, builder) {
    this.name = name;
    this.builder = builder;
}

DummyConn.prototype.sender = function (address, details, phase) {
    this.builder.link(this.name, true, address, details, phase);
};

DummyConn.prototype.receiver = function (address, details, phase) {
    this.builder.link(this.name, false, address, details, phase);
};

function StatsBuilder (router) {
    this.conn_names = new NameGenerator(router.name + '-conn');
    this.link_names = new NameGenerator(router.name + '-link');
    this.router = router;
}

StatsBuilder.prototype.connection = function (details) {
    var name = this.conn_names.next();
    var attributes = details || {};
    if (attributes.role === undefined) attributes.role = 'normal';
    if (attributes.identity === undefined) attributes.identity = name;
    this.router.create_object('org.apache.qpid.dispatch.connection', name, attributes);
    return new DummyConn(name, this);
};

StatsBuilder.prototype.link = function (connection, is_sender, address, details, phase) {
    var attributes = details || {};
    if (attributes.linkType === undefined) attributes.linkType='endpoint';
    attributes.connectionId = connection;
    attributes.linkDir = is_sender ? 'in' : 'out';
    attributes.owningAddr = 'M' + (phase || 0) + address;
    this.router.create_object('org.apache.qpid.dispatch.router.link', this.link_names.next(), attributes);
};

StatsBuilder.prototype.anycast = function (name, messages_in, messages_out, details) {
    var attributes = details || {};
    attributes.deliveriesIngress = messages_in;
    attributes.deliveriesEgress = messages_out;
    this.router.create_object('org.apache.qpid.dispatch.router.config.address', name, {prefix:name, distribution:'balanced', waypoint:false});
    this.router.create_object('org.apache.qpid.dispatch.router.address', 'M0' + name, attributes);
};

StatsBuilder.prototype.queue = function (name, messages_in, messages_out, details) {
    var attributes_0 = details || {};
    var attributes_1 = details || {};
    attributes_0.deliveriesIngress = messages_in;
    attributes_1.deliveriesEgress = messages_out;
    this.router.create_object('org.apache.qpid.dispatch.router.config.address', name, {prefix:name, distribution:'balanced', waypoint:true});
    this.router.create_object('org.apache.qpid.dispatch.router.address', 'M0' + name, attributes_0);
    this.router.create_object('org.apache.qpid.dispatch.router.address', 'M1' + name, attributes_1);
};

StatsBuilder.prototype.topic = function (name) {
    for (var dir in {'in':'in', 'out':'out'}) {
        this.router.create_object('org.apache.qpid.dispatch.router.config.linkRoute', name+'-'+dir, {prefix:name, dir:dir});
    }
};

var router_names = new NameGenerator('router');

function MockRouter (name) {
    this.name = name || router_names.next();
    events.EventEmitter.call(this);
    this.objects = {};
    this.create_object('listener', 'default', {name:'default', addr:name, port:55672, role:'inter-router'});
    this.container = rhea.create_container({id:this.name});
    this.container.on('message', this.on_message.bind(this));
    var self = this;
    this.container.on('sender_open', function(context) {
        if (context.sender.source.dynamic) {
            var id = self.container.generate_uuid();
            context.sender.set_source({address:id});
        }
    });
    this.nodes = {};
    this.add_node(this);
    this.populate = new StatsBuilder(this);
}

util.inherits(MockRouter, events.EventEmitter);

function match_source_address(link, address) {
   return link && link.local && link.local.attach && link.local.attach.source
        && link.local.attach.source.value[0].toString() === address;
}

MockRouter.prototype.add_node = function (router) {
    this.nodes[router.get_mgmt_node_address()] = router;
};

MockRouter.prototype.connect = function (port) {
    this.connection = this.container.connect({'port':port || 55672, properties:{product:'qpid-dispatch-router'}});
    var self = this;
    this.connection.on('connection_open', function () { self.emit('connected', self); });
};

MockRouter.prototype.listen = function (port) {
    this.listener = this.container.listen({'port':port, properties:{product:'qpid-dispatch-router'}});
    return this.listener;
};

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
    if (this.connection) this.connection.close();
    if (this.listener) this.listener.close();
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
    var node = this;
    var target = context.message.to || context.receiver.remote.attach.target.address;
    if (target !== '$management' && this.nodes[target]) {
        node = this.nodes[target];
    }
    var reply_to = request.reply_to;
    var response = {to: reply_to};
    if (request.correlation_id) {
        response.correlation_id = request.correlation_id;
    }
    response.application_properties = {};
    response.application_properties.statusCode = 200;

    if (request.application_properties.operation === 'CREATE') {
        node.create_object(request.application_properties.type, request.application_properties.name, request.body);
    } else if (request.application_properties.operation === 'DELETE') {
        node.delete_object(request.application_properties.type, request.application_properties.name);
    } else if (request.application_properties.operation === 'QUERY') {
        var results = node.list_objects(request.application_properties.entityType);
        var attributes = request.body.attributeNames;
        if (!attributes || attributes.length === 0) {
            attributes = get_attribute_names(results);
        }
        response.body = query_result(attributes, results);
    } else if (request.application_properties.operation === 'GET-MGMT-NODES') {
        response.body = Object.keys(node.nodes);
    }

    var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, reply_to); });
    if (reply_link) {
        reply_link.send(response);
    }
};

MockRouter.prototype.get_mgmt_node_address = function () {
    return "amqp:/_topo/0/" + this.name +"/$management"
};

describe('router stats', function() {
    var router;
    var router_stats;
    var extra_nodes;

    beforeEach(function(done) {
        router = new MockRouter();
        extra_nodes = [];
        var l = router.listen(0);
        l.on('listening', function () {
            router_stats = new RouterStats(rhea.connect({port:l.address().port}));
            done();
        });
    });

    function add_router_nodes(count) {
        for (var i = 0; i < count; i++) {
            var r = new MockRouter();
            extra_nodes.push(r);
            router.add_node(r);
        }
    }

    afterEach(function(done) {
        router_stats.close();
        router.close();
        done();
    });

    it('retrieves stats for an anycast address from a single router', function(done) {
        //populate router:
        router.populate.anycast('foo', 10, 8);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo', {presettledCount:1, undeliveredCount:2, unsettledCount:3, acceptedCount:4, rejectedCount:5, releasedCount:6, modifiedCount:7});
            c.receiver('foo', {presettledCount:7, undeliveredCount:6, unsettledCount:5, acceptedCount:4, rejectedCount:3, releasedCount:2, modifiedCount:1});
        }

        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.foo.senders, 2);
            assert.equal(results.addresses.foo.receivers, 2);
            assert.equal(results.addresses.foo.messages_in, 10);
            assert.equal(results.addresses.foo.messages_out, 8);

            assert.equal(results.addresses.foo.outcomes.ingress.presettled, 2);
            assert.equal(results.addresses.foo.outcomes.ingress.undelivered, 4);
            assert.equal(results.addresses.foo.outcomes.ingress.unsettled, 6);
            assert.equal(results.addresses.foo.outcomes.ingress.accepted, 8);
            assert.equal(results.addresses.foo.outcomes.ingress.rejected, 10);
            assert.equal(results.addresses.foo.outcomes.ingress.released, 12);
            assert.equal(results.addresses.foo.outcomes.ingress.modified, 14);

            assert.equal(results.addresses.foo.outcomes.egress.presettled, 14);
            assert.equal(results.addresses.foo.outcomes.egress.undelivered, 12);
            assert.equal(results.addresses.foo.outcomes.egress.unsettled, 10);
            assert.equal(results.addresses.foo.outcomes.egress.accepted, 8);
            assert.equal(results.addresses.foo.outcomes.egress.rejected, 6);
            assert.equal(results.addresses.foo.outcomes.egress.released, 4);
            assert.equal(results.addresses.foo.outcomes.egress.modified, 2);

            assert.equal(results.addresses.foo.outcomes.ingress.links.length, 2);
            assert.equal(results.addresses.foo.outcomes.ingress.links[0].backlog, 5);
            assert.equal(results.addresses.foo.outcomes.ingress.links[0].backlog, 5);
            assert.equal(results.addresses.foo.outcomes.egress.links.length, 2);
            assert.equal(results.addresses.foo.outcomes.egress.links[0].backlog, 11);
            assert.equal(results.addresses.foo.outcomes.egress.links[0].backlog, 11);

            done();
        });
    });
    it('retrieves stats for a queue from a single router', function(done) {
        //populate router:
        router.populate.queue('foo', 64, 46);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo');
            c.receiver('foo');
        }

        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.foo.senders, 2);
            assert.equal(results.addresses.foo.receivers, 2);
            assert.equal(results.addresses.foo.messages_in, 64);
            assert.equal(results.addresses.foo.messages_out, 46);
            done();
        });
    });
    it('retrieves stats for an anycast address from multiple routers', function(done) {
        add_router_nodes(2);
        //populate routers:
        router.populate.anycast('foo', 10, 8);
        extra_nodes[0].populate.anycast('foo', 6, 4);
        extra_nodes[1].populate.anycast('foo', 24, 3);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo', {presettledCount:1, undeliveredCount:2, unsettledCount:3, acceptedCount:4, rejectedCount:5, releasedCount:6, modifiedCount:7});
            c.receiver('foo', {presettledCount:7, undeliveredCount:6, unsettledCount:5, acceptedCount:4, rejectedCount:3, releasedCount:2, modifiedCount:1});
        }
        for (var i = 0 ; i < extra_nodes.length; i++) {
            var cx = router.populate.connection();
            var delta = i+1;
            cx.sender('foo', {presettledCount:delta, undeliveredCount:delta, unsettledCount:delta, acceptedCount:delta, rejectedCount:delta, releasedCount:delta, modifiedCount:delta});
            cx.receiver('foo', {presettledCount:delta, undeliveredCount:delta, unsettledCount:delta, acceptedCount:delta, rejectedCount:delta, releasedCount:delta, modifiedCount:delta});
        }

        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.foo.senders, 4);
            assert.equal(results.addresses.foo.receivers, 4);
            assert.equal(results.addresses.foo.messages_in, 40);
            assert.equal(results.addresses.foo.messages_out, 15);

            assert.equal(results.addresses.foo.outcomes.ingress.presettled, 5);
            assert.equal(results.addresses.foo.outcomes.ingress.undelivered, 7);
            assert.equal(results.addresses.foo.outcomes.ingress.unsettled, 9);
            assert.equal(results.addresses.foo.outcomes.ingress.accepted, 11);
            assert.equal(results.addresses.foo.outcomes.ingress.rejected, 13);
            assert.equal(results.addresses.foo.outcomes.ingress.released, 15);
            assert.equal(results.addresses.foo.outcomes.ingress.modified, 17);

            assert.equal(results.addresses.foo.outcomes.egress.presettled, 17);
            assert.equal(results.addresses.foo.outcomes.egress.undelivered, 15);
            assert.equal(results.addresses.foo.outcomes.egress.unsettled, 13);
            assert.equal(results.addresses.foo.outcomes.egress.accepted, 11);
            assert.equal(results.addresses.foo.outcomes.egress.rejected, 9);
            assert.equal(results.addresses.foo.outcomes.egress.released, 7);
            assert.equal(results.addresses.foo.outcomes.egress.modified, 5);

            assert.equal(results.addresses.foo.outcomes.ingress.links.length, 4);
            assert.equal(results.addresses.foo.outcomes.egress.links.length, 4);

            done();
        }).catch(done);
    });
    it('retrieves stats for a queue from multiple routers', function(done) {
        add_router_nodes(2);
        //populate router:
        router.populate.queue('foo', 64, 46);
        extra_nodes[0].populate.queue('foo', 6, 4);
        extra_nodes[1].populate.queue('foo', 20, 5);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo');
            c.receiver('foo');
        }
        for (var i = 0; i < 3; i++) {
            extra_nodes[0].populate.connection().sender('foo');
        }
        var c2 = extra_nodes[1].populate.connection();
        for (var i = 0; i < 3; i++) {
            c2.receiver('foo');
        }
        c2.sender('foo');

        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.foo.senders, 6);
            assert.equal(results.addresses.foo.receivers, 5);
            assert.equal(results.addresses.foo.messages_in, 90);
            assert.equal(results.addresses.foo.messages_out, 55);
            done();
        }).catch(done);
    });
    it('retrieves propagation for a topic from a single router', function(done) {
        //populate router:
        router.populate.topic('bar');
        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.bar.propagated, 100);
            done();
        });
    });
    it('retrieves sender and receiver stats for a topic from a single router', function(done) {
        //populate router:
        router.populate.topic('bar');
        for (var i = 0; i < 10; i++) {
            var c = router.populate.connection();
            c.receiver('bar');
            c.receiver('bar');
            c.sender('bar');
            var c2 = router.populate.connection({role:'route-container'});
            c2.receiver('bar');
            c2.sender('bar');
        }
        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.bar.senders, 10);
            assert.equal(results.addresses.bar.receivers, 20);
            done();
        }).catch(function (error) {
            done(error);
        });
    });
    it('updates registries with stats for a queue from a single router', function(done) {
        //populate router:
        router.populate.queue('foo', 64, 46);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo');
            c.receiver('foo');
        }

        //retrieve stats:
        var connections = new Registry();
        var addresses = new AddressList();
        addresses.set({foo:{address:'foo'}});
        router_stats.retrieve(addresses, connections).then(function() {
            assert.equal(addresses.get('foo').senders, 2);
            assert.equal(addresses.get('foo').receivers, 2);
            assert.equal(addresses.get('foo').messages_in, 64);
            assert.equal(addresses.get('foo').messages_out, 46);
            done();
        });
    });
    it('retrieves stats for a queue from a changing set of routers', function(done) {
        //populate router:
        router.populate.queue('foo', 64, 46);
        //create some fake connection- and link- stats:
        var c = router.populate.connection();
        for (var i = 0; i < 2; i++) {
            c.sender('foo');
            c.receiver('foo');
        }

        //retrieve stats:
        router_stats._retrieve().then(function(results) {
            assert.equal(results.addresses.foo.senders, 2);
            assert.equal(results.addresses.foo.receivers, 2);
            assert.equal(results.addresses.foo.messages_in, 64);
            assert.equal(results.addresses.foo.messages_out, 46);
            add_router_nodes(2);
            extra_nodes[0].populate.queue('foo', 6, 4);
            extra_nodes[1].populate.queue('foo', 20, 5);
            for (var i = 0; i < 3; i++) {
                extra_nodes[0].populate.connection().sender('foo');
            }
            var c2 = extra_nodes[1].populate.connection();
            for (var i = 0; i < 3; i++) {
                c2.receiver('foo');
            }
            c2.sender('foo');

            //retrieve stats:
            router_stats._retrieve().then(function(results) {
                assert.equal(results.addresses.foo.senders, 6);
                assert.equal(results.addresses.foo.receivers, 5);
                assert.equal(results.addresses.foo.messages_in, 90);
                assert.equal(results.addresses.foo.messages_out, 55);
                done();
            });
        });
    });
});
