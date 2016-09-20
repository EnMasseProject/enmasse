/*
 * Copyright 2015 Red Hat Inc.
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

var assert = require('assert');
var rhea = require('rhea');

describe('subscription services', function() {
    this.slow(1000);

    var container;
    var connections = [];

    beforeEach(function(done) {
        container = rhea.create_container();
        done();
    });

    afterEach(function() {
        for (var i = 0; i < connections.length; i++) {
            connections[i].close();
        }
    });

    function new_connection(id) {
        var opts = {};
        opts.host = process.env.MESSAGING_SERVICE_HOST || 'localhost';
        opts.port = process.env.MESSAGING_SERVICE_PORT || 5672;
        if (id) opts.container_id = id;
        var conn = container.connect(opts);
        connections.push(conn);
        return conn;
    }

    it('simple link routed topic', function(done) {
        var s = new_connection().open_sender({target:{address:'mytopic', capabilities: ['topic']}});
        var ready = 0;
        var received = 0;
        for (var i = 0; i < 5; i++) {
            var r = new_connection('client_' + i).open_receiver({source:{
                address:'mytopic',
                durable:2,
                capabilities: ['topic']
            }});
            r.on('receiver_open', function (context) {
                if (++ready === 5) s.send({body:'test1'});
            });
            r.on('message', function (context) {
                assert.equal(context.message.body, 'test1');
                if (++received === 5) {
                    r.close();
                    done();
                }
            });
        }
    });

    it('link routed durable subscription', function(done) {
        var subname = 'mysub' + container.id;
        var sent = 0;
        var received = 0;
        var redirect = undefined;
        var s = new_connection().open_sender({target:{address:'mytopic', capabilities: ['topic']}});
        new_connection().open_receiver({name:subname, source:{
            address:'locate/mytopic',
            durable:2,
            capabilities: ['topic']
        }});
        container.on('accepted', function () {
            if (sent === 2) {
                new_connection('myclient').open_receiver({name:subname, source:{
                    address:redirect,
                    durable:2,
                    capabilities: ['topic']
                }});
            }
        });
        container.on('receiver_error', function (context) {
            assert.equal(context.receiver.error.condition, 'amqp:link:redirect');
            if (context.receiver.error.info) {
                redirect = context.receiver.error.info.address;
            } else {
                //workaround for DISPATCH-499
                redirect = context.receiver.error.description;
            }
            new_connection().open_receiver({name:subname, source:{
                address:redirect,
                durable:2,
                capabilities: ['topic']
            }});
        });
        container.on('receiver_open', function (context) {
            if (redirect !== undefined) {
                s.send({body:'test' + (++sent)});
            }
        });
        container.on('message', function (context) {
            assert.equal(context.message.body, 'test' + (++received));
            if (received === 1) {
                context.receiver.detach();
            } else if (received === 3) {
                context.receiver.close();
            }
        });
        container.on('receiver_close', function (context) {
            if (!context.receiver.error) {
                if (received === 3) {
                    done();
                } else {
                    s.send({body:'test' + (++sent)});
                }
                context.connection.close();
            }
        });
    });

    it('simple message routed subscription', function(done) {
        var sender = new_connection().open_sender({target:{address:'mytopic', capabilities: ['topic']}});
        var client = new_connection();
        var address = container.generate_uuid();
        var ctrl = client.open_sender('$subctrl');
        var recv = client.open_receiver(address);
        ctrl.send({to:'$subctrl', correlation_id:address, subject:'subscribe', application_properties:{'root_address':'mytopic'}});
        ctrl.once('accepted', function (context) {
            sender.send({body:'test1'});
        });
        recv.on('message', function (context) {
            assert.equal(context.message.body, 'test1');
            ctrl.send({to:'$subctrl', correlation_id:address, subject:'close', application_properties:{'root_address':'mytopic'}});
            ctrl.once('accepted', function (context) {
                done();
            });
        });
    });

});
