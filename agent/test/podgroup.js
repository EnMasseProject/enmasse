/*
 * Copyright 2018 Red Hat Inc.
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
var podgroup = require('../lib/podgroup.js');

function dummy (details) { return details; }

describe('podgroup update', function() {
    it('adds new pods', function(done) {
        var group = podgroup(dummy);
        assert(group.update([{name:'foo', value:'bar'}]));
        assert(group.pods.foo);
        assert.equal(group.pods.foo.name, 'foo');
        assert.equal(group.pods.foo.value, 'bar');
        done();
    });
    it('ignores existing pods', function(done) {
        var group = podgroup(dummy);
        assert(group.update([{name:'foo', value:'bar'}]));
        assert(group.pods.foo);
        assert.equal(group.pods.foo.name, 'foo');
        assert.equal(group.pods.foo.value, 'bar');
        assert(!group.update([{name:'foo', value:'baz'}]));
        assert.equal(group.pods.foo.value, 'bar');
        done();
    });
    it('removes stale pods', function(done) {
        var group = podgroup(dummy);
        assert(group.update([{name:'foo', value:'x'},{name:'bar',colour:'blue'}]));
        assert.equal(group.pods.foo.name, 'foo');
        assert.equal(group.pods.foo.value, 'x');
        assert.equal(group.pods.bar.name, 'bar');
        assert.equal(group.pods.bar.colour, 'blue');
        assert(group.update([{name:'bar',colour:'blue'}]));
        assert.equal(group.pods.foo, undefined);
        assert.equal(group.pods.bar.name, 'bar');
        assert.equal(group.pods.bar.colour, 'blue');
        done();
    });
});
