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
var https = require('https');
var fs = require('fs');
var util = require("util");
var events = require("events");
var querystring = require("querystring");

var Subscription = function () {
    events.EventEmitter.call(this);
    this.closed = false;
};

util.inherits(Subscription, events.EventEmitter);

Subscription.prototype.close = function () {
    this.closed = true;
}
Subscription.prototype.subscribe = function (options, handler) {
    var self = this;
    var request = https.get(options, function(response) {
	console.log('STATUS: ' + response.statusCode);
	response.setEncoding('utf8');
	response.on('data', handler);
	response.on('end', function () {
            if (!this.closed) {
	        console.log('response ended; reconnecting...');
	        self.subscribe(options, handler);
            }
	});
    });
    request.on('error', function(e) {
	console.log('problem with request: ' + e.message);
    });
}

function subscribe(options, handler) {
    var subscription = new Subscription();
    subscription.subscribe(options, handler.bind(subscription));
    return subscription;
}

function get_pod_handler() {
    var current = {};
    var partial = undefined;
    return function (msg) {
        var content = partial ? partial + msg : msg;
        var start = 0;
        for (var end = content.indexOf('\n', start); end > 0; start = end + 1, end = start < content.length ? content.indexOf('\n', start) : -1) {
            var line = content.substring(start, end);
            var o;
            try {
	        o = JSON.parse(line);
            } catch (e) {
	        console.log('Could not parse message as JSON, assuming incomplete: ' + line);
                break;
            }
            var pod = {
                name: o.object.metadata.name,
                ip: o.object.status.podIP,
                status: o.object.status.phase
            };
            if (o.object.status.conditions) {
                console.log(JSON.stringify(o.object.status.conditions));
                for (var i = 0; i < o.object.status.conditions.length; i++) {
                    var condition = o.object.status.conditions[i];
                    if (condition.type === 'Ready') {
                        pod.ready = o.object.status.conditions[i].status === 'True';
                    }
                }
            }
            if (o.object.spec.containers) {
                pod.ports = {};
                for (var i in o.object.spec.containers) {
                    var c = o.object.spec.containers[i];
                    if (c.ports) {
                        pod.ports[c.name] = {};
                        for (var j in c.ports) {
                            var p = c.ports[j];
                            pod.ports[c.name][p.name] = p.containerPort;
                        }
                    }
                }
            }
            if (o.type === 'ADDED') {
	        this.emit('added', pod);
            } else if (o.type === 'MODIFIED') {
	        this.emit('modified', pod);
            } else if (o.type === 'DELETED') {
	        this.emit('removed', pod);
            } else {
                console.log('ERROR: unknown type for pod watcher ' + o.type);
            }
        }
	partial = content.substring(start);
    }
}

module.exports.watch_pods = function (selector, namespace) {
    var ns = namespace || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/namespace') || 'default';
    var options = {
	hostname: process.env.KUBERNETES_SERVICE_HOST,
	port: process.env.KUBERNETES_SERVICE_PORT,
	rejectUnauthorized: false,
	path: '/api/v1/watch/namespaces/' + ns + '/pods?' + querystring.stringify({'labelSelector':selector}),
	headers: {
	    'Authorization': 'Bearer ' + fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/token')
	}
    };
    console.log('subscribing with path: ' + options.path);
    return subscribe(options, get_pod_handler());
};
