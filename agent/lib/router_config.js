/*
 * Copyright 2018 Red Hat Inc.
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

var util = require('util');
var qdr = require('./qdr.js');
var myutils = require('./utils.js');
var log = require('./log.js').logger();

const ID_QUALIFIER = 'ragent-';
const MAX_RETRIES = 3;

function matches_qualifier (record) {
    return record.name && record.name.indexOf(ID_QUALIFIER) === 0;
}

function address_compare (a, b) {
    return myutils.string_compare(a.prefix, b.prefix);
}

function same_address_definition (a, b) {
    return a.prefix === b.prefix && a.distribution === b.distribution && a.waypoint === b.waypoint;
}

function address_describe (a) {
    return 'address ' + a.prefix;
}

function autolink_compare (a, b) {
    return myutils.string_compare(a.addr, b.addr) || myutils.string_compare(a.direction, b.direction) || myutils.string_compare(a.containerId, b.containerId);
}

function is_not_defined (a) {
    return a === null || a === '' || a === undefined;
}

function equivalent_container_id(a, b) {
    // empty string, null & undefined are all considered equivalent
    // for containerId
    return (is_not_defined(a) && is_not_defined(b)) || a === b;
}

function same_autolink_definition (a, b) {
    return a.addr === b.addr && a.direction === b.direction && equivalent_container_id(a.containerId, b.containerId);
}

function autolink_describe (a) {
    return 'autolink ' + a.name + ' (dir: ' + a.direction + ', addr: ' + a.addr + ')';
}

function linkroute_compare (a, b) {
    var result = myutils.string_compare(a.prefix, b.prefix);
    if (result === 0) {
        result = myutils.string_compare(a.direction, b.direction);
    }
    if (result === 0) {
        result = myutils.string_compare(a.containerId, b.containerId);
    }
    return result;
}

function same_linkroute_definition (a, b) {
    return a.prefix === b.prefix && a.direction === b.direction && equivalent_container_id(a.containerId, b.containerId);
}

function linkroute_describe (a) {
    return 'linkroute ' + a.direction + ' ' + a.prefix;
}

function listener_compare(a, b) {
    return myutils.string_compare(a.host, b.host) || myutils.string_compare(a.port, b.port);
}

function same_listener_definition(a, b) {
    return a.host === b.host && a.port === b.port && a.authenticatePeer === b.authenticatePeer && a.metrics === b.metrics && a.healthz === b.healthz && a.http === b.http && a.websockets === b.websockets && a.httpRootDir === b.httpRootDir;
}

function listener_describe (a) {
    return 'listener ' + a.name + ' (' + a.host + ':' + a.port + ')';
}

const entities = [
    {
        name:'addresses',
        comparator:address_compare,
        equality:same_address_definition,
        describe:address_describe,
        type:'org.apache.qpid.dispatch.router.config.address',
        singular:'address'
    },
    {
        name:'autolinks',
        comparator:autolink_compare,
        equality:same_autolink_definition,
        describe:autolink_describe,
        type:'org.apache.qpid.dispatch.router.config.autoLink',
        singular:'autolink'
    },
    {
        name:'linkroutes',
        comparator:linkroute_compare,
        equality:same_linkroute_definition,
        describe:linkroute_describe,
        type:'org.apache.qpid.dispatch.router.config.linkRoute',
        singular:'linkroute'
    },
    {
        name:'listeners',
        comparator:listener_compare,
        equality:same_listener_definition,
        describe:listener_describe,
        type:'org.apache.qpid.dispatch.listener',
        singular:'listener'
    }
];

const directions = ['in', 'out'];

function RouterConfig(prefix) {
    this.prefix = prefix;
    this.autolinks = [];
    this.addresses = [];
    this.linkroutes = [];
    this.listeners = [];
}

RouterConfig.prototype.add_address = function (a) {
    this.addresses.push(myutils.merge({name:this.prefix + a.prefix}, a));
};

RouterConfig.prototype.add_autolink = function (a) {
    this.autolinks.push(myutils.merge({name: this.prefix + a.addr + '-' + a.containerId}, a));
};

RouterConfig.prototype.add_listener = function (a) {
    this.listeners.push(myutils.merge({name:this.prefix + a.host + '-' + a.port}, a));
};

RouterConfig.prototype.add_linkroute = function (l) {
    this.linkroutes.push(myutils.merge({name:this.prefix + l.prefix + '-' + l.containerId}, l));
};

function distinct_container_per_direction(props) {
    if (props.containerId) {
        props.containerId = props.containerId + '-' + props.direction;
    }
    return props;
}

RouterConfig.prototype.add_autolink_pair = function (def) {
    for (let i = 0; i < directions.length; i++) {
        this.add_autolink(distinct_container_per_direction(myutils.merge({direction:directions[i]}, def)));
    }
};

RouterConfig.prototype.add_autolink_in = function (def) {
    this.add_autolink(distinct_container_per_direction(myutils.merge({direction:'in'}, def)));
}

RouterConfig.prototype.add_linkroute_pair = function (def) {
    for (let i = 0; i < directions.length; i++) {
        this.add_linkroute(distinct_container_per_direction(myutils.merge({direction:directions[i]}, def)));
    }
};

function get_router_id (router) {
    return router.connection ? router.connection.container_id : 'unknown-router';
}

function sort_config (config) {
    entities.forEach(function (entity) {
        config[entity.name].sort(entity.comparator);
    });
};

function delete_config_element(router, entity, element) {
    let router_id = get_router_id(router);
    log.debug('deleting %s on %s', entity.describe(element), router_id);
    return router.delete_entity(entity.type, element.name).then(function () {
        log.info('deleted %s on %s', entity.describe(element), router_id);
        return true;
    }).catch(function (error) {
        log.error('deleting %s on %s => %s', entity.describe(element), router_id, error.description);
        return false;
    });
}

function create_config_element(router, entity, element) {
    let router_id = get_router_id(router);
    log.debug('creating %s on %s', entity.describe(element), router_id);
    return router.create_entity(entity.type, element.name, element).then(function () {
        log.info('created %s on %s', entity.describe(element), router_id);
        return true;
    }).catch(function (error) {
        log.error('creating %s on %s => %s', entity.describe(element), router_id, error.description);
        return false;
    });
}

function retrieve_elements(entity, router) {
    let router_id = get_router_id(router);
    return router.query(entity.type).then(function (results) {
        if (Array.isArray(results)) {
            log.debug('retrieved %s from %s', entity.name, router_id);
            results.sort(entity.comparator);
            return results;
        } else {
            log.warn('unexpected result from retrieving %s from %s: %j', entity.name, router_id, results);
            return [];
        }
    }).catch(function (error) {
        log.error('error retrieving %s from %s: %s', entity.name, router_id, error);
        throw error;
    });

}

function print_list(prefix, list) {
    log.info('  %s', prefix);
    list.forEach(function (o) {
        log.info('    %j', o);
    });
}

function is_false(v) { return v === false; }

function debug_failures(entity, targets, results, actual) {
    for (let i = 0; i < results.length; i++) {
        if (!results[i]) {
            if (actual.some(entity.equality.bind(null, targets[i]))) {
                log.info('%s IS in retrieved list', entity.describe(targets[i]));
            } else {
                log.info('%s IS NOT in retrieved list', entity.describe(targets[i]));
            }
        }
    }
}

function report(entity, targets, results, actual, operation) {
    if (results.some(is_false)) {
        log.info('had %d %s, %s %d of which %d failed:', actual.length, entity.name, operation, targets.length, results.filter(is_false).length);
        debug_failures(entity, targets, results, actual);
    } else if (targets.length) {
        log.info('had %d %s, %s %d', actual.length, entity.name, operation, targets.length);
    }
}

function ensure_elements(entity, desired, router, collected) {
    let router_id = get_router_id(router);
    return retrieve_elements(entity, router).then(function (actual) {
        var delta = myutils.changes(actual, desired, entity.comparator, entity.equality);
        if (delta) {
            log.debug('on %s, have %j, want %j => %s', router_id, actual, desired, delta.description);
            let stale = delta.removed.filter(matches_qualifier).concat(delta.modified);
            let missing = delta.added.concat(delta.modified);

            if (stale.length || missing.length) {
                let delete_fn = delete_config_element.bind(null, router, entity);
                let create_fn = create_config_element.bind(null, router, entity);
                return Promise.all(stale.map(delete_fn)).then(
                    function (deletions) {
                        report(entity, stale, deletions, actual, 'deleted')
                        return Promise.all(missing.map(create_fn)).then(
                            function (creations) {
                                report(entity, missing, creations, actual, 'created')
                                return false;//recheck when changed
                            }
                        ).catch(function (error) {
                            log.error('Failed to create required %s: %s', entity.name, error);
                        });
                    }).catch(function (error) {
                        log.error('Failed to delete stale %s: %s', entity.name, error);
                    });
            } else {
                log.info('%s up to date on %s (ignoring %d elements)', entity.name, router_id, delta.removed.length);
                collected[entity.name] = actual;
                return true;
            }
        } else {
            log.info('%s up to date on %s', entity.name, router_id);
            collected[entity.name] = actual;
            return true;
        }
    }).catch(function (error) {
        log.error('error retrieving %s from %s: %s', entity.name, router_id, error);
        return false;
    });
}

function apply_config(desired, router, count) {
    let iteration = count || 1;
    let router_id = get_router_id(router);
    log.info('checking configuration of %s', router_id);
    log.debug('applying %j to %s', desired, router_id);
    var actual = {};
    let promise = Promise.resolve(true);
    for (let i = 0; i < entities.length; i++) {
        let entity = entities[i];
        promise = promise.then(function (result_a) {
            return ensure_elements(entity, desired[entity.name], router, actual).then(function (result_b) {
                return result_a && result_b;
            });
        });
    }
    return promise.then(function (ok) {
        if (ok) {
            log.info('configuration of %s is up to date', router_id);
            return actual;
        } else {
            log.error('configuration update for %s not up to date (attempt %d of %d)', router_id, iteration, MAX_RETRIES);
            if (iteration < MAX_RETRIES) {
                return apply_config(desired, router, iteration + 1);
            } else {
                log.error('Unable to apply desired configuration; gave up after %d attempts', iteration);
                throw new Error(util.format('Unable to apply desired configuration; gave up after %d attempts', iteration));
            }
        }
    }).catch(function (error) {
        log.error('error while applying configuration to %s, retrying: %j', router_id, error);
        if (iteration < MAX_RETRIES) {
            return apply_config(desired, router, iteration + 1);
        } else {
            log.error('Unable to apply desired configuration; gave up after %d attempts (%s)', iteration, error);
            throw new Error(util.format('Unable to apply desired configuration; gave up after %d attempts (%s)', iteration, error));
        }
    });
}

function desired_address_config(high_level_address_definitions) {
    var config = new RouterConfig(ID_QUALIFIER);
    for (var i in high_level_address_definitions) {
        var def = high_level_address_definitions[i];
        if (def.type === 'queue') {
            config.add_address({prefix:def.address, distribution:'balanced', waypoint:true});
            if (def.allocated_to) {
                log.debug("Constructing config for queue %s allocated to: %j", def.address, def.allocated_to);
                for (var j in def.allocated_to) {
                    var brokerStatus = def.allocated_to[j];
                    if (brokerStatus.state === 'Active') {
                        config.add_autolink_pair({addr:def.address, containerId: brokerStatus.containerId});
                    } else if (brokerStatus.state === 'Migrating') {
                        config.add_autolink_pair({addr:def.address, containerId: brokerStatus.containerId});
                    } else if (brokerStatus.state === 'Draining') {
                        config.add_autolink_in({addr:def.address, containerId: brokerStatus.containerId});
                    }
                }
            } else {
                log.debug("Constructing old config for queue %s", def.address);
                config.add_autolink_pair({addr:def.address, containerId: def.address});
            }
        } else if (def.type === 'topic') {
            if (def.allocated_to) {
                for (var j in def.allocated_to) {
                    var brokerStatus = def.allocated_to[j];
                    config.add_linkroute_pair({prefix:def.address, containerId: brokerStatus.containerId});
                    // TODO: Handle Draining?
                }
            } else {
                config.add_linkroute_pair({prefix:def.address, containerId: def.address});
            }
        } else if (def.type === 'subscription') {
            if (def.allocated_to) {
                for (var j in def.allocated_to) {
                    var brokerStatus = def.allocated_to[j];
                    config.add_linkroute(distinct_container_per_direction({prefix:def.topic+'::'+def.address, containerId: brokerStatus.containerId, direction:'out'}));
                    // TODO: Handle Draining?
                }
            } else {
                log.warn('subscription %s not allocated to broker', def.address);
            }
        } else if (def.type === 'anycast') {
            config.add_address({prefix:def.address, distribution:'balanced', waypoint:false});
        } else if (def.type === 'multicast') {
            config.add_address({prefix:def.address, distribution:'multicast', waypoint:false});
        }
    }
    config.add_listener({host:'0.0.0.0', port: '8080', authenticatePeer: false, metrics: true, healthz: true, http: true, websockets: false, httpRootDir: 'invalid'})
    sort_config(config);
    log.debug('mapped %j => %j', high_level_address_definitions, config);
    return config;
}

function deduce_type(address_config) {
    if (address_config.distribution === 'balanced') {
        if (address_config.waypoint) return 'queue';
        else return 'anycast';
    } else if (address_config.distribution === 'multicast') {
        return 'multicast';
    } else {
        return undefined;
    }
}

function deduce_definition(actual_config) {
    var definition = {};
    var linkroutes = {};
    for (var i in actual_config.addresses) {
        var a = actual_config.addresses[i];
        definition[a.prefix] = {address:a.prefix, type:deduce_type(a)};
    };
    for (var i in actual_config.linkroutes) {
        var l = actual_config.linkroutes[i];
        var e = linkroutes[l.prefix];
        if (e === undefined) {
            e = {};
            linkroutes[l.prefix] = e;
        }
        e[l.direction] = true;
    };
    for (var i in linkroutes) {
        var l = linkroutes[i];
        if (l['in'] && l['out']) {
            definition[i] = {address:i, type:'topic'};
        } else if (l['out']) {
            var s = i.indexOf('::');
            if (s > 0) {
                var name = i.substr(s+2);
                definition[name] = {address:name, type:'subscription'};
            }
        }
    }
    return definition;
}

module.exports = {
    realise_address_definitions: function (high_level_address_definitions, router) {
        return apply_config(desired_address_config(high_level_address_definitions), router).then(function (actual) {
            return deduce_definition(actual);
        });
    }
};
