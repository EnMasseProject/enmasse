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

var Promise = require('bluebird');

function SubscriptionLocator(pods) {
    this.pods = pods;
}

SubscriptionLocator.prototype.locate = function (subscription_id, topic) {
    var broker_list = this.pods.broker_list();
    return new Promise(function (resolve, reject) {
        Promise.all(broker_list.map(function (b) { return b.getBoundQueues(topic); } )).then(
            function (results) {
                var address = topic;
                for (var i = 0; i < results.length; i++) {
                    for (var j = 0; j < results[i].length; j++) {
                        if (results[i][j].indexOf(subscription_id) >= 0) {
                            address = topic + '/' + broker_list[i].connection.container_id;
                            console.log('matched result ' + i + ','  + j + ' of ' + results[i].length + ': ' + results[i][j]);
                            break;
                        } else {
                            console.log('did not match result ' + i + ','  + j + ' of ' + results.length + ': ' + results[i][j]);
                        }
                    }
                }
                console.log('resolved ' + subscription_id + ' on ' + topic + ' to ' + address);
                resolve(address);
            }
        ).catch (
            function(e) {
                reject(e);
            }
        );
    });
}

module.exports = function (pods) {
    return new SubscriptionLocator(pods);
}
