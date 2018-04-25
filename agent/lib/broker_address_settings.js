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

var events = require('events');
var util = require('util');
var log = require('./log.js').logger();
var kubernetes = require('./kubernetes.js');
var myutils = require('./utils.js');
var artemis = require('./artemis.js');

function match_compare(a, b) {
    return myutils.string_compare(a.match, b.match);
}

function get_match(name) {
    return name + '/#';
}

function extract_match(o) {
    return o.match;
}

function extract_address_plan(object) {
    try {
        return JSON.parse(object.data['definition']);
    } catch (e) {
        log.error('Failed to parse definition for address plan: %s %j', e, object);
    }
}

function required_broker_resource(plan) {
    var brokerRequired = plan.requiredResources ? plan.requiredResources.filter(function (o) { return o.name === 'broker'; })[0] : undefined;
    return brokerRequired ? brokerRequired.credit : undefined;
}

function equivalent_settings(a, b) {
    return a.maxSizeBytes === b.maxSizeBytes && a.addressFullMessagePolicy === b.addressFullMessagePolicy;
}

function to_routing_type(address_type) {
    if (address_type === 'queue') {
        return 'ANYCAST';
    } else if (address_type === 'topic') {
        return 'MULTICAST';
    } else {
        return undefined;
    }
}

function to_address_setting(global_max_size, plan) {
    var r = required_broker_resource(plan);
    var routing_type = to_routing_type(plan.addressType);
    if (r && r > 0 && r < 1 && routing_type) {
        return {
            match: get_match(plan.metadata.name),
            maxSizeBytes: r * global_max_size,
            addressFullMessagePolicy: 'FAIL',
            defaultAddressRoutingType: routing_type
        }
    } else {
        log.info('not applying address settings for %s', plan.metadata.name);
        return undefined;
    }
};

function defined(o) {
    return o !== undefined;
}

function BrokerAddressSettings(config) {
    events.EventEmitter.call(this);
    this.plans = [];
    this.watcher = kubernetes.watch('configmaps', myutils.merge({selector: 'type=address-plan'}, config));
    this.watcher.on('updated', this.updated.bind(this));
}

util.inherits(BrokerAddressSettings, events.EventEmitter);

BrokerAddressSettings.prototype.close = function () {
    this.watcher.close();
};

BrokerAddressSettings.prototype.updated = function (objects) {
    this.plans = objects.map(extract_address_plan);
    this.emit('changed', this.plans);
};

BrokerAddressSettings.prototype.create_controller = function (connection) {
    var controller = new AddressSettingsController(connection);
    if (this.plans.length) {
        controller.on_changed(this.plans);
    }
    this.on('changed', controller.on_changed.bind(controller));
    return controller;
};

function AddressSettingsController(connection) {
    this.broker = new artemis.Artemis(connection);
    var self = this;
    this.global_max_size = this.broker.getGlobalMaxSize();
}

AddressSettingsController.prototype.close = function () {
    this.broker.close();
};

AddressSettingsController.prototype.on_changed = function (plans) {
    var id = this.broker.connection.container_id;
    var self = this;
    this.global_max_size.then(function (global_max_size) {
        if (global_max_size) {
            log.info('global max size for broker %s is %d', id, global_max_size);
            var desired_address_settings = plans.map(to_address_setting.bind(null, global_max_size)).filter(defined);
            self.check_address_settings(desired_address_settings);
        } else {
            log.info('no global max size retrieved for %s, will not create address-settings', id);
        }
    }).catch(function (error) {
        log.info('could not retrieve global max size for %s: %s', id, error);
    });
};

AddressSettingsController.prototype.check_address_settings = function (desired_address_settings) {
    var id = this.broker.connection.container_id;
    var broker = this.broker;
    function ensure_address_settings (match, settings) {
        return broker.getAddressSettings(match).then(function (result) {
            if (!result) {
                return broker.addAddressSettings(match, settings).then(function () {
                    log.info('address settings %s created on %s', match, id);
                });
            } else if (!equivalent_settings(settings, result)) {
                log.info('recreating address settings %s on %s (%j != %j)', match, id, settings, result);
                console.log('recreating address settings %s on %s', match, id);
                return broker.removeAddressSettings(match).then(function() {
                    log.info('address settings %s removed on %s', match, id);
                    broker.addAddressSettings(match, settings);
                }).then(function () {
                    log.info('address settings %s created on %s', match, id);
                });
            } else {
                log.info('address settings %s exist on %s', match, id);
            }
        });
    };
    var promise;
    for (var i = 0; i < desired_address_settings.length; i++) {
        var s = desired_address_settings[i];
        if (promise) {
            promise.then(ensure_address_settings.bind(null, s.match, s));
        } else {
            promise = ensure_address_settings(s.match, s);
        }
    }
    promise.then(function () {
        log.info('desired address settings created on %s', id);
    });
};

module.exports.controller_factory = function (env) {
    return new BrokerAddressSettings(env);
}
