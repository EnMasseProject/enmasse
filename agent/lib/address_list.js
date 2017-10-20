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

var util = require('util');
var Registry =require('./registry.js');

function Addresses() {
    Registry.call(this);
}

util.inherits(Addresses, Registry);

Addresses.prototype.update_stats = function (name, stats) {
    this.update_if_exists(name, stats);
};

Addresses.prototype.addresses_defined = function (addresses) {
    this.set(addresses.reduce(function (map, a) { map[a.address] = a; return map; }, {}));
};

module.exports = Addresses;
