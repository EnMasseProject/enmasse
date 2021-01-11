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

var path = require('path');

var levels = {
    "error": 0,
    "warn": 1,
    "info": 2,
    "debug": 3
};

function Logger (name) {
    this.logger = require('debug')(name);
    this.level = 2;
    this.name = name;

    if (process.env.LOGLEVEL) {
        var desired_level = levels[process.env.LOGLEVEL];
        if (desired_level !== undefined) {
            this.level = desired_level;
        }
    }
}

function formatLevel(level, spaces) {
    var nchars = level.length;
    while (nchars < spaces) {
        level += " ";
        nchars++;
    }
    return level.toUpperCase();
}

Logger.prototype.log = function (level, str) {
    var args = Array.prototype.slice.call(arguments, 2);
    if (this.isEnabled(level)) {
        this.logger.apply(this.logger, [formatLevel(level, 5) + " " + str].concat(args));
    }
}

Logger.prototype.isEnabled = function (level) {
    return (levels[level] !== undefined && levels[level] <= this.level);
}

module.exports.logger = function() {
    var name = path.basename(process.argv[1], ".js");
    if (!process.env.DEBUG) {
        process.env.DEBUG = name + "*";
    }
    var logger = new Logger(name);
    logger.debug = logger.log.bind(logger, "debug");
    logger.info  = logger.log.bind(logger, "info");
    logger.warn  = logger.log.bind(logger, "warn");
    logger.error = logger.log.bind(logger, "error");
    logger.isDebugEnabled = logger.isEnabled.bind(logger, "debug");
    logger.isInfoEnabled = logger.isEnabled.bind(logger, "info");
    logger.isWarnEnabled = logger.isEnabled.bind(logger, "warn");
    logger.isErrorEnabled = logger.isEnabled.bind(logger, "error");
    return logger;
}
