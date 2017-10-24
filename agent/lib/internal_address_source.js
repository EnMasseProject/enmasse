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

var util = require('util');
var events = require('events');
var kubernetes = require('./kubernetes.js');
var log = require('./log.js').logger();

function AddressSource() {
    events.EventEmitter.call(this);
    this.watcher = kubernetes.watch('configmaps', {selector:'type=address-config'});
    this.watcher.on('updated', this.updated.bind(this));
}

util.inherits(AddressSource, events.EventEmitter);

AddressSource.prototype.notify = function () {
    var self = this;
};

function extract_address_spec(object) {
    try {
        var def = JSON.parse(object.data['config.json']);
        if (def.spec === undefined) {
            console.error('no spec found on %j', def);
        }
        return def.spec;
    } catch (e) {
        console.error('Failed to parse config.json for address: %s %j', e, object);
    }
}

AddressSource.prototype.updated = function (objects) {
    log.info('addresses updated: %j', objects.map(extract_address_spec));
    this.emit('addresses_defined', objects.map(extract_address_spec));
};

module.exports = AddressSource;
