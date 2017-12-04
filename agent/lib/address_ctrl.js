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

var fs = require("fs");

var AddressCtrl = function (host, port, ca, auth_string, rejectUnauthorized) {
    this.host = host;
    this.port = port;
    this.address_space = process.env.ADDRESS_SPACE
    this.http = ca ? require('https') : require('http');
    this.ca = ca;
    this.auth_string = auth_string;

    this.addr_path = "/apis/enmasse.io/v1/addresses/";
    if (this.address_space) {
        this.addr_path += this.address_space + "/";
    }
    this.rejectUnauthorized = rejectUnauthorized;
}

AddressCtrl.prototype.request = function (addr_path, method, headers, body, handler) {
    var self = this;
    return new Promise(function (resolve, reject) {
        var options = {
            hostname: self.host,
            port: self.port
        };
        options.path = addr_path;
        options.method = method || 'GET';
        options.headers = headers || {};
        if (self.rejectUnauthorized !== undefined) options.rejectUnauthorized = self.rejectUnauthorized;

        if (self.auth_string) {
            options.headers['Authorization'] = self.auth_string
        }

        if (self.ca) {
            options.ca = self.ca;
        }

        var req = self.http.request(options, function(res) {
            if (res.statusCode === 200) {
                if (handler) {
                    var text = '';
                    res.setEncoding('utf8');
                    res.on('data', function (chunk) {
                        text += chunk;
                    });
                    res.on('end', function () {
                        try {
                            resolve(handler(text));
                        } catch (e) {
                            reject(e);
                        }
                    });
                } else {
                    resolve();
                }
            } else {
                var text = 'Error: ' + res.statusCode + ' for ' + options.method + ' on ' + options.path;
                console.error(text);
                text += ' ';
                res.setEncoding('utf8');
                res.on('data', function (chunk) {
                    text += chunk;
                });
                res.on('end', function () {
                    reject(new Error(text));
                });
            }
        });
        if (body) {
            req.write(body);
        }
        req.on('error', function (error) { reject(error); });
        req.end();
    });
}

AddressCtrl.prototype.create_address = function (address) {
    var data = {
        "apiVersion": "enmasse.io/v1",
        "kind": "AddressList",
        "items" : [
            {
                "metadata": {
                    "name": address.address
                },
                "spec": {
                    "address": address.address,
                    "type": address.type,
                    "plan": address.plan
                }
            }
        ]
    };
    return this.request(this.addr_path, 'POST', {'content-type': 'application/json'}, JSON.stringify(data));
}

AddressCtrl.prototype.delete_address = function(address) {
    return this.request(this.addr_path + encodeURIComponent(address.address), 'DELETE');
}

function index(list) {
    return list.reduce(function (map, item) { map[item.name] = item; return map; }, {});
}

function address_types(text) {
    var object = JSON.parse(text);
    if (object.kind === 'Schema') {
        var address_space_type = process.env.ADDRESS_SPACE_TYPE || 'standard';
        return index(object.spec.addressSpaceTypes)[address_space_type].addressTypes;
    } else {
        throw new Error('Unexpected object kind: ' + object.kind);
    }
}

AddressCtrl.prototype.get_address_types = function () {
    return this.request('/apis/enmasse.io/v1/schema/', 'GET', undefined, undefined, address_types);
}

module.exports.create = function (env) {
    var host = 'localhost';
    var port;
    var rejectUnauthorized;
    if (env.ADDRESS_SPACE_SERVICE_HOST) {
        host = env.ADDRESS_SPACE_SERVICE_HOST;
    } else if (env.ADDRESS_CONTROLLER_SERVICE_HOST) {
        host = env.ADDRESS_CONTROLLER_SERVICE_HOST;
        rejectUnauthorized = false;
    }
    if (env.ADDRESS_CONTROLLER_SERVICE_HOST && env.ADDRESS_CONTROLLER_SERVICE_PORT_HTTPS) {
        port = env.ADDRESS_CONTROLLER_SERVICE_PORT_HTTPS;
    }
    var ca = undefined;
    if (env.ADDRESS_CONTROLLER_CA) {
        ca = fs.readFileSync(env.ADDRESS_CONTROLLER_CA);
        if (port === undefined) port = 443;
    }
    if (port === undefined) port = 8080;

    var auth_string = 'Bearer ' + (env.KUBERNETES_TOKEN || fs.readFileSync('/var/run/secrets/kubernetes.io/serviceaccount/token'));

    return new AddressCtrl(host, port, ca, auth_string, rejectUnauthorized);
};
