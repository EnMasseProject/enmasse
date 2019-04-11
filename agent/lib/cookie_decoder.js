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
const algorithm = 'aes-128-cfb';
const AES_BLOCKSIZE = 16;

function CookieDecoder (secret) {
    this.secretBuf = this._secretToBuf(secret);
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
    var refresh_token_token_ciphered = cookie_chunks[3];

    session_state.access_token = this._do_decode(access_token_ciphered);
    if (refresh_token_token_ciphered) {
        session_state.refresh_token = this._do_decode(refresh_token_token_ciphered);
    }

    return session_state;
};

CookieDecoder.prototype._secretToBuf = function(secret_base64url) {
    var addPadding = function (secret) {
        var padding = secret.length % 4;
        if (padding === 1) {
            return secret + "===";
        } else if (padding === 2) {
            return secret + "==";
        } else if (padding === 3) {
            return secret + "=";
        }
        return secret;
    };
    try {
        const secret = Buffer.from(decodeURI(addPadding(secret_base64url)), 'base64');
        // TODO pad out if necessary
        return secret;
    } catch (e) {
        return Buffer.from(secret_base64url);
    }
};

CookieDecoder.prototype._do_decode = function(encrypted_base64) {
    var encryptedBuf = Buffer.from(encrypted_base64, 'base64');

    if (encryptedBuf.length < AES_BLOCKSIZE) {
        throw Error(util.format("encrypted cookie value should be "+
            "at least %d bytes, but is only %d bytes", AES_BLOCKSIZE, encryptedBuf.length));
    }

    var iv = encryptedBuf.slice(0, AES_BLOCKSIZE);
    var remains = encryptedBuf.slice(AES_BLOCKSIZE);

    // TODO handle other secret sizes.
    const decipher = crypto.createDecipheriv(algorithm, this.secretBuf, iv);
    var decryptedBuf = Buffer.concat([decipher.update(remains), decipher.final()]);
    return decryptedBuf.toString();
};

module.exports = CookieDecoder;
