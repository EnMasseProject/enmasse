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
var https = require('https');
var fs = require('fs');
var util = require("util");
var events = require("events");
var querystring = require("querystring");

function get_handler(emitter) {
    var partial = undefined;
    return function (msg) {
        var content = partial ? partial + msg : msg;
        var start = 0;
        for (var end = content.indexOf('\n', start); end > 0; start = end + 1, end = start < content.length ? content.indexOf('\n', start) : -1) {
            var line = content.substring(start, end);
            var event;
            try {
	        event = JSON.parse(line);
            } catch (e) {
	        console.log('Could not parse message as JSON (%s), assuming incomplete: %s', e, line);
                break;
            }
            emitter.emit(event.type.toLowerCase(), event.object);
        }
	partial = content.substring(start);
    }
}

var Watcher = function (options) {
    events.EventEmitter.call(this);
    this.closed = false;
    this.options = options;
    this.handler = get_handler(this);
    console.log('watching path: ' + options.path);
    var self = this;
    var request = https.get(this.options, function(response) {
	console.log('STATUS: ' + response.statusCode);
	response.setEncoding('utf8');
	response.on('data', self.handler);
	response.on('end', function () {
            if (!this.closed) {
	        console.log('response ended; reconnecting...');
	        self.watch();
            }
	});
    });
    request.on('error', function(e) {
	self.emit('error', e);
    });
};

util.inherits(Watcher, events.EventEmitter);

Watcher.prototype.close = function () {
    this.closed = true;
};

function get_path(resource, options) {
    var namespace = options.namespace || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/namespace');
    return '/api/v1/watch/namespaces/' + namespace + '/' + resource + '?' + querystring.stringify({'labelSelector':options.selector});
}

module.exports.watch = function (resource, options) {
    var opts = {
	hostname: process.env.KUBERNETES_SERVICE_HOST,
	port: process.env.KUBERNETES_SERVICE_PORT,
	rejectUnauthorized: false,
	path: get_path(resource, options),
	headers: {
	    'Authorization': 'Bearer ' + (options.token || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/token'))
	}
    };
    return new Watcher(opts);
};
