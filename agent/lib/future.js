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

var Future = function (handler) {
    this.handler = handler;
    this.completed = false;
    this.error = undefined;
    this.callback = undefined;
};

Future.prototype.get_error = function () {
    return this.error;
}

Future.prototype.is_complete = function () {
    return this.completed;
}

Future.prototype.as_callback = function () {
    return this.complete.bind(this);
}

Future.prototype.complete = function (context) {
    this.completed = true;
    if (this.handler) {
        this.error = this.handler(context);
    }
    if (this.callback) {
        this.callback(this.error);
    }
};

Future.prototype.then = function (callback) {
    this.callback = callback;
    if (this.is_complete()) {
        this.callback(this.get_error());
    }
};

Future.prototype.and = function (other) {
    return new FutureSet().and(this).and(other);
}

var FutureSet = function (actions) {
    this.actions = actions || [];
    this.callback = undefined;
};

FutureSet.prototype.and = function (action) {
    this.actions.push(action);
    action.then(this.complete.bind(this));
    return this;
}

function has_error (a) {
    return a.error !== undefined;
}

function get_error (a) {
    return a.error;
}

FutureSet.prototype.get_error = function () {
    if (this.actions.some(has_error)) {
        return this.actions.filter(has_error).map(get_error).join();
    } else {
        return undefined;
    }
}

FutureSet.prototype.is_complete = function () {
    return this.actions.every(function (a) { return a.is_complete(); });
}

FutureSet.prototype.complete = function () {
    if (this.is_complete()) {
        if (this.callback) {
            this.callback(this.get_error());
        }
    }
}

FutureSet.prototype.then = function (callback) {
    this.callback = callback;
    if (this.is_complete()) {
        this.callback(this.get_error());
    }
};

module.exports = {
    future : function (handler) { return new Future(handler); },
    and : function (a, b) { return a.and(b); }
};
