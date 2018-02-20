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
    this.debug = false;
    if (this.debug) {
        this.on('added', function (o) {
            console.log('ADDED: %j', o);
        });
        this.on('deleted', function (o) {
            console.log('DELETED: %j', o);
        });
    }
}

util.inherits(ResourceServer, events.EventEmitter);

ResourceServer.prototype.update = function (index, object) {
    this.resources[index] = this.internalize ? this.codec.internalize(object) : object;
};

ResourceServer.prototype.push = function (object) {
    this.resources.push(this.internalize ? this.codec.internalize(object) : object);
    this.emit('added', this.externalize(object));
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
        if (self.debug) console.log('%s %s => %j', request.method, request.url, path);
        if (path === undefined) {
            error(request, response, 404);
        } else {
            if (path.watch && request.method === 'GET') {
                self.watch_resources(request, response);
            } else if (request.method === 'GET') {
                if (path.name) {
                    self.get_resource(request, response, path.name);
                } else {
                    self.get_resources(request, response);
                }
            } else if (request.method === 'PUT' && path.name) {
                self.put_resource(request, response, path.name);
            } else if (request.method === 'DELETE' && path.name) {
                self.delete_resource(request, response, path.name);
            } else if (request.method === 'POST') {
                self.post_resource(request, response);
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
    if (this.resource_initialiser) {
        this.resource_initialiser(resource);
    }
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
            return true;
        }
    }
    return false;
};

function getLabelSelectorFn(request) {
    var parsed = url.parse(request.url, true);
    var selector = parsed.query.labelSelector;
    if (selector) {
        var parts = selector.split('=');
        return function (object) {
            return object.metadata.labels && object.metadata.labels[parts[0]] === parts[1];
        }
    } else {
        return function () {
            return true;
        }
    }
}

function Watcher (parent, response, selector) {
    this.parent = parent;
    this.response = response;
    this.selector = selector;
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
    var filter = this.selector;
    this.callbacks[type] = function (resource) {
        if (filter(resource)) {
            response.write(JSON.stringify({type:type.toUpperCase(), object:resource}) + '\n');
        }
    };
};

Watcher.prototype.close = function () {
    for (var k in this.callbacks) {
        this.parent.removeListener(k, this.callbacks[k]);
    }
    this.response.end();
}

ResourceServer.prototype.get_resources = function (request, response) {
    var filter = getLabelSelectorFn(request);
    response.end(JSON.stringify({kind: this.list_kind, items: this.externalized.filter(filter)}));
};

ResourceServer.prototype.watch_resources = function (request, response) {
    var filter = getLabelSelectorFn(request);
    for (var i = 0; i < this.resources.length; i++) {
        if (filter(this.externalized[i])) {
            response.write(JSON.stringify({type:'ADDED', object:this.externalized[i]}) + '\n');
        }
    }
    var watcher = new Watcher(this, response, filter);
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

ResourceServer.prototype.add_resource_if_not_exists = function (resource) {
    for (var i = 0; i < this.resources.length; i++) {
        if (this.externalized[i].metadata.name === resource.metadata.name) {
            return false;
        }
    }
    this.add_resource(resource);
    return true;
};

ResourceServer.prototype.delete_resource = function (request, response, name) {
    if (this.remove_resource_by_name(name)) {
        response.statusCode = 200;
        response.end();
    } else {
        error(request, response, 404);
    }
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

ResourceServer.prototype.post_resource = function (request, response) {
    var self = this;
    var bodytext = '';
    request.on('data', function (data) { bodytext += data; });
    request.on('end', function () {
        var body = JSON.parse(bodytext);
        if (body.kind === self.kind) {
            if (self.add_resource_if_not_exists(body)) {
                response.statusCode = 200;
                response.end();
            } else {
                error(request, response, 409);
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

function get_config_map(name, type, key, content) {
    var cm = {
        kind:'ConfigMap',
        metadata: {
            name: name,
            labels: {
                type: type
            }
        },
        data:{}
    };
    cm.data[key] = JSON.stringify(content);
    return cm;
}

ConfigMapServer.prototype.add_address_definition = function (def, name, annotations, status) {
    var address = {kind: 'Address', metadata: {name: name || def.address}, spec:def};
    if (annotations) {
        address.metadata.annotations = annotations;
    }
    address.status = status || { phase: 'Active' };
    this.add_resource(get_config_map(name || def.address, 'address-config', 'config.json', address));
};

ConfigMapServer.prototype.add_address_definitions = function (defs) {
    for (var i in defs) {
        this.add_address_definition(defs[i]);
    }
};

ConfigMapServer.prototype.add_address_plan = function (params) {
    var plan = {
        kind: 'AddressPlan',
        metadata: {name: params.plan_name},
        displayName: params.display_name,
        shortDescription: params.shortDescription,
        longDescription: params.longDescription,
        displayOrder: params.displayOrder,
        addressType: params.address_type
    };
    this.add_resource(get_config_map(plan.name, 'address-plan', 'definition', plan));
};

ConfigMapServer.prototype.resource_initialiser = function (resource) {
    if (resource.data['config.json']) {
        try {
            var address = JSON.parse(resource.data['config.json']);
            var changed = true;
            if (address.status === undefined) {
                address.status = {'phase': 'Active'};
            } else if (address.status.phase === undefined) {
                address.status.phase = 'Active';
            } else {
                changed = false;
            }
            if (changed) {
                resource.data['config.json'] = JSON.stringify(address);
            }
        } catch (e) {
            console.error('Failed to parse address for resource initialisation: %s', e);
        }
    }
};

module.exports.ResourceServer = ResourceServer;
module.exports.ConfigMapServer = ConfigMapServer;
