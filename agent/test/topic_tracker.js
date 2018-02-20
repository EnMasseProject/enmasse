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
var topic_tracker = require('../lib/topic_tracker.js');
var podgroup = require('../lib/podgroup.js');

function dummy (details) { return details; }

function MockTopic (name) {
    this.name = name;
    this.pods = podgroup(dummy);
}

MockTopic.prototype.empty = function () {
    return this.pods.empty();
};

MockTopic.prototype.close = function () {
    return this.pods.close();
};

function create_topic (name) {
    return new MockTopic(name);
}

describe('topic tracker', function() {
    it('adds new pods', function(done) {
        var topics = {};
        var tracker = topic_tracker(topics, create_topic);
        tracker([{name:'foo', value:'bar', annotations:{address:'a'}, ready:'True', phase:'Running'}]);
        assert(topics.a);
        assert.equal(topics.a.pods.pods.foo.name, 'foo');
        assert.equal(topics.a.pods.pods.foo.value, 'bar');
        done();
    });
    it('removes deleted pods from the same topic', function(done) {
        var topics = {};
        var tracker = topic_tracker(topics, create_topic);
        tracker([{name:'foo', value:'bar', annotations:{address:'a'}, ready:'True', phase:'Running'},
                {name:'baz', value:'boo', annotations:{address:'a'}, ready:'True', phase:'Running'}]);
        assert(topics.a);
        assert.equal(topics.a.pods.pods.foo.name, 'foo');
        assert.equal(topics.a.pods.pods.foo.value, 'bar');
        assert.equal(topics.a.pods.pods.baz.name, 'baz');
        assert.equal(topics.a.pods.pods.baz.value, 'boo');
        tracker([{name:'baz', value:'boo', annotations:{address:'a'}, ready:'True', phase:'Running'}]);
        assert(topics.a);
        assert.equal(topics.a.pods.pods.foo, undefined);
        assert.equal(topics.a.pods.pods.baz.name, 'baz');
        assert.equal(topics.a.pods.pods.baz.value, 'boo');
        done();
    });
    it('removes deleted pods from different topics', function(done) {
        var topics = {};
        var tracker = topic_tracker(topics, create_topic);
        tracker([{name:'foo', value:'bar', annotations:{address:'a'}, ready:'True', phase:'Running'},
                {name:'baz', value:'boo', annotations:{address:'b'}, ready:'True', phase:'Running'}]);
        assert(topics.a);
        assert.equal(topics.a.pods.pods.foo.name, 'foo');
        assert.equal(topics.a.pods.pods.foo.value, 'bar');
        assert(topics.b);
        assert.equal(topics.b.pods.pods.baz.name, 'baz');
        assert.equal(topics.b.pods.pods.baz.value, 'boo');
        tracker([{name:'baz', value:'boo', annotations:{address:'b'}, ready:'True', phase:'Running'}]);
        assert(topics.a === undefined);
        assert(topics.b);
        assert.equal(topics.b.pods.pods.baz.name, 'baz');
        assert.equal(topics.b.pods.pods.baz.value, 'boo');
        done();
    });
});
