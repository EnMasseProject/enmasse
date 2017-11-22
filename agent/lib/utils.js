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

module.exports.remove = function (list, predicate) {
    var count = 0;
    for (var i = 0; i < list.length;) {
        if (predicate(list[i])) {
            list.splice(i, 1);
            count++;
        } else {
            i++;
        }
    }
    return count;
};

module.exports.replace = function (list, object, match) {
    for (var i = 0; i < list.length; i++) {
        if (match(list[i])) {
            list[i] = object;
            return true;
        }
    }
    return false;
};

module.exports.merge = function () {
    return Array.prototype.slice.call(arguments).reduce(function (a, b) {
        for (var key in b) {
            a[key] = b[key];
        }
        return a;
    });
}

module.exports.basic_auth = function (request) {
    if (request.headers.authorization) {
        var parts = request.headers.authorization.split(' ');
        if (parts.length === 2 && parts[0].toLowerCase() === 'basic') {
            parts = new Buffer(parts[1], 'base64').toString().split(':');
            return { name: parts[0], pass: parts[1] };
        } else {
            throw new Error('Cannot handle authorization header ' + auth);
        }
    }
}
