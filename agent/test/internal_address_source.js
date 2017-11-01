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

var assert = require('assert');
var fs = require('fs');
var https = require('https');
var path = require('path');
var url = require('url');

var AddressSource = require('../lib/internal_address_source');


function ConfigMapServer () {
    this.configmaps = [];
}

function relativepath(name) {
    return path.resolve(__dirname, name);
}

ConfigMapServer.prototype.listen = function (port, callback) {
    var self = this;
    var options = {
        key: fs.readFileSync(relativepath('server-key.pem')),
        cert: fs.readFileSync(relativepath('server-cert.pem'))
    };
    this.server = https.createServer(options, function (request, response) {
        //console.log('%s %s', request.method, request.url);
        var path = url.parse(request.url).pathname;
        if (request.method === 'GET' && path.indexOf('/api/v1/namespaces/default/configmaps/') === 0) {
            self.get_configmap(request, response);
        } else if (request.method === 'GET' && path.indexOf('/api/v1/namespaces/default/configmaps') === 0) {
            self.get_configmaps(request, response);
        } else if (request.method === 'GET' && path.indexOf('/api/v1/watch/namespaces/default/configmaps') === 0) {
            self.watch_configmaps(request, response);
        } else if (request.method === 'PUT' && path.indexOf('/api/v1/namespaces/default/configmaps/') === 0) {
            self.put_configmap(request, response);
        } else {
            response.statusCode = 404;
            response.end('No such resource %s', request.url);
        }
    });
    this.server.listen(port || 0, function () {
        self.port = self.server.address().port;
        if (callback) callback();
    });
    return this.server;
};

ConfigMapServer.prototype.close = function (callback) {
    this.clear();
    this.server.close(callback);
};

ConfigMapServer.prototype.clear = function () {
    this.configmaps = [];
};

ConfigMapServer.prototype.add_address_definition = function (def, name) {
    var address = {kind: 'Address', metadata: {name: def.address}, spec:def, status:{}};
    this.configmaps.push({kind:'ConfigMap', metadata: {name: name || def.address}, data:{'config.json': JSON.stringify(address)}});
};

ConfigMapServer.prototype.get_configmaps = function (request, response) {
    response.end(JSON.stringify({kind: 'ConfigMapList', items: this.configmaps}));
};

ConfigMapServer.prototype.watch_configmaps = function (request, response) {
    for (var i = 0; i < this.configmaps.length; i++) {
        response.write(JSON.stringify({type:'ADDED', object:this.configmaps[i]}) + '\n');
    }
    response.end();
};

ConfigMapServer.prototype.find_configmap = function (name) {
    for (var i = 0; i < this.configmaps.length; i++) {
        if (this.configmaps[i].metadata.name === name) {
            return this.configmaps[i];
        }
    }
};

ConfigMapServer.prototype.update_configmap = function (name, updated) {
    for (var i = 0; i < this.configmaps.length; i++) {
        if (this.configmaps[i].metadata.name === name) {
            this.configmaps[i] = updated;
            return true;
        }
    }
    return false;
};

ConfigMapServer.prototype.get_configmap = function (request, response) {
    var path = url.parse(request.url).pathname;
    var name = path.split('/').pop();
    var result = this.find_configmap(name);
    if (result) {
        response.end(JSON.stringify(result));
    } else {
        response.statusCode = 404;
        response.end('No such resource %s', request.url);
    }
};

ConfigMapServer.prototype.put_configmap = function (request, response) {
    var path = url.parse(request.url).pathname;
    var name = path.split('/').pop();
    var self = this;
    var bodytext = '';
    request.on('data', function (data) { bodytext += data; });
    request.on('end', function () {
        var body = JSON.parse(bodytext);
        if (body.kind === 'ConfigMap') {
            if (self.update_configmap(name, body)) {
                response.statusCode = 200;
                response.end();
            } else {
                response.statusCode = 404;
                response.end();
            }
        } else {
            response.statusCode = 500;
            response.end('Invalid resource kind %s', body.kind);
        }
    });
};

describe('configmap backed address source', function() {
    var configmaps;

    beforeEach(function(done) {
        configmaps = new ConfigMapServer();
        configmaps.listen(0, done);
    });

    afterEach(function(done) {
        configmaps.close(done);
    });

    it('retrieves all addresses', function(done) {
        configmaps.add_address_definition({address:'foo', type:'queue'});
        configmaps.add_address_definition({address:'bar', type:'topic'});
        var source = new AddressSource({port:configmaps.port, host:'localhost', token:'foo', namespace:'default'});
        source.watcher.close();//prevents watching
        source.on('addresses_defined', function (addresses) {
            assert.equal(addresses.length, 2);
            assert.equal(addresses[0].address, 'foo');
            assert.equal(addresses[0].type, 'queue');
            assert.equal(addresses[1].address, 'bar');
            assert.equal(addresses[1].type, 'topic');
            done();
        });
    });
    it('watches for changes', function(done) {
        var source = new AddressSource({port:configmaps.port, host:'localhost', token:'foo', namespace:'default'});
        source.once('addresses_defined', function () {
            configmaps.add_address_definition({address:'foo', type:'queue'});
            configmaps.add_address_definition({address:'bar', type:'topic'});
            source.watcher.close();
            source.once('addresses_defined', function (addresses) {
                assert.equal(addresses.length, 2);
                assert.equal(addresses[0].address, 'foo');
                assert.equal(addresses[0].type, 'queue');
                assert.equal(addresses[1].address, 'bar');
                assert.equal(addresses[1].type, 'topic');
                done();
            });
        });
    });
    it('updates readiness', function(done) {
        configmaps.add_address_definition({address:'foo', type:'queue'});
        configmaps.add_address_definition({address:'bar', type:'topic'});
        var source = new AddressSource({port:configmaps.port, host:'localhost', token:'foo', namespace:'default'});
        source.watcher.close();
        source.on('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                var address = JSON.parse(configmaps.find_configmap('foo').data['config.json']);
                assert.equal(address.status.isReady, true);
                done();
            });
        });
    });
});
