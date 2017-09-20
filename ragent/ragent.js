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

var http = require('http');
var util = require('util');
var log = require("./log.js").logger();
var amqp = require('rhea');
var rtr = require('./router.js');
var podwatch = require('./podwatch.js');
var tls_options = require('./tls_options.js');


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
            log.info('   ' + r + ' => ' + router.listeners);
        }
    }
}

function Ragent() {
    this.known_routers = {};
    this.connected_routers = {};
    this.addresses = {};
    this.subscribers = {};
    this.clients = {};
    this.container = amqp.create_container();
    this.container.sasl_server_mechanisms.enable_anonymous();
    this.configure_handlers();
}

Ragent.prototype.subscribe = function (context) {
    this.subscribers[context.connection.container_id] = context.sender;
    //send initial routers:
    var payload = wrap_known_routers(connected_routers);
    context.sender.send({subject:'routers', body:payload});
};

Ragent.prototype.unsubscribe = function  (context) {
    delete this.subscribers[context.connection.container_id];
}

Ragent.prototype.add_client = function (context) {
    var id = amqp.generate_uuid();
    context.sender.set_source({address:id});
    this.clients[context.connection.container_id] = context.sender;
}

Ragent.prototype.remove_client = function (context) {
    delete this.clients[context.connection.container_id];
}

Ragent.prototype.get_all_routers = function () {
    var all_routers = {};
    for (var g in this.known_routers) {
        var group = this.known_routers[g];
        for (var key in group) {
            all_routers[key] = group[key];
        }
    }
    for (var r in this.connected_routers) {
        all_routers[r] = this.connected_routers[r];
    }

    return all_routers;
}

Ragent.prototype.check_connectivity = function () {
    var all_routers = this.get_all_routers();

    for (var r in this.connected_routers) {
        this.check_router_connectors(this.connected_routers[r], all_routers);
    }
}

Ragent.prototype.check_router_connectors = function (router, all_routers) {
    if (router.is_ready_for_connectivity_check()) {
        log.info('checking connectivity for ' + router.container_id);
        router.check_connectors(all_routers || this.get_all_routers());
    } else {
        log.info(router.container_id + ' not ready for connectivity check: ' + router.initial_provisioning_completed + ' ' + (router.connectors !== undefined));
    }
}

Ragent.prototype.connected_routers_updated = function (router) {
    this.check_connectivity();
    for (var id in this.subscribers) {
        var sender = this.subscribers[id];
        var payload = wrap_known_routers(this.connected_routers);
        sender.send({subject:'routers', body:payload});
    }
}

Ragent.prototype.router_disconnected = function (context) {
    delete this.connected_routers[context.connection.container_id];
    log.info('router ' + context.connection.container_id + ' disconnected');
    //update now, or wait a bit?
    //connected_routers_updated();
}

Ragent.prototype.addresses_updated = function () {
    for (var r in this.connected_routers) {
        try {
            this.sync_router_addresses(this.connected_routers[r]);
        } catch (e) {
            log.error('ERROR: failed to check addresses on router ' + r + ': ' + e + '; ' + JSON.stringify(this.addresses));
        }
    }
}

Ragent.prototype.sync_addresses = function (updated) {
    log.info('updating addresses: ' + JSON.stringify(updated));
    this.addresses = updated;
    this.addresses_updated();
}

Ragent.prototype.sync_router_addresses = function (router) {
    log.info('updating addresses for ' + router.container_id);
    router.sync_addresses(this.addresses);
}

Ragent.prototype.verify_addresses = function (expected) {
    log.info('verifying addresses to match: ' + JSON.stringify(expected));
    for (var r in this.connected_routers) {
        if (!this.connected_routers[r].verify_addresses(expected)) {
            return false;
        }
    }
    return true;
}

Ragent.prototype.on_router_agent_disconnect = function (context) {
    delete context.connection.connection_id;
}

function watch_pods(connection) {
    var watcher = undefined;
    if (process.env.ADMIN_SERVICE_HOST) {
        watcher = podwatch.watch_pods(connection, {"name": "admin"});
    } else {
        watcher = podwatch.watch_pods(connection, {"name": "ragent"});
    }
    var agent_connections = {}
    watcher.on('added', function (pods) {
        for (var pod_name in pods) {
            var pod = pods[pod_name];

            //pod name will be containerid, use that as the id for debug logging
            var agent_conn_info = {host:pod.host, port:port, id:pod_name, properties:connection_properties};
            var agent_conn = amqp.connect(agent_conn_info);
            agent_conn.open_receiver('routers');
            agent_connections[pod_name] = agent_conn;
            log.info('connecting to new agent ' + JSON.stringify(agent_conn_info));
        }
    });
    watcher.on('removed', function (pods) {
        for (var pod_name in pods) {
            var conn = agent_connections[pod_name];
            if (conn !== undefined) {
                log.info('disconnecting from agent ' + pod_name);
                conn.close();
            }
        }
    });
}

var connection_properties = {product:'ragent', container_id:process.env.HOSTNAME};

Ragent.prototype.on_message = function (context) {
    if (context.message.subject === 'routers') {
        this.known_routers[context.connection.container_id] = unwrap_known_routers (context.message.body);
        this.check_connectivity();
    } else if (context.message.subject === 'enmasse.io/v1/AddressList') {
        var semantics = {};
        var body;
        try{
            body = JSON.parse(context.message.body || '{}');
        } catch (e) {
            log.error('failed to parse addresses: ' + e + '; ' + context.message.body);
            return;
        }
        if (util.isArray(body.items)) {
            for (var i = 0; i < body.items.length; i++) {
                var addr = body.items[i];
                semantics[addr.spec.address] = {
                    name: addr.spec.address,
                    multicast: (addr.spec.type === 'multicast' || addr.spec.type === 'topic'),
                    store_and_forward: (addr.spec.type === 'queue' || addr.spec.type === 'topic')
                }
            }
        } else if (body.items !== undefined) {
            throw Error('invalid type for body.items: ' + (typeof body.items));
        }
        this.sync_addresses(semantics);
    } else if (context.message.subject === 'health-check') {
        var request = context.message;
        var content = JSON.parse(request.body);
        var reply_to = request.reply_to;
        var response = {to: reply_to};
        if (request.correlation_id) {
            response.correlation_id = request.correlation_id;
        }
        response.body = this.verify_addresses(content);
        var sender = this.clients[context.connection.container_id];
        if (sender) {
            sender.send(response);
        }
    } else {
        log.info('ERROR: unrecognised subject ' + context.message.subject);
    }
}

Ragent.prototype.configure_handlers = function () {
    var self = this;
    this.container.on('connection_open', function(context) {
        var product = get_product(context.connection);
        if (product === 'qpid-dispatch-router') {
            var r = rtr.connected(context.connection);
            log.info('Router connected from ' + r.container_id);
            self.connected_routers[r.container_id] = r;
            context.connection.on('disconnected', self.router_disconnected.bind(self));//todo: wait for a bit?
            r.on('ready', function (router) {
                router.retrieve_listeners();
                router.retrieve_addresses();
                router.retrieve_connectors();
                router.on('listeners_updated', self.connected_routers_updated.bind(self));//advertise only once have listeners
                router.on('addresses_updated', self.sync_router_addresses.bind(self));
                router.on('connectors_updated', self.check_router_connectors.bind(self));
                router.on('provisioned', self.check_router_connectors.bind(self));
            });
        } else {
            if (product === 'qdconfigd') {
                context.connection.on('disconnected', self.on_router_agent_disconnect.bind(self));
                context.connection.on('connection_close', self.on_router_agent_disconnect.bind(self));
            }
            context.connection.on('message', self.on_message.bind(self));
        }
    });

    this.container.on('sender_open', function(context) {
        if (context.sender.source.address === 'routers') {
            self.subscribe(context);
            context.session.on('session_closed', unsubscribe);
            context.sender.on('sender_closed', unsubscribe);
            context.connection.on('connection_close', unsubscribe);
            context.connection.on('disconnected', unsubscribe);
        } else {
            if (context.sender.source.dynamic) {
                self.add_client(context);
                context.session.on('session_closed', self.remove_client.bind(self));
                context.sender.on('sender_closed', self.remove_client.bind(self));
                context.connection.on('connection_close', self.remove_client.bind(self));
                context.connection.on('disconnected', self.remove_client.bind(self));
            }
        }
    });

    this.container.on('receiver_open', function(context) {
        if (context.receiver.target.address === 'health-check') {
            context.receiver.set_target({address:context.receiver.remote.attach.target.address});
        }
    });

};

Ragent.prototype.listen = function (options) {
    this.server = this.container.listen(options);
    return this.server;
}

Ragent.prototype.subscribe_to_addresses = function (env) {
    var config_host = env.ADMIN_SERVICE_HOST
    var config_port = env.ADMIN_SERVICE_PORT_CONFIGURATION

    if (env.CONFIGURATION_SERVICE_HOST) {
        config_host = env.CONFIGURATION_SERVICE_HOST
        config_port = env.CONFIGURATION_SERVICE_PORT
    }

    if (config_host) {
        this.container.options.username = 'ragent';
        var options;
        try {
            options = tls_options.get_client_options({ host: config_host, port: config_port, properties: connection_properties });
        } catch (error) {
            options = { host: config_host, port: config_port, properties: connection_properties };
        }

        var conn = this.container.connect(options);
        conn.open_receiver('v1/addresses');
        watch_pods(conn);
    }
};

Ragent.prototype.listen_probe = function () {
    if (process.env.PROBE_PORT) {
        var probe = http.createServer(function (req, res) {
            res.writeHead(200, {'Content-Type': 'text/plain'});
            res.end('OK');
        });
        probe.listen(process.env.PROBE_PORT);
    }
};

Ragent.prototype.run = function () {
    var options;
    try {
        options = tls_options.get_server_options({port:55671, properties:connection_properties});
    } catch (error) {
        options = {port:55672, properties:connection_properties};
        console.warn('Error setting TLS options ' + error + ' using ' + options);
    }
    var server = this.listen(options);
    server.on('listening', function() {
        log.info("Router agent listening on " + server.address().port);
    });
    this.subscribe_to_addresses(process.env);
    this.listen_probe();
};

if (require.main === module) {
    new Ragent().run();
} else {
    module.exports = Ragent;
}
