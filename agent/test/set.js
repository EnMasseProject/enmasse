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
var util = require('util');
var set = require('../lib/set.js');
var myutils = require('../lib/utils.js');

function numeric_compare(a, b) {
    return a - b;
}

function name_compare(a, b) {
    return myutils.string_compare(a.name, b.name);
}

function numeric_name_compare(a, b) {
    return numeric_compare(a.name.split('-')[1], b.name.split('-')[1]);
}

function random_number(max) {
  return Math.floor(Math.random() * Math.floor(max));
}

describe('sorted object set', function() {
    it('inserts simple strings', function(done) {
        var items = set.sorted_object_set(myutils.string_compare);
        assert(items.insert('x'));
        assert(items.insert('y'));
        assert(items.insert('z'));
        assert(items.insert('c'));
        assert(items.insert('a'));
        assert(items.insert('b'));
        assert(!items.insert('x'));
        assert(!items.insert('c'));
        assert.deepEqual(items.to_array(), ['a', 'b', 'c', 'x', 'y', 'z']);
        done();
    });
    it('removes simple strings', function(done) {
        var items = set.sorted_object_set(myutils.string_compare);
        assert(items.insert('x'));
        assert(items.insert('y'));
        assert(items.insert('z'));
        assert(items.insert('c'));
        assert(items.insert('a'));
        assert(items.insert('b'));
        assert(items.remove('x'));
        assert.deepEqual(items.to_array(), ['a', 'b', 'c', 'y', 'z']);
        assert(items.remove('c'));
        assert(!items.remove('foo'));
        assert.deepEqual(items.to_array(), ['a', 'b', 'y', 'z']);
        done();
    });
    it('inserts named objects', function(done) {
        var items = set.sorted_object_set(name_compare);
        assert(items.insert({name:'foo',value:'a'}));
        assert(items.insert({name:'bar',value:'a'}));
        assert(items.insert({name:'you',value:'d'}));
        assert(items.insert({name:'me',value:'a'}));
        assert(!items.insert({name:'foo',value:'b'}));
        assert(!items.insert({name:'you',value:'x'}));
        assert.deepEqual(items.to_array(), [{name:'bar',value:'a'}, {name:'foo',value:'a'}, {name:'me',value:'a'}, {name:'you',value:'d'}]);
        done();
    });
    it('removes named objects', function(done) {
        var items = set.sorted_object_set(name_compare);
        assert(items.insert({name:'foo',value:'a'}));
        assert(items.insert({name:'bar',value:'a'}));
        assert(items.insert({name:'pink',value:'elephant'}));
        assert(items.insert({name:'you',value:'d'}));
        assert(items.insert({name:'me',value:'a'}));
        assert(items.remove({name:'pink',value:'pig'}));
        assert(!items.remove({name:'pink',value:'elephant'}));
        assert(!items.remove({name:'blue',value:'whale'}));
        assert.deepEqual(items.to_array(), [{name:'bar',value:'a'}, {name:'foo',value:'a'}, {name:'me',value:'a'}, {name:'you',value:'d'}]);
        done();
    });
    it('replaces named objects', function(done) {
        var items = set.sorted_object_set(name_compare);
        assert(items.insert({name:'foo',value:'a'}));
        assert(items.insert({name:'bar',value:'a'}));
        assert(items.insert({name:'pink',value:'elephant'}));
        assert(items.insert({name:'you',value:'d'}));
        assert(items.insert({name:'me',value:'a'}));
        assert(items.replace({name:'pink',value:'pig'}));
        assert(!items.replace({name:'blue',value:'whale'}));
        assert.deepEqual(items.to_array(), [{name:'bar',value:'a'}, {name:'foo',value:'a'}, {name:'me',value:'a'}, {name:'pink',value:'pig'}, {name:'you',value:'d'}]);
        done();
    });
    it('handles lots of objects', function(done) {
        var items = set.sorted_object_set(numeric_name_compare);
        var size = 10000;
        for (var i = 0; i < size; i++) {
            assert(items.insert({name:'item-'+i, colour:'red'}));
        }
        for (var i = 0; i < size; i++) {
            assert(!items.insert({name:'item-'+random_number(size), colour:'purple'}));
        }
        var replaced = set.sorted_object_set(numeric_compare);
        for (var i = 0; i < size; i++) {
            var x = random_number(size);
            replaced.insert(x);
            var colour = x % 2 === 0 ? 'green' : 'blue';
            assert(items.replace({name:'item-'+x, colour:colour}));
        }
        var objects = items.to_array();
        var last;
        for (var i = 0; i < objects.length; i++) {
            assert.equal(objects[i].name, 'item-'+i);
            if (last) assert(numeric_name_compare(last, objects[i]) < 0);
            last = objects[i];
            if (replaced.find(i) < 0) {
                assert.equal(objects[i].colour, 'red', util.format('expected item-%d to be unaltered', i));
            } else {
                var colour = i % 2 === 0 ? 'green' : 'blue';
                assert.equal(objects[i].colour, colour);
            }
        }
        done();
    });
});
