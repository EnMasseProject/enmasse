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

const crypto = require('crypto');
const util = require('util');
const AES_BLOCKSIZE = 16;

function CookieDecoder (secret) {
    this.secretBuf = Buffer.from(secret);
    if (this.secretBuf.length === 16 || this.secretBuf.length === 24 || this.secretBuf.length === 32) {
        this.algorithm = util.format('aes-%d-cfb', 8 * this.secretBuf.length);
    } else {
        throw Error(util.format("Unsupported AES key size %d", this.secretBuf.length));
    }
}

CookieDecoder.prototype.decode = function (oauthcookie_base64url) {

    var b64string = decodeURI(oauthcookie_base64url);
    var oauthcookie_data = Buffer.from(b64string, 'base64').toString();
    var cookie_chunks = oauthcookie_data.split("|");

    var session_state = {};

    if (cookie_chunks.length > 0) {
        session_state['email'] = cookie_chunks[0];
        if (cookie_chunks[0].includes("@")) {
            session_state['user'] = cookie_chunks[0].split("@")[0];
        }
    }

    if (cookie_chunks.length === 1) {
        return session_state;
    }

    if (cookie_chunks.length !== 4) {
        throw Error(util.format("invalid number of fields (got %d expected 4)", cookie_chunks.length));
    }
    var access_token_ciphered = cookie_chunks[1];
    session_state.access_token = this._do_decode(access_token_ciphered);
    return session_state;
};

CookieDecoder.prototype._do_decode = function(encrypted_base64) {
    var encryptedBuf = Buffer.from(encrypted_base64, 'base64');

    if (encryptedBuf.length < AES_BLOCKSIZE) {
        throw Error(util.format("encrypted cookie value should be "+
            "at least %d bytes, but is only %d bytes", AES_BLOCKSIZE, encryptedBuf.length));
    }

    var iv = encryptedBuf.slice(0, AES_BLOCKSIZE);
    var remains = encryptedBuf.slice(AES_BLOCKSIZE);
    const decipher = crypto.createDecipheriv(this.algorithm, this.secretBuf, iv);
    var decryptedBuf = Buffer.concat([decipher.update(remains), decipher.final()]);
    return decryptedBuf.toString();
};

module.exports = CookieDecoder;
