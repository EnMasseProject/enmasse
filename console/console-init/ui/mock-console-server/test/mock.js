/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const assert = require('assert');
const mock = require('../mock-console-server.js');

describe('mock', function () {
    describe('create_address', function () {
        it('with_resource_name', function () {
            var a = mock.createAddress({
                metadata: {
                    name: "jupiter_as1.foo1",
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "fooaddr"
                }

            });

            assert.strictEqual(a.name, "jupiter_as1.foo1");
        });
        it('with_address', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "foo2"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.name, "jupiter_as1.foo2");
        });
        it('with_address_uppercase', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "FOO3"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.name, "jupiter_as1.foo3");
        });
        it('with_address_illegalchars', function () {
            var a = mock.createAddress({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "!foo4"
                }

            }, "jupiter_as1");

            assert.match(a.name, /^jupiter_as1\.foo4\..*$/);
        });
    });
    describe('patchAddress', function () {
        it('known_plan', function () {
            var result = mock.patchAddress({
                    name: "jupiter_as1.io",
                    namespace: "app1_ns"
                },
                "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-small-queue\"}]",
                "application/json-patch+json"
            );

            assert.strictEqual(result.spec.plan.metadata.name, "standard-small-queue");
        });
    });

    describe('addressCommand', function () {
        it('with_resource_name', function () {
            var cmd = mock.addressCommand({
                metadata: {
                    name: "jupiter_as1.bar1",
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "fooaddr"
                }

            });

            assert.match(cmd, /name: jupiter_as1.bar1/);
        });
        it('with_address', function () {
            var cmd = mock.addressCommand({
                metadata: {
                    namespace: "app1_ns"
                },
                spec: {
                    type: "queue",
                    plan: "standard-small-queue",
                    address: "bar2"
                }

            }, "jupiter_as1");

            assert.match(cmd, /name: jupiter_as1.bar2/);
        });

    });
    describe('patchAddressSpace', function () {
        it('known_plan', function () {
            var result = mock.patchAddressSpace({
                    name: "jupiter_as1",
                    namespace: "app1_ns"
                },
                "[{\"op\":\"replace\",\"path\":\"/spec/plan\",\"value\":\"standard-small\"}]",
                "application/json-patch+json"
            );

            assert.strictEqual(result.spec.plan.metadata.name, "standard-small");
        });
    });
});