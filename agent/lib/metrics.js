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

var log = require("./log.js").logger();
var util = require('util');
var events = require('events');

function Metrics(address_space_namespace, address_space) {
    this.address_space_namespace = address_space_namespace;
    this.address_space = address_space;
    this.addresses_total = 0;
    this.addresses_ready_total = 0;
    this.addresses_not_ready_total = 0;

    this.addresses_pending_total = 0;
    this.addresses_configuring_total = 0;
    this.addresses_active_total = 0;
    this.addresses_failed_total = 0;
    this.addresses_terminating_total = 0;
    events.EventEmitter.call(this);
}

util.inherits(Metrics, events.EventEmitter);

Metrics.prototype.addresses_defined = function (addresses) {
    this.addresses_total = 0;
    this.addresses_ready_total = 0;
    this.addresses_not_ready_total = 0;

    this.addresses_pending_total = 0;
    this.addresses_configuring_total = 0;
    this.addresses_active_total = 0;
    this.addresses_failed_total = 0;
    this.addresses_terminating_total = 0;

    for (var i = 0; i < addresses.length; i++) {
        var address = addresses[i];
        if (address.status !== undefined) {
            var phase = address.status.phase;
            if (phase == 'Pending') {
                this.addresses_pending_total++;
            } else if (phase == 'Configuring') {
               this.addresses_configuring_total++;
            } else if (phase == 'Active') {
                this.addresses_active_total++;
            } else if (phase == 'Failed') {
                this.addresses_failed_total++;
            } else if (phase == 'Terminating') {
                this.addresses_terminating_total++;
            }
            if (address.status.isReady) {
                this.addresses_ready_total++;
            } else {
                this.addresses_not_ready_total++;
            }
        }
        this.addresses_total++;
    }

}

Metrics.prototype.format_prometheus = function(timestamp) {
    var data = "";
    data += "# HELP addresses_total Total number of addresses\n";
    data += "# TYPE addresses_total gauge\n";
    data += "addresses_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_total + " " + timestamp + "\n";

    data += "# HELP addresses_ready_total Total number of addresses with status ready\n";
    data += "# TYPE addresses_ready_total gauge\n";
    data += "addresses_ready_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_ready_total + " " + timestamp + "\n";

    data += "# HELP addresses_not_ready_total Total number of addresses with status not ready\n";
    data += "# TYPE addresses_not_ready_total gauge\n";
    data += "addresses_not_ready_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_not_ready_total + " " + timestamp + "\n";

    data += "# HELP addresses_pending_total Total number of addresses in pending state\n";
    data += "# TYPE addresses_pending_total gauge\n";
    data += "addresses_pending_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_pending_total + " " + timestamp + "\n";

    data += "# HELP addresses_configuring_total Total number of addresses in configuring state\n";
    data += "# TYPE addresses_configuring_total gauge\n";
    data += "addresses_configuring_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_configuring_total + " " + timestamp + "\n";

    data += "# HELP addresses_active_total Total number of addresses in active state\n";
    data += "# TYPE addresses_active_total gauge\n";
    data += "addresses_active_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_active_total + " " + timestamp + "\n";

    data += "# HELP addresses_failed_total Total number of addresses in failed state\n";
    data += "# TYPE addresses_failed_total gauge\n";
    data += "addresses_failed_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_failed_total + " " + timestamp + "\n";

    data += "# HELP addresses_terminating_total Total number of addresses in terminating state\n";
    data += "# TYPE addresses_terminating_total gauge\n";
    data += "addresses_terminating_total{addressspace=\"" + this.address_space + "\",namespace=\"" + this.address_space_namespace + "\"} " + this.addresses_terminating_total + " " + timestamp + "\n";

    return data;
}

module.exports = Metrics;
