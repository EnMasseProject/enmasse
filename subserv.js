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
var Promise = require('bluebird');
var amqp = require('rhea').create_container();
var create_topic = require('./topic.js');


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
                console.log('starting to watch pods for topic ' + name);
                topic = create_topic(name);
                topic.watch_pods();
                topics[name] = topic;
            } else {
                console.log('already watching pods for topic ' + name);
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
    //TODO: handle errors by rejecting message
    if (message.application_properties && message.application_properties.root_address) {
        var root = message.application_properties.root_address;
        var topic = topics[root];
        if (topic !== undefined) {
            return topic;
        } else {
            console.log('subscription control message specified unrecognised root_address: ' + root);
        }
    } else {
        console.log('subscription control message must specify root_address in application properties');
    }
}

function handle_control_message(context) {
    if (context.message.to === SUBCTRL) {
        var subscription_id = context.message.correlation_id;
        var topic = get_topic(context.message);
        if (topic !== undefined) {
            if (context.message.subject === 'close') {
                console.log('closing subscription ' + subscription_id);
                topic.controller.close(subscription_id).then(function () {
                    context.delivery.accept();
                }).catch(function (e) {
                    context.delivery.reject({condition:'amqp:internal-error',description:''+e});
                });
            } else {
                var topics = context.message.body || topic.name;
                if (!util.isArray(topics)) topics = [topics];

                if (context.message.subject === 'subscribe') {
                    Promise.all(topics.map(function (t) {
                        console.log('subscribing ' + subscription_id + ' to ' + t + '...');
                        return topic.controller.subscribe(subscription_id, t);
                    })).then(function () {
                        console.log('successfully subscribed ' + subscription_id + ' to ' + topics);
                        context.delivery.accept();
                    }).catch(function (e) {
                        context.delivery.reject({condition:'amqp:internal-error',description:''+e});
                    });
                } else if (context.message.subject === 'unsubscribe') {
                    Promise.all(topics.map(function (t) {
                        console.log('unsubscribing ' + subscription_id + ' from ' + t + '...');
                        return topic.controller.unsubscribe(subscription_id, t);
                    })).then(function () {
                        console.log('successfully unsubscribed ' + subscription_id + ' from ' + topics);
                        context.delivery.accept();
                    }).catch(function (e) {
                        context.delivery.reject({condition:'amqp:internal-error',description:''+e});
                    });
                } else {
                    console.log('ignoring subscription control message with subject ' + context.message.subject);
                    context.delivery.reject({condition:'amqp:not-implemented',description:'control message has unrecognised subject ' + context.message.subject});
                }
            }
        } else {
            context.delivery.reject({condition:'amqp:precondition-failed',description:'no topic specified'});
        }
    } else if (context.message.subject === 'pods') {
        if (context.receiver.target.address) {
            var topic = get_or_create_topic(context.receiver.target.address);
            topic.pods.update(context.message.body);
        } else {
            console.log('Must specify topic as target address for messages with pods as subject');
        }
    } else if (context.message.subject === 'addresses' || context.message.subject === null) {
        var body_type = typeof context.message.body;
        if (body_type  === 'string') {
            var content;
            try {
                content = JSON.parse(context.message.body);
            } catch (e) {
                console.log('ERROR: failed to parse addresses as JSON: ' + e + '; ' + context.message.body);
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
            console.log('ERROR: unrecognised type for addresses: ' + body_type + ' ' + context.message.body);
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
        console.log('got attach request from subscribing client: ' + topic.name + ' ' + subscription_id);
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
    console.log('connected ' + context.connection.container_id + ' [' + context.connection.options.id + ']');
});
amqp.on('disconnected', function (context) {
    console.log('disconnected ' + context.connection.container_id + ' [' + context.connection.options.id + ']');
});

var connection_properties = {product:'subserv', container_id:process.env.HOSTNAME};

amqp.sasl_server_mechanisms.enable_anonymous();
amqp.listen({port:5672, properties:connection_properties});


if (process.env.MESSAGING_SERVICE_HOST) {
    amqp.connect({host:process.env.MESSAGING_SERVICE_HOST, port:process.env.MESSAGING_SERVICE_PORT, id:'messaging-service'}).open_receiver({autoaccept: false, source:SUBCTRL});
}

if (process.env.CONFIGURATION_SERVICE_HOST) {
    console.log('connecting to configuration service...');
    amqp.options.username = 'subserv';
    var conn = amqp.connect({host:process.env.CONFIGURATION_SERVICE_HOST, port:process.env.CONFIGURATION_SERVICE_PORT, properties:connection_properties, id:'configuration-service'});
    conn.open_receiver('maas');
}

