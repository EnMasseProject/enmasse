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
const kubernetes = require('../lib/kubernetes.js');
const oauth2_factory = require('simple-oauth2');
const openid_connect = require('openid-client');

const rhea = require('rhea');
var log = require("./log.js").logger();
const myutils = require('./utils.js');

function my_request(original, defaults, options, callback) {
    var merge = myutils.merge({}, defaults, options);
    return original(merge, callback);
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

function expunge_session(sessions, request, response) {
    let id = get_cookies(request)[SESSION_ID];
    if (id === undefined || sessions[id] === undefined) {
        return;
    }
    delete sessions[id];
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

function step(interceptors, request, response, i, handler) {
    if (i < interceptors.length) {
        interceptors[i](request, response, step.bind(undefined, interceptors, request, response, i+1, handler));
    } else {
        handler(request, response);
    }
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

function websocket_auth(authz, env, get_credentials) {
    return function (request, ws_upgrade_completion_callback) {

        let credentials = get_credentials(request);
        let token = credentials.token ? credentials.token.getAccessToken() : null;

        var options = {"token": token};
        var onrejected = function (error) {
            log.error('Failed to authorize websocket: %s', error);
            ws_upgrade_completion_callback(false, 401, 'Authorization Required');
        };

        const namespace = env.ADDRESS_SPACE_NAMESPACE;
        kubernetes.self_subject_access_review(options, namespace,
            "list", "enmasse.io", "addresses").then(({allowed: allowed_list, reason: reason_list}) => {
            if (allowed_list) {
                kubernetes.self_subject_access_review(options, namespace,
                    "create", "enmasse.io", "addresses").then(({allowed: allowed_create, reason: reason_create}) => {
                    if (allowed_create) {
                        authz.set_authz_props(request, credentials, {admin: allowed_create, console: true});
                        ws_upgrade_completion_callback(true);
                    } else {
                        kubernetes.self_subject_access_review(options, namespace,
                            "delete", "enmasse.io", "addresses").then(({allowed: allowed_delete, reason: reason_delete}) => {
                            authz.set_authz_props(request, credentials, {admin: allowed_delete, console: true});
                            ws_upgrade_completion_callback(true);
                            if (!allowed_delete) {
                                log.info("User has neither create nor delete address permission, not granting console admin permission. [%j, %j]",
                                    reason_create, reason_delete);
                            }
                        }).catch(onrejected);
                    }
                }).catch(onrejected);
            } else {
                log.warn("User does not have list address permission, not granting console access. [%j]",
                    reason_list);
                authz.set_authz_props(request, credentials, {admin: false, console: false});
                ws_upgrade_completion_callback(true);
            }
        }).catch(onrejected);
    }
}

let sessions = {};

module.exports.ws_auth_handler = function (authz, env) {
    return websocket_auth(authz, env, restore_oauth_credentials.bind(null, sessions));
};

module.exports.init_auth_handler = function (openshift, env) {
    var discovery_uri = env.CONSOLE_OAUTH_DISCOVERY_URL;
    log.debug("Discovery url : %s", discovery_uri);
    if (openshift) {
        if (discovery_uri.startsWith("data:")) {
            return new Promise((resolve, reject) => {
                try {
                    var b64string = discovery_uri.replace(/^data:.*,/, '');
                    var data = Buffer.from(b64string, 'base64');
                    resolve(JSON.parse(data));
                } catch (e) {
                    reject(e);
                }
            });
        } else {
            let discoveryUri = url.parse(discovery_uri, false);
            return new Promise((resolve, reject) => {
                https.get({
                    hostname: discoveryUri.hostname,
                    port: discoveryUri.port,
                    path: discoveryUri.path,
                    protocol: discoveryUri.protocol,
                    rejectUnauthorized: false,
                }, (response) => {
                    log.info('GET %s => %s ', discovery_uri, response.statusCode);
                    response.setEncoding('utf8');
                    var data = '';
                    response.on('data', function (chunk) { data += chunk; });
                    response.on('end', function () {
                        if (response.statusCode === 200) {
                            try {
                                resolve(JSON.parse(data));
                            } catch (e) {
                                reject(new Error(util.format('Could not parse message as JSON (%s): %s', e, data)));
                            }
                        } else {
                            var error = new Error(util.format('Failed to retrieve %s: %s %s', discovery_uri, response.statusCode, data));
                            error.statusCode = response.statusCode;
                            reject(error);
                        }
                    });
                });
            });
        }
    } else {
        return openid_connect.Issuer.discover(discovery_uri);
    }
};


module.exports.auth_handler = function (authz, env, handler, auth_context, openshift) {

    set_defaults({rejectUnauthorized:false});

    setInterval(purge_stale_sessions.bind(null, sessions, 15*60*1000), 60*1000);

    var patch_handler =  function (request, response, next) {
        request.protocol = request.connection.encrypted ? "https" : "http";
        let u = url.parse(request.url, true);
        request.hostname = u.hostname || request.headers.host.split(':')[0];

        response.redirect = function (target) {
            log.info("Sending redirect: %s", target);
            response.statusCode = 302;
            response.setHeader('Location', target);
            response.end();
        };
        response.status = function (code) {
            response.statusCode = code;
            return response;
        };
        next();
    };

    var global_console_handler =  function (request, response, next) {
        let u = url.parse(request.url, true);
        if (u.pathname === "/console") {
            response.redirect(env.CONSOLE_LINK);
        } else {
            next();
        }
    };

    var init_session_handler =  function (request, response, next) {
        init_session(sessions, request, response);
        next();
    };

    var logout_handler =  function (request, response, next) {
        let u = url.parse(request.url, true);
        if (u.pathname === "/logout") {
            var token = get_from_session(request, "token");
            expunge_session(sessions, request, response);
            if (token) {
                token.revokeAll().then(() => {
                    response.redirect("/");
                }).catch((e) => {
                    log.warn("Failed to revoke access token");
                    response.redirect("/");
                })
            } else {
                response.redirect("/");
            }
        } else {
            next();
        }
    };

    var openidconnect_handler =  function (request, response, next) {
        if (!get_from_session(request, "token")) {

            let u = url.parse(request.url, true);
            if (u.pathname === "/authcallback" && u.query.code) {
                var saved_request_url = get_from_session(request, "saved_request_url");
                var saved_redirect_url = get_from_session(request, "saved_redirect_url");
                var state = get_from_session(request, "state");
                var client = get_from_session(request, "openid");

                client.authorizationCallback(saved_redirect_url, u.query, {state: state, code: 'code'})
                    .then(function (tokenSet) {
                        tokenSet.getAccessToken = function () {
                            return tokenSet.id_token;
                        };
                        tokenSet.revokeAll = function () {
                            return client.revoke(tokenSet);
                        };
                        store_in_session(request, "token", tokenSet);

                        response.redirect(saved_request_url);
                    })
                    .catch((error) => {
                        console.error('OpenID Connect  Error', error);
                        response.status(500).end('Authentication failed');
                    }).finally(() => {
                        remove_from_session(request, "openid");
                        remove_from_session(request, "state");
                        remove_from_session(request, "saved_request_url");
                        remove_from_session(request, "saved_redirect_url");
                });
            } else {
                const client = new auth_context.Client({
                    client_id: env.CONSOLE_OAUTH_CLIENT_ID,
                    client_secret: env.CONSOLE_OAUTH_CLIENT_SECRET,
                });

                let state = rhea.generate_uuid();
                let redirect_uri = request.protocol + "://" + request.headers.host + "/authcallback";
                store_in_session(request, "openid", client);
                store_in_session(request, "state", state);
                store_in_session(request, "saved_request_url", request.url);
                store_in_session(request, "saved_redirect_url", redirect_uri);

                const authorization_url = client.authorizationUrl({
                    redirect_uri: redirect_uri,
                    scope: env.CONSOLE_OAUTH_SCOPE,
                    state: state,
                    response_type: 'code'
                });

                // redirect
                response.redirect(authorization_url)
            }
        } else {
            next();
        }
    };

    var oauth_handler =  function (request, response, next) {
        if (!get_from_session(request, "token")) {
            let u = url.parse(request.url, true);
            if (u.pathname === "/authcallback" && u.query.code) {
                var saved_request_url = get_from_session(request, "saved_request_url");
                var saved_redirect_url = get_from_session(request, "saved_redirect_url");
                var oauth2 = get_from_session(request, "oauth2");

                const code = u.query.code;

                try {
                    const options = {
                        code: code,
                        redirect_uri: saved_redirect_url
                    };

                    oauth2.authorizationCode.getToken(options).then(
                        result => {
                            const token = oauth2.accessToken.create(result);
                            if (!token.getAccessToken) {
                                token.getAccessToken = function() {
                                    if (this.token && this.token.access_token) {
                                        return this.token.access_token;
                                    } else {
                                        return null;
                                    }

                                }
                            }
                            store_in_session(request, "token", token);

                            // TODO not getting a refresh_token from openshift - not sure why?
                            if (token.token.expires_in && token.token.refresh_token) {
                                var scheduleRefreshFunc = function() {
                                    var timeout = token.token.expires_in  / 2;
                                    log.info("Scheduling OAuth token refresh %ds", timeout);
                                    setTimeout(() => {
                                        log.info("OAuth token has timed out");
                                        token.refresh().then(newToken => {
                                            log.info("OAuth token refresh token complete");
                                            store_in_session(request, "token", newToken);
                                            scheduleRefreshFunc();
                                        }).catch((e) => {
                                            log.error("Failed to refresh token", e);
                                        });
                                    }, timeout * 1000)
                                };
                                scheduleRefreshFunc();
                            }
                            response.redirect(saved_request_url);
                        }
                    ).catch(error => {
                            console.error('Access Token Error', error.message);
                            response.status(500).end('Authentication failed');

                        }
                    ).finally(() => {
                        remove_from_session(request, "oauth2");
                        remove_from_session(request, "saved_request_url");
                        remove_from_session(request, "saved_redirect_url");

                    });
                } catch(error) {
                    console.error('Access Token Error', error.message);
                    response.status(500).end('Authentication failed');
                }
            } else {
                try {
                    const credentials = {
                        client: {
                            id: env.CONSOLE_OAUTH_CLIENT_ID,
                            secret: env.CONSOLE_OAUTH_CLIENT_SECRET,
                        },
                        auth: {
                            tokenHost: auth_context.issuer,
                            authorizePath: auth_context.authorization_endpoint,
                            tokenPath: auth_context.token_endpoint,
                        },
                        options: {
                            authorizationMethod: 'body'
                        },
                    };

                    let state = rhea.generate_uuid();
                    let oauth2 = oauth2_factory.create(credentials);
                    let redirect_uri = request.protocol + "://" + request.headers.host + "/authcallback";
                    store_in_session(request, "oauth2", oauth2);
                    store_in_session(request, "saved_request_url", request.url);
                    store_in_session(request, "saved_redirect_url", redirect_uri);

                    const authorization_url = oauth2.authorizationCode.authorizeURL({
                        redirect_uri: redirect_uri,
                        scope: env.CONSOLE_OAUTH_SCOPE,
                        state: state
                    });

                    // redirect
                    response.redirect(authorization_url)
                } catch (error) {
                    console.error('Authorization Error', error.message);
                    response.status(500).end('Authentication failed');
                }
            }
        } else {
            next();
        }
    };

    var get_user_handler =  function (request, response, next) {
        const token = get_from_session(request, "token");
        if (token && !get_from_session(request, "username") ) {
            kubernetes.whoami({token: token.getAccessToken()}).then((data) => {
                if (data.username) {
                    log.info("User identified as : %s", data.username);
                    store_in_session(request, 'username', data.username);
                }
            }).catch((e) => {
                log.warn("Failed to get username for authenticated user", e);
            }).finally(() => {
                next();
            });
        } else {
            next();
        }
    };

    var logginghandler =  function (request, response, next) {
        log.debug("Serving %s", request.url);
        next();
    };

    let interceptors = [];
    interceptors.push(patch_handler);
    if (env.CONSOLE_LINK) {
        interceptors.push(global_console_handler);
    }
    interceptors.push(init_session_handler);
    interceptors.push(logout_handler);
    interceptors.push(openshift ? oauth_handler : openidconnect_handler);
    if (openshift) {
        interceptors.push(get_user_handler);
    }
    interceptors.push(logginghandler);

    return function (request, response) {
        step(interceptors, request, response, 0, handler);
    };
}
