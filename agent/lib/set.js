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

var myutils = require('./utils.js');

function SortedObjectSet(comparator) {
    this.comparator = comparator;
    this.objects = [];
}

SortedObjectSet.prototype.find = function(object) {
    var m = 0;
    var n = this.objects.length - 1;
    while (m <= n) {
        var k = (n + m) >> 1;
        var cmp = this.comparator(object, this.objects[k]);
        if (cmp > 0) {
            m = k + 1;
        } else if(cmp < 0) {
            n = k - 1;
        } else {
            return k;
        }
    }
    return -m - 1;
}

SortedObjectSet.prototype.insert = function (object) {
    var i = this.find(object);
    if (i < 0) {
        this.objects.splice(~i, 0, object);
        return true;
    } else {
        return false;
    }
};

SortedObjectSet.prototype.remove = function (object) {
    var i = this.find(object);
    if (i < 0) {
        return false;
    } else {
        this.objects.splice(i, 1);
        return true;
    }
};

SortedObjectSet.prototype.replace = function (object) {
    var i = this.find(object);
    if (i < 0) {
        return false;
    } else {
        this.objects[i] = object;
        return true;
    }
};

SortedObjectSet.prototype.reset = function (objects) {
    this.objects = objects;
    this.objects.sort(this.comparator);
};

SortedObjectSet.prototype.to_array = function () {
    return this.objects;
};

module.exports.sorted_object_set = function (comparator) {
    return new SortedObjectSet(comparator);
};
