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
var rhea = require('rhea');

var AddressList = require('../lib/address_list.js');
var AddressSource = require('../lib/external_address_source.js');

function MockConfigServ () {
    this.address_list = [];
    this.connections = {};
    this.container = rhea.create_container({id:'mock-config-serv'});
    this.container.on('sender_open', this.on_subscribe.bind(this));
    this.container.on('connection_open', this.register.bind(this));
    this.container.on('connection_close', this.unregister.bind(this));
    this.container.on('disconnected', this.unregister.bind(this));
}

MockConfigServ.prototype.listen = function (port) {
    this.port = port;
    this.server = this.container.listen({port:port});
    var self = this;
    this.server.on('listening', function () {
        self.port = self.server.address().port;
    });
    return this.server;
};

MockConfigServ.prototype.close = function (callback) {
    if (this.server) this.server.close(callback);
    else callback();
};

MockConfigServ.prototype.notify = function (sender) {
    var msg = {subject: 'enmasse.io/v1/AddressList'};

    if (this.address_list.length > 0) {
        var body = {};
        body.items = this.address_list.map(function (address) {
            return { spec: address };
        });
        msg.body = JSON.stringify(body);
    }
    sender.send(msg);
};

function is_subscriber(link) {
    return link.is_sender() && link.source.address === 'v1/addresses';
}

MockConfigServ.prototype.notify_all = function (message) {
    var self = this;
    var f;
    if (message === undefined) {
        f = function (link) { self.notify(link) };
    } else {
        f = function (link) { link.send(message); };
    }
    for (var c in this.connections) {
        this.connections[c].each_link(f, is_subscriber);
    }
}

MockConfigServ.prototype.on_subscribe = function (context) {
    this.notify(context.sender);
};

MockConfigServ.prototype.register = function (context) {
    this.connections[context.connection.container_id] = context.connection;
};

MockConfigServ.prototype.unregister = function (context) {
    delete this.connections[context.connection.container_id];
};

MockConfigServ.prototype.add = function (address, type) {
    this.address_list.push({'address':address, 'type':type});
};

MockConfigServ.prototype.delete_all = function () {
    this.address_list = [];
};

describe('address registry', function() {
    var address_source;
    var connection;

    beforeEach(function(done) {
        address_source = new MockConfigServ();
        address_source.listen(0).on('listening', done);
    });

    afterEach(function(done) {
        if (connection) connection.close();
        address_source.close(done);
    });

    function create_address_list() {
        connection = rhea.connect({port:address_source.port});
        var src = new AddressSource(connection);
        var address_list = new AddressList();
        src.on('addresses_defined', address_list.addresses_defined.bind(address_list));
        return address_list;
    }

    it('subscribes to configserv', function(done) {
        var addresses = create_address_list();
        address_source.add('foo', 'anycast');
        addresses.on('updated', function (addr) {
            assert.equal(addr.address, 'foo');
            assert.equal(addr.type, 'anycast');
            assert.equal(addresses.get('foo').type, 'anycast');
            done();
        });
    });
    it('handles empty body', function(done) {
        var addresses = create_address_list();
        address_source.add('foo', 'anycast');
        addresses.on('updated', function (addr) {
            assert.equal(addr.address, 'foo');
            assert.equal(addr.type, 'anycast');
            assert.equal(addresses.get('foo').type, 'anycast');
            address_source.delete_all();
            address_source.notify_all();
        });
        addresses.on('deleted', function (addr) {
            assert.equal(addr.address, 'foo');
            assert.equal(addr.type, 'anycast');
            assert.equal(addresses.get('foo'), undefined);
            done();
        });
    });
    it('handles body with no items', function(done) {
        var addresses = create_address_list();
        address_source.add('foo', 'anycast');
        addresses.on('updated', function (addr) {
            assert.equal(addr.address, 'foo');
            assert.equal(addr.type, 'anycast');
            assert.equal(addresses.get('foo').type, 'anycast');
            address_source.notify_all({subject: 'enmasse.io/v1/AddressList', body:'{}'});
        });
        addresses.on('deleted', function (addr) {
            assert.equal(addr.address, 'foo');
            assert.equal(addr.type, 'anycast');
            assert.equal(addresses.get('foo'), undefined);
            done();
        });
    });
    it('handles updates', function(done) {
        var addresses = create_address_list();
        address_source.add('foo', 'anycast');
        var count = 0;
        addresses.on('updated', function (addr) {
            if (++count === 1) {
                assert.equal(addr.address, 'foo');
                assert.equal(addr.type, 'anycast');
                assert.equal(addresses.get('foo').type, 'anycast');
                address_source.delete_all();
                address_source.add('foo', 'queue');
                address_source.notify_all();
            } else {
                assert.equal(addr.address, 'foo');
                assert.equal(addr.type, 'queue');
                assert.equal(addresses.get('foo').type, 'queue');
                done();
            }
        });
    });
});
