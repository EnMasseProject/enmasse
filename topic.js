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

function Topic (name) {
    this.name = name;
    this.pods = create_podgroup();
    this.locator = create_locator(this.pods);
    this.controller = create_controller(this.pods);
};

Topic.prototype.watch_pods = function () {
    if (process.env.KUBERNETES_SERVICE_HOST) {
        var topic = this;
        console.log('watching pods serving ' + topic.name);
        var current = {};
        this.watcher = require('./podwatch.js').watch_pods('address=' + topic.name);
        var add = function (pod) {
            if (current[pod.name] === undefined) {
                if (pod.ready) {
                    console.log('pod added for ' + topic.name + ': ' + JSON.stringify(pod));
                    current[pod.name] = pod;
	            topic.pods.added(pod);
                } else {
                    console.log('pod not yet ready for ' + topic.name + ': ' + JSON.stringify(pod));
                }
            } else {
                console.log('pod updated: now ' + JSON.stringify(pod) + ' was ' + JSON.stringify(current[pod.name]));
            }
        };
        this.watcher.on('added', add);
        this.watcher.on('modified', add);
        this.watcher.on('removed', function (pod) {
            var pod = current[pod.name];
            if (pod !== undefined) {
                console.log('pod removed for ' + topic.name + ': ' + JSON.stringify(pod));
	        topic.pods.removed(pod);
                delete current[pod.name];
            }
        });
    }
}

Topic.prototype.close = function () {
    this.pods.close();
    if (this.watcher) this.watcher.close();
}

module.exports = function (name) {
    return new Topic(name);
}
