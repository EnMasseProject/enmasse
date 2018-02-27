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

var util = require("util");
var events = require("events");
var qdr = require("./qdr.js");
var myutils = require("./utils.js");
var log = require("./log.js").logger();

function create_record(names, values) {
    var record = {};
    for (var j = 0; j < names.length; j++) {
        record[names[j]] = values[j];
    }
    return record;
}

function has_inter_router_role (record) {
    return record.role === 'inter-router';
}

function to_host_port (record) {
    return record.host + ':' + record.port;
}

function by_name (o) {
    return o.name;
}

function not_overridden (o) {
    return o.name.indexOf('override') !== 0;
}

function address_equivalence(a, b) {
    if (a === undefined) return b === undefined;
    return a.name === b.name && a.multicast === b.multicast && a.store_and_forward === b.store_and_forward;
}

function link_route_equivalence(a, b) {
    if (a === undefined) return b === undefined;
    return a.name === b.name && a.prefix === b.prefix && a.dir === b.dir;
}

/**
 * A KnownRouter instance represents routers this process knows about
 * but is not directly connected to (or responsible for).
 */
var KnownRouter = function (container_id, listeners) {
    this.container_id = container_id;
    this.listeners = listeners;
};

/**
 * A ConnectedRouter represents a router this process is connected to
 * and is therefore resonsible for configuring.
 */
var ConnectedRouter = function (connection) {
    events.EventEmitter.call(this);
    this.container_id = connection.container_id;
    this.listeners = undefined;
    this.connectors = undefined;
    this.addresses = {};
    this.initial_provisioning_completed = false;
    this.addresses_synchronized = false;

    this.router_mgmt = new qdr.Router(connection);
    this.update_types = {
        address: {add: this.define_address_and_autolinks.bind(this), remove: this.delete_address_and_autolinks.bind(this), equivalence: address_equivalence},
        link_route: {add: this.define_link_route.bind(this), remove: this.delete_link_route.bind(this), equivalence: link_route_equivalence}
    };
    var self = this;
    connection.on('receiver_open', function () {
        self.emit('ready', self);
    });
};

util.inherits(ConnectedRouter, events.EventEmitter);

function remove_current(set) {
    if (this.listeners) {
        this.listeners.forEach(function (l) { delete set[l]; });
    }
}

function has_listener(host_port) {
    return this.listeners && this.listeners.indexOf(host_port) !== -1;
}

ConnectedRouter.prototype.has_connector_to = function (router) {
    return this.connectors && this.connectors.some(has_listener.bind(router));
};

ConnectedRouter.prototype.expects_connector_to = function (router) {
    return router.container_id < this.container_id && router.listeners && router.listeners.length > 0;
};

ConnectedRouter.prototype.is_missing_connector_to = function (router) {
    return this.expects_connector_to(router) && !this.has_connector_to(router);
};

ConnectedRouter.prototype.is_ready_for_connectivity_check = function () {
    return this.initial_provisioning_completed && this.connectors !== undefined;
}

ConnectedRouter.prototype.check_connectors = function (routers) {
    var missing = [];
    var stale = myutils.index(this.connectors);
    for (var r in routers) {
        var router = routers[r];
        if (router === this) continue;
        if (this.is_missing_connector_to(router)) {
            missing.push(router.listeners[0]);
        }
        remove_current.call(router, stale);
    }
    stale = Object.keys(stale);

    var num_connectors = 1;
    if (process.env.ROUTER_NUM_CONNECTORS) {
        num_connectors = process.env.ROUTER_NUM_CONNECTORS;
    }
    var do_create = this.forall_connectors.bind(this, num_connectors, this.create_connector.bind(this));
    var do_delete = this.forall_connectors.bind(this, num_connectors, this.delete_connector.bind(this));
    var work = missing.map(do_create).concat(stale.map(do_delete));
    var self = this;
    if (work.length) {
        log.info('[%s] checking connectors on router, missing=%j, stale=%j', this.container_id, missing, stale);
        //if made changes, requery when they are complete
        Promise.all(work).then(function () {
            self.retrieve_connectors();
        }).catch(function (error) {
            log.warn('[%s] error on updating connectors: %s', self.container_id, error);
            self.retrieve_connectors();
        });
        //prevent any updates to connectors until we have re-retrieved
        //them from router after updates:
        this.connectors = undefined;
    } else {
        log.info('[%s] fully connected', this.container_id);
    }
};

function is_topic(address) {
    return address.multicast === true && address.store_and_forward === true;
}

function to_link_route(direction, address) {
    return {name:address.name + '_' + direction, prefix:address.name, dir:direction, containerId:address.allocated_to};
}

function to_in_link_route(address) {
    return to_link_route('in', address);
}

function to_out_link_route(address) {
    return to_link_route('out', address);
}

function to_link_routes(address_list) {
    var links = address_list.map(to_in_link_route).concat(address_list.map(to_out_link_route));
    return myutils.index(links, by_name);
}

function update(actual, desired, type) {
    var removed = myutils.values(myutils.difference(actual, desired, type.equivalence));
    var added = myutils.values(myutils.difference(desired, actual, type.equivalence));
    return removed.map(type.remove).concat(added.map(type.add));
}

ConnectedRouter.prototype.verify_addresses = function (expected) {
    if (!expected || this.addresses === undefined || this.link_routes == undefined) {
        return false;
    }

    for (var i = 0; i < expected.length; i++) {
        var address = expected[i];
        if (address["store_and_forward"] && !address["multicast"]) {
            if (this.addresses[address.name] === undefined) {
                return false;
            }
        }
    }
    return true;
}

ConnectedRouter.prototype.sync_addresses = function (desired) {
    if (this.addresses === undefined || this.link_routes === undefined) {
        log.info('[%s] router is not ready for address check', this.container_id);
        return;
    }
    var topics = {};
    var others = {};
    myutils.separate(desired, is_topic, topics, others);

    var work = update(this.addresses, others, this.update_types.address);
    work = work.concat(update(this.link_routes, to_link_routes(myutils.values(topics)), this.update_types.link_route));

    var self = this;
    if (work.length) {
        this.addresses_synchronized = false;
        log.info('[%s] updating addresses...', self.container_id);
        //if made changes, requery when they are complete
        Promise.all(work).then(function () {
            log.info('[%s] addresses updated', self.container_id);
            self.retrieve_addresses();
        }).catch(function (error) {
            log.warn('[%s] error on updating addresses: %s', self.container_id, error);
            self.retrieve_addresses();
        });
        //prevent any updates to addresses until we have re-retrieved
        //them from router after updates:
        this.addresses = undefined;
        this.link_routes = undefined;
    } else {
        if (this.initial_provisioning_completed !== true) {
            this.initial_provisioning_completed = true;
            this.emit('provisioned', this);
        }
        if (!this.addresses_synchronized) {
            this.addresses_synchronized = true;
            log.info('[%s] addresses synchronized', self.container_id);
        }
    }
};

ConnectedRouter.prototype.forall_connectors = function (num_connectors, connector_operation, host_port) {
    var futures = [];
    for (var i = 0; i < num_connectors; i++) {
        var connector_name = host_port + "-" + i;
        futures.push(connector_operation(host_port, connector_name));
    }
    return Promise.all(futures);
}

ConnectedRouter.prototype.create_connector = function (host_port, connector_name) {
    log.info('[%s] creating connector %s to %s', this.container_id, connector_name, host_port);
    var parts = host_port.split(':');
    return this.create_entity('connector', connector_name, {role:'inter-router', host:parts[0], port:parts[1],
                                                            sslProfile:'ssl_internal_details', verifyHostName:'no'});
};

ConnectedRouter.prototype.delete_connector = function (host_port, connector_name) {
    log.info('[%s] deleting connector %s to %s', this.container_id, connector_name, host_port);
    return this.delete_entity('connector', connector_name);
};

function actual_distribution(address) {
    return address.distribution === "multicast";
}

function from_router_address(address) {
    return {name:address.prefix, multicast:actual_distribution(address), store_and_forward:address.waypoint};
}

ConnectedRouter.prototype.define_address_and_autolinks = function (address) {
    if (address.store_and_forward) {
        return Promise.all([this.define_address(address), this.define_autolink(address, "in"), this.define_autolink(address, "out")]);
    } else {
        return this.define_address(address);
    }
}

ConnectedRouter.prototype.delete_address_and_autolinks = function (address) {
    if (address.store_and_forward) {
        return Promise.all([this.delete_address(address), this.delete_autolink(address, "in"), this.delete_autolink(address, "out")]);
    } else {
        return this.delete_address(address);
    }
}

ConnectedRouter.prototype.define_address = function (address) {
    var dist = address.multicast ? "multicast" : "balanced";
    log.info('[%s] defining address %s', this.container_id, address.name);
    return this.router_mgmt.create_address({name:address.name, prefix:address.name, distribution:dist, waypoint:address.store_and_forward});
};

ConnectedRouter.prototype.delete_address = function (address) {
    log.info('[%s] deleting address %s', this.container_id, address.name);
    return this.router_mgmt.delete_address(address);
};

ConnectedRouter.prototype.define_autolink = function (address, direction) {
    var name = (direction == "in" ? "autoLinkIn" : "autoLinkOut") + address.name;
    log.info('[%s] defining %s autolink for %s', this.container_id, direction, address.name);
    var brokerId = address.allocated_to || address.name;
    return this.create_entity('org.apache.qpid.dispatch.router.config.autoLink', name, {dir:direction, addr:address.name, containerId:brokerId});
}

ConnectedRouter.prototype.delete_autolink = function (address, direction) {
    var name = (direction == "in" ? "autoLinkIn" : "autoLinkOut") + address.name;
    log.info('[%s] deleting %s autolink for %s', this.container_id, direction, address.name);
    return this.delete_entity('org.apache.qpid.dispatch.router.config.autoLink', name);
}

ConnectedRouter.prototype.define_link_route = function (route) {
    log.info('[%s] defining %s link route %s', this.container_id, route.dir, route.prefix);
    var props = {'prefix':route.prefix, dir:route.dir};
    if (route.containerId) props.containerId = route.containerId;
    return this.create_entity('org.apache.qpid.dispatch.router.config.linkRoute', route.name, props);
};

ConnectedRouter.prototype.delete_link_route = function (route) {
    log.info('[%s] deleting %s link route %s', this.container_id, route.dir, route.prefix);
    return this.delete_entity('org.apache.qpid.dispatch.router.config.linkRoute', route.name);
};

ConnectedRouter.prototype.retrieve_listeners = function () {
    var self = this;
    return this.query('listener', {attributeNames:['identity', 'name', 'host', 'port', 'role']}).then(function (results) {
        self.listeners = results.filter(has_inter_router_role).map(to_host_port);
        log.debug('[%s] retrieved listeners: %j', self.container_id, self.listeners);
        self.emit('listeners_updated', self);
    }).catch(function (error) {
        log.warn('[%s] failed to retrieve listeners: %s', self.container_id, error);
        return self.retrieve_listeners();
    });
};

ConnectedRouter.prototype.retrieve_connectors = function () {
    var self = this;
    return this.query('connector', {attributeNames:['identity', 'name', 'host', 'port', 'role']}).then(function (results) {
        self.connectors = results.filter(has_inter_router_role).map(to_host_port);
        log.debug('[%s] retrieved connectors: %j', self.container_id, self.connectors);
        self.emit('connectors_updated', self);
    }).catch(function (error) {
        log.warn('[%s] failed to retrieve connectors: %s', self.container_id, error);
        return self.retrieve_connectors();
    });
};

ConnectedRouter.prototype.retrieve_addresses = function () {
    var self = this;
    return this.query('org.apache.qpid.dispatch.router.config.address', {attributeNames:[]}).then(function (results) {
        self.addresses = myutils.index(results.filter(not_overridden), by_name, from_router_address);
        log.debug('[%s] retrieved addresses: %j', self.container_id, self.addresses);
        return self.retrieve_link_routes();
    }).catch(function (error) {
        log.warn('[%s] failed to retrieve addresses: %s', self.container_id, error);
        return self.retrieve_addresses();
    });

};

ConnectedRouter.prototype.retrieve_link_routes = function () {
    var self = this;
    return this.query('org.apache.qpid.dispatch.router.config.linkRoute').then(function (records) {
        self.link_routes = myutils.index(records.filter(not_overridden), by_name);
        log.debug('[%s] retrieved link routes: %j', self.container_id, self.link_routes);
        self.emit('addresses_updated', self);
    }).catch(function (error) {
        log.warn('[%s] failed to retrieve link routes: %s', self.container_id, error);
        this.retrieve_link_routes();
    });
};

ConnectedRouter.prototype.query = function (type, options) {
    return this.router_mgmt.query(type, options);
};

ConnectedRouter.prototype.create_entity = function (type, name, attributes) {
    return this.router_mgmt.create_entity(type, name, attributes);
};

ConnectedRouter.prototype.delete_entity = function (type, name) {
    return this.router_mgmt.delete_entity(type, name);
};

module.exports = {
    connected: function (conn) { return new ConnectedRouter(conn); },
    known: function (container_id, listeners) { return new KnownRouter(container_id, listeners); }
};
