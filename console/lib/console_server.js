/*
 * Copyright 2016 Red Hat Inc.
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

var express = require('express');
var basic_auth = require('basic-auth');
var http = require('http');
var path = require('path');
var rhea = require('rhea');
var WebSocketServer = require('ws').Server;
var auth_service = require('../lib/auth_service.js');
var Registry = require('../lib/registry.js');

function ConsoleServer (address_list, address_ctrl) {
    this.address_ctrl = address_ctrl;
    this.address_list = address_list;
    this.connections = new Registry();
    this.listeners = {};
    var self = this;
    this.address_list.on('updated', function (address) {
        self.publish({subject:'address',body:address});
    });
    this.address_list.on('deleted', function (address) {
        console.log('address ' + address.address + ' has been deleted, notifying clients...');
        self.publish({subject:'address_deleted',body:address.address});
    });
    this.connections.on('updated', function (conn) {
        self.publish({subject:'connection',body:conn});
    });
    this.connections.on('deleted', function (conn) {
        console.log('connection ' + conn.host + ' has been deleted, notifying clients...');
        self.publish({subject:'connection_deleted',body:conn.id});
    });
    this.amqp_container = rhea.create_container({autoaccept:false});

    this.amqp_container.on('sender_open', function (context) {
        self.subscribe(context.connection.remote.open.container_id, context.sender);
    });
    this.amqp_container.on('sender_close', function (context) {
        self.unsubscribe(context.connection.remote.open.container_id);
    });
    this.amqp_container.on('connection_close', function (context) {
        self.unsubscribe(context.connection.remote.open.container_id);
    });
    this.amqp_container.on('disconnected', function (context) {
        if (context.connection.remote.open) {
            self.unsubscribe(context.connection.remote.open.container_id);
        }
    });
    this.amqp_container.on('message', function (context) {
        var accept = function () {
            console.log(context.message.subject + ' succeeded');
            context.delivery.accept();
        };
        var reject = function (e, code) {
            console.log(context.message.subject + ' failed: ' + e);
            context.delivery.reject({condition: code || 'amqp:internal-error', description: '' + e});
        };

        if (context.message.subject === 'create_address') {
            console.log('creating address ' + JSON.stringify(context.message.body));
            self.address_ctrl.create_address(context.message.body).then(accept).catch(reject);
        } else if (context.message.subject === 'delete_address') {
            console.log('deleting address ' + JSON.stringify(context.message.body));
            self.address_ctrl.delete_address(context.message.body).then(accept).catch(reject);
        } else {
            console.log('ignoring message: ' + context.message);
        }
    });
}

ConsoleServer.prototype.ws_bind = function (server) {
    var self = this;
    var ws_server = new WebSocketServer({'server': server, 'path': '/websocket'});
    ws_server.on('connection', function (ws) {
        console.log('Accepted incoming websocket connection');
        self.amqp_container.websocket_accept(ws);
    });
};

ConsoleServer.prototype.listen = function (port) {
    var self = this;
    var app = express();
    app.get('/probe', function (req, res) { res.send('OK'); });

    var auth_ca_path = process.env.AUTH_SERVICE_CA || path.resolve('/opt/console/authservice-ca', 'tls.crt');

    var auth = function (req, res, next) {
        var authrequired = function() {
            res.set('WWW-Authenticate', 'Basic realm=Authorization Required');
            return res.sendStatus(401);
        };
        var user = basic_auth(req);

        auth_service.authenticate(user, auth_service.default_options(auth_ca_path)).then( next ).catch( authrequired );
    };

    app.use(auth);

    app.use('/', express.static(path.join(__dirname, '../www/')))

    this.server = http.createServer(app);
    this.server.listen(port || 8080);
    this.ws_bind(this.server);
};

ConsoleServer.prototype.subscribe = function (name, sender) {
    this.listeners[name] = sender;
    this.address_list.for_each(function (address) {
        sender.send({subject:'address', body:address});
    });
    this.connections.for_each(function (conn) {
        sender.send({subject:'connection', body:conn});
    });
    //TODO: poll for changes in address_types
    this.address_ctrl.get_address_types().then(function (address_types) {
        sender.send({subject:'address_types', body:address_types});
    });
};

ConsoleServer.prototype.unsubscribe = function (name) {
    delete this.listeners[name];
};

ConsoleServer.prototype.publish = function (message) {
    for (var name in this.listeners) {
        this.listeners[name].send(message);
    }
};

module.exports = ConsoleServer;
