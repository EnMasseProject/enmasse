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
var log = require('./log.js').logger();
var https = require('https');
var fs = require('fs');
var util = require('util');
var events = require('events');
var querystring = require('querystring');
var myutils = require('./utils.js');
var myevents = require('./events.js');

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

var cache = {};

function read(file) {
    if (cache[file] === undefined) {
        cache[file] = fs.readFileSync(file);
        setTimeout(function () { cache[file] = undefined; }, 1000*60*5);//force refresh every 5 minutes
    }
    return cache[file];
}

function get_options(options, path) {
    return {
        hostname: options.host || process.env.KUBERNETES_SERVICE_HOST,
        port: options.port || process.env.KUBERNETES_SERVICE_PORT,
        rejectUnauthorized: false,
        path: options.path || path,
        headers: {
            'Authorization': 'Bearer ' + (options.token || process.env.KUBERNETES_TOKEN || read('/var/run/secrets/kubernetes.io/serviceaccount/token'))
        }
    };
}

function get_path(base, resource, options) {
    var namespace = options.namespace || process.env.KUBERNETES_NAMESPACE || read('/var/run/secrets/kubernetes.io/serviceaccount/namespace');
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

function do_get_with_options(opts) {
    return new Promise(function (resolve, reject) {
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
        request.on('error', reject);
    });
};

function do_get(resource, options) {
    var opts = list_options(resource, options || {});
    return do_get_with_options(opts);
};

function do_request(method, resource, input, options) {
    return new Promise(function (resolve, reject) {
        var opts = list_options(resource, options || {});
        opts.method = method;
        var request = https.request(opts, function(response) {
            var data = '';
	    response.on('data', function (chunk) { data += chunk; });
	    response.on('end', function () {
	        log.info('%s %s => %s', opts.method, opts.path, response.statusCode);
                resolve(response.statusCode, data);
            });
        });
        request.on('error', reject);
        if (input) request.write(input);
        request.end();
    });
};

function do_post(resource, object, options) {
    return do_request('POST', resource, JSON.stringify(object), options);
};

function do_put(resource, object, options) {
    return do_request('PUT', resource, JSON.stringify(object), options);
};

function do_delete(resource, options) {
    return do_request('DELETE', resource, undefined, options);
};

function Watcher (resource, options) {
    events.EventEmitter.call(this);
    this.closed = false;
    this.resource = resource;
    this.options = options;
    this.objects = [];
}

util.inherits(Watcher, events.EventEmitter);

Watcher.prototype.notify = function () {
    var self = this;
    setImmediate( function () {
        self.emit('updated', self.objects);
    });
};

Watcher.prototype.list = function () {
    var self = this;
    do_get(this.resource, this.options).then(function (result) {
        self.objects = result.items;
        self.notify();
        if (!self.closed) {
            log.info('list retrieved; watching...');
            self.watch();
        } else {
            self.emit('closed');
        }
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
            if (!self.closed) {
                log.info('response ended; reconnecting...');
                self.list();
            } else {
                self.emit('closed');
            }
        });
    });
};

function matcher(object) {
    return function (o) { return o.metadata.name === object.metadata.name; };
};

Watcher.prototype.added = function (object) {
    if (!this.objects.some(matcher(object))) {
        this.objects.push(object);
        this.notify();
    }
};

Watcher.prototype.modified = function (object) {
    myutils.replace(this.objects, object, matcher(object));
    this.notify();
};

Watcher.prototype.deleted = function (object) {
    myutils.remove(this.objects, matcher(object));
    this.notify();
};

Watcher.prototype.close = function () {
    this.closed = true;
    var self = this;
    return new Promise(function (resolve) {
        self.once('closed', function () {
            resolve();
        });
    });
};

module.exports.get = function (resource, options) {
    return do_get(resource, options);
};

module.exports.post = function (resource, object, options) {
    return do_post(resource, object, options);
};

module.exports.delete_resource = function(resource, options) {
    return do_delete(resource, options);
}

module.exports.watch = function (resource, options) {
    var w = new Watcher(resource, options);
    w.list();
    return w;
};

module.exports.update = function (resource, transform, options) {
    return do_get(resource, options).then(function (original) {
        var updated = transform(original);
        if (updated !== undefined) {
            return do_put(resource, updated, options);
        } else {
            return 304;
        }
    });
};

function post_new_event(event) {
    return do_post('events', event).then(function (code, message) {
        if (code >= 400) log.warn('failed to post new event: %j %s %s', event, code, message);
        else log.debug('posted new event: %j %s %s', event, code);
    }).catch(function (error) {
        log.warn(' failed to post event: %j %s', event, error);
    });
}

module.exports.post_event = function (event) {
    event.involvedObject.namespace = process.env.KUBERNETES_NAMESPACE || read('/var/run/secrets/kubernetes.io/serviceaccount/namespace').toString();
    return do_get(util.format('events/%s', event.metadata.name)).then(function (original) {
        if (myevents.equivalent(event, original)) {
            original.count++;
            original.lastTimestamp = event.lastTimestamp;
            return do_put(util.format('events/%s', event.metadata.name), original).then(function (code, message) {
                log.debug('updated existing event: %j %s %s', event, code, message);
            });
        } else {
            return post_new_event(event);
        }
    }).catch(function () {
        return post_new_event(event);
    });
};


module.exports.get_messaging_route_hostname = function (options) {
    if (options.MESSAGING_ROUTE_HOSTNAME === undefined && options.KUBERNETES_SERVICE_HOST !== undefined) {
        var messaging_route_name = options.MESSAGING_ROUTE_NAME || 'messaging';
        var opts = get_options(options, get_path('/oapi/v1/namespaces/', 'routes/' + messaging_route_name, options));
        return do_get_with_options(opts).then(function (definition) {
            return definition.spec.host;
        }).catch(function () {
            log.info('could not retrieve messaging route hostname');
            return undefined;
        });
    } else {
        return Promise.resolve(options.MESSAGING_ROUTE_HOSTNAME);
    }
};
