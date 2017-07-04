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

var create_podgroup = require('./podgroup.js');
var create_locator = require('./subloc.js');
var create_controller = require('./subctrl.js');
var log = require('./log.js').logger();

function Topic (name) {
    this.name = name;
    this.pods = create_podgroup();
    this.locator = create_locator(this.pods);
    this.controller = create_controller(this.pods);
};

Topic.prototype.watch_pods = function () {
    var topic = this;
    log.debug('watching pods serving ' + topic.name);
    var current = {};
    this.watcher = require('./podwatch.js').watch_pods({"cluster_id": topic.name});
    var changed = function (pods) {
        var newpods = {};
        for (var i in pods) {
            newpods[pods[i].name] = pods[i];
        }
        for (var pod in newpods) {
            if (current[pod] === undefined) {
                current[pod] = newpods[pod];
                topic.pods.added(newpods[pod]);
                log.debug('pod added for ' + topic.name + ': ' + JSON.stringify(pod));
            } else {
                log.debug('pod updated: now ' + JSON.stringify(pod) + ' was ' + JSON.stringify(current[pod]));
            }
        }

        for (var pod in current) {
            if (newpods[pod] === undefined) {
                topic.pods.removed(current[pod]);
                log.debug('pod removed for ' + topic.name + ': ' + JSON.stringify(current[pod]));
                delete current[pod];
            }
        }
    };
    this.watcher.on('changed', changed);
}

Topic.prototype.close = function () {
    this.pods.close();
    if (this.watcher) this.watcher.close();
}

module.exports = function (name) {
    return new Topic(name);
}
