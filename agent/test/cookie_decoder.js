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
var log = require('../lib/log.js');
//var aesjs = require('aes-js');


const crypto = require('crypto');
const algorithm = 'aes-128-cfb';

/*

// cookies are stored in a 3 part (value + timestamp + signature) to enforce that the values are as originally set.
// additionally, the 'value' is encrypted so it's opaque to the browser



//|1554972808|P08CtZOk7YgRa5jQIIJlkObg2kU=; 3dd1c04b7c1e1582a1d35f4b1b5c9124=25e3728c9f8027c25509524950137256

// _oauth_proxy=1554990102|lFi73TstRQF0AgJO2RmlpgCNXNQ=; 3dd1c04b7c1e1582a1d35f4b1b5c9124=142de6bc3ffc64686901e12a79a9655f


 _oauth_proxy=ZGV2ZWxvcGVyQGNsdXN0ZXIubG9jYWx8UmN1cCtlQm9EWmZVcnB4bGZwdDM3d0s0bElRZFRWUWFRTjNaNlpXS2FTb1p4MTlkZ1pQS2lpWDFBbGg4RUdkbHZjQldpOFpZZWVES3RTRT18LTYyMTM1NTk2ODAwfA==
 |1554972808
 |P08CtZOk7YgRa5jQIIJlkObg2kU=; 3dd1c04b7c1e1582a1d35f4b1b5c9124=25e3728c9f8027c25509524950137256



_oauth_proxy=ZGV2ZWxvcGVyQGNsdXN0ZXIubG9jYWx8NlJsWWJvbnlSUkNYczhmV3c5RGJwajRhb0c2anlvK0R1elc5dDdmMGVTZFdKM0hqY0VjdkJiNm5lL01oYTVvNkVTSXUrNGxRcnZnQmUwWT18LTYyMTM1NTk2ODAwfA==|1554992709|GeLKK7-Hn2dOKpVGyRP7MYuipF4=



Latest




ZGV2ZWxvcGVyQGNsdXN0ZXIubG9jYWx8QXRwQUx4ak8vRGVsNnBDeThHMTIxLzY4ODNGMjl5bk5oejdUc1o5cHFhYWNJN1RYQUdCelo5UHFadGZOeUd0M3N6N2hPK25PYWRoN2xXQT18LTYyMTM1NTk2ODAwfA==




 */
describe('cookie_decoder', function() {
    it('decode cookie', function (done) {
        var oauthcookie = 'ZGV2ZWxvcGVyQGNsdXN0ZXIubG9jYWx8QXRwQUx4ak8vRGVsNnBDeThHMTIxLzY4ODNGMjl5bk5oejdUc1o5cHFhYWNJN1RYQUdCelo5UHFadGZOeUd0M3N6N2hPK25PYWRoN2xXQT18LTYyMTM1NTk2ODAwfA==';
        var cookie_secret = "rRrcG8qj2mwg7bF8CBq2dA==";
        var expectedToken = "gwGey4ZnQyFkY8F7sDKX3qPcSYZnD7nhzX_IGJhLdSE";

        const cookieDecoder = new CookieDecoder(cookie_secret);
        var state = cookieDecoder.decode(oauthcookie);
        assert.equal(state.user, "developer");
        assert.equal(state.email, "developer@cluster.local");
        assert.equal(state.access_token, expectedToken);
        done();

//
//         var f = function (oauthcookie) {
//             var b64string = decodeURI(oauthcookie);
//             var data = Buffer.from(b64string, 'base64').toString();
//
//             var chunks = data.split("|");
//
//             var session_state = {};
//
//             if (chunks.length > 0) {
//                 session_state['email'] = chunks[0];
//
//                 if (chunks[0].includes("@")) {
//                     session_state['user'] = chunks[0].split("@")[0];
//                 }
//             }
//             if (chunks.length === 1) {
//                 return session_state;
//             }
//
//             if (chunks.length !== 4) {
//                 throw Error(util.format("invalid number of fields (got %d expected 4)", chunks.length));
//
//             }
//
//
//
//
//             var d = function(encrypted, secret) {
//                 // secretBytes attempts to base64 decode the secret, if that fails it treats the secret as binary
//                 var secretBytes = function(secret)  {
//                     var addPadding = function(secret) {
//                         var padding = secret.length % 4;
//                         if (padding === 1) {
//                             return secret + "===";
//                         } else if (padding === 2) {
//                             return secret + "==";
//                         } else if (padding === 3) {
//                             return secret + "=";
//                         }
//                         return secret;
//                     };
//                     try {
//                         const secret1 = Buffer.from(decodeURI(addPadding(secret)), 'base64');
//                         console.log("Decoded");
//                         console.log(secret1);
//                         // return addPadding(secret1.toString());
//                         return secret1;
//                     } catch (e) {
//                         throw Error("ugh");
//                         return secret;
//                     }
//                 };
//
//                 const key = secretBytes(secret);
//                 console.log("incoming secret %s", secret);
//                 console.log("actual secret ", key);
//
//
// //                b: 00000000  ad 1a dc 1b ca a3 da 6c  20 ed b1 7c 08 1a b6 74  |.......l ..|...t|
//
//                 // var key = text;
//
//                 console.log("incoming encrypted %s", encrypted);
//                 var encryptedBytes = Buffer.from(encrypted, 'base64');
//                 console.log("encryptedBytes ", encryptedBytes);
//                 console.log("encryptedBytes length ", encryptedBytes.length);
//
//                 const AES_BLOCKSIZE = 16;
//                 if (encryptedBytes.length < AES_BLOCKSIZE) {
//                     throw Error(util.format("encrypted cookie value should be "+
//                         "at least %d bytes, but is only %d bytes", AES_BLOCKSIZE, encryptedBytes.length));
//                 }
//
//                 var iv = encryptedBytes.slice(0, AES_BLOCKSIZE);
//                 var remains = encryptedBytes.slice(AES_BLOCKSIZE);
//
//                 console.log("iv ", iv);
//                 console.log("iv length ", iv.length);
//
//                 console.log("remains ", remains);
//                 console.log("remains length ", remains.length);
//
//
//                 const decipher = crypto.createDecipheriv(algorithm, key, iv);
//                 var decryptedBytes = Buffer.concat([decipher.update(remains) , decipher.final()]);
//
//                 // var aesCfb = new aesjs.ModeOfOperation.cfb(key, iv);
//                 // var decryptedBytes = aesCfb.decrypt(remains);
//
//                 console.log("decryptedBytes ", decryptedBytes);
//                 console.log("decryptedBytes length ", decryptedBytes.length);
//                 //var fromBytes = aesjs.utils.utf8.fromBytes(decryptedBytes);
//                 return decryptedBytes.toString();
//
//             };
//
//             var access_token_ciphered = chunks[1];
//             var refresh_token_token_ciphered = chunks[3];
//
//             session_state.access_token = d(access_token_ciphered, cookie_secret);
//
//
//
//             return session_state;
//
//         };
//
//
//
//         var state = f(oauthcookie);



    });
    // it('ignores invalid level in env', function (done) {
    //     process.env.LOGLEVEL = 'foo';
    //     assert.equal(log.logger().level, 2);
    //     delete process.env.LOGLEVEL;
    //     done();
    // });
    // it('ignores invalid level in log request', function (done) {
    //     log.logger().log('foo', 'ignore me!');
    //     done();
    // });
});
