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
var container = require('rhea');
var fs = require('fs');
var path = require('path');
var cert_dir = (process.env.CERT_DIR !== undefined) ? process.env.CERT_DIR : "/opt/none-authservice/cert";

function authenticate(username, password) {
    console.log('Authenticating as ' + username);
    return true;
}
container.sasl_server_mechanisms.enable_plain(authenticate);
container.sasl_server_mechanisms.enable_anonymous();
var server = container.listen({
    port: process.env.LISTENPORT,
    require_sasl: true,
    transport: 'tls',
    key: fs.readFileSync(path.resolve(cert_dir, 'tls.key')),
    cert: fs.readFileSync(path.resolve(cert_dir, 'tls.crt'))
});

console.log('Listening on port ' + process.env.LISTENPORT);
container.on('connection_open', function (context) {
    var authenticatedIdentity = { 'sub' : context.connection.sasl_transport.username || 'anonymous' };
    var groups = [ "manage" ];
    var properties = context.connection.local.open.properties || {};
    properties["authenticated-identity"] = authenticatedIdentity;
    properties["groups"] = groups;
    context.connection.local.open.properties = properties;
    context.connection.close();
});
container.on('disconnected', function (context) {
});
