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


function AddressSpacePlanSource(config) {
    this.config = config || {};
    this.selector = "";
    events.EventEmitter.call(this);
};

AddressSpacePlanSource.prototype.start = function () {
    this.addressSpacePlan = this.config.ADDRESS_SPACE_PLAN;
    var options = myutils.merge({api: '/apis/admin.enmasse.io/v1beta2/'}, this.config);
    this.watcher = kubernetes.watch('addressspaceplans', options);
    this.watcher.on('updated', this.updated.bind(this));

    this.last = {};
};

util.inherits(AddressSpacePlanSource, events.EventEmitter);

AddressSpacePlanSource.prototype.get_changes = function (name, addressspaceplan, unchanged) {
    var c = myutils.changes(this.last[name], addressspaceplan, addressspaceplan_compare, unchanged, description);
    this.last[name] = clone(addressspaceplan);
    return c;
};

AddressSpacePlanSource.prototype.dispatch = function (name, addresses, description) {
    log.info('%s: %s', name, description);
    this.emit(name, addresses);
};

AddressSpacePlanSource.prototype.updated = function (objects) {
    log.debug('addressspaceplans updated: %j', objects);
    var self = this;
    var addressspaceplans = objects.filter(asp => asp.metadata && self.addressSpacePlan === asp.metadata.name);
    var changes = this.get_changes('addressspaceplan_defined', addressspaceplans, same_addressspaceplan_spec_and_status);
    if (changes) {
        this.dispatch('addressspaceplan_defined', addressspaceplans[0], changes.description);
    }
};

function same_addressspaceplan_spec_and_status(a, b) {
    return same_addressspaceplan_definition(a.spec, b.spec) && same_addressspaceplan_status(a.status, b.status);
}

function same_addressspaceplan_definition(a, b) {
    return same_addressplans(a.addressPlans, b.addressPlans);
}

function same_addressplans(a, b) {
    if (a === undefined) return b === undefined;
    if (a.length !== b.length) return false;
    for (var i = 0; i < a.length; i++) {
        var found  = false;
        for (var j = 0; j < b.length; j++) {
            if (b[j] === a[i]) {
                found = true;
                break
            }
        }
        if (!found) {
            return false;
        }
    }
    return true;
}

function same_addressspaceplan_status(a, b) {
    if (a === undefined) return b === undefined;
    return a.isReady === b.isReady && a.phase === b.phase && same_messages(a.messages, b.messages);
}

function addressspaceplan_compare(a, b) {
    return myutils.string_compare(a.name, b.name);
}

function by_addressspaceplan_name(a) {
    return a.metadata.name;
}

function description(list) {
    const max = 5;
    if (list.length > max) {
        return list.slice(0, max).map(by_addressplan_name).join(', ') + ' and ' + (list.length - max) + ' more';
    } else {
        return JSON.stringify(list.map(by_addressspaceplan_name));
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

module.exports = AddressSpacePlanSource;
