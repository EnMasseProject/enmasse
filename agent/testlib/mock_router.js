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

var events = require('events');
var util = require('util');
var rhea = require('rhea');
var tls_options = require('../lib/tls_options.js');
var match_source_address = require('../lib/utils.js').match_source_address;

function MockRouter (name, port, opts) {
    this.name = name;
    events.EventEmitter.call(this);
    var options = {'port':port, container_id:name, properties:{product:'qpid-dispatch-router'}};
    for (var o in opts) {
        options[o] = opts[o];
    }
    this.connection = rhea.connect(options);
    this.connection.on('message', this.on_message.bind(this));
    var self = this;
    this.connection.on('connection_open', function () { self.emit('connected', self); });
    this.connection.on('sender_open', function (context) {
        if (context.sender.source.dynamic) {
            var id = rhea.generate_uuid();
            context.sender.set_source({address:id});
        }
    });
    this.objects = {};
    this.create_object('listener', 'default', {name:'default', host:name, 'port':port, role:'inter-router'});
}

util.inherits(MockRouter, events.EventEmitter);

MockRouter.prototype.create_object = function (type, name, attributes)
{
    if (this.objects[type] === undefined) {
        this.objects[type] = {};
    }
    this.objects[type][name] = (attributes === undefined) ? {} : attributes;
    this.objects[type][name].name = name;
    this.objects[type][name].id = name;
};

MockRouter.prototype.delete_object = function (type, name)
{
    delete this.objects[type][name];
};

MockRouter.prototype.list_objects = function (type)
{
    var results = [];
    for (var key in this.objects[type]) {
        results.push(this.objects[type][key]);
    }
    return results;
};

MockRouter.prototype.close = function ()
{
    if (this.connection && !this.connection.is_closed()) {
        this.connection.close();
        var conn = this.connection;
        return new Promise(function(resolve, reject) {
            conn.on('connection_close', resolve);
        });
    } else {
        return Promise.resolve();
    }
};

MockRouter.prototype.close_with_error = function (error)
{
    if (this.connection && !this.connection.is_closed()) {
        this.connection.local.close.error = error;
    }
    return this.close();
};

MockRouter.prototype.has_connector_to = function (other) {
    return this.list_objects('connector').some(function (c) { return c.host === other.name; });
};

MockRouter.prototype.check_connector_from = function (others) {
    var self = this;
    return others.some(function (router) { return router !== self && router.has_connector_to(self); });
};

function get_attribute_names(objects) {
    var names = {};
    for (var i in objects) {
        for (var f in objects[i]) {
            names[f] = f;
        }
    }
    return Object.keys(names);
}

function query_result(names, objects) {
    var results = [];
    for (var j in objects) {
        var record = [];
        for (var i = 0; i < names.length; i++) {
            record.push(objects[j][names[i]]);
        }
        results.push(record);
    }
    return {attributeNames:names, 'results':results};
}

MockRouter.prototype.on_message = function (context)
{
    var request = context.message;
    var reply_to = request.reply_to;
    var response = {to: reply_to};
    if (!(this.special && this.special(request, response, context))) {// the 'special' method when defined lets errors be injected
        if (request.correlation_id) {
            response.correlation_id = request.correlation_id;
        }
        response.application_properties = {};

        if (request.application_properties.operation === 'CREATE') {
            response.application_properties.statusCode = 201;
            this.create_object(request.application_properties.type, request.application_properties.name, request.body);
        } else if (request.application_properties.operation === 'DELETE') {
            response.application_properties.statusCode = 204;
            this.delete_object(request.application_properties.type, request.application_properties.name);
        } else if (request.application_properties.operation === 'QUERY') {
            response.application_properties.statusCode = 200;
            var results = this.list_objects(request.application_properties.entityType);
            var attributes = request.body.attributeNames;
            if (!attributes || attributes.length === 0) {
                attributes = get_attribute_names(results);
            }
            response.body = query_result(attributes, results);
        }
    }

    var reply_link = context.connection.find_sender(function (s) { return match_source_address(s, reply_to); });
    if (reply_link) {
        reply_link.send(response);
    }
};

MockRouter.prototype.set_onetime_error_response = function (operation, type, name) {
    var f = function (request, response) {
        if ((operation === undefined || request.application_properties.operation === operation)
            && (type === undefined || (operation === 'QUERY' && request.application_properties.entityType === type) || request.application_properties.type === type)
            && (name === undefined || request.application_properties.name === name)) {
            response.application_properties = {};
            response.application_properties.statusCode = 500;
            response.application_properties.statusDescription = 'error simulation';
            response.correlation_id = request.correlation_id;
            delete this.special;
            return true;
        } else {
            return false;
        }
    };
    this.special = f.bind(this);
};

module.exports = MockRouter;
