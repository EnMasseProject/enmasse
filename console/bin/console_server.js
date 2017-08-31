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
var path = require('path');
var rhea = require('rhea');
var WebSocketServer = require('ws').Server;
var address_ctrl = require('../lib/address_ctrl.js');
var AddressList = require('../lib/address_list.js');
var address_list = new AddressList();
var auth_service = require('../lib/auth_service.js');
var RouterStats = require('../lib/router_stats.js');
var router_stats = new RouterStats();
var BrokerStats = require('../lib/broker_stats.js');
var broker_stats = new BrokerStats();
var Registry = require('../lib/registry.js');
var http = require('http');

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

var server = http.createServer(app);
server.listen(8080);

var ws_server = new WebSocketServer({server: server, 'path': '/websocket'});
var amqp_container = rhea.create_container({autoaccept:false});
ws_server.on('connection', function (ws) {
    console.log('Accepted incoming websocket connection');
    amqp_container.websocket_accept(ws);
});

var connections = new Registry();
//connections.debug = true;

var listeners = {};

function subscribe(name, sender) {
    listeners[name] = sender;
    address_list.for_each(function (address) {
        sender.send({subject:'address', body:address});
    });
    connections.for_each(function (conn) {
        sender.send({subject:'connection', body:conn});
    });
    //TODO: poll for changes in address_types
    address_ctrl.get_address_types().then(function (address_types) {
        sender.send({subject:'address_types', body:address_types});
    });
}

function unsubscribe(name) {
    delete listeners[name];
}

setInterval(function () {
    router_stats.retrieve(address_list, connections);
}, 5000);//poll router stats every 5 secs

setInterval(function () {
    broker_stats.retrieve(address_list);
}, 5000);//poll broker stats every 5 secs

address_list.on('updated', function (address) {
    for (var name in listeners) {
        listeners[name].send({subject:'address',body:address});
    }
});
address_list.on('deleted', function (address) {
    console.log('address ' + address.address + ' has been deleted, notifying clients...');
    for (var name in listeners) {
        listeners[name].send({subject:'address_deleted',body:address.address});
    }
});
connections.on('updated', function (conn) {
    for (var name in listeners) {
        listeners[name].send({subject:'connection',body:conn});
    }
});
connections.on('deleted', function (conn) {
    console.log('connection ' + conn.host + ' has been deleted, notifying clients...');
    for (var name in listeners) {
        listeners[name].send({subject:'connection_deleted',body:conn.id});
    }
});

amqp_container.on('sender_open', function (context) {
    subscribe(context.connection.remote.open.container_id, context.sender);
});
amqp_container.on('sender_close', function (context) {
    unsubscribe(context.connection.remote.open.container_id);
});
amqp_container.on('connection_close', function (context) {
    unsubscribe(context.connection.remote.open.container_id);
});
amqp_container.on('disconnected', function (context) {
    if (context.connection.remote.open) {
        unsubscribe(context.connection.remote.open.container_id);
    }
});
amqp_container.on('message', function (context) {
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
        address_ctrl.create_address(context.message.body).then(accept).catch(reject);
    } else if (context.message.subject === 'delete_address') {
        console.log('deleting address ' + JSON.stringify(context.message.body));
        address_ctrl.delete_address(context.message.body).then(accept).catch(reject);
    } else {
        console.log('ignoring message: ' + context.message);
    }
});
