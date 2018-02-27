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

var RouterStats = require('../lib/router_stats.js');
var BrokerStats = require('../lib/broker_stats.js');
var log = require("./log.js").logger();

function StandardStats () {
    this.router_stats = new RouterStats();
    this.broker_stats = new BrokerStats();
}

StandardStats.prototype.init = function (console_server) {
    var self = this;
    setInterval(function () {
        log.info('triggering router stats retrieval');
        self.router_stats.retrieve(console_server.addresses, console_server.connections);
        log.info('router stats retrieval triggered');
    }, 10000);

    setInterval(function () {
        log.info('triggering broker stats retrieval');
        self.broker_stats.retrieve(console_server.addresses);
        log.info('broker stats retrieval triggered');
    }, 10000);
};

module.exports = StandardStats;
