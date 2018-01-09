/*
 * Copyright 2017 Red Hat Inc.
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
var kubernetes = require('./kubernetes.js');

function extract_pod_definition (object) {
    var pod = {
        name: object.metadata.name,
        host: object.status.podIP,
        phase: object.status.phase,
        annotations: object.metadata.annotations
    };
    for (var i in object.status.conditions) {
        if (object.status.conditions[i].type === 'Ready') {
            pod.ready = object.status.conditions[i].status;
        }
    }
    if (object.spec.containers) {
        pod.ports = {};
        for (var i in object.spec.containers) {
            var c = object.spec.containers[i];
            if (c.ports) {
                pod.ports[c.name] = {};
                for (var j in c.ports) {
                    var p = c.ports[j];
                    pod.ports[c.name][p.name] = p.containerPort;
                }
            }
        }
    }
    return pod;
};

function PodWatcher (options) {
    events.EventEmitter.call(this);
    this.watcher = kubernetes.watch('pods', options);
    this.watcher.on('updated', this.on_update.bind(this));
}

util.inherits(PodWatcher, events.EventEmitter);

PodWatcher.prototype.on_update = function (pods) {
    this.emit('updated', pods.map(extract_pod_definition));
};

PodWatcher.prototype.close = function () {
    this.watcher.close();
};

module.exports.watch = function (selector, env) {
    var options = env || {};
    if (selector) {
        options.selector = selector;
    }
    return new PodWatcher(options);
};
