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

var events = require('events');
var fs = require('fs');
var http = require('http');
var https = require('https');
var path = require('path');
var url = require('url');
var util = require('util');

function shift_if_matches(path, match) {
    if (path[0] === match) {
        path.shift();
        return true;
    } else {
        return false;
    }
}

function parse(request_url) {
    var result = {};
    var path = url.parse(request_url).pathname.split('/');
    shift_if_matches(path, '');
    if (path.shift() === 'api' && path.shift() === 'v1') {
        result.watch = shift_if_matches(path, 'watch');
        if (shift_if_matches(path, 'namespaces')) {
            result.namespace = path.shift();
            result.type = path.shift();
            result.name = path.shift();
            return result;
        }
    }
    return undefined;
}

function ResourceServer (kind, read_only, externalize, internalize) {
    events.EventEmitter.call(this);
    this.kind = kind;
    this.read_only = read_only;
    this.externalize = externalize || function (o) { return o; };
    this.internalize = internalize;
    this.list_kind = kind + 'List';
    this.resources = [];
    this.watch_timeout = 1000;
    Object.defineProperty(this, 'externalized', { get: function () { return this.resources.map(this.externalize); } });
}

util.inherits(ResourceServer, events.EventEmitter);

ResourceServer.prototype.update = function (index, object) {
    this.resources[index] = this.internalize ? this.codec.internalize(object) : object;
};

function relativepath(name) {
    return path.resolve(__dirname, name);
}

function error(request, response, status_code, message) {
    response.statusCode = status_code;
    var text = message || http.STATUS_CODES[status_code];
    response.end(util.format('%s: %s %s', text, request.method, request.url));
}

ResourceServer.prototype.listen = function (port, callback) {
    var self = this;
    var options = {
        key: fs.readFileSync(relativepath('../test/server-key.pem')),
        cert: fs.readFileSync(relativepath('../test/server-cert.pem'))
    };
    this.server = https.createServer(options, function (request, response) {
        var path = parse(request.url);
        //console.log('%s %s => %j', request.method, request.url, path);
        if (path === undefined) {
            error(request, response, 404);
        } else {
            if (path.watch && request.method === 'GET') {
                var before = self.listeners('added').length;
                self.watch_resources(request, response);
            } else if (request.method === 'GET') {
                if (path.name) {
                    self.get_resource(request, response, path.name);
                } else {
                    self.get_resources(request, response);
                }
            } else if (request.method === 'PUT' && path.name) {
                self.put_resource(request, response, path.name);
            } else {
                error(request, response, 405);
            }
        }
    });
    this.server.listen(port || 0, function () {
        self.port = self.server.address().port;
        if (callback) callback();
    });
    return this.server;
};

ResourceServer.prototype.close = function (callback) {
    this.clear();
    this.server.close(callback);
};

ResourceServer.prototype.clear = function () {
    this.resources = [];
};

ResourceServer.prototype.add_resource = function (resource) {
    this.resources.push(resource);
    this.emit('added', this.externalize(resource));
};

ResourceServer.prototype.resource_modified = function (resource) {
    this.emit('modified', this.externalize(resource));
};

ResourceServer.prototype.remove_resource = function (resource) {
    var i = this.resources.indexOf(resource);
    if (i >= 0) {
        this.resources.splice(i, 1);
        this.emit('deleted', this.externalize(resource));
    }
};

ResourceServer.prototype.remove_resource_by_name = function (name) {
    for (var i = 0; i < this.resources.length; i++) {
        if (this.resources[i].metadata.name === name) {
            var removed = this.resources[i];
            this.resources.splice(i, 1);
            this.emit('deleted', this.externalize(removed));
            break;
        }
    }
};

function Watcher (parent, response) {
    this.parent = parent;
    this.response = response;
    this.callbacks = {};
    this.add_callback('added');
    this.add_callback('deleted');
    this.add_callback('modified');
    for (var k in this.callbacks) {
        this.parent.on(k, this.callbacks[k]);
    }
}

Watcher.prototype.add_callback = function (type) {
    var response = this.response;
    this.callbacks[type] = function (resource) {
        response.write(JSON.stringify({type:type.toUpperCase(), object:resource}) + '\n');
    };
};

Watcher.prototype.close = function () {
    for (var k in this.callbacks) {
        this.parent.removeListener(k, this.callbacks[k]);
    }
    this.response.end();
}

ResourceServer.prototype.get_resources = function (request, response) {
    response.end(JSON.stringify({kind: this.list_kind, items: this.externalized}));
};

ResourceServer.prototype.watch_resources = function (request, response) {
    for (var i = 0; i < this.resources.length; i++) {
        response.write(JSON.stringify({type:'ADDED', object:this.externalized[i]}) + '\n');
    }
    var watcher = new Watcher(this, response);
    setTimeout(function () { watcher.close(); }, this.watch_timeout);
};

ResourceServer.prototype.find_resource = function (name) {
    for (var i = 0; i < this.resources.length; i++) {
        if (this.externalized[i].metadata.name === name) {
            return this.externalized[i];
        }
    }
};

ResourceServer.prototype.get_resource = function (request, response, name) {
    var result = this.find_resource(name);
    if (result) {
        response.end(JSON.stringify(result));
    } else {
        error(request, response, 404);
    }
};

ResourceServer.prototype.update_resource = function (name, updated) {
    for (var i = 0; i < this.resources.length; i++) {
        if (this.externalized[i].metadata.name === name) {
            this.update(i, updated);
            return true;
        }
    }
    return false;
};

ResourceServer.prototype.put_resource = function (request, response, name) {
    var self = this;
    var bodytext = '';
    request.on('data', function (data) { bodytext += data; });
    request.on('end', function () {
        var body = JSON.parse(bodytext);
        if (body.kind === self.kind) {
            if (self.update_resource(name, body)) {
                response.statusCode = 200;
                response.end();
            } else {
                error(request, response, 404);
            }
        } else {
            error(request, response, 500, 'Invalid resource kind ' + body.kind);
        }
    });
};

function ConfigMapServer () {
    ResourceServer.call(this, 'ConfigMap');
}

util.inherits(ConfigMapServer, ResourceServer);

ConfigMapServer.prototype.add_address_definition = function (def, name) {
    var address = {kind: 'Address', metadata: {name: def.address}, spec:def, status:{}};
    this.add_resource({kind:'ConfigMap', metadata: {name: name || def.address}, data:{'config.json': JSON.stringify(address)}});
};

ConfigMapServer.prototype.add_address_definitions = function (defs) {
    for (var i in defs) {
        this.add_address_definition(defs[i]);
    }
};

module.exports.ResourceServer = ResourceServer;
module.exports.ConfigMapServer = ConfigMapServer;
