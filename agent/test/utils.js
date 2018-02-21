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
var myutils = require('../lib/utils.js');

describe('replace', function() {
    it('updates matching string', function(done) {
        var list = ['a', 'b', 'c'];
        assert.equal(myutils.replace(list, 'x', function (o) { return o === 'b'}), true);
        assert.deepEqual(list, ['a', 'x', 'c']);
        done();
    });
    it('returns false when there is no match', function(done) {
        var list = ['a', 'b', 'c'];
        assert.equal(myutils.replace(list, 'x', function (o) { return o === 'foo'}), false);
        assert.deepEqual(list, ['a', 'b', 'c']);
        done();
    });
});

describe('merge', function() {
    it('combines fields for multiple objects', function(done) {
        var obj = myutils.merge({'foo':10}, {'bar':9});
        assert.deepEqual(obj, {'foo':10, 'bar':9});
        done();
    });
});

describe('kubernetes_name', function () {
    it ('removes invalid chars', function (done) {
        var invalid = '!"£$%^&*()_?><~#@\':;`/\|ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        var input = 'a!b"c£d$e%f^g&h*i()j_?><k~#l@\'m:n;opq`/r\s|t';
        var output = myutils.kubernetes_name(input);
        assert.notEqual(output, input);
        for (var i = 0; i < invalid.length; i++) {
            assert(output.indexOf(invalid.charAt(i)) < 0, 'invalid char ' + invalid.charAt(i) + ' found in ' + output);
        }
        done();
    });
    it ('removes uppercase chars', function (done) {
        var invalid = '!"£$%^&*()_?><~#@\':;`/\|ABCDEFGHIJKLMNOPQRSTUVWXYZ';
        var input = 'myAddress';
        var output = myutils.kubernetes_name(input);
        assert.notEqual(output, input);
        for (var i = 0; i < invalid.length; i++) {
            assert(output.indexOf(invalid.charAt(i)) < 0, 'invalid char ' + invalid.charAt(i) + ' found in ' + output);
        }
        done();
    });
    it ('removes leading -', function (done) {
        var input = '-my-address';
        var output = myutils.kubernetes_name(input);
        assert.notEqual(output, input);
        assert.notEqual(output.charAt(0), '-');
        done();
    });
    it ('removes trailing -', function (done) {
        var input = 'my-address-';
        var output = myutils.kubernetes_name(input);
        assert.notEqual(output, input);
        assert.equal(input.charAt(input.length-1), '-');
        assert.notEqual(output.charAt(output.length-1), '-');
        done();
    });
    it ('differentiates modified names', function (done) {
        var a = myutils.kubernetes_name('foo@bar');
        var b = myutils.kubernetes_name('foo~bar');
        var c = myutils.kubernetes_name('foo/bar/baz');
        assert.notEqual(a, b);
        assert.notEqual(a, c);
        assert.notEqual(b, c);
        done();
    });
    it ('truncates names without special chars over 64 chars of length', function (done) {
        var too_long = 'abcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-xxxxxxx';
        assert(too_long.length > 63);
        var name = myutils.kubernetes_name(too_long);
        assert(name.length <= 63);
        done();
    });
    it ('ensures names for similar input over 63 chars are unique', function (done) {
        var long_a = 'abcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-xxxxxxa';
        var long_b = 'abcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-xxxxxxa';
        assert(long_a.length > 63);
        assert(long_b.length > 63);
        var name_a = myutils.kubernetes_name(long_a);
        var name_b = myutils.kubernetes_name(long_b);
        assert(name_a.length <= 63);
        assert(name_b.length <= 63);
        assert.notEqual(name_a, name_b);
        done();
    });
    it ('truncates names with special chars over 64 chars of length', function (done) {
        var too_long = 'a!"£$%^&*()_+=-bcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-';
        assert(too_long.length > 63);
        var name = myutils.kubernetes_name(too_long);
        assert(name.length <= 63);
        var also_too_long = 'a!"£$%^&*()_+==bcdefghiklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-';
        var another_name = myutils.kubernetes_name(also_too_long);
        assert(another_name.length <= 63);
        assert.notEqual(name, another_name);
        done();
    });
});

describe('serialize', function () {
    var in_use = false;
    var calls = 0;
    function one_at_a_time() {
        assert.equal(in_use, false, 'function already in use');
        in_use = true;
        calls++;
        return new Promise(function (resolve, reject) {
            setTimeout(function () {
                in_use = false;
                resolve();
            }, 10);
        });
    }

    it ('ensures a new invocation is not made until the previous one has finished', function (done) {
        calls = 0;
        var f = myutils.serialize(one_at_a_time);
        for (var i = 0; i < 10; i++) {
            f();
        }
        setTimeout(function () {
            assert.equal(calls, 10);
            done();
        }, 500);
    });
    it ('handles invocations at different times', function (done) {
        calls = 0;
        var f = myutils.serialize(one_at_a_time);
        setTimeout(f, 5);
        setTimeout(f, 15);
        setTimeout(f, 16);
        setTimeout(f, 20);
        setTimeout(f, 28);

        setTimeout(function () {
            assert.equal(calls, 5);
            done();
        }, 100);
    });
});
