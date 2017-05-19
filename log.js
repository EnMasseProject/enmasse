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

var debug = require('debug');

var levels = {
    "error": 0,
    "warn": 1,
    "info": 2,
    "debug": 3
};

function Logger (name) {
    this.logger = debug(name);
    this.level = 3;
    if (process.env.LOGLEVEL) {
        var desired_level = levels[process.env.LOGLEVEL];
        if (desired_level !== undefined) {
            this.level = desired_level;
        }
    }
}

Logger.prototype.log = function (level, msg) {
    if (levels[level] !== undefined && levels[level] <= this.level) {
        this.logger(level + " " + msg);
    }
}

Logger.prototype.debug = function (msg) {
    this.log("debug", msg)
}

Logger.prototype.info = function (msg) {
    this.log("info", msg)
}

Logger.prototype.warn = function (msg) {
    this.log("warn", msg)
}

Logger.prototype.error = function (msg) {
    this.log("error", msg)
}

module.exports.logger = function (name) {
    return new Logger(name);
}
