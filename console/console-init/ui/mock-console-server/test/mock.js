/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const assert = require('assert');
const mock = require('../mock-console-server.js');

describe('mock', function () {
    describe('create_address', function () {
        it('with_resource_name', function () {
            var a = mock.createAddress({
                ObjectMeta: {
                    Name: "jupiter_as1.foo1",
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "fooaddr"
                }

            });

            assert.strictEqual(a.Name, "jupiter_as1.foo1");
        });
        it('with_address', function () {
            var a = mock.createAddress({
                ObjectMeta: {
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "foo2"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.Name, "jupiter_as1.foo2");
        });
        it('with_address_uppercase', function () {
            var a = mock.createAddress({
                ObjectMeta: {
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "FOO3"
                }

            }, "jupiter_as1");

            assert.strictEqual(a.Name, "jupiter_as1.foo3");
        });
        it('with_address_illegalchars', function () {
            var a = mock.createAddress({
                ObjectMeta: {
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "!foo4"
                }

            }, "jupiter_as1");

            assert.match(a.Name, /^jupiter_as1\.foo4\..*$/);
        });
    });
    describe('addressCommand', function () {
        it('with_resource_name', function () {
            var cmd = mock.addressCommand({
                ObjectMeta: {
                    Name: "jupiter_as1.bar1",
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "fooaddr"
                }

            });

            assert.match(cmd, /name: jupiter_as1.bar1/);
        });
        it('with_address', function () {
            var cmd = mock.addressCommand({
                ObjectMeta: {
                    Namespace: "app1_ns"
                },
                Spec: {
                    Type: "queue",
                    Plan: "standard-small-queue",
                    Address: "bar2"
                }

            }, "jupiter_as1");

            assert.match(cmd, /name: jupiter_as1.bar2/);
        });

    });
});