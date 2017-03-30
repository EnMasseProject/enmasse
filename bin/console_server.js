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
var path = require('path');
var rhea = require('rhea');
var WebSocketServer = require('ws').Server;
var address_ctrl = require('../lib/address_ctrl.js');
var address_list = require('../lib/address_list.js');
var router_stats = require('../lib/router_stats.js');
var broker_stats = require('../lib/broker_stats.js');
var UserCtrl = require('../lib/user_ctrl.js');
var Registry = require('../lib/registry.js');

var app = express();
app.set('port', 8080);
app.use(express.static(path.join(__dirname, '../www/')))
var http_server = app.listen(app.get('port'));

var ws_server = WebSocketServer({'port':56720});
var amqp_container = rhea.create_container({autoaccept:false});
ws_server.on('connection', function (ws) {
    console.log('Accepted incoming websocket connection');
    amqp_container.websocket_accept(ws);
});

var connections = new Registry();
//connections.debug = true;

var users = new Registry();
users.debug = true;
var user_ctrl;
if (process.env.SASLDB) {
    user_ctrl = new UserCtrl(process.env.SASLDB, process.env.SASLDOMAIN || 'enmasse');
    function update_users() {
        user_ctrl.list_users().then(function (results) {
            results.forEach(function (u) { users.update(u.name, u); });
        });
    }
    setInterval(update_users, 5000);//check user db every 5 secs
}


var listeners = {};

function subscribe(name, sender) {
    listeners[name] = sender;
    address_list.for_each(function (address) {
        sender.send({subject:'address', body:address});
    });
    connections.for_each(function (conn) {
        sender.send({subject:'connection', body:conn});
    });
    users.for_each(function (user) {
        sender.send({subject:'user', body:user});
    });
    //TODO: poll for changes in flavors
    address_ctrl.get_flavors().then(function (flavors) {
        var flavor_list = [];
        for (var name in flavors) {
            var flavor = flavors[name];
            flavor.name = name;
            flavor_list.push(flavor);
        }
        sender.send({subject:'flavors', body:flavor_list});
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

users.on('updated', function (user) {
    console.log('user changed: ' + JSON.stringify(user));
    for (var name in listeners) {
        listeners[name].send({subject:'user',body:user});
    }
});
users.on('deleted', function (user) {
    for (var name in listeners) {
        listeners[name].send({subject:'user_deleted',body:user.name});
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
    unsubscribe(context.connection.remote.open.container_id);
});
amqp_container.on('message', function (context) {
    var accept = function () {
        console.log(context.message.subject + ' succeeded');
        context.delivery.accept();
    };
    var accept_and_update_users = function () {
        accept();
        update_users();
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
    } else if (context.message.subject === 'create_user' && user_ctrl) {
        console.log('creating user ' + JSON.stringify(context.message.body));
        user_ctrl.create_user(context.message.body.name, context.message.body.password).then(accept_and_update_users).catch(reject);
    } else if (context.message.subject === 'delete_user' && user_ctrl) {
        console.log('deleting user ' + JSON.stringify(context.message.body));
        user_ctrl.delete_user(context.message.body).then(accept_and_update_users).catch(reject);
    } else {
        console.log('ignoring message: ' + context.message);
    }
});
