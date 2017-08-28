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

var path = require('path');
var fs = require('fs');

function get_client_options(config) {
    var options = config || {};
    var cert_dir = (process.env.CERT_DIR !== undefined) ? process.env.CERT_DIR : '/etc/enmasse-certs';
    var ca_path = path.resolve(cert_dir, 'ca.crt');//TODO: allow setting via env var
    var crt_path = path.resolve(cert_dir, 'tls.crt');//TODO: allow setting via env var
    var key_path = path.resolve(cert_dir, 'tls.key');//TODO: allow setting via env var
    options.ca = [fs.readFileSync(ca_path)];
    options.key = fs.readFileSync(key_path);
    options.cert = fs.readFileSync(crt_path);
    options.enable_sasl_external = true;
    options.transport = 'tls';
    options.rejectUnauthorized = false;
    return options;
}

function get_server_options(config) {
    var options = config || {};
    get_client_options(options);
    options.requestCert = true;
    options.rejectUnauthorized = true;
    return options;
}

module.exports.get_client_options = get_client_options;
module.exports.get_server_options = get_server_options;
