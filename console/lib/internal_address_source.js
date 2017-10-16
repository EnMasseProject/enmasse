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

function AddressSource() {
    this.addresses = {};
    events.EventEmitter.call(this);
    this.watcher = kubernetes.watch('configmaps', {selector:'type=address-config'});
    this.watcher.on('added', this.added.bind(this));
    this.watcher.on('modified', this.added.bind(this));
    this.watcher.on('deleted', this.deleted.bind(this));
}

util.inherits(AddressSource, events.EventEmitter);

AddressSource.prototype.notify = function () {
    var self = this;
    this.emit('addresses_defined', Object.keys(this.addresses).map(function (key) { return self.addresses[key]; }));
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

AddressSource.prototype.added = function (object) {
    var spec = extract_address_spec(object);
    if (spec) {
        this.addresses[spec.address] = spec;
        this.notify();
    }
};

AddressSource.prototype.deleted = function (object) {
    var spec = extract_address_spec(object);
    if (spec) {
        delete this.addresses[spec.address];
        this.notify();
    }
};

module.exports = AddressSource;
