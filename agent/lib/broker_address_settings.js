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

var log = require("./log.js").logger();
var kubernetes = require('./kubernetes.js');
var myutils = require('./utils.js');

function kubernetes_resource_compare(a, b) {
    return myutils.string_compare(a.metadata.name, b.metadata.name);
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

function same_broker_resource(a, b) {
    return required_broker_resource(a) === required_broker_resource(b);
}

function BrokerAddressSettings(config) {
    this.watcher = kubernetes.watch('configmaps', myutils.merge({selector: 'type=address-plan'}, config));
    this.watcher.on('updated', this.updated.bind(this));
    this.required_broker_resource = {};
    this.last = undefined;
}

BrokerAddressSettings.prototype.wait_for_plans = function () {
    var self = this;
    return new Promise(function (resolve, reject) {
        self.watcher.once('updated', function () {
            log.info('plans have been retrieved');
        });
    });
}

BrokerAddressSettings.prototype.update_settings = function (plan) {
    var r = required_broker_resource(plan);
    if (r && r > 0 && r < 1) {
        this.required_broker_resource[plan.metadata.name] = r;
        log.info('updated required broker resource for %s: %d', plan.metadata.name, this.required_broker_resource[plan.metadata.name]);
    }
};

BrokerAddressSettings.prototype.generate_address_settings = function (plan, global_max_size) {
    if (global_max_size) {
        var r = this.required_broker_resource[plan];
        if (r) {
            return {
                maxSizeBytes: r * global_max_size,
                addressFullMessagePolicy: 'FAIL'
            };
        } else {
            log.info('no broker resource required for %s, therefore not applying address settings', plan);
        }
    } else {
        log.info('no global max, therefore not applying address settings');
    }
};

BrokerAddressSettings.prototype.delete_settings = function (plan) {
    delete this.required_broker_resource[plan.metadata.name];
    log.info('deleted required broker resource for %s', plan.metadata.name);
};

BrokerAddressSettings.prototype.updated = function (objects) {
    var plans = objects.map(extract_address_plan).filter(required_broker_resource);
    plans.sort(kubernetes_resource_compare);
    var changes = myutils.changes(this.last, plans, kubernetes_resource_compare, same_broker_resource);
    this.last = plans;
    if (changes) {
        log.info('address plans: %s', changes.description);
        changes.added.map(this.update_settings.bind(this));
        changes.modified.map(this.update_settings.bind(this));
        changes.removed.map(this.delete_settings.bind(this));
    }
};

BrokerAddressSettings.prototype.get_address_settings_async = function (address, global_max_size_promise) {
    var self = this;
    if (this.last === undefined) {
        return this.wait_for_plans().then(function () {
            return global_max_size_promise.then(function (global_max_size) {
                var settings = self.generate_address_settings(address.plan, global_max_size);
                log.info('using settings %j for %s', settings, address.address);
                return settings;
            });
        });
    } else {
        return global_max_size_promise.then(function (global_max_size) {
            var settings = self.generate_address_settings(address.plan, global_max_size);
            log.info('using settings %j for %s', settings, address.address);
            return settings;
        });
    }
};

module.exports = BrokerAddressSettings;
