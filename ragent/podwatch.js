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
var util = require("util");
var events = require("events");
var rhea = require('rhea');
var log = require("./log.js").logger();

function difference(a, b) {
    var diff = undefined;
    for (var k in a) {
        if (b[k] === undefined) {
            if (diff === undefined) {
                diff = {};
            }
            diff[k] = a[k];
        }
    }
    return diff;
}

var Subscription = function (connection) {
    events.EventEmitter.call(this);
    this.podsense = connection;
    this.pods = {};
    this.receiver = undefined;
    this.localhost = process.env.HOSTNAME;
}

util.inherits(Subscription, events.EventEmitter);

Subscription.prototype.close = function () {
    this.receiver.close();
}

Subscription.prototype.subscribe = function (selector, handler) {
    log.info('Subscribing with selector: ' + JSON.stringify(selector));
    this.receiver = this.podsense.open_receiver({source:{address:"podsense", filter:selector}});
    this.receiver.on('message', handler);
}

function create_pod_handler() {
    return function (context) {
        var content = context.message.body;
        if (!content) {
            return;
        }
        var myhost = this.localhost;
        log.info('Current pods: ' + JSON.stringify(this.pods) + '. Received updated pods: ' + JSON.stringify(content));
        newpods = content.filter(function (pod) {
            return pod.ready === 'True' && pod.phase === 'Running' && pod.name != myhost;
        }).reduce(function (map, pod) {
            map[pod.name] = pod;
            return map;
        }, {});

        var added = difference(newpods, this.pods);
        var removed = difference(this.pods, newpods);

        added && this.emit('added', added);
        removed && this.emit('removed', removed);

        this.pods = newpods;
    }
}

module.exports.watch_pods = function subscribe(connection, selector) {
    var subscription = new Subscription(connection);
    var handler = create_pod_handler();
    subscription.subscribe(selector, handler.bind(subscription));
    return subscription;
}
