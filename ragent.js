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

var amqp = require('rhea');
var rtr = require('./router.js');

var known_routers = {};
var connected_routers = {};
var addresses = {};
var subscribers = {};

function subscribe(context) {
    subscribers[context.connection.container_id] = context.sender;
    //send initial routers:
    for (var id in connected_routers) {
        context.sender.send({subject:'routers', body:connected_routers[id].listeners});
    }
}

function unsubscribe (context) {
    delete subscribers[context.connection.container_id];
}

function wrap_known_routers (routers) {
    var data = {};
    for (var k in routers) {
        data[k] = routers[k].listeners;
    }
    return data;
}

function unwrap_known_routers (data) {
    var result = {};
    for (var k in data) {
        result[k] = rtr.known(k, data[k]);
    }
    return result;
}

function get_product (connection) {
    if (connection.properties) {
	return connection.properties.product;
    } else {
	return undefined;
    }
}

function print_routers(routers) {
    for (var r in routers) {
        var router = routers[r];
        if (router) {
            console.log('   ' + r + ' => ' + router.listeners);
        }
    }
}

function get_all_routers() {
    var all_routers = {};
    for (var g in known_routers) {
        var group = known_routers[g];
        for (var key in group) {
            all_routers[key] = group[key];
        }
    }
    for (var r in connected_routers) {
        all_routers[r] = connected_routers[r];
    }

    return all_routers;
}

function check_connectivity() {
    var all_routers = get_all_routers();

    for (var r in connected_routers) {
        check_router_connectors(connected_routers[r], all_routers);
    }
}

function check_router_connectors (router, all_routers) {
    if (router.is_ready_for_connectivity_check()) {
        console.log('checking connectivity for ' + router.container_id);
        router.check_connectors(all_routers || get_all_routers());
    } else {
        console.log(router.container_id + ' not ready for connectivity check: ' + router.initial_provisioning_completed + ' ' + router.connectors !== undefined);
    }
}

function connected_routers_updated (router) {
    check_connectivity();
    for (var id in subscribers) {
        var sender = subscribers[id];
        sender.send({subject:router.container_id, kind:'router', status:'defined', body:router.listeners});
    }
}

function router_disconnected (context) {
    delete connected_routers[context.connection.container_id];
    console.log('router ' + context.connection.container_id + ' disconnected');
    //update now, or wait a bit?
    //connected_routers_updated();
}

function addresses_updated () {
    for (var r in connected_routers) {
        check_router_addresses(connected_routers[r]);
    }
}

function check_router_addresses (router) {
    console.log('checking addresses for ' + router.container_id);
    router.check_addresses(addresses);
}

function on_router_agent_disconnect (context) {
    delete context.connection.connection_id;
}

var connection_properties = {product:'qdconfigd', container_id:process.env.HOSTNAME};

function on_message(context) {
    if (context.message.subject === 'routers') {
        known_routers[context.connection.container_id] = unwrap_known_routers (context.message.body);
        check_connectivity();
    } else if (context.message.subject === 'addresses' || context.message.subject === null) {
        var body_type = typeof context.message.body;
        if (body_type  === 'string') {
            try {
                var content = JSON.parse(context.message.body);
                if (content.json !== undefined) {
                    content = content.json;
                }
                for (var v in content) {
                    if (content[v].name === undefined) {
                        content[v].name = v;
                    }
                    if (content[v]['store-and-forward'] !== undefined) {
                        content[v].store_and_forward = content[v]['store-and-forward'];
                        delete content[v]['store-and-forward'];
                    }
                }
                addresses = content;
                addresses_updated();
            } catch (e) {
                console.log('ERROR: failed to parse addresses as JSON: ' + e);
            }
        } else if (body_type  === 'object') {
            addresses = context.message.body;
            addresses_updated();
        } else {
            console.log('ERROR: unrecognised type for addresses: ' + body_type + ' ' + context.message.body);
        }
    } else if (context.message.subject === 'connect') {
        var details = context.message.body;
        details.properties = connection_properties;
        amqp.connect();
    } else {
        console.log('ERROR: unrecognised subject ' + context.message.subject);
    }
}

amqp.on('connection_open', function(context) {
    var product = get_product(context.connection);
    if (product === 'qpid-dispatch-router') {
        var r = rtr.connected(context.connection);
        console.log('Router connected from ' + r.container_id);
        connected_routers[r.container_id] = r;
        context.connection.on('disconnected', router_disconnected);//todo: wait for a bit?
        r.on('ready', function (router) {
            router.retrieve_listeners();
            router.retrieve_addresses();
            router.retrieve_connectors();
            router.on('listeners_updated', connected_routers_updated);//advertise only once have listeners
            router.on('addresses_updated', check_router_addresses);
            router.on('connectors_updated', check_router_connectors);
        });
    } else {
        if (product === 'qdconfigd') {
            context.connection.on('disconnected', on_router_agent_disconnect);
            context.connection.on('connection_close', on_router_agent_disconnect);
        }
	context.connection.on('message', on_message);
    }
});

amqp.on('sender_open', function(context) {
    if (context.sender.source.address === 'routers') {
        subscribe(context);
        context.session.on('session_closed', unsubscribe);
        context.sender.on('sender_closed', unsubscribe);
        context.connection.on('connection_close', unsubscribe);
        context.connection.on('disconnected', unsubscribe);
    }
});

amqp.sasl_server_mechanisms.enable_anonymous();
amqp.listen({port:55672, properties:connection_properties});

if (process.env.KUBERNETES_SERVICE_HOST) {
    var watcher = require('./kube_utils.js').watch_service('ragent');
    watcher.on('added', function (procs) {
	for (var name in procs) {
            if (name < process.env.HOSTNAME) {
	        var proc = procs[name];
	        proc.id = name;//hostname will be containerid, use that as the id for debug logging
                proc.properties = connection_properties;
	        amqp.connect(proc);
	        console.log('connecting to new agent ' + JSON.stringify(proc));
            }
	}
    });
    watcher.on('removed', function (procs) {
	console.log('agents removed from service: ' + JSON.stringify(procs));
    });
}

if (process.env.CONFIGURATION_SERVICE_HOST) {
    amqp.options.username = 'ragent';
    var conn = amqp.connect({host:process.env.CONFIGURATION_SERVICE_HOST, port:process.env.CONFIGURATION_SERVICE_PORT, properties:connection_properties});
    conn.open_receiver('maas');
}
