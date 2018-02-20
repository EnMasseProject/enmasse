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
var amqp = require('rhea');
var AddressSource = require('./internal_address_source.js');
var log = require("./log.js").logger();
var rtr = require('./router.js');
var pod_watcher = require('./pod_watcher.js');
var tls_options = require('./tls_options.js');
var Artemis = require('./artemis.js');
var broker_controller = require('./broker_controller.js');

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

function Ragent() {
    this.known_routers = {};
    this.connected_routers = {};
    this.connected_brokers = {};
    this.addresses = {};//addresses indexed by address and transformed for router syncs
    this.address_list = [];//original list as obtained from source
    this.subscribers = {};
    this.clients = {};
    this.container = amqp.create_container();
    this.container.sasl_server_mechanisms.enable_anonymous();
    this.configure_handlers();
}

Ragent.prototype.subscribe = function (context) {
    this.subscribers[context.connection.container_id] = context.sender;
    //send initial routers:
    var payload = wrap_known_routers(this.connected_routers);
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
        this.sync_router_addresses(this.connected_routers[r]);
    }
}

function transform_address (addr) {
    if (addr.address === undefined || addr.type === undefined) {
        console.error('BAD ADDRESS: %j', addr);
    }
    return {
        name: addr.address,
        multicast: (addr.type === 'multicast' || addr.type === 'topic'),
        store_and_forward: (addr.type === 'queue' || addr.type === 'topic'),
        allocated_to: addr.allocated_to
    };
}

Ragent.prototype.sync_addresses = function (updated) {
    this.sync_brokers(updated);
    this.addresses = updated.map(transform_address).reduce(function (map, a) { map[a.name] = a; return map; }, {});
    log.info('updating addresses: %j', this.addresses);
    this.addresses_updated();
}

Ragent.prototype.sync_router_addresses = function (router) {
    log.info('updating addresses for ' + router.container_id);
    router.sync_addresses(this.addresses);
}

Ragent.prototype.verify_addresses = function (expected) {
    log.debug('verifying addresses to match: ' + JSON.stringify(expected));
    for (var r in this.connected_routers) {
        if (!this.connected_routers[r].verify_addresses(expected)) {
            return false;
        }
    }
    return true;
}

Ragent.prototype.on_router_agent_disconnect = function (context) {
    delete this.known_routers[context.connection.container_id];
}

Ragent.prototype.on_broker_disconnect = function (context) {
    log.info('broker disconnected: %s', context.connection.container_id);
    delete this.connected_brokers[context.connection.container_id];
}

function if_allocated_to (id) {
    return function (a) { return a.allocated_to === id; };
}

function get_address (a) { return a.address; }

Ragent.prototype.sync_brokers = function (addresses) {
    this.address_list = addresses;
    for (var id in this.connected_brokers) {
        this.sync_broker(this.connected_brokers[id], addresses);
    }
}

Ragent.prototype.sync_broker = function (broker, addresses) {
    var allocated = addresses.filter(if_allocated_to(broker.id));
    log.info('syncing broker %s with %j', broker.id, allocated.map(get_address));
    broker.sync_addresses(allocated);
}

var connection_properties = {product:'ragent', container_id:process.env.HOSTNAME};

// watch for- and connect to- other router agents to share knowledge of
// all routers
Ragent.prototype.watch_pods = function (env) {
    var parent = this;
    var agents;
    function RagentPod(pod) {
        this.name = pod.name;
        var options = {
            host:pod.host,
            port:agents.get_port_from_pod_definition(pod, 'ragent'),
            id:pod.name,
            properties:connection_properties
        };
        log.info('connecting to new agent %s', options);
        this.agent = parent.container.connect(options);
        this.agent.open_receiver('routers');
        var agent = this.agent;
        this.close = function () {
            log.info('disconnecting from agent ' + pod.name);
            agent.close();
        }
    }
    agents = require('./podgroup.js')(RagentPod);
    this.watcher = pod_watcher.watch('name=admin', env);
    this.watcher.on('updated', agents.update.bind(agents));
}

Ragent.prototype.on_message = function (context) {
    if (context.message.subject === 'routers') {
        this.known_routers[context.connection.container_id] = unwrap_known_routers (context.message.body);
        this.check_connectivity();
    } else if (context.message.subject === 'health-check') {
        var request = context.message;
        var content = JSON.parse(request.body);
        var reply_to = request.reply_to;
        var response = {to: reply_to};
        response.correlation_id = request.correlation_id;
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
            context.connection.on('connection_close', self.router_disconnected.bind(self));//todo: wait for a bit?
            r.on('ready', function (router) {
                router.retrieve_listeners();
                router.retrieve_addresses();
                router.retrieve_connectors();
                router.on('listeners_updated', self.connected_routers_updated.bind(self));//advertise only once have listeners
                router.on('addresses_updated', self.sync_router_addresses.bind(self));
                router.on('connectors_updated', self.check_router_connectors.bind(self));
                router.on('provisioned', self.check_router_connectors.bind(self));
            });
        } else if (product === 'apache-activemq-artemis') {
            var broker = broker_controller.create_controller(context.connection);
            self.connected_brokers[broker.id] = broker;
            self.sync_broker(broker, self.address_list);
            context.connection.on('disconnected', self.on_broker_disconnect.bind(self));
            context.connection.on('connection_close', self.on_broker_disconnect.bind(self));
        } else {
            if (product === 'ragent') {
                context.connection.on('disconnected', self.on_router_agent_disconnect.bind(self));
                context.connection.on('connection_close', self.on_router_agent_disconnect.bind(self));
            }
            context.connection.on('message', self.on_message.bind(self));
        }
    });

    this.container.on('sender_open', function(context) {
        if (context.sender.source.address === 'routers') {
            self.subscribe(context);
            context.session.on('session_closed', self.unsubscribe.bind(self));
            context.sender.on('sender_closed', self.unsubscribe.bind(self));
            context.connection.on('connection_close', self.unsubscribe.bind(self));
            context.connection.on('disconnected', self.unsubscribe.bind(self));
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
    var address_source = new AddressSource('foo', env);
    address_source.on('addresses_ready', this.sync_addresses.bind(this));
};

Ragent.prototype.listen_probe = function (env) {
    if (env.PROBE_PORT !== undefined) {
        var probe = http.createServer(function (req, res) {
            res.writeHead(200, {'Content-Type': 'text/plain'});
            res.end('OK');
        });
        return probe.listen(env.PROBE_PORT);
    }
};

Ragent.prototype.start_listening = function (env, callback) {
    var options = {properties:connection_properties};
    try {
        options = tls_options.get_server_options(options, env);
        options.port = env.AMQP_PORT === undefined ? 55671 : env.AMQP_PORT;
    } catch (error) {
        options.port = env.AMQP_PORT === undefined ? 55672 : env.AMQP_PORT;
        log.warn('Error setting TLS options ' + error + ' using ' + JSON.stringify(options));
    }
    var server = this.listen(options);
    server.on('listening', function() {
        log.info("Router agent listening on " + server.address().port);
        if (callback) callback(server.address().port);
    });
};

Ragent.prototype.run = function (env, callback) {
    this.start_listening(env, callback);
    this.subscribe_to_addresses(env);
    this.listen_probe(env);
};

if (require.main === module) {
    new Ragent().run(process.env);
} else {
    module.exports = Ragent;
}
