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
var rhea = require('rhea');
var create_podgroup = require('./podgroup.js');

function is_pod_ready(pod) {
    return pod.ready === 'True' && pod.phase === 'Running';
}

function list_queues(broker) {
    return broker.listQueues();
}

function list_addresses(broker) {
    return broker.getAllQueuesAndTopics();
}

function get_stats_for_address(stats, address) {
    var s = stats[address];
    if (s === undefined) {
        s = {depth:0, shards:[]};
        stats[address] = s;
    }
    return s;
}

function merge() {
    var c = {};
    for (var i = 0; i < arguments.length; i++) {
        for (var k in arguments[i]) {
            c[k] = arguments[i][k];
        }
    }
    return c;
}

function BrokerStats () {
    this.queues = {};
    this.brokers = create_podgroup();
    var configserv = require('./admin_service.js').connect_service(rhea, 'CONFIGURATION');
    var self = this;
    configserv.open_receiver({source:{address:"podsense", filter:{'role':'broker'}}}).on('message', function (context) {
        if (util.isArray(context.message.body)) {
            self.brokers.update(context.message.body.filter(is_pod_ready));
        } else {
            log.info('unexpected content from podsense (expected body to be array): ' + context.message);
        }
    });
}

BrokerStats.prototype.retrieve = function(addresses) {
    return this._retrieve().then(function (stats) {
        for (var s in stats) {
            addresses.update_stats(s, stats[s]);
        }
    });
};

BrokerStats.prototype._retrieve = function() {
    var brokers = this.brokers.broker_list();
    return Promise.all(brokers.map(list_addresses)).then(function (results) {
        var stats = {};
        for (var i = 0; i < results.length; i++) {
            for (var name in results[i]) {
                var s = get_stats_for_address(stats, name);
                var shard = merge(results[i][name], {name:brokers[i].connection.container_id});
                s.depth += shard.messages;
                s.shards.push(shard);
            }
        }
        return stats;
    });
};

module.exports = BrokerStats;
