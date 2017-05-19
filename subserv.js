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
var config_service = require('./config_service.js');
var Promise = require('bluebird');
var amqp = require('rhea').create_container();
var create_topic = require('./topic.js');
var log = require('log4js').getLogger();

var topics = {};

function get_or_create_topic(name) {
    var topic = topics[name];
    if (topic === undefined) {
        topic = create_topic(name);
        topics[name] = topic;
    }
    return topic;
}

function is_topic (address) {
    return address.multicast && address.store_and_forward;
}

function addresses_updated(addresses) {
    for (var name in addresses) {
        var address = addresses[name];
        if (is_topic(address)) {
            var topic = topics[name];
            if (topic === undefined) {
                log.info('starting to watch pods for topic ' + name);
                topic = create_topic(name);
                topic.watch_pods();
                topics[name] = topic;
            } else {
                log.info('already watching pods for topic ' + name);
            }
        }
    }
    for (var name in topics) {
        var address = addresses[name];
        if (address === undefined || !is_topic(address)) {
            topics[name].close();
            delete topics[name];
        }
    }
}

var SUBCTRL = '$subctrl';

function get_topic(message) {
    if (message.application_properties && message.application_properties.root_address) {
        var root = message.application_properties.root_address;
        var topic = topics[root];
        if (topic !== undefined) {
            return topic;
        } else {
            throw Error('subscription control message specified unrecognised root_address: ' + root + ' [' + Object.getOwnPropertyNames(topics) + ']');
        }
    } else {
        throw Error('subscription control message must specify root_address in application properties');
    }
}

function combine(objects) {
    var r = {};
    objects.forEach(function (o) {
        Object.keys(o).forEach( function(key) { r[key] = o[key]; } );
    });
    return r;
}

function list(subscription_id, key) {
    return topics[key].controller.list(subscription_id);
}

function close(subscription_id, key) {
    return topics[key].controller.close(subscription_id);
}

function subscribe(subscription_id, request) {
    return request.topic.controller.subscribe(subscription_id, request.addresses);
}

function unsubscribe(subscription_id, request) {
    return request.topic.controller.unsubscribe(subscription_id, request.addresses);
}

function request_string(message) {
    var params = message.body ? [message.correlation_id, message.body] : [message.correlation_id];
    return message.subject + '(' + params.join() + ')';
}

function get_separator(address) {
    return address.indexOf('/') >= 0 ? '/' : '.';
}

function get_root(address) {
    if (topics[address]) {
        return address;
    } else {
        var separator = get_separator(address);
        var root = address;
        for (var end = root.lastIndexOf(separator); end > 0; end = root.lastIndexOf(separator)) {
            root = root.substr(0, end);
            if (topics[root]) return root;
        }
    }
    throw Error('Unrecognised topic: ' + address);
}

function subreqs(input) {
    var grouped = {};
    if (util.isArray(input)) {
        input.forEach(function (address) {
            var root = get_root(address);
            if (grouped[root] === undefined) {
                grouped[root] = {};
            }
            grouped[root][address] = undefined;
        });
    } else if ((typeof input) === 'string') {
        var addresses = {};
        addresses[input] = undefined;
        grouped[get_root(input)] = addresses;
    } else {
        //assume map
        for (var address in input) {
            var root = get_root(address);
            if (grouped[root] === undefined) {
                grouped[root] = {};
            }
            grouped[root][address] = input[address];
        }
    }
    log.info('in subreqs(' + input + '): type=' + (typeof input) + ', grouped=' + JSON.stringify(grouped));
    return Object.keys(grouped).map(
        function(key) {
            return {
                topic: topics[key],
                addresses: grouped[key]
            };
        }
    );
}

function handle_control_message(context) {
    log.info('received message: ' + context.message);
    if (context.message.to === SUBCTRL || (context.receiver.target && context.receiver.target.address === SUBCTRL)) {
        var subscription_id = context.message.correlation_id;
        var accept = function () {
            log.info(request_string(context.message) + ' succeeded');
            context.delivery.accept();
        };
        var reject = function (e, code) {
            log.info(request_string(context.message) + ' failed: ' + e);
            context.delivery.reject({condition: code || 'amqp:internal-error', description: '' + e});
        };
        var reply = function (type, value) {
            if (sender) {
                sender.send({to:context.message.reply_to, subject:type, correlation_id:subscription_id, body:value});
            }
            accept();
        };

        log.info(request_string(context.message));
        try {
            if (context.message.subject === 'list') {
                Promise.all(Object.keys(topics).map(list.bind(null, subscription_id))).then(
                    function (results) {
                        reply('subscriptions', combine(results));
                    }
                ).catch(reject);
            } else if (context.message.subject === 'close') {
                Promise.all(Object.keys(topics).map(close.bind(null, subscription_id))).then(accept).catch(reject);
            } else if (context.message.subject === 'subscribe') {
                Promise.all(subreqs(context.message.body).map(subscribe.bind(null, subscription_id))).then(accept).catch(reject);
            } else if (context.message.subject === 'unsubscribe') {
                Promise.all(subreqs(context.message.body).map(unsubscribe.bind(null, subscription_id))).then(accept).catch(reject);
            } else {
                reject('unrecognised subject ' + context.message.subject, 'amqp:not-implemented');
            }
        } catch (e) {
            reject(e, 'amqp:precondition-failed');
        }
    } else if (context.message.subject === 'pods') {
        if (context.receiver.target.address) {
            var topic = get_or_create_topic(context.receiver.target.address);
            topic.pods.update(context.message.body);
        } else {
            log.info('Must specify topic as target address for messages with pods as subject');
        }
    } else if (context.message.subject === 'addresses' || !context.message.subject) {
        var body_type = typeof context.message.body;
        if (body_type  === 'string') {
            var content;
            try {
                content = JSON.parse(context.message.body);
            } catch (e) {
                log.info('ERROR: failed to parse addresses as JSON: ' + e + '; ' + context.message.body);
            }
            if (content) {
                if (content.json !== undefined) {
                    content = content.json;
                }
                for (var v in content) {
                    if (content[v].name === undefined) {
                        content[v].name = v;
                    }
                    if (content[v]['store-and-forward'] !== undefined) {
                        content[v].store_and_forward = content[v]['store-and-forward'];
                        delete content[v]['store-and-forward'];
                    }
                }
                addresses_updated(content);
            }
        } else if (body_type  === 'object') {
            addresses_updated(context.message.body);
        } else {
            log.info('ERROR: unrecognised type for addresses: ' + body_type + ' ' + context.message.body);
        }
    }
}

amqp.on('message', handle_control_message);

amqp.on('sender_open', function (context) {
    //treat as link to be redirected
    var source = context.sender.source.address;
    if (source.indexOf('locate/') === 0 || source.indexOf('locate.') === 0) {
        source = source.substring(7);
    }
    var topic = topics[source];
    if (topic === undefined) {
        context.sender.close({condition:'amqp:not-found', description:'unknown topic ' + source});
    } else {
        var subscription_id = context.sender.name;
        log.info('got attach request from subscribing client: ' + topic.name + ' ' + subscription_id);
        var link = context.sender;
        topic.locator.locate(subscription_id, topic.name).then(
            function (address) {
                link.close({condition:'amqp:link:redirect', description:address, info:{'address':address}});
            }
        ).catch (
            function (e) {
                var desc = 'Could not locate subscription: ' + e;
                link.close({condition:'amqp:internal-error', description:desc});
            }
        );
    }
});

amqp.on('connection_open', function (context) {
    log.info('connected ' + context.connection.container_id + ' [' + context.connection.options.id + ']');
});
amqp.on('disconnected', function (context) {
    log.info('disconnected ' + context.connection.container_id + ' [' + context.connection.options.id + ']');
});

var connection_properties = {product:'subserv', container_id:process.env.HOSTNAME};

log.info("Starting subserv");

amqp.sasl_server_mechanisms.enable_anonymous();
amqp.listen({port:5672, properties:connection_properties});

var sender;
if (process.env.MESSAGING_SERVICE_HOST) {
    var conn = amqp.connect({host:process.env.MESSAGING_SERVICE_HOST, port:process.env.MESSAGING_SERVICE_PORT, properties:connection_properties, id:'messaging-service'});
    conn.open_receiver({autoaccept: false, source:SUBCTRL, target:SUBCTRL});
    sender = conn.open_sender({target:{}});
    conn.on('sender_open', function (context) {
        log.info('opened anonymous sender');
    });
}

var conn = config_service.connect(amqp, 'configuration-service');
if (conn) {
    log.info('opening link to configuration service...');
    conn.open_receiver('maas');
}
