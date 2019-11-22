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
    var api = path.shift();
    if (api === 'api' && path.shift() === 'v1') {
        result.watch = shift_if_matches(path, 'watch');
        if (shift_if_matches(path, 'namespaces')) {
            result.namespace = path.shift();
            result.type = path.shift();
            result.name = path.shift();
            return result;
        }
    } else if (api === 'apis') {
        result.api_group = path.shift();
        result.api_version = path.shift();
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

function ResourceServer (read_only, externalize, internalize) {
    events.EventEmitter.call(this);
    this.read_only = read_only;
    this.externalize = externalize || function (o) { return o; };
    this.internalize = internalize;
    this.resources = {};
    this.watch_timeout = 250;
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

ResourceServer.prototype.update = function (type, index, object) {
    this.resources[type][index] = this.internalize ? this.codec.internalize(object) : object;
};

ResourceServer.prototype.push = function (type, object) {
    this.resources[type].push(this.internalize ? this.codec.internalize(object) : object);
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
        } else if (self.failure_injector && self.failure_injector.match(path)) {
            error(request, response, self.failure_injector.code(path));
        } else {
            if (path.watch && request.method === 'GET') {
                self.watch_resources(request, response, path.type);
            } else if (request.method === 'GET') {
                if (path.name) {
                    self.get_resource(request, response, path.type, path.name);
                } else {
                    self.get_resources(request, response, path.type);
                }
            } else if (request.method === 'PUT' && path.name) {
                self.put_resource(request, response, path.type, path.name);
            } else if (request.method === 'DELETE' && path.name) {
                self.delete_resource(request, response, path.type, path.name);
            } else if (request.method === 'POST') {
                self.post_resource(request, response, path.type);
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
    this.resources = {};
};

ResourceServer.prototype.add_resource = function (type, resource) {
    if (this.resource_initialiser) {
        this.resource_initialiser(resource);
    }
    if (this.resources[type] === undefined) {
        this.resources[type] = []
    }
    this.resources[type].push(resource);
    this.emit('added', this.externalize(resource));
};

ResourceServer.prototype.resource_modified = function (type, resource) {
    this.emit('modified', this.externalize(resource));
};

ResourceServer.prototype.remove_resource = function (type, resource) {
    var i = this.resources[type].indexOf(resource);
    if (i >= 0) {
        this.resources[type].splice(i, 1);
        this.emit('deleted', this.externalize(resource));
    }
};

ResourceServer.prototype.remove_resource_by_name = function (type, name) {
    for (var i = 0; i < this.get_resources_type(type).length; i++) {
        if (this.resources[type][i].metadata.name === name) {
            var removed = this.resources[type][i];
            this.resources[type].splice(i, 1);
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
        var labelentries = selector.split(',');
        var labels = {};
        for (var i = 0; i < labelentries.length; i++) {
            var labelentry = labelentries[i];
            var parts = labelentry.split('=');
            labels[parts[0]] = parts[1];
        }
        return function (object) {
            var equal = false;
            if (object.metadata.labels) {
                for (var label in labels) {
                    if (object.metadata.labels[label] == undefined || object.metadata.labels[label] !== labels[label]) {
                        return false;
                    }
                }
                return true;
            }
            return false;
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

ResourceServer.prototype.get_resources_type = function (type) {
    return this.resources[type] || [];
};

ResourceServer.prototype.get_resources = function (request, response, type) {
    var filter = getLabelSelectorFn(request);
    response.end(JSON.stringify({kind: 'List', items: this.get_resources_type(type).map(this.externalize).filter(filter)}));
};

ResourceServer.prototype.watch_resources = function (request, response, type) {
    var filter = getLabelSelectorFn(request);
    for (var i = 0; i < this.get_resources_type(type).length; i++) {
        var externalized = this.resources[type].map(this.externalize)[i];
        if (filter(externalized)) {
            response.write(JSON.stringify({type:'ADDED', object:externalized }) + '\n');
        }
    }
    var watcher = new Watcher(this, response, filter);
    setTimeout(function () { watcher.close(); }, this.watch_timeout);
};

ResourceServer.prototype.find_resource = function (type, name) {
    for (var i = 0; i < this.get_resources_type(type).length; i++) {
        var externalized = this.resources[type].map(this.externalize)[i];
        if (externalized.metadata.name === name) {
            return externalized;
        }
    }
};

ResourceServer.prototype.get_resource = function (request, response, type, name) {
    var result = this.find_resource(type, name);
    if (result) {
        response.end(JSON.stringify(result));
    } else {
        error(request, response, 404);
    }
};

ResourceServer.prototype.update_resource = function (type, name, updated) {
    for (var i = 0; i < this.get_resources_type(type).length; i++) {
        var externalized = this.resources[type].map(this.externalize)[i];
        if (externalized.metadata.name === name) {
            this.update(type, i, updated);
            return true;
        }
    }
    return false;
};

ResourceServer.prototype.add_resource_if_not_exists = function (type, resource) {
    for (var i = 0; i < this.get_resources_type(type).length; i++) {
        var externalized = this.resources[type].map(this.externalize)[i];
        if (externalized.metadata.name === resource.metadata.name) {
            return false;
        }
    }
    this.add_resource(type, resource);
    return true;
};

ResourceServer.prototype.delete_resource = function (request, response, type, name) {
    if (this.remove_resource_by_name(type, name)) {
        response.statusCode = 200;
        response.end();
    } else {
        error(request, response, 404);
    }
};

ResourceServer.prototype.put_resource = function (request, response, type, name) {
    var self = this;
    var bodytext = '';
    request.on('data', function (data) { bodytext += data; });
    request.on('end', function () {
        var body = JSON.parse(bodytext);
        if (self.update_resource(type, name, body)) {
            response.statusCode = 200;
            response.end();
        } else {
            error(request, response, 404);
        }
    });
};

ResourceServer.prototype.post_resource = function (request, response, type) {
    var self = this;
    var bodytext = '';
    request.on('data', function (data) { bodytext += data; });
    request.on('end', function () {
        var body = JSON.parse(bodytext);
        if (self.add_resource_if_not_exists(type, body)) {
            response.statusCode = 200;
            response.end();
        } else {
            error(request, response, 409);
        }
    });
};

function AddressServer() {
    ResourceServer.call(this);
}

util.inherits(AddressServer, ResourceServer);

AddressServer.prototype.add_address_definition = function (def, name, infra_uuid, annotations, status) {
    var address = {kind: 'Address', metadata: {name: name || def.address}, spec:def};
    if (annotations) {
        address.metadata.annotations = annotations;
    }
    address.metadata.labels = {};
    if (infra_uuid) {
        address.metadata.labels.infraUuid = infra_uuid;
    }
    address.status = status || { phase: 'Active' };
    this.add_resource('addresses', address);
};

AddressServer.prototype.add_address_definitions = function (defs) {
    for (var i in defs) {
        this.add_address_definition(defs[i]);
    }
};

AddressServer.prototype.add_address_space_plan = function (params) {
    var plan = {
        kind: 'AddressSpacePlan',
        metadata: {name: params.plan_name},
        displayName: params.display_name,
        shortDescription: params.shortDescription,
        longDescription: params.longDescription,
        displayOrder: params.displayOrder,
        addressSpaceType: params.address_space_type,
        addressPlans: params.address_plans
    };
    if (params.required_resources) {
        plan.requiredResources = params.required_resources;
    }
    this.add_resource('addressspaceplans', plan);
}

AddressServer.prototype.add_address_plan = function (params) {
    var plan = {
        kind: 'AddressPlan',
        metadata: {name: params.plan_name},
        spec: {
            displayName: params.display_name,
            shortDescription: params.shortDescription,
            longDescription: params.longDescription,
            displayOrder: params.displayOrder,
            addressType: params.address_type,
        }
    };
    if (params.resources) {
        plan.resources = params.resources;
    }
    this.add_resource('addressplans', plan);
};

AddressServer.prototype.resource_initialiser = function (resource) {
    if (resource.status === undefined) {
        resource.status = {'phase': 'Active'};
    }
};

module.exports.ResourceServer = ResourceServer;
module.exports.AddressServer = AddressServer;
