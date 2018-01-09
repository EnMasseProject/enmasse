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
var ConfigMapServer = require('../testlib/mock_resource_server.js').ConfigMapServer;

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
            setTimeout(function () {
                configmaps.add_address_definition({address:'foo', type:'queue'});
                configmaps.add_address_definition({address:'bar', type:'topic'});
                var addresses;
                source.on('addresses_defined', function (latest) {
                    addresses = latest;
                });
                setTimeout(function () {
                    assert.equal(addresses.length, 2);
                    assert.equal(addresses[0].address, 'foo');
                    assert.equal(addresses[0].type, 'queue');
                    assert.equal(addresses[1].address, 'bar');
                    assert.equal(addresses[1].type, 'topic');
                    source.watcher.close();
                    done();
                }, 100);
            }, 100);
        });
    });
    it('updates readiness', function(done) {
        configmaps.add_address_definition({address:'foo', type:'queue'});
        configmaps.add_address_definition({address:'bar', type:'topic'});
        var source = new AddressSource({port:configmaps.port, host:'localhost', token:'foo', namespace:'default'});
        source.watcher.close();
        source.on('addresses_defined', function (addresses) {
            source.check_status({foo:{propagated:100}}).then(function () {
                var address = JSON.parse(configmaps.find_resource('foo').data['config.json']);
                assert.equal(address.status.isReady, true);
                done();
            });
        });
    });
});
