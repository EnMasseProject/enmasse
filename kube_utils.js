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

function difference(a, b) {
    var diff = undefined;
    for (var k in a) {
	if (b[k] === undefined) {
	    if (diff === undefined) diff = {};
	    diff[k] = a[k];
	}
    }
    return diff;
}

var Subscription = function () {
    events.EventEmitter.call(this);
    this.current = {};
    this.partial = undefined;
};

util.inherits(Subscription, events.EventEmitter);

Subscription.prototype.on_message = function (msg) {
    var content = this.partial ? this.partial + msg : msg;
    try {
	var o = JSON.parse(content);
	this.partial = undefined;
	if (o.object.subsets) {
	    var base = o.object.subsets[0];
	    if (base) {
		var addresses = base.addresses;
		var port = base.ports[0].port;
		var processes = {};
		addresses.forEach(function (e) { processes[e.targetRef.name] = {'host':e.ip,'port':port}; });
		console.log('process list [' + o.type + ']: ' + JSON.stringify(processes));
		var added = difference(processes, this.current);
		var removed = difference(this.current, processes);
		this.current = processes;
		added && this.emit('added', added);
		removed && this.emit('removed', removed);
	    } else {
		console.log('process list is empty [' + o.type + ']');
	    }
	} else {
	    console.log('unexpected message format [' + o.type + ']: ' + JSON.stringify(o));
	}
    } catch (e) {
	console.log('Could not parse message as JSON, assuming incomplete');
	if (this.partial) {
	    this.partial += msg;
	} else {
	    this.partial = msg;
	}
    }
}

function get_options(service, namespace) {
    var options = {
	hostname: process.env.KUBERNETES_SERVICE_HOST,
	port: process.env.KUBERNETES_SERVICE_PORT,
	rejectUnauthorized: false,
	path: '/api/v1/watch/namespaces/' + namespace + '/endpoints/' + service,
	headers: {
	    'Authorization': 'Bearer ' + fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/token')
	}
    };
    return options;
};

function subscribe(service) {
    var namespace = fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/namespace');

    var subscription = new Subscription();
    var request = https.get(get_options(service, namespace || 'default'), function(response) {
	console.log('STATUS: ' + response.statusCode);
	response.setEncoding('utf8');
	response.on('data', subscription.on_message.bind(subscription));
	response.on('end', function () {
	    console.log('response ended; reconnecting...');
	    subscribe(service, namespace);
	});
    });
    request.on('error', function(e) {
	console.log('problem with request: ' + e.message);
    });
    return subscription;
}
/**
 * To use pass in service name and optionally namespace. Returns a subscription object that emits 'added' and 'removed' events.
 * E.g.
 * var sub = subscribe('qdrouterd')
 * sub.on('added', function (processes) { 
 *      console.log('ADDED: ' + JSON.stringify(processes));
 * });
 * sub.on('removed', function (processes) { 
 *      console.log('REMOVED: ' + JSON.stringify(processes));
 * });
 */
module.exports = {
    'watch_service': subscribe
};
