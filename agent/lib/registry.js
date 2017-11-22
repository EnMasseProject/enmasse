/*
 * Copyright 2016 Red Hat Inc.
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

var log = require("./log.js").logger();
var util = require('util');
var events = require('events');

function Registry() {
    events.EventEmitter.call(this);
    this.objects = {};
}

util.inherits(Registry, events.EventEmitter);

Registry.prototype.deleted = function (key) {
    var a = this.objects[key]
    delete this.objects[key];
    this.emit('deleted', a);
};

Registry.prototype.get = function (key) {
    return this.objects[key];
};

Registry.prototype.set = function (known) {
    for (var key in known) {
        if (this.objects[key] === undefined) {
            this.objects[key] = known[key];
            this.emit('updated', known[key]);
        } else {
            this.update(key, known[key]);
        }
    }
    for (var key in this.objects) {
        if (known[key] === undefined) {
            this.deleted(key);
        }
    }
};

function equals(a, b) {
    return JSON.stringify(a) === JSON.stringify(b);
}

Registry.prototype.update = function (id, latest) {
    var current = this.objects[id];
    if (current === undefined) {
        this.objects[id] = latest;
        log.debug('setting ' + id + ' to ' + JSON.stringify(latest));
        this.emit('updated', latest);
    } else {
        var changed = false;
        for (var s in latest) {
            if (!equals(current[s], latest[s])) {
                log.debug('changing ' + s + ' on ' + id + ' from ' + JSON.stringify(current[s]) + ' to ' + JSON.stringify(latest[s]));
                current[s] = latest[s];
                changed = true;
            }
        }
        if (changed) {
            this.emit('updated', current);
        }
    }
};

Registry.prototype.update_if_exists = function (id, latest) {
    var current = this.objects[id];
    if (current) {
        var changed = false;
        for (var s in latest) {
            if (!equals(current[s], latest[s])) {
                log.debug('changing ' + s + ' on ' + id + ' from ' + JSON.stringify(current[s]) + ' to ' + JSON.stringify(latest[s]));
                current[s] = latest[s];
                changed = true;
            }
        }
        if (changed) {
            this.emit('updated', current);
        }
        return true;
    } else {
        return false;
    }
};

Registry.prototype.update_existing = function (updates) {
    for (var k in updates) {
        this.update_if_exists(k, updates[k]);
    }
};

Registry.prototype.for_each = function (action, filter) {
    for (var key in this.objects) {
        if (filter === undefined || filter(this.objects[key])) {
            action(this.objects[key]);
        }
    }
}

module.exports = Registry;
