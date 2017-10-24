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

var kubernetes = require('./kubernetes.js');
var log = require('./log.js').logger();

function AddressStatusMonitor() {
    this.readiness = {};
}

AddressStatusMonitor.prototype.do_update = function (address, ready) {
    if (this.readiness[address] !== undefined && ready !== this.readiness[address].ready) {
        var self = this;
        function update(configmap) {
            var def = JSON.parse(configmap.data['config.json'])
            def.status.isReady = ready;
            configmap.data['config.json'] = JSON.stringify(def);
            return configmap;
        }
        kubernetes.update('configmaps/address-config-'+address, update).then(function (result) {
            if (result === 200) {
                self.readiness[address].ready = ready;
                log.info('updated status for %s: %s', address, result);
            } else {
                log.error('failed to update status for %s: %s', address, result);
            }
        }).catch(function (error) {
            log.error('failed to update status for %s: %j', address, error);
        });
    }
};

AddressStatusMonitor.prototype.updated = function (address_stats) {
    for (var address in address_stats) {
        this.do_update(address, address_stats[address].propagated === 100);
    }
};

AddressStatusMonitor.prototype.addresses_defined = function (addresses) {
    var self = this;
    this.readiness = addresses.reduce(function (map, a) { map[a.address] = self.readiness[a.address] || {ready:false}; return map; }, {});
};

module.exports = AddressStatusMonitor;
