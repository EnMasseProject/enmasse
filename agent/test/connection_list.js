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

var ConnectionList = require('../lib/connection_list.js');


describe('connection registry', function() {
    var registry;

    beforeEach(function(done) {
        registry = new ConnectionList();
        done();
    });

    afterEach(function(done) {
        done();
    });

    function expect(events, done) {
        registry.on('updated', verifier('updated', events, done));
        registry.on('deleted', verifier('updated', events, done));
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

    it('update ignores close function', function(done) {
        expect([{type:'updated', value:{id:'foo', x:100, y:['a','b','c']}},
            {type:'updated', value:{id:'foo', x:100, y:['a','b','d']}}], done);
        registry.update('foo', {id:'foo', x:100, y:['a','b','c'], close: () => 1});//new item
        registry.update('foo', {id:'foo', x:100, y:['a','b','c'], close: () => null});//no change
        registry.update('foo', {id:'foo', x:100, y:['a','b','d'], close: () => null});//change
    });

    it('ignore drifting creationTimestamp', function(done) {
        expect([{type:'updated', value:{id:'foo', x:100, creationTimestamp:100}},
            {type:'updated', value:{id:'foo', x:101, creationTimestamp:100}}], done);
        registry.update('foo', {id:'foo', x:100, creationTimestamp:100});//new item
        registry.update('foo', {id:'foo', x:101, creationTimestamp:101});//no change - ignores creationTimestamp drifting
    });

    it('ignore drifting creationTimestamp2', function(done) {
        expect([{type:'updated', value:{id:'foo', x:101}},
            {type:'updated', value:{id:'foo', x:101, creationTimestamp:101}}], done);
        registry.update('foo', {id:'foo', x:101}); //new item without creationTimestamp
        registry.update('foo', {id:'foo', x:101, creationTimestamp:101});// change
        registry.update('foo', {id:'foo', x:101, creationTimestamp:102});// no change
    });

    it('update', function(done) {
        expect([{type:'updated', value:{id:'foo', x:100, y:['a','b','c']}},
            {type:'updated', value:{id:'foo', x:100, y:['d','e','f']}},
            {type:'updated', value:{id:'foo', x:200, y:['d','e','f']}}], done);
        registry.update('foo', {id:'foo', x:100, y:['a','b','c']});//new item
        registry.update('foo', {id:'foo', x:100, y:['a','b','c']});//no change
        registry.update('foo', {id:'foo', x:100, y:['d','e','f']});
        registry.update('foo', {id:'foo', x:200, y:['d','e','f']});
    });

});
