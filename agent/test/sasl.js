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
var myutils = require('../lib/sasl.js');


describe('xoauth', function () {
    it ('parse', function (done) {
        var knownGood = "dXNlcj1zb21ldXNlckBleGFtcGxlLmNvbQFhdXRoPUJlYXJlciB5YTI5LnZGOWRmdDRxbVRjMk52YjNSbGNrQmhkSFJoZG1semRHRXVZMjl0Q2cBAQ==";
        var buf = Buffer.from(knownGood, 'base64');
        var res = myutils.parseXOAuth2Reponse(buf);
        assert.equal(res.user, "someuser@example.com");
        assert.equal(res.token, "ya29.vF9dft4qmTc2Nvb3RlckBhdHRhdmlzdGEuY29tCg");
        done();
    });
});
