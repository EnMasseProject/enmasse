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
var http = require('http');
var url = require('url');
var rhea = require('rhea');
var AddressList = require('./address_list.js');
var BufferedSender = require('./buffered_sender.js');
var Registry = require('./registry.js');
var tls_options = require('./tls_options.js');
var Metrics = require('./metrics.js');
var queue = require('../lib/queue.js');
var kubernetes = require('./kubernetes.js');
var sasl = require('./sasl.js');

function ConsoleServer (address_ctrl, env, openshift) {
    this.address_ctrl = address_ctrl;
    this.addresses = new AddressList();
    this.metrics = new Metrics(env.ADDRESS_SPACE_NAMESPACE, env.ADDRESS_SPACE);
    this.connections = new Registry();
    this.listeners = {};
    this.openshift = openshift;
    var self = this;
    this.addresses.on('updated', function (address) {
        self.publish({subject:'address',body:address});

    });
    this.addresses.on('deleted', function (address) {
        log.debug('address %s has been deleted, notifying clients...', address.address);
        self.publish({subject:'address_deleted',body:address.address});
    });
    this.addresses.on('purged', function (address) {
        log.info('address %s has been purged, notifying clients...', address.address);
        self.publish({subject:'address_purged',body:address.address});
    });
    this.connections.on('updated', function (conn) {
        self.publish({subject:'connection',body:conn});
    });
    this.connections.on('deleted', function (conn) {
        log.debug('connection %s has been deleted, notifying clients...', conn.host);
        self.publish({subject:'connection_deleted',body:conn});
    });
    this.amqp_container = rhea.create_container({autoaccept:false});

    this.amqp_container.on('sender_open', function (context) {
        context.sender.set_source({address: "admin_data_source"});
        self.subscribe(context.connection.remote.open.container_id, context.sender);
    });
    function unsubscribe (context) {
        if (context.connection.remote.open) {
            self.unsubscribe(context.connection.remote.open.container_id);
        }
    }
    this.amqp_container.on('sender_close', unsubscribe);
    this.amqp_container.on('connection_open', function (context) {
        log.info('connection_open %j', context);
    });
    this.amqp_container.on('connection_close', unsubscribe);
    this.amqp_container.on('disconnected', unsubscribe);
    this.amqp_container.on('message', function (context) {
        var accept = function () {
            log.info('%s request succeeded', context.message.subject);
            context.delivery.accept();
        };
        var reject = function (e, code) {
            log.info('%s request failed: %s', context.message.subject, e);
            context.delivery.reject({condition: code || 'amqp:internal-error', description: '' + e});

            var sender = self.listeners[context.connection.remote.open.container_id];
            if (sender) {
                sender.send({subject:'request_error', body:"Error processing request: " + e});
            }
        };
        var handleServerResponse = function (e) {
            if (e && e.body) {
                try {
                    // Might be a Kubernetes Status response, if so use its message.
                    var status = JSON.parse(e.body);
                    if (status.message) {
                        e.toString = () => {return "" + e.statusCode + " : " + status.message};
                    }
                } catch (ignored) {
                    e.toString = () => {return "" + e.statusCode + " : " + e.body};
                }
            }
            reject(e);
        };
        var access_token = self.authz.get_access_token(context.connection);
        var user = self.authz.get_user(context.connection);
        if (!self.authz.is_admin(context.connection)) {
            reject(context, 'amqp:unauthorized-access', 'not authorized');
        } else if (context.message.subject === 'create_address') {
            log.info('[%s] creating address definition %s', user, JSON.stringify(context.message.body));
            self.address_ctrl.create_address(context.message.body, access_token).then(accept).catch(handleServerResponse);
        } else if (context.message.subject === 'delete_address') {
            log.info('[%s] deleting address definition %s', user, context.message.body.address);
            self.address_ctrl.delete_address(context.message.body, access_token).then(accept).catch(handleServerResponse);
        } else if (context.message.subject === 'purge_address') {
            log.info('[%s] purging address %s', user, context.message.body.address);
            queue(context.message.body).purge().then(purged => {
                log.info("[%s] Purged %d message(s) from address %s", user, purged, context.message.body.address);
                accept();
            }).catch(handleServerResponse);
        } else {
            reject('ignoring message: ' + context.message);
        }
    });
}


ConsoleServer.prototype.close = function (callback) {
    var self = this;
    return new Promise(function (resolve, reject) {
    }).then(function () {
        new Promise(function (resolve, reject) {
            if (self.server) {
                server.close(resolve);
            } else {
                resolve();
            }
        });
    }).then(callback);
};

ConsoleServer.prototype.listen = function (env, callback) {
    var self = this;
    this.authz = require('./authz.js').policy(env);

    return new Promise((resolve, reject) => {
        var port = env.port === undefined ? 56710 : env.port;
        var opts = tls_options.get_console_server_options({port: port}, env);

        self.amqp_container.sasl_server_mechanisms['XOAUTH2'] = function () {
            return {
                outcome: undefined,
                start: function (response, hostname) {
                    var resp = sasl.parseXOAuth2Reponse(response);
                    if (!"token" in resp) {
                        this.connection.sasl_failed('Unexpected response in XOAUTH2, no token part found');
                    }

                    var self = this;
                    function authenticate(token) {
                        return kubernetes.whoami({"token": token}).then((user) => {
                            log.info("Authenticated as user : %s ", user.username);
                            self.username = user.username;
                            return true;
                        }).catch((e) => {
                            log.error("Failed to authenticate using token", e);
                            return false;
                        })
                    }

                    return Promise.resolve(authenticate(resp.token, hostname))
                        .then(function (result) {
                            if (result) {
                                self.outcome = true;
                            } else {
                                self.outcome = false;
                            }
                        });
                },
            };
        };

        self.server = self.amqp_container.listen(opts);
        log.info("AMQP server listening on port %d", port);
        resolve(self.server);
    });
};

ConsoleServer.prototype.listen_health = function (env, callback) {
    if (env.HEALTH_PORT !== undefined) {
        var self = this;
        var health = http.createServer(function (req, res) {
            var pathname = url.parse(req.url).pathname;
            if (pathname === "/metrics") {
                var data = self.metrics.format_prometheus(new Date().getTime());
                res.writeHead(200, {'Content-Type': 'text/html'});
                res.end(data);
            } else {
                res.writeHead(200, {'Content-Type': 'text/plain'});
                res.end('OK');
            }
        });
        return health.listen(env.HEALTH_PORT, callback);
    }
};

function indexer(message) {
    if (message.subject === 'address' && message.body) {
        return message.body.address;
    } else if (message.subject === 'address_deleted') {
        return message.body;
    } else if (message.subject === 'address_purged') {
        return message.body;
    } else if (message.subject === 'connection' && message.body) {
        return message.body.id;
    } else if (message.subject === 'connection_deleted') {
        return message.body;
    } else {
        return undefined;
    }
}

ConsoleServer.prototype.subscribe = function (name, sender) {

    var buffered_sender = new BufferedSender(sender, indexer);
    this.listeners[name] = buffered_sender;
    this.addresses.for_each(function (address) {
        buffered_sender.send({subject:'address', body:address});
    }, this.authz.address_filter(sender.connection));
    this.connections.for_each(function (conn) {
        buffered_sender.send({subject:'connection', body:conn});
    }, this.authz.connection_filter(sender.connection));
    //TODO: poll for changes in address_types
    var self = this;
    this.address_ctrl.get_address_types().then(function (address_types) {
        var props = {};
        props.address_space_type = process.env.ADDRESS_SPACE_TYPE || 'standard';
        props.disable_admin = !self.authz.is_admin(sender.connection);
        props.user = self.authz.get_user(sender.connection);

        buffered_sender.send({subject:'address_types', application_properties:props, body:address_types});
    }).catch(function (error) {
        log.error('failed to get address types from address controller: %s', error);
    });
};

ConsoleServer.prototype.unsubscribe = function (name) {
    delete this.listeners[name];
};

ConsoleServer.prototype.publish = function (message) {
    for (var name in this.listeners) {
        var sender = this.listeners[name];
        if (this.authz.can_publish(sender.sender, message)) {
            sender.send(message);
        }
    }
};

module.exports = ConsoleServer;
