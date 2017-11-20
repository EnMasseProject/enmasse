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

function AddressSource(config) {
    this.config = config;
    var options = config || {};
    options.selector = 'type=address-config';
    events.EventEmitter.call(this);
    this.watcher = kubernetes.watch('configmaps', options);
    this.watcher.on('updated', this.updated.bind(this));
    this.readiness = {};
}

util.inherits(AddressSource, events.EventEmitter);

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
    var addresses = objects.map(extract_address_spec);
    log.debug('addresses updated: %j', addresses);
    var self = this;
    this.readiness = objects.reduce(function (map, configmap) {
        var address = extract_address_spec(configmap).address;
        map[address] = self.readiness[address] || {ready: false, address: address};
        map[address].name = configmap.metadata.name;
        return map;
    }, {});
    this.emit('addresses_defined', addresses);
};

AddressSource.prototype.update_status = function (record, ready) {
    function update(configmap) {
        var def = JSON.parse(configmap.data['config.json'])
        if (def.status.isReady !== ready) {
            def.status.isReady = ready;
            configmap.data['config.json'] = JSON.stringify(def);
            return configmap;
        } else {
            return undefined;
        }
    }
    return kubernetes.update('configmaps/' + record.name, update, this.config).then(function (result) {
        if (result === 200) {
            record.ready = ready;
            log.info('updated status for %j: %s', record, result);
        } else if (result === 304) {
            record.ready = ready;
            log.debug('no need to update status for %j: %s', record, result);
        } else {
            log.error('failed to update status for %j: %s', record, result);
        }
    }).catch(function (error) {
        log.error('failed to update status for %j: %j', record, error);
    });
};

AddressSource.prototype.check_status = function (address_stats) {
    for (var address in address_stats) {
        var record = this.readiness[address];
        var ready = address_stats[address].propagated === 100;
        if (record !== undefined && ready !== record.ready) {
            return this.update_status(record, ready);
        }
    }
};

module.exports = AddressSource;
