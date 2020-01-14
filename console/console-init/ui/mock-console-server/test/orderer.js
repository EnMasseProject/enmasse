/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const assert = require('assert');
const orderer = require('../orderer.js');

describe('order_by', function () {
    describe('single_column', function () {
        it('orders_values_asc', function () {
            var values = [
                {
                    foo: 3
                },
                {
                    foo: 1
                },
                {
                    foo: 2
                }
            ];
            var ordererfunc = orderer("`$.foo`");
            var results = values.sort(ordererfunc).map(v => v.foo);

            assert.deepStrictEqual([1,2,3], results);
        });
        it('orders_alpha_values_asc', function () {
            var values = [
                {
                    foo: 'cow'
                },
                {
                    foo: 'ant'
                },
                {
                    foo: 'bee'
                }
            ];
            var ordererfunc = orderer("`$.foo`");
            var results = values.sort(ordererfunc).map(v => v.foo);

            assert.deepStrictEqual(['ant','bee','cow'], results);
        });
        it('orders_values_desc', function () {
            var values = [
                {
                    foo: 3
                },
                {
                    foo: 1
                },
                {
                    foo: 2
                }
            ];
            var ordererfunc = orderer("`$.foo` DESC");
            var results = values.sort(ordererfunc).map(v => v.foo);

            assert.deepStrictEqual([3,2,1], results);
        });
        it('undefined_values_use_null_first', function () {
            var values = [
                {
                    foo: 'a'
                },
                {
                    foo: undefined
                },
                {
                    foo: 'c'
                },
                {
                    foo: 'b'
                },
                {
                    foo: undefined,
                }
            ];
            var ordererfunc = orderer("`$.foo`");
            var results = values.sort(ordererfunc).map(v => v.foo);

            assert.deepStrictEqual([undefined,undefined,'a','b','c'], results);
        });
    });
    describe('multiple_columns', function () {
        it('orders_values_asc', function () {
            var values = [
                {
                    foo: 2,
                    bar: 'a'
                },
                {
                    foo: 1,
                    bar: 'c'
                },
                {
                    foo: 1,
                    bar: 'b'
                }
            ];
            var ordererfunc = orderer("`$.foo`, `$.bar`");
            var results = values.sort(ordererfunc);

            var expected = [
                {
                    foo: 1,
                    bar: 'b'
                },
                {
                    foo: 1,
                    bar: 'c'
                },
                {
                    foo: 2,
                    bar: 'a'
                }
            ];

            assert.deepStrictEqual(expected, results);
        });
    })
});