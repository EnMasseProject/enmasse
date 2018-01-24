/*
 * Copyright 2017 Red Hat Inc.
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

var util = require('util');
var myutils = require('./utils.js');

function event (component, reason, message, type, kind, name) {
    var event_id = util.format('%s.%s', component, myutils.hash([kind, reason, name, message].join()));
    var now = new Date().toISOString();
    return {
        kind: 'Event',
        metadata: {
            name: event_id
        },
        count: 1,
        reason: reason,
        message: message,
        type: type,
        firstTimestamp: now,
        lastTimestamp: now,
        source: {
            component: component
        },
        involvedObject: {
            kind: kind,
            name: name
        }
    };
}

module.exports.address_create = function (address) {
    return event('agent', 'AddressCreated', util.format('%s %s created', address.type, address.address), 'Normal', 'Address', address.address);
};

module.exports.address_failed_create = function (address, error) {
    return event('agent', 'AddressCreateFailed', util.format('Failed to create %s %s: %s', address.type, address.address, error), 'Warning', 'Address', address.address);
};

module.exports.address_delete = function (address) {
    return event('agent', 'AddressDeleted', util.format('%s %s deleted', address.type, address.address), 'Normal', 'Address', address.address);
};

module.exports.address_failed_delete = function (address, error) {
    return event('agent', 'AddressDeleteFailed', util.format('Failed to delete %s %s: %s', address.type, address.address, error), 'Warning', 'Address', address.address);
};

module.exports.equivalent = function (a, b) {
    return a.name === b.name && a.reason === b.reason && a.involvedObject.kind === b.involvedObject.kind && a.involvedObject.name === b.involvedObject.name
        && a.message === b.message;
}
