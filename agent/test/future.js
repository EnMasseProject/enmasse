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
var future = require('../lib/future.js');

describe('future', function() {
    it('handles completion before then is called', function (done) {
        var f = future.future();
        f.complete();
        f.then(done);
    });
    it('handles completion of FutureSet before then is called', function (done) {
        var a = future.future();
        var b = future.future(function () { return 'fake error'; });
        var f = future.and(a, b);
        a.complete();
        b.complete();
        f.then(function (error) {
            assert.equal(error, 'fake error');
            done();
        });
    });
});
