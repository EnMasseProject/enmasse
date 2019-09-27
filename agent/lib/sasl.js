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

module.exports.parseXOAuth2Reponse = function (buffer) {

    var i = 0;
    var start = 0;
    var results = {};
    while (i < buffer.length) {
        if (buffer[i] === 0x01) {
            if (i > start) {
                var keyvalue = buffer.toString('utf8', start, i);
                var sep = keyvalue.split("=",  2)
                results[sep[0]] = sep[1];
            }
            start = ++i;
        } else {
            ++i;
        }
    }

    if ("auth" in results) {
        results.token = results.auth.replace(/^bearer +/i, "");
    }
    return results;
};
