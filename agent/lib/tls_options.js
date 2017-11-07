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

var path = require('path');
var fs = require('fs');

function get_paths(env) {
    var opts = env || process.env;
    var cert_dir = opts.CERT_DIR || '/etc/enmasse-certs';
    var paths = {};
    paths.ca = opts.CA_PATH || path.resolve(cert_dir, 'ca.crt');
    paths.cert = opts.CERT_PATH || path.resolve(cert_dir, 'tls.crt');
    paths.key = opts.KEY_PATH || path.resolve(cert_dir, 'tls.key');
    return paths;
}

function get_client_options(config, env) {
    var options = config || {};
    var paths = get_paths(env);
    options.ca = [fs.readFileSync(paths.ca)];
    options.key = fs.readFileSync(paths.key);
    options.cert = fs.readFileSync(paths.cert);
    options.enable_sasl_external = true;
    options.transport = 'tls';
    options.rejectUnauthorized = false;
    return options;
}

function get_server_options(config, env) {
    var options = config || {};
    get_client_options(options, env);
    options.requestCert = true;
    options.rejectUnauthorized = true;
    return options;
}

function get_console_paths(env) {
    var opts = env || process.env;
    var cert_dir = opts.CONSOLE_CERT_DIR || '/etc/console-certs';
    var paths = {};
    paths.cert = opts.CONSOLE_CERT_PATH || path.resolve(cert_dir, 'tls.crt');
    paths.key = opts.CONSOLE_KEY_PATH || path.resolve(cert_dir, 'tls.key');
    return paths;
}

function get_console_server_options(config, env) {
    var options = config || {};
    var paths = get_console_paths(env);
    options.key = fs.readFileSync(paths.key);
    options.cert = fs.readFileSync(paths.cert);
    options.transport = 'tls';
    return options;
}

module.exports.get_client_options = get_client_options;
module.exports.get_server_options = get_server_options;
module.exports.get_console_server_options = get_console_server_options;
module.exports.get_paths = get_paths;
