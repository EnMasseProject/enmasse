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

var path = require('path');
var fs = require('fs');
var artemis = require('./artemis.js');
var cert_dir = (process.env.CERT_DIR !== undefined) ? process.env.CERT_DIR : '/etc/enmasse-certs';
var ca_path = path.resolve(cert_dir, 'ca.crt');
var client_crt_path = path.resolve(cert_dir, 'tls.crt');
var client_key_path = path.resolve(cert_dir, 'tls.key');
var log = require('./log.js').logger();

function get(object, fields, default_value) {
    var o = object;
    for (var i = 0; o && i < fields.length; i++) {
        o = o[fields[i]];
    }
    return o || default_value;
};

function get_broker_port(pod) {
    return get(pod.ports, ['broker', 'amqp'], 5673);
};

function Pod(pod) {
    this.name = pod.name;
    var options = {
        id:   pod.name,
        host: pod.host,
        port: get_broker_port(pod)
    };
    try {
        options.ca                 = [fs.readFileSync(ca_path)];
        options.transport          = 'tls';
        options.rejectUnauthorized = false;
        options.key                = fs.readFileSync(client_key_path);
        options.cert               = fs.readFileSync(client_crt_path);
    } catch (error) {
        console.warn('Unable to load certificates: ' + error);
    }
    this.broker = artemis.connect(options);
    log.debug("this.broker has been set to artemis.connect() to " + pod.host + " for " + this.name);
};

Pod.prototype.close = function () {
    this.broker.close();
};

function PodGroup() {
    this.pods = {};
};

PodGroup.prototype.added = function (pod) {
    this.pods[pod.name] = new Pod(pod);
};

PodGroup.prototype.removed = function (pod) {
    //TODO: may be restarted, so should wait for a while before removing
    this.pods[pod.name].close();
    delete this.pods[pod.name];
};

PodGroup.prototype.pod_list = function () {
    var list = [];
    for (var i in this.pods) {
        log.info("pod_list, found pod with name " + i);
        list.push(this.pods[i]);
    }
    return list;
};

PodGroup.prototype.broker_list = function () {
    var list = [];
    for (var i in this.pods) {
        list.push(this.pods[i].broker);
    }
    return list;
};

PodGroup.prototype.update = function (pods) {
    this.close();
    pods.forEach(this.added.bind(this));
};

PodGroup.prototype.close = function () {
    for (var p in this.pods) {
        this.pods[p].close();
    }
};

module.exports = function () {
    return new PodGroup();
}
