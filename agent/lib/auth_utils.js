/*
 * Copyright 2018 Red Hat Inc.
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

const https = require('https');
const url = require('url');
const Keycloak = require('keycloak-connect');
const rhea = require('rhea');
var auth_service = require('./auth_service.js');
var log = require("./log.js").logger();
const myutils = require('./utils.js');

function my_request(original, defaults, options, callback) {
    return original(myutils.merge({}, defaults, options), callback);
}

function set_defaults(defaults) {
    https.request = my_request.bind(undefined, https.request, defaults)
}

function set_cookie(response, name, value, options) {
    let cookie = name + '=' + value;
    for (let i = 0; options && i < options.length; i++) {
        cookie += '; ' + options[0];
    }
    if (response.cookies === undefined) {
        response.cookies = [cookie];
    } else {
        response.cookies.push(cookie);
    }
    response.setHeader('Set-Cookie', response.cookies);
}


function clear_cookie(response, name) {
    set_cookie(response, name, '', ['Expires ' + new Date().toUTCString()]);
}

function get_cookies(request) {
    let cookies = {};
    let header = request.headers.cookie;
    if (header) {
        let items = header.split(';');
        for (let i = 0; i < items.length; i++) {
            let parts = items[i].split('=');
            cookies[parts.shift().trim()] = decodeURI(parts.join('='));
        }
    }
    return cookies;
}

const SESSION_ID = 'session-id';

function init_session(sessions, request, response) {
    let id = get_cookies(request)[SESSION_ID];
    if (id === undefined || sessions[id] === undefined) {
        id = rhea.generate_uuid();
        sessions[id] = {};
        set_cookie(response, SESSION_ID, id);
        log.info('created session: %s', id);
    }
    request.session = sessions[id];
    touch_session(request.session);
    return request.session;
}

function restore_session(sessions, request) {
    let id = get_cookies(request)[SESSION_ID];
    request.session = sessions[id];
    if (request.session) {
        touch_session(request.session);
    } else {
        log.info('session not found: %s', id);
    }
    return request.session;
}

function touch_session(session) {
    session.last_used = Date.now();
}

function store_in_session(request, key, value) {
    request.session[key] = value;
}

function get_from_session(request, key) {
    return request.session ? request.session[key] : undefined;
}

function remove_from_session(request, key) {
    delete request.session[key];
}

function purge_stale_sessions(sessions, max_idle_time) {
    for (let id in sessions) {
        if ((Date.now() - sessions[id].last_used) > max_idle_time) {
            log.info('Deleting stale session %s', id);
            delete sessions[id];
        }
    }
}

function patch(sessions) {
    return function (request, response, next) {
        let u = url.parse(request.url, true);
        request.query = u.query;
        request.protocol = 'https';
        request.hostname = u.hostname || request.headers.host.split(':')[0];
        init_session(sessions, request, response);
        response.redirect = function (target) {
            response.statusCode = 302;
            response.setHeader('Location', target);
            response.end();
        };
        response.status = function (code) {
            response.statusCode = code;
            return response;
        };
        next();
    }
};

function step(interceptors, request, response, i, handler) {
    if (i < interceptors.length) {
        interceptors[i](request, response, step.bind(undefined, interceptors, request, response, i+1, handler));
    } else {
        handler(request, response);
    }
}

const TOKEN_KEY = 'keycloak-token';

function store_grant(grant) {
    return function (request, response) {
        store_in_session(request, TOKEN_KEY, grant.__raw);
    };
};

function unstore_grant(request, response) {
    remove_from_session(request, TOKEN_KEY);
};

const SessionStore = {};

SessionStore.get = function (request) {
    return get_from_session(request, TOKEN_KEY);
};

SessionStore.wrap = (grant) => {
    grant.store = store_grant(grant);
    grant.unstore = unstore_grant;
};


function get_keycloak_auth_url (env) {
    let u = 'https://' + (env.AUTHENTICATION_SERVICE_HOST || 'localhost');
    if (env.AUTHENTICATION_SERVICE_PORT_HTTPS) {
        u += ':' + env.AUTHENTICATION_SERVICE_PORT_HTTPS;
    }
    u += '/auth';
    return u;
}

function record_token(token, request) {
    store_in_session(request, 'token', token.token);
    store_in_session(request, 'username', token.content.preferred_username);
    return true;
}

function get_oauth_credentials(request) {
    return {
        username: get_from_session(request, 'username'),
        token: get_from_session(request, 'token')
    };
}

function restore_oauth_credentials(sessions, request) {
    restore_session(sessions, request);
    return get_oauth_credentials(request);
}

function auth_failed(response, error) {
    if (error) log.error('Failed to authenticate: %s', error);
    response.statusCode = 500;
    response.end('Failed to authenticate: ' + error);
}

function auth_required(response, error) {
    if (error) log.error('Failed to authenticate http request: %s', error);
    response.setHeader('WWW-Authenticate', 'Basic realm=Authorization Required');
    response.statusCode = 401;
    response.end('Authorization Required');
}

function authenticate(authz, env, get_credentials, failed) {
    return function (request, response, next) {
        try {
            let credentials = get_credentials(request);
            auth_service.authenticate(credentials, auth_service.default_options(env)).then(function (properties) {
                if (authz.access_console(properties)) {
                    next();
                } else {
                    response.statusCode = 403;
                    response.end('You do not have permission to view the console');
                }
            }).catch(failed.bind(null, response));
        } catch (error) {
            response.statusCode = 500;
            response.end(error.message);
        }
    };
}

function websocket_auth(authz, env, get_credentials) {
    return function (request, callback) {
        let credentials = get_credentials(request);
        auth_service.authenticate(credentials, auth_service.default_options(env)).then(function (properties) {
            authz.set_authz_props(request, credentials, properties);
            if (authz.access_console(properties)) {
                callback(true);
            } else {
                log.error('Access to console denied to %s [%j]', credentials.name, properties);
                callback(false, 403, 'You do not have permission to use this console');
            }
        }).catch(function (error) {
            log.error('Failed to authorize websocket: %s', error);
            callback(false, 401, 'Authorization Required');
        });
    }
}

function use_oauth(env) {
    return env.AUTHENTICATION_SERVICE_OAUTH_URL;
}

let sessions = {};

module.exports.ws_auth_handler = function (authz, env) {
    return websocket_auth(authz, env, use_oauth(env) ? restore_oauth_credentials.bind(null, sessions) : myutils.basic_auth);
}

module.exports.auth_handler = function (authz, env, handler) {
    if (use_oauth(env)) {
        set_defaults({rejectUnauthorized:false});
        let keycloak_config = {
            "realm": env.AUTHENTICATION_SERVICE_SASL_INIT_HOST,
            "auth-server-url": env.AUTHENTICATION_SERVICE_OAUTH_URL || get_keycloak_auth_url(env),
            "ssl-required": "external",
            "resource": "enmasse-console",
            "public-client": true,
            "confidential-port": 0
        };
        let keycloak = new Keycloak({}, keycloak_config);
        keycloak.stores.push(SessionStore);
        setTimeout(purge_stale_sessions.bind(null, sessions, 5*60*1000), 60*1000);
        let interceptors = [patch(sessions)].concat(keycloak.middleware()).concat([keycloak.protect(record_token), authenticate(authz, env, get_oauth_credentials, auth_failed)]);
        return function (request, response) {
            step(interceptors, request, response, 0, handler);
        };
    } else {
        let interceptors = [authenticate(authz, env, myutils.basic_auth, auth_required)];
        return function (request, response) {
            step(interceptors, request, response, 0, handler);
        };
    }
}
