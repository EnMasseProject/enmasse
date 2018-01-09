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

var create_podgroup = require('./podgroup.js');
var create_locator = require('./subloc.js');
var create_controller = require('./subctrl.js');
var log = require('./log.js').logger();

function Topic (name) {
    this.name = name;
    this.pods = create_podgroup();
    this.locator = create_locator(this.pods);
    this.controller = create_controller(this.pods);
};

Topic.prototype.close = function () {
    this.pods.close();
}

Topic.prototype.empty = function () {
    return this.pods.empty();
}

module.exports = function (name) {
    return new Topic(name);
}
