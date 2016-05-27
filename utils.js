/*
 * Copyright 2016 Red Hat Inc.
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

function self(o) {
    return o;
}

function default_equality(a, b) {
    return a == b;
}


module.exports.index = function (a, key, value) {
    var fk = key || self;
    var fv = value || self;
    var m = {};
    a.forEach(function (i) { m[fk(i)] = fv(i); });
    return m;
}

module.exports.values = function (map) {
    var v = [];
    for (var k in map) {
        v.push(map[k]);
    }
    return v;
}

module.exports.difference = function (a, b, equivalence) {
    var equivalent = equivalence || default_equality;
    var diff = {};
    for (var k in a) {
	if (!equivalent(b[k], a[k])) {
	    diff[k] = a[k];
	}
    }
    return diff;
}


