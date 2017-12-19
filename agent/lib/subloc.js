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

var log = require('./log.js').logger();

function SubscriptionLocator(pods) {
    this.pods = pods;
}

SubscriptionLocator.prototype.locate = function (subscription_id, topic) {
    var self = this;
    return new Promise(function (resolve, reject) {
        var pod_list = self.pods.pod_list();
        if (pod_list === undefined || pod_list.length === 0) {
            reject("no connected brokers for " + topic);
        } else {
            Promise.all(pod_list.map(function (p) { return p.broker.getBoundQueues(topic + '/' + p.name); } )).then(
                function (results) {
                    var found = false;
                    var address, min;
                    for (var i = 0; !found && i < results.length; i++) {
                        for (var j = 0; !found && j < results[i].length; j++) {
                            if (results[i][j].indexOf(subscription_id) >= 0) {
                                address = topic + '/' + pod_list[i].name;
                                found = true;
                                log.debug('matched result ' + i + ','  + j + ' of ' + results[i].length + ': ' + results[i][j]);
                            } else {
                                log.debug('did not match result ' + i + ','  + j + ' of ' + results.length + ': ' + results[i][j]);
                            }
                        }
                        if (!found && (min === undefined || results[i].length < min)) {
                            min = results[i].length;
                            address = topic + '/' + pod_list[i].name;
                        }
                    }
                    log.debug('resolved ' + subscription_id + ' on ' + topic + ' to ' + address);
                    resolve(address);
                }
            ).catch (
                function(e) {
                    reject(e);
                }
            );
        }
    });
}

module.exports = function (pods) {
    return new SubscriptionLocator(pods);
}
