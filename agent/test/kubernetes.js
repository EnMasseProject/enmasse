/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

var assert = require('assert');
var kubernetes = require('../lib/kubernetes.js');
var AddressServer = require('../testlib/mock_resource_server.js').AddressServer;


describe('kubernetes interaction', function () {

    function createAddr() {
        address_server.add_address_definition({address: 's1.addr1', type: 'queue'}, "addr1", '1234');
    }

    const addr = {
        kind: 'Address',
        metadata: {labels: {infraUuid: '1234'}, name: 'addr1'},
        spec: {address: 's1.addr1', type: 'queue'},
        status: {phase: 'Active'}
    };
    var address_server;
    var watcher;
    var options;

    beforeEach(function (done) {
        address_server = new AddressServer();
        address_server.listen(0, () => {
            options = {namespace: "ns", token: "tok", host: "localhost", port: address_server.port};
            done();
        });
    });

    afterEach(function (done) {
        if (!watcher) {
            watcher = {
                close: () => {
                    return Promise.resolve();
                }
            }
        }
        watcher.close().finally(() => address_server.close(done));
    });

    it('no population', function (done) {
        try {
            watcher = kubernetes.watch("addresses", options);
            watcher.list();
            watcher.on("updated", (objs) => {
                assert.deepEqual(objs, []);
                done();
            });
        } catch (e) {
            done(e);
        }
    });

    it('initial population', function (done) {
        try {
            createAddr();

            watcher = kubernetes.watch("addresses", options);
            watcher.list();
            watcher.on("updated", (objs) => {
                assert.deepEqual(objs, [addr]);
                done();
            });
        } catch (e) {
            done(e);
        }
    });

    it('new member added', function (done) {
        try {
            watcher = kubernetes.watch("addresses", options);

            watcher.once("updated", (objs) => {
                assert.deepEqual(objs, []);

                process.nextTick(() => {
                    watcher.once("updated", objs => {
                        assert.deepEqual(objs, [addr]);
                        done();
                    });
                    createAddr();
                });
            });
            watcher.list();
        } catch (e) {
            done(e);
        }
    });

    it('watcher recovers from 500', function (done) {
        var count = 0;

        address_server.failure_injector = {
            match: (request) => {
                var match = request.watch === true && count === 0;
                if (match) {
                    count++;
                    process.nextTick(() => {
                        createAddr();
                    });
                }
                return match;
            },
            code: () => {
                return 500;
            }
        };
        try {
            watcher = kubernetes.watch("addresses", options);

            watcher.on("updated",  (objs) => {
                if (objs.length === 1) {
                    done();
                }
            });

            watcher.list();
        } catch (e) {
            done(e);
        }
    });

    it('watcher recovers from connection lost', function (done) {
        try {
            watcher = kubernetes.watch("addresses", options);

            watcher.once("updated", () => {
                address_server.close(() => {
                    address_server.listen(options.port, () => {
                        createAddr();
                    });
                });

                watcher.on("updated", (objs) => {
                    if (objs.length === 1) {
                        done();
                    }
                });
            });

            watcher.list();
        } catch (e) {
            done(e);
        }
    });
});
