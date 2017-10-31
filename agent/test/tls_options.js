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
var path = require('path');
var tls_options = require('../lib/tls_options.js');

describe('tls options', function() {
    it('uses correct default path for ca', function (done) {
        assert.equal(tls_options.get_paths().ca, '/etc/enmasse-certs/ca.crt');
        done();
    });
    it('uses correct default path for cert', function (done) {
        assert.equal(tls_options.get_paths().cert, '/etc/enmasse-certs/tls.crt');
        done();
    });
    it('uses correct default path for key', function (done) {
        assert.equal(tls_options.get_paths().key, '/etc/enmasse-certs/tls.key');
        done();
    });
    it('uses CERT_DIR in determining path for ca', function (done) {
        process.env.CERT_DIR = '/tmp/foo';
        assert.equal(tls_options.get_paths().ca, '/tmp/foo/ca.crt');
        done();
    });
    it('uses CERT_DIR in determining path for cert', function (done) {
        process.env.CERT_DIR = '/tmp/foo';
        assert.equal(tls_options.get_paths().cert, '/tmp/foo/tls.crt');
        done();
    });
    it('uses CERT_DIR in determining path for key', function (done) {
        process.env.CERT_DIR = '/tmp/foo';
        assert.equal(tls_options.get_paths().key, '/tmp/foo/tls.key');
        done();
    });
    it('uses CA_PATH as path for ca', function (done) {
        process.env.CA_PATH = '/x/y/ca.pem';
        assert.equal(tls_options.get_paths().ca, '/x/y/ca.pem');
        done();
    });
    it('uses CERT_PATH as path for cert', function (done) {
        process.env.CERT_PATH = '/foo/bar/cert.pem';
        assert.equal(tls_options.get_paths().cert, '/foo/bar/cert.pem');
        done();
    });
    it('uses KEY_PATH as path for key', function (done) {
        process.env.KEY_PATH = '/blah/blah/key.pem';
        assert.equal(tls_options.get_paths().key, '/blah/blah/key.pem');
        done();
    });
    it('retrieves client options', function (done) {
        process.env.CA_PATH = path.resolve(__dirname,'ca-cert.pem');
        process.env.CERT_PATH = path.resolve(__dirname, 'server-cert.pem');
        process.env.KEY_PATH = path.resolve(__dirname, 'server-key.pem');
        var options = tls_options.get_client_options();
        assert.equal(options.transport, 'tls');
        assert.equal(options.rejectUnauthorized, false);
        done();
    });
    it('retrieves server options', function (done) {
        process.env.CA_PATH = path.resolve(__dirname,'ca-cert.pem');
        process.env.CERT_PATH = path.resolve(__dirname, 'server-cert.pem');
        process.env.KEY_PATH = path.resolve(__dirname, 'server-key.pem');
        var options = tls_options.get_server_options();
        assert.equal(options.transport, 'tls');
        assert.equal(options.rejectUnauthorized, true);
        assert.equal(options.requestCert, true);
        done();
    });
});
