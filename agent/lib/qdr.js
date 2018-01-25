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

var Router = function (connection, router, agent) {
    if (router) {
        this.target = agent;
        this.connection = router.connection;
        this.counter = router.counter;
        this.handlers = router.handlers;
        this.requests = router.requests;
        this.address = router.address;
        this.sender = this.connection.open_sender(agent);
    } else {
        this.target = '$management';
        this.sender = connection.open_sender('$management');
        this.connection = connection;
        this.counter = 0;
        this.handlers = {};
        this.requests = [];
        connection.open_receiver({source:{dynamic:true}});
        connection.on('message', this.incoming.bind(this));
        connection.on('connection_close', this.closed.bind(this));
    }
    this.connection.on('receiver_open', this.ready.bind(this));
    this.connection.on('disconnected', this.disconnected.bind(this));
    this.connection.on('sender_error', this.on_sender_error.bind(this));
};

Router.prototype.closed = function (context) {
    if (context.connection.error) {
        log.error('ERROR: router closed connection with ' + context.connection.error.description);
    }
    log.info('router closed ' + this.target);
    this.connection = undefined;
    this.sender = undefined;
    this.address = undefined;
    this._abort_requests('closed');
};

Router.prototype._abort_requests = function (error) {
    log.info('aborting pending requests: ' + error);
    for (var h in this.handlers) {
        this.handlers[h](error);
        delete this.handlers[h];
    }
    while (this.requests.length > 0) { this.requests.shift(); };
}

Router.prototype.on_sender_error = function (context) {
    var error = this.connection.container_id + ' sender error ' + JSON.stringify(context.sender.error);
    log.info('[' + this.connection.container_id + '] ' + error);
};

Router.prototype.disconnected = function (context) {
    this.address = undefined;
    this._abort_requests('disconnected');
}

Router.prototype.ready = function (context) {
    log.info('router ready');
    this.address = context.receiver.source.address;
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
    return body.results ? body.results.map(create_record.bind(null, body.attributeNames)) : body;
}

function as_handler(resolve, reject) {
    return function (context) {
        var message = context.message === undefined ? context : context.message;
        if (message.application_properties && message.application_properties.statusCode >= 200 && message.application_properties.statusCode < 300) {
            if (message.body) resolve(extract_records(message.body));
            else resolve({code:message.statusCode, description:message.application_properties.statusDescription});
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
    if (this.sender) {
        request.reply_to = this.address;
        this.sender.send(request);
        log.debug('sent: ' + JSON.stringify(request));
    } else {
        log.info('router agent has no sender!');
    }
}

Router.prototype.request = function (operation, properties, body) {
    var id = this.target + this.counter.toString();
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

Router.prototype.get_mgmt_nodes = function () {
    return this.request('GET-MGMT-NODES', {}, {});
};

Router.prototype._get_all_routers = function () {
    var self = this;
    return this.get_mgmt_nodes().then(
        function (agents) {
            return agents.map(function (agent) { return new Router(undefined, self, agent); });
        }
    );
};

Router.prototype.get_all_routers = function (current) {
    if (current === undefined || current.length === 0) {
        return this._get_all_routers();
    } else {
        var self = this;
        return this.get_mgmt_nodes().then(
            function (results) {
                var agents = {};
                results.forEach(function (name) { agents[name] = true; });
                var routers = [];
                for (var i = 0; i < current.length; i++) {
                    if (agents[current[i].target] !== undefined) {
                        delete agents[current[i].target];
                        routers.push(current[i]);
                    }
                }
                return routers.concat( Object.keys(agents).map(function (agent) { return new Router(undefined, self, agent); }) );
            }
        );
    }
};

Router.prototype.incoming = function (context) {
    log.debug('recv: ' + JSON.stringify(context.message));
    var message = context.message;
    var handler = this.handlers[message.correlation_id];
    if (handler) {
        handler(context);
        delete this.handlers[message.correlation_id];
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

function add_queryable_type (name, typename) {
    var resource_type = typename || name;
    Router.prototype['get_' + name] = function (options) {
        return this.query(resource_type, options);
    };
}

add_resource_type('connector');
add_resource_type('listener');
add_resource_type('address', 'org.apache.qpid.dispatch.router.config.address', 'addresses');
add_resource_type('link_route', 'org.apache.qpid.dispatch.router.config.linkRoute');

add_queryable_type('connections', 'org.apache.qpid.dispatch.connection');
add_queryable_type('links', 'org.apache.qpid.dispatch.router.link');
add_queryable_type('address_stats', 'org.apache.qpid.dispatch.router.address');

var amqp = require('rhea').create_container();
module.exports.Router = Router;
module.exports.connect = function (options) {
    return new Router(amqp.connect(options));
}
