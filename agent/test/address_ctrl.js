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
var express = require('express');
var body_parser = require('body-parser');

var address_ctrl = require('../lib/address_ctrl').create({});


function MockAddressSource () {
    this.addresses = {
        apiVersion: 'enmasse.io/v1',
        kind: 'AddressList',
        items : []
    };
    this.schema = {
        "apiVersion": "enmasse.io/v1",
        "kind": "Schema",
        "spec": {
            "addressSpaceTypes": [
                {
                    "name": "standard",
                    "addressTypes": [
                        {
                            "name": "queue",
                            "description": "store and forward, competing consumers",
                            "plans": [
                                {
                                    "name": "standard",
                                    "description": "simple"
                                },
                                {
                                    "name": "another",
                                    "description": "perhaps more complex"
                                }
                            ]
                        },
                        {
                            "name": "topic",
                            "description": "store and forward, non-competing consumers",
                            "plans": [
                                {
                                    "name": "standard",
                                    "description": "only one available"
                                }
                            ]
                        }
                    ],
                    "plans": [
                        {
                            "name": "unrestrained",
                            "description": "no address-space level constraints"
                        }
                    ]
                }
            ]
        }
    };
    this.app = express();
    this.app.use(body_parser.urlencoded({ extended: true }));
    this.app.use(body_parser.json());
    this.app.get('/v1/schema', this.get_schema.bind(this));
    this.app.get('/v1/addresses', this.get_addresses.bind(this));
    this.app.post('/v1/addresses', this.post_addresses.bind(this));
    this.app.delete('/v1/addresses/:address', this.delete_address.bind(this));
}

MockAddressSource.prototype.clear = function () {
    this.addresses.items = [];
};

MockAddressSource.prototype.listen = function (port, callback) {
    var self = this;
    var server = this.app.listen(port || 0, function () {
        self.port = server.address().port;
        if (callback) callback();
    });
    this.server = server;
    return server;
};

MockAddressSource.prototype.close = function (callback) {
    this.server.close(callback);
}

MockAddressSource.prototype.get_schema = function (request, response) {
    response.send(JSON.stringify(this.schema));
};

MockAddressSource.prototype.get_addresses = function (request, response) {
    response.send(JSON.stringify(this.addresses));
};

MockAddressSource.prototype.post_addresses = function (request, response) {
    if (request.body.kind === 'AddressList') {
        var self = this;
        request.body.items.forEach(function (a) { self.addresses.items.push(a); });
        response.sendStatus(200);
    } else if (request.body.kind === 'Address') {
        this.addresses.items.push(o);
        response.sendStatus(200);
    } else {
        response.sendStatus(500);
    }
};

MockAddressSource.prototype.delete_address = function (request, response) {
    var address = request.params.address;
    for (var i = 0; i < this.addresses.items.length; i++) {
        if (this.addresses.items[i].spec.address === address) {
            this.addresses.items.splice(i, 1);
            return response.sendStatus(200);
        }
    }
    return response.sendStatus(404);
};

describe('address controller interaction', function() {
    var address_source;

    beforeEach(function(done) {
        address_source = new MockAddressSource();
        address_source.listen(8080, done);
    });

    afterEach(function(done) {
        address_source.close(done);
    });

    it('requests creation of an address', function(done) {
        address_ctrl.create_address({address:'myaddress', type:'queue', plan:'standard'}).then(function() {
            assert.equal(address_source.addresses.items.length, 1);
            assert.equal(address_source.addresses.items[0].spec.address, 'myaddress');
            assert.equal(address_source.addresses.items[0].spec.type, 'queue');
            assert.equal(address_source.addresses.items[0].spec.plan, 'standard');
            done();
        });
    });

    it('requests deletion of addresses', function (done) {
        var addresses = [{address:'myaddress', type:'queue', plan:'standard'}, {address:'foo', type:'bar', plan:'baz'}, {address:'another', type:'topic', plan:'fancy'}];
        var deletions = [addresses[0].address, addresses[2].address];
        Promise.all(addresses.map(function (a) { return address_ctrl.create_address(a); })).then(function() {
            Promise.all(deletions.map(function (a) { return address_ctrl.delete_address({address:a}); })).then(function() {
                assert.equal(address_source.addresses.items.length, 1);
                assert.equal(address_source.addresses.items[0].spec.address, 'foo');
                assert.equal(address_source.addresses.items[0].spec.type, 'bar');
                assert.equal(address_source.addresses.items[0].spec.plan, 'baz');
                done();
            });
        });
    });

    it('retrieves address types for current address-space', function (done) {
        address_ctrl.get_address_types().then(function (address_types) {
            assert.equal(address_types.length, 2);
            assert.equal(address_types[0].name, 'queue');
            assert.equal(address_types[0].plans.length, 2);
            assert.equal(address_types[0].plans[0].name, 'standard');
            assert.equal(address_types[0].plans[1].name, 'another');
            assert.equal(address_types[1].name, 'topic');
            assert.equal(address_types[1].plans.length, 1);
            assert.equal(address_types[1].plans[0].name, 'standard');
            done();
        });
    });
});
