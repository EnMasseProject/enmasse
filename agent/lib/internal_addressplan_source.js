/*
 * Copyright 2020 Red Hat Inc.
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
var events = require('events');
var kubernetes = require('./kubernetes.js');
var log = require('./log.js').logger();
var myutils = require('./utils.js');
var clone = require('clone');

function AddressPlanSource(config) {
    this.config = config || {};
    events.EventEmitter.call(this);
}

AddressPlanSource.prototype.start = function (addressspaceplansource) {
    var self = this;
    var options = myutils.merge({api: '/apis/admin.enmasse.io/v1beta2/'}, this.config);
    var started = false;
    addressspaceplansource.on('addressspaceplan_defined', (addressspaceplan) => {
        if (!started) {
            self.watcher = kubernetes.watch('addressplans', options);
            self.watcher.on('updated', self.updated.bind(self));
            started = true;
        }
        self.addressspaceplan = addressspaceplan;
        if ('addressplans_defined' in self.last) {
            self.updated(self.last['addressplans_defined']);
        } else {
            self.updated([]);
        }
    });
    this.last = {};
};

util.inherits(AddressPlanSource, events.EventEmitter);

AddressPlanSource.prototype.get_changes = function (name, addressplans, unchanged) {
    var c = myutils.changes(this.last[name], addressplans, addressplan_compare, unchanged, description);
    this.last[name] = clone(addressplans);
    return c;
};

AddressPlanSource.prototype.dispatch = function (name, addresses, description) {
    log.info('%s: %s', name, description);
    this.emit(name, addresses);
};

AddressPlanSource.prototype.updated = function (objects) {
    log.debug('addressplans updated: %j', objects);
    var self = this;
    var addressplans = objects.filter(ap => ap.metadata
        && self.addressspaceplan
        && self.addressspaceplan.spec
        && self.addressspaceplan.spec.addressPlans
        && self.addressspaceplan.spec.addressPlans.includes(ap.metadata.name));
    var changes = this.get_changes('addressplans_defined', addressplans, same_addressplan_definition_and_status);
    if (changes) {
        this.dispatch('addressplans_defined', addressplans, changes.description);
    }
};

function same_addressplan_definition_and_status(a, b) {
    return same_addressplan_definition(a.spec, b.spec) && same_addressplan_status(a.status, b.status);
}

function same_addressplan_definition(a, b) {
    if (a === b) return true;
    return a && b && a.addressType === b.addressType && same_addressplan_resources(a.resources, b.resources) && same_ttl(a.ttl, b.ttl);
}

function same_addressplan_resources(a, b) {
    if (a === b) return true;
    return a && b && a.broker === b.broker && a.router === b.router;
}

function same_ttl(a, b) {
    if (a === b) return true;
    return a && b && a.minimum === b.minimum && a.maximum === b.maximum;
}

function same_addressplan_status(a, b) {
    if (a === b) return true;
    return a && b && a.isReady === b.isReady && a.phase === b.phase && same_messages(a.messages, b.messages) && same_ttl(a.ttl, b.ttl);
}

function addressplan_compare(a, b) {
    return myutils.string_compare(a.name, b.name);
}

function by_addressplan_name(a) {
    return a.metadata.name;
}

function description(list) {
    const max = 5;
    if (list.length > max) {
        return list.slice(0, max).map(by_addressplan_name).join(', ') + ' and ' + (list.length - max) + ' more';
    } else {
        return JSON.stringify(list.map(by_addressplan_name));
    }
}

function same_messages(a, b) {
    if (a === b) {
        return true;
    } else if (a == null || b == null || a.length !== b.length) {
        return false;
    }

    for (var i in a) {
        if (!b.includes(a[i])) {
            return false;
        }
    }
    return true;
}

module.exports = AddressPlanSource;
