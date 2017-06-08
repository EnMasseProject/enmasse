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
    if (process.env.API_SERVER_HOSTNAME) {
        result.host = process.env.API_SERVER_HOSTNAME;
    } else if (process.env.ADDRESS_CONTROLLER_SERVICE_HOST) {
        result.host = process.env.ADDRESS_CONTROLLER_SERVICE_HOST;
        result.port = process.env.ADDRESS_CONTROLLER_SERVICE_PORT_HTTP;
    }
    return result;
}

var ctrlr = hostport({host:'localhost', port:8080});

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

function create_address(address) {
    var data = {
        "apiVersion": "v3",
        "kind": "Address",
        "metadata": {
            "name": address.address
        },
        "spec": {
            "store_and_forward": address.store_and_forward,
            "multicast": address.multicast,
        }
    };
    if (address.flavor) {
        data.spec.flavor = address.flavor;
    }
    if (address.group_id) {
        data.spec.group = address.group_id;
    }
    return request('/v3/address', 'POST', {'content-type': 'application/json'}, JSON.stringify(data));
}

function delete_address(address) {
    return request('/v3/address/' + address.address, 'DELETE');
}

function flavor_list(text) {
    var object = JSON.parse(text);
    if (object.kind === 'FlavorList') {
        var flavors = {};
        object.items.forEach(function(item) {
            flavors[item.metadata.name] = {
                'name':item.metadata.name,
                'type':item.spec.type,
                'description':item.spec.description
            }
        });
        return flavors;
    } else {
        throw new Error('Unexpected object kind: ' + object.kind);
    }
}

function get_flavors() {
    console.log('retrieving flavors from ' + JSON.stringify(ctrlr));
    return request('/v3/flavor/', 'GET', undefined, undefined, flavor_list);
}

module.exports.create_address = create_address;
module.exports.delete_address = delete_address;
module.exports.get_flavors = get_flavors;
