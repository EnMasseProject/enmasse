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

var http = require('http');
var Promise = require('bluebird');

function hostport(defaults) {
    var result = defaults;
    if (process.env.ADDRESS_SPACE_SERVICE_HOST) {
        result.host = process.env.ADDRESS_SPACE_SERVICE_HOST;
    } else if (process.env.ADDRESS_CONTROLLER_SERVICE_HOST) {
        result.host = process.env.ADDRESS_CONTROLLER_SERVICE_HOST;
        if (process.env.ADDRESS_CONTROLLER_SERVICE_PORT_HTTP) {
            result.port = process.env.ADDRESS_CONTROLLER_SERVICE_PORT_HTTP;
        }
    }
    return result;
}

function address_path() {
    var path = "/v1/addresses/";
    if (process.env.ADDRESS_SPACE) {
        path += process.env.ADDRESS_SPACE + "/";
    }
    return path;
}

var ctrlr = hostport({host:'localhost', port:8080});
var addr_path = address_path();

function request(path, method, headers, body, handler) {
    return new Promise(function (resolve, reject) {
        var options = {
            hostname: ctrlr.host,
            port: ctrlr.port
        };
        options.path = path;
        options.method = method || 'GET';
        if (headers) {
            options.headers = headers;
        }

        var req = http.request(options, function(res) {
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
        req.on('error', function(e) {
            console.error('Error: ' + e.message + ' for ' + options.method + ' on ' + options.path);
            reject(e);
        });
        if (body) {
            req.write(body);
        }
        req.end();
    });
}

function get_type(address) {
    if (address.store_and_forward) {
        if (address.multicast) {
            return 'topic';
        } else {
            return 'queue';
        }
    } else {
        if (address.multicast) {
            return 'multicast';
        } else {
            return 'anycast';
        }
    }
}

function create_address(address) {
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
    return request(addr_path, 'POST', {'content-type': 'application/json'}, JSON.stringify(data));
}

function delete_address(address) {
    return request(addr_path + address.address, 'DELETE');
}

function index(list) {
    return list.reduce(function (map, item) { map[item.name] = item; return map; }, {});
}

function address_types(text) {
    var object = JSON.parse(text);
    if (object.kind === 'Schema') {
        //TODO: retrieve type for current address-space (assume 'standard' for now)
        return index(object.spec.addressSpaceTypes)['standard'].addressTypes;
    } else {
        throw new Error('Unexpected object kind: ' + object.kind);
    }
}

function get_address_types() {
    return request('/v1/schema/', 'GET', undefined, undefined, address_types);
}

module.exports.create_address = create_address;
module.exports.delete_address = delete_address;
module.exports.get_address_types = get_address_types;
