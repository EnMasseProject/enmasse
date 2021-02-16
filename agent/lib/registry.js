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
var myutils = require('./utils.js');

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
    throw 'unsupported operation';
}

Registry.prototype.setAsync = function (known, chunkSize = 500) {
    var created = 0;
    var updated = 0;
    var deleted = 0;
    var knownKeys = Object.keys(known);
    var oldPop = Object.keys(this.objects).length;

    return new Promise((resolve, reject) => {
        myutils.applyAsync(knownKeys, (subset) => {
            subset.forEach(key => {
                if (this.objects[key] === undefined) {
                    this.objects[key] = known[key];
                    this.emit('updated', known[key]);
                    created++;
                } else {
                    if (this.update(key, known[key])) {
                        updated++;
                    }
                }
            });
        }, chunkSize).then(() => {
            var keys = Object.keys(this.objects);
            myutils.applyAsync(keys, (subset) => {
                subset.forEach(key => {
                    if (known[key] === undefined) {
                        this.deleted(key);
                        deleted++;
                    }
                });
            }, chunkSize).then(() => {
                let newPop = Object.keys(this.objects).length;
                log.info("%s setAsync created: %d updated: %d deleted: %d population: %d => %d", this.constructor.name, created, updated, deleted, oldPop, newPop);
                resolve();
            }).catch(reject);
        }).catch(reject);
    });
};

function equals(a, b) {
    return JSON.stringify(a) === JSON.stringify(b);
}

Registry.prototype.update = function (id, latest) {
    var current = this.objects[id];
    if (current === undefined) {
        this.objects[id] = latest;
        if (log.isDebugEnabled()) {
            log.debug('setting %s  to %s', id, JSON.stringify(latest));
        }
        this.emit('updated', latest);
        return true;
    } else {
        var changed = false;
        for (var s in latest) {
            if (latest.hasOwnProperty(s) && this.property_filter(s, latest[s])) {
                if (!equals(current[s], latest[s])) {
                    if (log.isDebugEnabled()) {
                        log.debug('changing %s on %s from %s to %s', s, id, JSON.stringify(current[s]), JSON.stringify(latest[s]));
                    }
                    current[s] = latest[s];
                    changed = true;
                }
            }
        }
        if (changed) {
            this.emit('updated', current);
            return true;
        }
    }
    return false;
};

Registry.prototype.update_if_exists = function (id, latest) {
    var current = this.objects[id];
    if (current) {
        return this.update(id, latest);
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
            action(key, this.objects[key]);
        }
    }
};

Registry.prototype.first = function (action, filter) {
    for (var key in this.objects) {
        if (filter === undefined || filter(this.objects[key])) {
            action(this.objects[key]);
        }
    }
};

Registry.prototype.property_filter =  function (name, latest) {
  return true;
};

module.exports = Registry;
