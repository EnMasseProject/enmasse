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
var util = require('util');
var events = require('events');
var rhea = require('rhea');

function AddressSource(connection) {
    var self = this;
    events.EventEmitter.call(this);
    var configserv = connection || require('./admin_service.js').connect_service(rhea, 'CONFIGURATION');
    configserv.open_receiver('v1/addresses').on('message', function (context) {
        if (context.message.subject === 'enmasse.io/v1/AddressList') {
            if (context.message.body && context.message.body.length) {
                try {
                    var content = JSON.parse(context.message.body);
                    var defs = content.items ? content.items.map(function (address) { return address.spec; }) : [];
                    self.emit('addresses_defined', defs);
                } catch (e) {
                    log.info('ERROR: failed to parse addresses: ' + e + '; ' + context.message.body);
                }
            } else {
                self.emit('addresses_defined', []);
            }
        } else {
            log.info('WARN: unexpected subject: ' + context.message.subject);
        }
    });
}

util.inherits(AddressSource, events.EventEmitter);

module.exports = AddressSource;
