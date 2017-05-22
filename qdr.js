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

var Promise = require('bluebird');
var log = require('./log.js').logger();

var Router = function (connection) {
    this.connection = connection;
    this.sender = connection.open_sender('$management');
    this.counter = 0;
    this.handlers = {};
    this.requests = [];
    connection.open_receiver({source:{dynamic:true}});
    connection.on('receiver_open', this.ready.bind(this));
    connection.on('message', this.incoming.bind(this));
    connection.on('disconnected', this.disconnected.bind(this));
    connection.on('connection_close', this.closed.bind(this));
};

Router.prototype.closed = function (context) {
    if (context.connection.error) {
        log.warn('ERROR: router closed connection with ' + context.connection.error.description);
    }
    this.connection = undefined;
    this.sender = undefined;
};

Router.prototype.disconnected = function (context) {
    this.connection = undefined;
    this.sender = undefined;
};

Router.prototype.ready = function (context) {
    this.address = context.receiver.remote.attach.source.address;
    this._send_pending_requests();
};

function create_record(names, values) {
    var record = {};
    for (var j = 0; j < names.length; j++) {
        record[names[j]] = values[j];
    }
    return record;
}

function extract_records(body) {
    return body.results ? body.results.map(create_record.bind(null, body.attributeNames)) : [];
}

function as_handler(resolve, reject) {
    return function (context) {
        var message = context.message;
        if (message.statusCode >= 200 && message.statusCode < 300) {
            if (message.body) resolve(extract_records(message.body));
            else resolve({code:message.statusCode, description:message.statusDescription});
        } else {
            reject(message.toString());
        }
    };
}

Router.prototype._send_pending_requests = function () {
    for (var i = 0; i < this.requests.length; i++) {
        this._send_request(this.requests[i]);
    }
    this.requests = [];
}

Router.prototype._send_request = function (request) {
    request.reply_to = this.address;
    this.sender.send(request);
    //log.debug('sent: ' + JSON.stringify(request));
}

Router.prototype.request = function (operation, properties, body) {
    var id = this.counter.toString();
    this.counter++;
    var req = {correlation_id:id};
    req.application_properties = properties || {};
    req.application_properties.operation = operation;
    req.body = body;

    if (this.address) {
        this._send_pending_requests();
        this._send_request(req);
    } else {
        this.requests.push(req);
    }
    var handlers = this.handlers;
    return new Promise(function (resolve, reject) {
        handlers[id] = as_handler(resolve, reject);
    });
}

Router.prototype.query = function (type, options) {
    return this.request('QUERY', {entityType:type}, options || {attributeNames:[]});
};

Router.prototype.create_entity = function (type, name, attributes) {
    return this.request('CREATE', {'type':type, 'name':name}, attributes || {});
};

Router.prototype.delete_entity = function (type, name) {
    return this.request('DELETE', {'type':type, 'name':name}, {});
};

Router.prototype.incoming = function (context) {
    //log.debug('Got message: ' + JSON.stringify(context.message));
    var message = context.message;
    var handler = this.handlers[message.correlation_id];
    if (handler) {
        handler(context);
    } else {
        log.warn('WARNING: unexpected response: ' + message.correlation_id + ' [' + JSON.stringify(message) + ']');
    }
};

Router.prototype.close = function () {
    if (this.connection) this.connection.close();
}

function add_resource_type (name, typename, plural) {
    var resource_type = typename || name;
    Router.prototype['create_' + name] = function (o) {
        return this.create_entity(resource_type, o.name, o);
    };
    Router.prototype['delete_' + name] = function (o) {
        return this.delete_entity(resource_type, o.name);
    };
    var plural_name = plural || name + 's';
    Router.prototype['get_' + plural_name] = function (options) {
        return this.query(resource_type, options);
    };

}

add_resource_type('connector');
add_resource_type('listener');
add_resource_type('address', 'org.apache.qpid.dispatch.router.config.address', 'addresses');
add_resource_type('link_route', 'org.apache.qpid.dispatch.router.config.linkRoute');

var amqp = require('rhea').create_container();
module.exports.Router = Router;
module.exports.connect = function (options) {
    return new Router(amqp.connect(options));
}
