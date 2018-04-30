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

function BufferedSender(sender, key_function) {
    this.sender = sender;
    this.key_function = key_function;
    this.buffer = [];
    this.index = {};
    this.sender.on('sendable', this._send_pending.bind(this));
}

BufferedSender.prototype.send = function (message) {
    if (this._send_pending()) {
        this.sender.send(message);
    } else {
        this.buffer.push(message);
    }
};

BufferedSender.prototype._get_previous = function (message) {
    if (this.key_function) {
        var key = this.key_function(message);
        if (key) {
            return this.index[key];
        }
    }
    return undefined;
};

BufferedSender.prototype._add_pending = function (message) {
    var previous = this._get_previous(message);
    if (previous) {
        //replace previous
        this.buffer[previous] = message;
    } else {
        this.buffer.push(message);
    }
};

BufferedSender.prototype._send_pending = function () {
    while (this.sender.sendable() && this.buffer.length > 0) {
        this.sender.send(this.buffer.shift());
    }
    return this.buffer.length === 0 && this.sender.sendable();
};

module.exports = BufferedSender;

