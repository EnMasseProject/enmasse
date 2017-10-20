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
var log = require("./log.js").logger();
var https = require('https');
var fs = require('fs');
var util = require("util");
var events = require("events");
var querystring = require("querystring");
var myutils = require("./utils.js");

function watch_handler(collection) {
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
                log.warn('Could not parse message as JSON (%s), assuming incomplete: %s', e, line);
                break;
            }
            collection[event.type.toLowerCase()](event.object);
        }
        partial = content.substring(start);
    }
}

function get_options(options, path) {
    return {
        hostname: options.host || process.env.KUBERNETES_SERVICE_HOST,
        port: options.port || process.env.KUBERNETES_SERVICE_PORT,
        rejectUnauthorized: false,
        path: path,
        headers: {
            'Authorization': 'Bearer ' + (options.token || process.env.KUBERNETES_TOKEN || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/token'))
        }
    };
}

function get_path(base, resource, options) {
    var namespace = options.namespace || process.env.KUBERNETES_NAMESPACE || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/namespace');
    return base + namespace + '/' + resource + '?' + querystring.stringify({'labelSelector':options.selector});
}

function list_path(resource, options) {
    return get_path('/api/v1/namespaces/', resource, options);
}

function watch_path(resource, options) {
    return get_path('/api/v1/watch/namespaces/', resource, options);
}

function list_options(resource, options) {
    return get_options(options, get_path('/api/v1/namespaces/', resource, options));
}

function watch_options(resource, options) {
    return get_options(options, get_path('/api/v1/watch/namespaces/', resource, options));
}

function Watcher (resource, options) {
    events.EventEmitter.call(this);
    this.closed = false;
    this.resource = resource;
    this.options = options;
    this.objects = [];
}

util.inherits(Watcher, events.EventEmitter);

Watcher.prototype.list = function () {
    var self = this;
    var request = https.get(list_options(this.resource, this.options), function(response) {
    log.info('STATUS: ' + response.statusCode);
    response.setEncoding('utf8');
    var data = '';
    response.on('data', function (chunk) { data += chunk; });
    response.on('end', function () {
            try {
                var list = JSON.parse(data);
                self.objects = list.items;
                self.emit('updated', self.objects);
                log.info('list retrieved; watching...');
                self.watch();
            } catch (e) {
                log.warn('Could not parse message as JSON (%s): %s', e, data);
            }
        });
    });
    request.on('error', function(e) {
        self.emit('error', e);
    });
};

Watcher.prototype.watch = function () {
    var self = this;
    var request = https.get(watch_options(this.resource, this.options), function(response) {
    log.info('STATUS: ' + response.statusCode);
    response.setEncoding('utf8');
    response.on('data', watch_handler(self));
    response.on('end', function () {
            if (!this.closed) {
                log.info('response ended; reconnecting...');
                self.list();
            }
        });
    });
    request.on('error', function(e) {
        self.emit('error', e);
    });
};

function matcher(object) {
    return function (o) { return o.metadata.name === object.metadata.name; };
};

Watcher.prototype.added = function (object) {
    if (!this.objects.some(matcher(object))) {
        this.objects.push(object);
        this.emit('updated', this.objects);
    }
};

Watcher.prototype.modified = function (object) {
    myutils.replace(this.objects, object, matcher(object));
    this.emit('updated', this.objects);
};

Watcher.prototype.deleted = function (object) {
    myutils.remove(this.objects, matcher(object));
    this.emit('updated', this.objects);
};

Watcher.prototype.close = function () {
    this.closed = true;
};

module.exports.watch = function (resource, options) {
    var w = new Watcher(resource, options);
    w.list();
    return w;
};
