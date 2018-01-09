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
var log = require('../lib/log.js');

describe('logger', function() {
    it('sets level from LOGLEVEL env var', function (done) {
        process.env.LOGLEVEL = 'warn';
        assert.equal(log.logger().level, 1);
        delete process.env.LOGLEVEL;
        done();
    });
    it('ignores invalid level in env', function (done) {
        process.env.LOGLEVEL = 'foo';
        assert.equal(log.logger().level, 2);
        delete process.env.LOGLEVEL;
        done();
    });
    it('ignores invalid level in log request', function (done) {
        log.logger().log('foo', 'ignore me!');
        done();
    });
});
