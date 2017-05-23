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
var util = require("util");
var events = require("events");
var rhea = require('rhea');
var config_service = require('./config_service.js');

var Subscription = function () {
    events.EventEmitter.call(this);
};

util.inherits(Subscription, events.EventEmitter);

Subscription.prototype.close = function () {
    if (this.conn) this.conn.close();
}

Subscription.prototype.subscribe = function (selector, handler) {
    var self = this;
    var amqp = rhea.create_container();
    amqp.on('message', handler);
    var conn = config_service.connect(amqp, "podsense-" + selector.group_id);
    if (conn) {
        conn.open_receiver({source:{address:"podsense", filter:{"labels": {"role": "broker"}, "annotations": selector}});
        this.conn = conn;
    }
}



function get_pod_handler() {
    return function (context) {
        var content = context.message.body;
        if (content === undefined) {
            return;
        }
        filtered = content.filter(function (pod) {
            return pod.ready === 'True' && pod.phase === 'Running';
        });
        this.emit('changed', filtered);
    }
}

module.exports.watch_pods = function subscribe(selector) {
    var subscription = new Subscription();
    var handler = get_pod_handler();
    subscription.subscribe(selector, handler.bind(subscription));
    return subscription;
}
