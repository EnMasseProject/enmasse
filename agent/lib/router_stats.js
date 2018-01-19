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

var log = require("./log.js").logger();
var rhea = require('rhea');
var path = require('path');
var fs = require('fs');
var Router = require('./qdr.js').Router;



function RouterStats(connection) {
    var options = { host: process.env.MESSAGING_SERVICE_HOST, port: process.env.MESSAGING_SERVICE_PORT_AMQPS_NORMAL};

    //TODO: fix admin_service to be more sensibly generic
    var conn = connection || require('./admin_service.js').connect(rhea, options, 'MESSAGING');
    this.router = new Router(conn);
    var self = this;
    this.router.get_all_routers().then(function (routers) {
        self.routers = routers;
        log.info('routers: ' + self.routers.map(function (r) { return r.target; }));
    });
}

function clean_address(address) {
    if (!address) {
        return address;
    } else if (address.charAt(0) === 'M') {
        return address.substring(2);
    } else {
        return address.substring(1);
    }
}

function address_phase(address) {
    if (address && address.charAt(0) === 'M') {
        return parseInt(address.substr(1, 1));
    } else {
        return undefined;
    }
}

var defined_outcomes = ['accepted', 'released', 'rejected', 'modified', 'unsettled', 'presettled', 'undelivered'];
function backlog (link_stats) {
    var backlog = 0
    if (link_stats.undeliveredCount) backlog += link_stats.undeliveredCount
    if (link_stats.unsettledCount) backlog += link_stats.unsettledCount
    return backlog
}
function routerName (link_stats, router) {
    return router && router.target ? router.target.split('/')[3] : undefined;
}
var defined_linkDetails = ['identity', 'name', 'operStatus', 'adminStatus', 'deliveryCount', 'capacity', backlog, routerName];
defined_linkDetails.push.apply(defined_linkDetails, defined_outcomes.map((d) => d+'Count'))

function init_outcomes(outcomes) {
    defined_outcomes.forEach(function (name) {
        outcomes[name] = 0;
    });
    outcomes.links = []
    return outcomes;
}

function update_outcomes(outcomes, link_stats, router) {
    if (link_stats) {
        defined_outcomes.forEach(function (name) {
            if (link_stats[name + 'Count']) outcomes[name] += link_stats[name + 'Count'];
        });
        var link_details = {}
        defined_linkDetails.forEach(function (name) {
            if (typeof name === 'function')
              link_details[name.name] = name(link_stats, router)
            else if (link_stats[name] !== undefined) link_details[name] = link_stats[name]
        });
        link_details.lastUpdated = Date.now()
        outcomes.links.push(link_details)
    }
    return outcomes;
}

function get_stats_for_address(stats, address) {
    var s = stats[address];
    if (s === undefined) {
        s = {
            senders: 0, receivers: 0, propagated: 0,
            messages_in: 0, messages_out: 0,
            outcomes: {
                ingress: init_outcomes({}),
                egress: init_outcomes({})
            }
        };
        stats[address] = s;
    }
    return s;
}

function collect_by_address(links, stats, router, connections) {
    for (var l in links) {
        var link = links[l];
        if (link.linkType === 'endpoint' && link.owningAddr && connections[link.connectionId]) {
            var address = clean_address(link.owningAddr);
            var counts = get_stats_for_address(stats, address);
            if (link.name.indexOf('qdlink.') !== 0) {
                if (link.linkDir === 'in') {
                    counts.senders++;
                    update_outcomes(counts.outcomes.ingress, link, router);
                } else if (link.linkDir === 'out') {
                    counts.receivers++;
                    update_outcomes(counts.outcomes.egress, link, router);
                }
            }
        }
    }
}

function collect_by_connection(links, connections, router) {
    links.forEach(function (link) {
        var connection = connections[link.connectionId];
        if (connection) {
            var l = update_outcomes(init_outcomes({address:clean_address(link.owningAddr),name:link.name}), link);
            l.deliveries = link.deliveryCount;
            if (link.linkDir === 'in') {
                connection.senders.push(l);
                update_outcomes(connection.outcomes.ingress, link, router);
                connection.messages_in += l.deliveries;
            } else if (link.linkDir === 'out') {
                connection.receivers.push(l);
                update_outcomes(connection.outcomes.egress, link, router);
                connection.messages_out += l.deliveries;
            }
        }
    });
}

function log_error(error) {
    if (error.message) log.error('ERROR: ' + error.message);
    else log.error('ERROR: ' + JSON.stringify(error));

}

function same_list(a, b, comparator) {
    var equal = comparator || function (x, y) { return x === y; };
    if (a === undefined || b === undefined || a.length !== b.length) {
        return false;
    } else {
        for (var i = 0; i < a.length; i++) {
            if (!equal(a[i], b[i])) return false;
        }
        return true;
    }
}

function same_routers(a, b) {
    return same_list(a, b, function (x, y) { return x.target === y.target; });
}

RouterStats.prototype.update_routers = function () {
    var self = this;
    return this.router.get_all_routers(this.routers).then(function (routers) {
        if (routers === undefined) {
            log.info('no routers found');
            return [];
        } else {
            if (!same_routers(routers, self.routers)) {
                log.info('routers changed: ' + routers.map(function (r) { return r.target; }));
            }
            self.routers = routers;
            return self.routers;
        }
    });
}

function check_link_routes (link_routes) {
    var by_address = {};
    link_routes.forEach(function (link_route) {
        if (link_route.name.indexOf('override') !== 0) {
            var lr = by_address[link_route.prefix];
            if (lr === undefined) {
                lr = {};
                by_address[link_route.prefix] = lr;
            }
            lr[link_route.dir] = true;
        }
    });
    var results = [];
    for (var a in by_address) {
        if (by_address[a]['in'] && by_address[a]['out']) {
            results.push(a);
        }
    }
    return results;
}

function is_role_normal (c) {
    return c.role === 'normal';
}

var internal_identifiers = ['agent', 'ragent', 'qdconfigd', 'subserv', 'lwt-service'];

function is_internal_identifier (s) {
    return internal_identifiers.indexOf(s) >= 0;
}

function is_internal (c) {
    return (c.properties && is_internal_identifier(c.properties.product)) || is_internal_identifier(c.container);
}

function is_application_connection (c) {
    return is_role_normal(c) && !is_internal(c);
}

function get_normal_connections (results) {
    var connections = {};
    results.forEach(function (stats) {
        stats.filter(is_application_connection).forEach(function (c) {
            connections[c.identity] = {
                id: c.identity,
                host: c.host,
                container: c.container,
                properties: c.properties,
                encrypted: c.isEncrypted,
                sasl_mechanism: c.isAuthenticated ? c.sasl : 'none',
                user: c.user,
                messages_in: 0,
                messages_out: 0,
                outcomes: {
                    ingress: init_outcomes({}),
                    egress: init_outcomes({})
                },
                senders: [],
                receivers: []
            };
        });
    });
    return connections;
}

RouterStats.prototype.close = function () {
    this.router.close();
}

RouterStats.prototype.retrieve = function (addresses, connection_registry) {
    return this._retrieve().then(function (results) {
        if (results) {
            connection_registry.set(results.connections);
            for (var a in results.addresses) {
                addresses.update_stats(a, results.addresses[a]);
            }
        }
    });
};

RouterStats.prototype._retrieve = function () {
    return this.update_routers().then(function (routers) {
        return Promise.all(routers.map(function (router) { return router.get_connections(); })).then(function (connection_results) {
            var connections = get_normal_connections(connection_results);
            return Promise.all(routers.map(function (router) { return router.get_links(); })).then(function (results) {
                var address_stats = {};
                results.forEach(function (links, i) {
                    collect_by_address(links, address_stats, routers[i], connections);
                    collect_by_connection(links, connections, routers[i]);
                });
                return Promise.all(routers.map(function (router) { return router.get_addresses(); })).then(function (results) {
                    results.forEach(function (configured) {
                        configured.forEach(function (address) {
                            var s = get_stats_for_address(address_stats, address.name);
                            s.propagated++;
                            if (address.waypoint) s.waypoint = true;
                        });
                    });

                    return Promise.all(routers.map(function (router) { return router.get_link_routes(); } )).then(function (results) {
                        results.forEach(function (lrs) {
                            check_link_routes(lrs).forEach(function (a) {
                                get_stats_for_address(address_stats, a).propagated++;
                            });
                        });
                        //convert propagated to a percentage of all routers
                        for (var a in address_stats) {
                            address_stats[a].propagated = (address_stats[a].propagated / routers.length) * 100;
                        }

                        return Promise.all(routers.map(function (router) {
                            return router.get_address_stats();
                        })).then(function (results) {
                            results.forEach(function (configured) {
                                configured.forEach(function (address) {
                                    var s = get_stats_for_address(address_stats, clean_address(address.name));
                                    if (s.waypoint) {
                                        var phase = address_phase(address.name);
                                        if (phase === 0) s.messages_in += address.deliveriesIngress;
                                        else if (phase === 1) s.messages_out += address.deliveriesEgress;
                                    } else {
                                        s.messages_in += address.deliveriesIngress;
                                        s.messages_out += address.deliveriesEgress;
                                    }
                                });
                            });
                            return {addresses: address_stats, connections: connections};
                        }).catch(log_error);
                    }).catch(log_error);
                }).catch(log_error);
            }).catch(log_error);
        }).catch(log_error);
    });
};

module.exports = RouterStats;
