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

var rhea = require('rhea');
var Router = require('./qdr.js').Router;

function AddressStats() {
    //TODO: fix admin_service to be more sensibly generic
    var conn = require('./admin_service.js').connect(rhea, 'MESSAGING');
    this.router = new Router(conn);
    var self = this;
    this.router.get_all_routers().then(function (routers) {
        self.routers = routers;
        console.log('routers: ' + self.routers.map(function (r) { return r.target; }));
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

var defined_outcomes = ['accepted', 'released', 'rejected', 'modified', 'settled', 'presettled'];


function init_outcomes(outcomes) {
    defined_outcomes.forEach(function (name) {
        outcomes[name] = 0;
    });
    return outcomes;
}

function update_outcomes(outcomes, link_stats) {
    defined_outcomes.forEach(function (name) {
        if (link_stats[name + 'Count']) outcomes[name] += link_stats[name + 'Count'];
    });
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

function count_by_address(links, stats) {
    for (var l in links) {
        var link = links[l];
        if (link.linkType === 'endpoint' && link.owningAddr) {
            var address = clean_address(link.owningAddr);
            var counts = get_stats_for_address(stats, address);
            if (link.name.indexOf('qdlink.') !== 0) {
                if (link.linkDir === 'in') {
                    counts.senders++;
                    update_outcomes(counts.outcomes.ingress, link);
                } else if (link.linkDir === 'out') {
                    counts.receivers++;
                    update_outcomes(counts.outcomes.egress, link);
                }
            }
        }
    }
}

function update_stats(addresses, stats) {
    for (var a in stats) {
        if (addresses.addresses[a]) {
            addresses.update_stats(a, stats[a]);
        }
    }
}

function log_error(error) {
    if (error.message) console.error('ERROR: ' + error.message);
    else console.error('ERROR: ' + JSON.stringify(error));

}

function same_list(a, b, comparator) {
    var equal = comparator || function (x, y) { return x === y; };
    if (a.length !== b.length) {
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

AddressStats.prototype.update_routers = function () {
    var self = this;
    return this.router.get_all_routers(this.routers).then(function (routers) {
        if (!same_routers(routers, self.routers)) {
            console.log('routers changed: ' + routers.map(function (r) { return r.target; }));
        }
        self.routers = routers;
        return self.routers;
    });
}

AddressStats.prototype.retrieve = function (addresses) {
    this.update_routers().then(function (routers) {
        Promise.all(routers.map(function (router) {
            return router.get_links();
        })).then(function (results) {
            var stats = {};
            results.forEach(function (links) {
                count_by_address(links, stats);
            });
            Promise.all(routers.map(function (router) {
                return router.get_addresses();
            })).then(function (results) {
                results.forEach(function (configured) {
                    configured.forEach(function (address) {
                        var s = get_stats_for_address(stats, address.name);
                        s.propagated++;
                        if (address.waypoint) s.waypoint = true;
                    });
                });

                Promise.all(routers.map(function (router) { return router.get_link_routes(); } )).then(function (results) {
                    var link_routes = {}
                    results.forEach(function (lrs) {
                        lrs.forEach(function (link_route) {
                            if (link_route.name.indexOf('override') !== 0) {
                                var lr = link_routes[link_route.prefix];
                                if (lr === undefined) {
                                    lr = {};
                                    link_routes[link_route.prefix] = lr;
                                }
                                lr[link_route.dir] = true;
                            }
                        });
                        for (var a in link_routes) {
                            if (link_routes[a]['in'] && link_routes[a]['out']) {
                                get_stats_for_address(stats, a).propagated++;
                            }
                        }
                    });
                    //convert propagated to a percentage of all routers
                    for (var a in stats) {
                        stats[a].propagated = (stats[a].propagated / routers.length) * 100;
                    }

                    Promise.all(routers.map(function (router) {
                        return router.get_address_stats();
                    })).then(function (results) {
                        results.forEach(function (configured) {
                            configured.forEach(function (address) {
                                var s = get_stats_for_address(stats, clean_address(address.name));
                            if (s.waypoint) {
                                var phase = address_phase(address.name);
                                if (phase === 0) s.messages_in += address.deliveriesIngress;
                                else if (phase === 1) s.messages_out = address.deliveriesEgress;
                            } else {
                                s.messages_in += address.deliveriesIngress;
                                s.messages_out += address.deliveriesEgress;
                            }
                            });
                        });
                        update_stats(addresses, stats);
                    }).catch(log_error);
                }).catch(log_error);
            }).catch(log_error);
        }).catch(log_error);
    });
}

module.exports = new AddressStats();
