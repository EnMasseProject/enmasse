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
var podwatch = require('./podwatch.js');

var known_routers = {};
var connected_routers = {};
var addresses = {};
var subscribers = {};
var clients = {};

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

function add_client(context) {
    var id = amqp.generate_uuid();
    context.sender.set_source({address:id});
    clients[context.connection.container_id] = context.sender;
}

function remove_client(context) {
    delete clients[context.connection.container_id];
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
        console.log(router.container_id + ' not ready for connectivity check: ' + router.initial_provisioning_completed + ' ' + (router.connectors !== undefined));
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
        try {
            sync_router_addresses(connected_routers[r]);
        } catch (e) {
            console.log('ERROR: failed to check addresses on router ' + r + ': ' + e + '; ' + addresses);
        }
    }
}

function sync_addresses (updated) {
    console.log('updating addresses: ' + JSON.stringify(updated));
    addresses = updated;
    addresses_updated();
}

function verify_addresses (expected) {
    console.log('verifying addresses to match: ' + JSON.stringify(expected));
    for (var r in connected_routers) {
        if (!connected_routers[r].verify_addresses(expected)) {
            return false;
        }
    }
    return true;
}

function sync_router_addresses (router) {
    console.log('updating addresses for ' + router.container_id);
    router.sync_addresses(addresses);
}

function on_router_agent_disconnect (context) {
    delete context.connection.connection_id;
}

function watch_pods(connection) {
    var watcher = undefined;
    if (process.env.ADMIN_SERVICE_HOST) {
        watcher = podwatch.watch_pods(connection, {"name": "admin"});
    } else {
        watcher = podwatch.watch_pods(connection, {"name": "ragent"});
    }
    watcher.on('added', function (pods) {
        for (var pod_name in pods) {
            var pod = pods[pod_name];

            //pod name will be containerid, use that as the id for debug logging
            var agent_conn = {host:pod.host, port:port, id:pod.name, properties:connection_properties};
            amqp.connect(agent_conn);
            console.log('connecting to new agent ' + JSON.stringify(agent_conn));
        }
    });
    watcher.on('removed', function (pods) {
        console.log('agents removed from service: ' + JSON.stringify(pods));
    });
}

var connection_properties = {product:'qdconfigd', container_id:process.env.HOSTNAME};

function on_message(context) {
    if (context.message.subject === 'routers') {
        known_routers[context.connection.container_id] = unwrap_known_routers (context.message.body);
        check_connectivity();
    } else if (context.message.subject === 'addresses' || !context.message.subject) {
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
                sync_addresses(content);
            } catch (e) {
                console.log('ERROR: failed to parse addresses as JSON: ' + e + '; ' + context.message.body);
            }
        } else if (body_type  === 'object') {
            sync_addresses(context.message.body);
        } else {
            console.log('ERROR: unrecognised type for addresses: ' + body_type + ' ' + context.message.body);
        }
    } else if (context.message.subject === 'health-check') {
        var request = context.message;
        var content = JSON.parse(request.body);
        var reply_to = request.reply_to;
        var response = {to: reply_to};
        if (request.correlation_id) {
            response.correlation_id = request.correlation_id;
        }
        response.body = verify_addresses(content);
        var sender = clients[context.connection.container_id];
        if (sender) {
            sender.send(response);
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
            router.on('addresses_updated', sync_router_addresses);
            router.on('connectors_updated', check_router_connectors);
            router.on('provisioned', check_router_connectors);
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
    } else {
        if (context.sender.source.dynamic) {
            add_client(context);
            context.session.on('session_closed', remove_client);
            context.sender.on('sender_closed', remove_client);
            context.connection.on('connection_close', remove_client);
            context.connection.on('disconnected', remove_client);
        }
    }
});

amqp.on('receiver_open', function(context) {
    if (context.receiver.target.address === 'health-check') {
        context.receiver.set_target({address:context.receiver.remote.attach.target.address});
    }
});

var port = 55672;
amqp.sasl_server_mechanisms.enable_anonymous();
amqp.listen({port:port, properties:connection_properties});

var config_host = process.env.ADMIN_SERVICE_HOST
var config_port = process.env.ADMIN_SERVICE_PORT_CONFIGURATION

if (process.env.CONFIGURATION_SERVICE_HOST) {
    config_host = process.env.CONFIGURATION_SERVICE_HOST
    config_port = process.env.CONFIGURATION_SERVICE_PORT
}

if (config_host) {
    amqp.options.username = 'ragent';
    var conn = amqp.connect({host:config_host, port:config_port, properties:connection_properties});
    conn.open_receiver('maas');
    watch_pods(conn);
}
