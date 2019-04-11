/*
 * Copyright 2019 Red Hat Inc.
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
var CookieDecoder = require('../lib/cookie_decoder.js');

const oauthcookie = 'ZGV2ZWxvcGVyQGNsdXN0ZXIubG9jYWx8QXRwQUx4ak8vRGVsNnBDeThHMTIxLzY4ODNGMjl5bk5oejdUc1o5cHFhYWNJN1RYQUdCelo5UHFadGZOeUd0M3N6N2hPK25PYWRoN2xXQT18LTYyMTM1NTk2ODAwfA==';
const cookie_secret = "rRrcG8qj2mwg7bF8CBq2dA==";
const expectedToken = "gwGey4ZnQyFkY8F7sDKX3qPcSYZnD7nhzX_IGJhLdSE";

describe('cookie_decoder', function() {
    it('decode cookie', function (done) {
        const cookieDecoder = new CookieDecoder(cookie_secret);
        var state = cookieDecoder.decode(oauthcookie);
        assert.equal(state.user, "developer");
        assert.equal(state.email, "developer@cluster.local");
        assert.equal(state.access_token, expectedToken);
        done();
    });
});
