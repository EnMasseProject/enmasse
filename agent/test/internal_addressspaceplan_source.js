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

var AddressSpacePlanSource = require('../lib/internal_addressspaceplan_source');
var AddressServer = require('../testlib/mock_resource_server.js').AddressServer;

describe('addressspaceplan source', function() {
    var address_server;

    beforeEach(function(done) {
        address_server = new AddressServer();
        address_server.listen(0, done);
    });

    afterEach(function(done) {
        address_server.close(done);
    });

    it('retrieves addressspace plan', function(done) {
        address_server.add_address_space_plan({plan_name:'space', address_plans:['small', 'medium', 'large']});
        address_server.add_address_space_plan({plan_name:'anotherplan', address_plans:['small', 'medium', 'large']});

        var source = new AddressSpacePlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space'});
        source.start();
        source.watcher.close();
        source.on('addressspaceplan_defined', (addressspaceplan) => {
            assert.equal(addressspaceplan.metadata.name, "space");
            assert.deepEqual(addressspaceplan.spec.addressPlans, ['small', 'medium', 'large']);

            done();
        });
    });

    it('watches for changes', function(done) {
        address_server.add_address_space_plan({plan_name:'space', address_plans:['small', 'medium', 'large']});
        var source = new AddressSpacePlanSource({port:address_server.port, host:'localhost', token:'foo', namespace:'default', ADDRESS_SPACE_PLAN: 'space', ADDRESS_SPACE_PREFIX: 's1.'});
        source.start();
        source.once('addressspaceplan_defined', (addressspaceplan) => {
            assert.equal(addressspaceplan.metadata.name, "space");
            process.nextTick(() => {
                address_server.update_address_space_plan({plan_name:'space', address_plans:['small', 'medium', 'large', 'xlarge']});
            });

            source.on('addressspaceplan_defined', (update) => {
                source.watcher.close();
                assert.equal(update.metadata.name, "space");
                assert.deepEqual(update.spec.addressPlans, ['small', 'medium', 'large', 'xlarge']);
                done();
            });
        });
    });
});
