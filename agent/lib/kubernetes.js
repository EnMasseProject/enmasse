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
    var path = base + namespace + '/' + resource;
    if (options.selector) {
        path += '?' + querystring.stringify({'labelSelector':options.selector});
    }
    return path;
}

function list_options(resource, options) {
    return get_options(options, get_path('/api/v1/namespaces/', resource, options));
}

function watch_options(resource, options) {
    return get_options(options, get_path('/api/v1/watch/namespaces/', resource, options));
}

function do_get(resource, options) {
    return new Promise(function (resolve, reject) {
        var opts = list_options(resource, options || {});
        var request = https.get(opts, function(response) {
	    log.info('GET %s => %s ', opts.path, response.statusCode);
	    response.setEncoding('utf8');
            var data = '';
	    response.on('data', function (chunk) { data += chunk; });
	    response.on('end', function () {
                if (response.statusCode === 200) {
                    try {
                        resolve(JSON.parse(data));
                    } catch (e) {
                        reject(new Error(util.format('Could not parse message as JSON (%s): %s', e, data)));
                    }
                } else {
                    reject(new Error(util.format('Failed to retrieve %s: %s %s', opts.path, response.statusCode, data)));
                }
	    });
        });
        request.on('error', function (error) {
            log.info('error while doing GET on %s: %j', opts.path, error);
        });
    });
};

function do_put(resource, object, options) {
    return new Promise(function (resolve, reject) {
        var opts = list_options(resource, options || {});
        opts.method = 'PUT';
        var request = https.request(opts, function(response) {
	    log.info('PUT %s => %s ', opts.path, response.statusCode);
            resolve(response.statusCode);
        });
        request.on('error', reject);
        request.write(JSON.stringify(object));
        request.end();
    });
};

function Watcher (resource, options) {
    events.EventEmitter.call(this);
    this.closed = false;
    this.resource = resource;
    this.options = options;
    this.objects = [];
}

util.inherits(Watcher, events.EventEmitter);

Watcher.prototype.list = function () {
    do_get(this.resource, this.options).then(this.handle_list_result.bind(this)).catch(this.handle_list_error.bind(this));
};

Watcher.prototype.handle_list_error = function (error) {
    this.emit('error', error);
};

Watcher.prototype.handle_list_result = function (result) {
    this.objects = result.items;
    this.emit('updated', this.objects);
    log.info('list retrieved; watching...');
    this.watch();
};

Watcher.prototype.list = function () {
    var self = this;
    do_get(this.resource, this.options).then(function (result) {
        self.objects = result.items;
        self.emit('updated', self.objects);
        log.info('list retrieved; watching...');
        self.watch();
    }).catch(function (error) {
        self.emit('error', error);
    });
};

Watcher.prototype.watch = function () {
    var self = this;
    var opts = watch_options(this.resource, this.options);
    var request = https.get(opts, function(response) {
        log.info('GET %s => %s ', opts.path, response.statusCode);
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

module.exports.update = function (resource, transform, options) {
    return do_get(resource, options).then(function (original) {
        return do_put(resource, transform(original), options);
    });
};
