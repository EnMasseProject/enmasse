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

var child_process = require('child_process');
var Promise = require('bluebird');

function UserCtrl(sasldb, domain) {
    this.sasldb = sasldb;
    this.default_domain = domain;
    this.verbose = false;
}

UserCtrl.prototype.exec = function (cmd, args, input) {
    var verbose = this.verbose;
    if (verbose) console.log('exec: ' + cmd + ' ' + args.join(' '));
    var p = child_process.spawn(cmd, args, {stdio:'pipe'});
    var output = [];
    p.stdout.on('data', function (data) {
        output = output.concat(data.toString().split('\n'));
        if (verbose) console.log('stdout[' + cmd + ' ' + args.join(' ') + ']: ' + data);
    });
    p.stderr.on('data', function (data) {
        output.push(data.toString());
        if (verbose) console.warn('stderr[' + cmd + ' ' + args.join(' ') + ']: ' + data);
    });
    if (input) {
        p.stdin.write(input);
    }
    p.stdin.end();
    return new Promise(function (resolve, reject) {
        p.on('exit', function (code, signal) {
            if (code === 0) {
                resolve(output);
            } else {
                reject(new Error(output.join('\n')));
            }
        });
    });
};

UserCtrl.prototype.saslpasswd2 = function (flags, username, domain, input) {
    var cmd = 'saslpasswd2';
    var args = flags.concat(['-p','-f',this.sasldb,'-u', domain || this.default_domain, username]);
    return this.exec(cmd, args, input);
};

UserCtrl.prototype.create_user = function (username, password, domain) {
    return this.saslpasswd2(['-c'], username, domain, password);
};

UserCtrl.prototype.delete_user = function (username, domain) {
    return this.saslpasswd2(['-d'], username, domain);
};

UserCtrl.prototype.list_all_users = function () {
    return this.exec('sasldblistusers2', ['-f',this.sasldb]).then(function (output) {
        return output.filter(function (l) {return l.length; }).map(function (line) {
            var parts = line.split(':')[0].split('@');
            return {name: parts[0], domain:parts[1]};
        });
    });
};

UserCtrl.prototype.list_users = function (domain) {
    var d = domain || this.default_domain;
    return this.list_all_users().filter(function (u) { return u.domain === d; });
}

UserCtrl.prototype.list_usernames = function (domain) {
    return this.list_users(domain || this.default_domain).map(function (u) { return u.name; });
};

module.exports = UserCtrl;
