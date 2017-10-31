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

var assert = require('assert');
var fs = require('fs');
var path = require('path');

var rhea = require('rhea');
var as = require('../lib/auth_service.js');
var MockAuthService = require('../testlib/mock_authservice.js');

describe('auth service', function() {
    var auth_service;
    var auth_service_options;

    beforeEach(function(done) {
        auth_service = new MockAuthService();
        auth_service.listen().on('listening', function () {
            auth_service_options = {port: auth_service.port};
            done();
        });
    });

    afterEach(function(done) {
        auth_service.close(done);
    });

    it('accepts valid credentials', function(done) {
        as.authenticate({name:'bob', pass:'bob'}, auth_service_options).then(function () {
            done();
        });
    });
    it('rejects invalid credentials', function(done) {
        as.authenticate({name:'foo', pass:'bar'}, auth_service_options).catch(function () {
            done();
        });
    });
    it('anonymous', function(done) {
        as.authenticate(undefined, auth_service_options).then(function () {
            done();
        });
    });
    it('provides default options', function(done) {
        var env = {};
        env.AUTHENTICATION_SERVICE_HOST = 'foo';
        env.AUTHENTICATION_SERVICE_PORT = 1010;
        env.AUTH_SERVICE_CA = path.resolve(__dirname,'ca-cert.pem');
        var opts = as.default_options(env);
        assert.equal('foo', opts.host);
        assert.equal(1010, opts.port);
        assert.equal('tls', opts.transport);
        assert.equal(1, opts.ca.length);
        done();
    });
});
describe('auth service with tls', function() {
    var auth_service;
    var auth_service_options;

    beforeEach(function(done) {
        auth_service = new MockAuthService();
        var server_opts = {
            port: 0,
            transport:'tls',
            key: fs.readFileSync(path.resolve(__dirname, 'server-key.pem')),
            cert: fs.readFileSync(path.resolve(__dirname,'server-cert.pem'))
        };
        auth_service.listen(server_opts).on('listening', function () {
            auth_service_options = {
                port: auth_service.port,
                transport:'tls',
                ca: [ fs.readFileSync(path.resolve(__dirname,'ca-cert.pem')) ]
            };
            done();
        });
    });

    afterEach(function(done) {
        auth_service.close(done);
    });

    it('accepts valid credentials', function(done) {
        as.authenticate({name:'bob', pass:'bob'}, auth_service_options).then(function () {
            done();
        });
    });
    it('rejects invalid credentials', function(done) {
        as.authenticate({name:'foo', pass:'bar'}, auth_service_options).catch(function () {
            done();
        });
    });
    it('anonymous', function(done) {
        as.authenticate(undefined, auth_service_options).then(function () {
            done();
        });
    });
    it('works with default options', function(done) {
        var env = {};
        env.AUTHENTICATION_SERVICE_HOST = 'localhost';
        env.AUTHENTICATION_SERVICE_PORT = auth_service_options.port;
        env.AUTH_SERVICE_CA = path.resolve(__dirname,'ca-cert.pem');
        var default_options = as.default_options(env);
        as.authenticate({name:'bob', pass:'bob'}, default_options).then(function () {
            done();
        });
    });
});
