/*
 * Copyright 2020 Red Hat Inc.
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
var EventEmitter = require('events');

var AddressPlanSource = require('../lib/internal_addressplan_source');
var AddressServer = require('../testlib/mock_resource_server.js').AddressServer;

describe('addressplan source', function() {
    var address_server;
    var address_space_plan_source;

    beforeEach(function(done) {
        address_space_plan_source = new EventEmitter();
        address_server = new AddressServer();
        address_server.listen(0, done);
    });

    afterEach(function(done) {
        address_server.close(done);
    });

    it('retrieves address plans', function(done) {
        address_server.add_address_plan({plan_name:'small', address_type:'queue'});
        address_server.add_address_plan({plan_name:'medium', address_type:'queue'});
        address_server.add_address_plan({plan_name:'large', address_type:'queue'});
        address_server.add_address_plan({plan_name:'belongs_to_another_space_plan', address_type:'queue'});

        var source = new AddressPlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default'});
        source.start(address_space_plan_source);
        address_space_plan_source.emit("addressspaceplan_defined", {
            kind: 'AddressPlan',
            metadata: {name: 'spaceplan'},
            spec: {
                addressPlans: ['small', 'medium', 'large'],
            }
        });

        source.on('addressplans_defined', function (addressplans) {
            source.watcher.close();
            assert.equal(addressplans.length, 3);
            done();
        });
    });

    it('watches for changes - additional plan', function(done) {

        address_server.add_address_plan({plan_name:'small', address_type:'queue'});
        address_server.add_address_plan({plan_name:'large', address_type:'queue'});

        var source = new AddressPlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start(address_space_plan_source);
        address_space_plan_source.emit("addressspaceplan_defined", {
            kind: 'AddressPlan',
            metadata: {name: 'spaceplan'},
            spec: {
                addressPlans: ['small', 'large'],
            }
        });
        source.once('addressplans_defined', (addressplans) => {
            assert.equal(addressplans.length, 2);
            process.nextTick(() => {
                address_server.add_address_plan({plan_name:'medium', address_type:'queue'});
                address_space_plan_source.emit("addressspaceplan_defined", {
                    kind: 'AddressPlan',
                    metadata: {name: 'spaceplan'},
                    spec: {
                        addressPlans: ['small', 'medium', 'large'],
                    }
                });
            });

            source.on('addressplans_defined', (update) => {
                source.watcher.close();
                assert.equal(update.length, 3);
                done();
            });
        });
    });
    it('watches for changes - redefined plan - resources', function(done) {

        address_server.add_address_plan({plan_name:'small', address_type:'queue', resources: {broker: 0.4}});

        var source = new AddressPlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start(address_space_plan_source);
        address_space_plan_source.emit("addressspaceplan_defined", {
            kind: 'AddressPlan',
            metadata: {name: 'spaceplan'},
            spec: {
                addressPlans: ['small'],
            }
        });
        source.once('addressplans_defined', (addressplans) => {
            assert.equal(addressplans.length, 1);
            process.nextTick(() => {
                address_server.update_address_plan({plan_name:'small', address_type:'queue', resources: {broker: 0.5}});
            });

            source.on('addressplans_defined', (update) => {
                source.watcher.close();
                assert.equal(update.length, 1);
                done();
            });
        });
    });
    it('watches for changes - redefined plan - ttl', function(done) {

        address_server.add_address_plan({plan_name:'small', address_type:'queue', messageTtl: {minimum: 1000, maximum: 2000}});

        var source = new AddressPlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start(address_space_plan_source);
        address_space_plan_source.emit("addressspaceplan_defined", {
            kind: 'AddressPlan',
            metadata: {name: 'spaceplan'},
            spec: {
                addressPlans: ['small'],
            }
        });
        source.once('addressplans_defined', (addressplans) => {
            assert.equal(addressplans.length, 1);
            process.nextTick(() => {
                    address_server.update_address_plan({plan_name:'small', address_type:'queue', messageTtl: {minimum: 1001, maximum: 1999}});
            });

            source.on('addressplans_defined', (update) => {
                source.watcher.close();
                assert.equal(update.length, 1);
                done();
            });
        });
    });
});
