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
var http = require('http');
var path = require('path');
var rhea = require('rhea');
var WebSocket = require('ws');
var ws = rhea.websocket_connect(WebSocket);

var ConsoleServer = require('../lib/console_server.js');
var MockAuthService = require('../testlib/mock_authservice.js');

function AddressCtrl() {
    this.addresses = {};
};

AddressCtrl.prototype.get_address_types = function () {
    return Promise.resolve([{name:'queue'}, {name:'topic'}]);
};

AddressCtrl.prototype.create_address = function (address) {
    var self = this;
    return new Promise(function (resolve, reject) {
        if (self.addresses[address.address] === undefined) {
            self.addresses[address.address] = address;
            resolve();
        } else {
            reject('already exists!');
        }
    });
};

AddressCtrl.prototype.delete_address = function (address) {
    var self = this;
    return new Promise(function (resolve, reject) {
        if (self.addresses[address.address] === undefined) {
            reject('does not exist!');
        } else {
            delete self.addresses[address.address];
            resolve();
        }
    });
};

describe('console server', function() {
    var console_server;
    var auth_service;

    beforeEach(function(done) {
        auth_service = new MockAuthService();
        auth_service.listen().on('listening', function () {
            var options = {port:0, AUTHENTICATION_SERVICE_PORT: auth_service.port};
            console_server = new ConsoleServer(new AddressCtrl());
            console_server.listen(options, done);
        });
    });

    function connect() {
        var url = 'ws://localhost:' + console_server.server.address().port + '/websocket';
        return rhea.connect({connection_details:ws(url)});
    }

    afterEach(function(done) {
        Promise.all([
            new Promise(function (resolve, reject) {
                auth_service.close(resolve);
            }),
            new Promise(function (resolve, reject) {
                console_server.server.close(resolve);
            })
        ]).then(function () {
            done();
        });
    });

    it('supports a probe path', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/probe'}, function (response) {
            assert.equal(response.statusCode, 200);
            done();
        });
    });

    it('retrieves a resource that exists', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/help.html'}, function (response) {
            assert.equal(response.statusCode, 200);
            done();
        });
    });

    it('retrieves index.html for a directory', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/'}, function (response) {
            assert.equal(response.statusCode, 200);
            assert.equal(response.headers['content-type'], 'text/html');
            done();
        });
    });

    it('gives a 404 for a resource that does not exist', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/whatsit.html'}, function (response) {
	    response.setEncoding('utf8');
            assert.equal(response.statusCode, 404);
            done();
        });
    });

    it('gives a 405 for an unsupported method', function(done) {
        var request = http.request({method: 'PUT', port:console_server.server.address().port, hostname:'localhost',path:'/whatsit.html'}, function (response) {
	    response.setEncoding('utf8');
            assert.equal(response.statusCode, 405);
            done();
        });
        request.end();
    });

    it('gives a 500 for an unsupported authorization header', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/help.html', headers:{'Authorization':'foo bar'}}, function (response) {
            assert.equal(response.statusCode, 500);
            done();
        });
    });

    it('accepts valid username and password', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/help.html', auth:'bob:bob'}, function (response) {
            assert.equal(response.statusCode, 200);
            done();
        });
    });

    it('rejects invalid username and password', function(done) {
        http.get({port:console_server.server.address().port, hostname:'localhost',path:'/help.html', auth:'foo:bar'}, function (response) {
            assert.equal(response.statusCode, 401);
            assert.equal(response.headers['www-authenticate'], 'Basic realm=Authorization Required');
            done();
        });
    });

    it('sends subscribing client address stats', function(done) {
        console_server.addresses.set({foo: {address:'foo', messages_in:100, messages_out:50}});
        var connection = connect();
        var receiver = connection.open_receiver();
        receiver.on('message', function (context) {
            if (context.message.subject === 'address') {
                assert.equal(context.message.body.address, 'foo');
                assert.equal(context.message.body.messages_in, 100);
                assert.equal(context.message.body.messages_out, 50);
                receiver.close();
                connection.close();
            }
        });
        connection.on('connection_close', function () {
            done();
        });
    });
    it('sends subscribing client connection stats', function(done) {
        console_server.connections.set({foo: {container:'foo', messages_in:100, messages_out:50}});
        var connection = connect();
        var receiver = connection.open_receiver();
        receiver.on('message', function (context) {
            if (context.message.subject === 'connection') {
                assert.equal(context.message.body.container, 'foo');
                assert.equal(context.message.body.messages_in, 100);
                assert.equal(context.message.body.messages_out, 50);
                receiver.close();
                connection.close();
            }
        });
        connection.on('connection_close', function () {
            done();
        });
    });
    it('sends subscribing client notification of deleted address', function(done) {
        console_server.addresses.set({foo: {address:'foo', messages_in:100, messages_out:50}, bar: {address:'bar', messages_in:75, messages_out:80}});
        var connection = connect();
        var receiver = connection.open_receiver();
        receiver.on('receiver_open', function () {
            console_server.addresses.set({bar: {address:'bar', messages_in:75, messages_out:80}});
        });
        receiver.on('message', function (context) {
            if (context.message.subject === 'address_deleted') {
                assert.equal(context.message.body, 'foo');
                connection.close();
            }
        });
        connection.on('connection_close', function () {
            done();
        });
    });
    it('sends subscribing client notification of deleted connection', function(done) {
        console_server.connections.set({foo: {id:'foo', messages_in:100, messages_out:50}, bar: {id:'bar', messages_in:75, messages_out:80}});
        var connection = connect();
        var receiver = connection.open_receiver();
        receiver.on('receiver_open', function () {
            console_server.connections.set({bar: {id:'bar', messages_in:75, messages_out:80}});
        });
        receiver.on('message', function (context) {
            if (context.message.subject === 'connection_deleted') {
                assert.equal(context.message.body, 'foo');
                connection.close();
            }
        });
        connection.on('connection_close', function () {
            done();
        });
    });
    it('accepts valid address creation requests', function(done) {
        var connection = connect();
        var sender = connection.open_sender();
        sender.on('accepted', function () {
            connection.close();
        });
        sender.send({subject:'create_address', body:{address:'foo',type:'queue',plan:'surprise-me'}});
        connection.on('connection_close', function () {
            assert.deepEqual(console_server.address_ctrl.addresses.foo, {address:'foo',type:'queue',plan:'surprise-me'});
            done();
        });
    });
    it('rejects invalid address creation requests', function(done) {
        var connection = connect();
        var sender = connection.open_sender();
        sender.on('accepted', function () {
            sender.on('rejected', function (context) {
                assert.equal(context.delivery.remote_state.error.description, 'already exists!');
                connection.close();
            });
            sender.send({subject:'create_address', body:{address:'foo',type:'topic',plan:'this-should-not-work'}});
        });
        sender.send({subject:'create_address', body:{address:'foo',type:'queue',plan:'surprise-me'}});
        connection.on('connection_close', function () {
            assert.deepEqual(console_server.address_ctrl.addresses.foo, {address:'foo',type:'queue',plan:'surprise-me'});
            done();
        });
    });
    it('accepts valid address deletion requests', function(done) {
        console_server.address_ctrl.addresses.foo = {address:'foo',type:'queue',plan:'delete-me'};
        var connection = connect();
        var sender = connection.open_sender();
        sender.on('accepted', function () {
            connection.close();
        });
        sender.send({subject:'delete_address', body:{address:'foo'}});
        connection.on('connection_close', function () {
            assert(console_server.address_ctrl.addresses.foo === undefined);
            done();
        });
    });
    it('rejects invalid address deletion requests', function(done) {
        console_server.address_ctrl.addresses.foo = {address:'foo',type:'queue',plan:'delete-me'};
        var connection = connect();
        var sender = connection.open_sender();
        sender.on('rejected', function (context) {
            assert.equal(context.delivery.remote_state.error.description, 'does not exist!');
            connection.close();
        });
        sender.send({subject:'delete_address', body:{address:'bar'}});
        connection.on('connection_close', function () {
            assert.deepEqual(console_server.address_ctrl.addresses.foo, {address:'foo',type:'queue',plan:'delete-me'});
            done();
        });
    });
    it('rejects unrecognised requests', function(done) {
        var connection = connect();
        var sender = connection.open_sender();
        sender.on('rejected', function (context) {
            assert.equal(context.delivery.remote_state.error.description, 'ignoring message: ' + JSON.stringify({subject:'random_nonsense', body:'foo'}));
            connection.close();
        });
        sender.send({subject:'random_nonsense', body:'foo'});
        connection.on('connection_close', function () {
            done();
        });
    });
});
