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

var Registry = require('../lib/registry.js');

describe('registry', function() {
    var registry;

    beforeEach(function(done) {
        registry = new Registry();
        done();
    });

    afterEach(function(done) {
        done();
    });

    function check_expected(expected, actual, done) {
        assert.equal(expected.type, actual.type);
        assert.equal(JSON.stringify(expected.value), JSON.stringify(actual.value));
        if (++i >= events.length && done) {
            done();
        }
    }

    function verifier(type, events, done) {
        return function (o) {
            var e = events.shift();
            assert.equal(e.type, type);
            assert.equal(JSON.stringify(e.value), JSON.stringify(o));
            if (events.length === 0) {
                done();
            }
        }
    }

    function expect(events, done) {
        registry.on('updated', verifier('updated', events, done));
        registry.on('deleted', verifier('updated', events, done));
    }

    it('update', function(done) {
        expect([{type:'updated', value:{id:'foo', x:100, y:['a','b','c']}},
                {type:'updated', value:{id:'foo', x:100, y:['d','e','f']}},
                {type:'updated', value:{id:'foo', x:200, y:['d','e','f']}}], done);
        registry.update('foo', {id:'foo', x:100, y:['a','b','c']});//new item
        registry.update('foo', {id:'foo', x:100, y:['a','b','c']});//no change
        registry.update('foo', {id:'foo', x:100, y:['d','e','f']});
        registry.update('foo', {id:'foo', x:200, y:['d','e','f']});
    });

    it('update_if_exists', function(done) {
        expect([{type:'updated', value:{id:'foo', x:100, y:['d','e','f']}},
                {type:'updated', value:{id:'foo', x:200, y:['d','e','f']}}], done);
        registry.update_if_exists('foo', {id:'foo', x:100, y:['a','b','c']});//new item
        registry.update('foo', {id:'foo', x:100, y:['d','e','f']});
        registry.update_if_exists('foo', {id:'foo', x:200, y:['d','e','f']});
    });

    it('for_each', function(done) {
        registry.set({
            'a': {foo:'bar',baz:10},
            'b': {colour:'red',age:99},
            'c': {name:'bob',type:'kangaroo'}
        });
        var collected = {};
        registry.for_each(function (o) {
            for (var f in o) {
                collected[f] = o[f];
            }
        });
        var expected = {
            foo:'bar',baz:10,
            colour:'red',age:99,
            name:'bob',type:'kangaroo'
        };
        assert.equal(JSON.stringify(expected), JSON.stringify(collected));
        done();
    });
});
