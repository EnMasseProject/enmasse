/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

const assert = require('assert');
const parser = require('../filter_parser.js');

describe('filter_parser', function () {
    describe('parse_and_eval', function () {
        it('equality', function () {
            assert.strictEqual(parser.parse("'a' = 'a'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'a' = 'b'")
                .evaluate({}), false);
        });
        it('inequality', function () {
            assert.strictEqual(parser.parse("'a' != 'a'")
                .evaluate({}), false);
            assert.strictEqual(parser.parse("'a' != 'b'")
                .evaluate({}), true);
        });
        it('greaterThanEquals', function () {
            assert.strictEqual(parser.parse("'abc' >= 'abc'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abd' >= 'abc'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' >= 'abd'")
                .evaluate({}), false);
        });
        it('lessThanEquals', function () {
            assert.strictEqual(parser.parse("'abc' <= 'abc'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' <= 'abd'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abd' <= 'abc'")
                .evaluate({}), false);
        });
        it('greaterThan', function () {
            assert.strictEqual(parser.parse("'abc' > 'abc'")
                .evaluate({}), false);
            assert.strictEqual(parser.parse("'abd' > 'abc'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' > 'abd'")
                .evaluate({}), false);
        });
        it('lessThan', function () {
            assert.strictEqual(parser.parse("'abc' < 'abc'")
                .evaluate({}), false);
            assert.strictEqual(parser.parse("'abc' < 'abd'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abd' < 'abc'")
                .evaluate({}), false);
        });

        it('unary', function () {
            assert.strictEqual(parser.parse("not('a' != 'a')")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("not('a' != 'b')")
                .evaluate({}), false);
        });

        it('boolean and', function () {
          assert.strictEqual(parser.parse("'a' = 'a' AND 'b' = 'b'")
              .evaluate({}), true);
          assert.strictEqual(parser.parse("'a' = 'a' AND 'b' = 'c'")
              .evaluate({}), false);
        });

        it('boolean or', function () {
          assert.strictEqual(parser.parse("'a' = 'a' OR 'b' = 'b'")
              .evaluate({}), true);
          assert.strictEqual(parser.parse("'a' = 'b' OR 'b' = 'b'")
              .evaluate({}), true);
          assert.strictEqual(parser.parse("'a' = 'a' OR 'b' = 'c'")
              .evaluate({}), true);
        });

        it('parentheses', function () {
            assert.strictEqual(parser.parse("'a' = 'a' AND ('a' = 'b' OR 'a' = 'a')")
                .evaluate({}), true);
        });

        it('like%', function () {
            assert.strictEqual(parser.parse("'a' like 'a'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'ab' like 'a%'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'ab' like 'ab%'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'ab' like '%'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'ba' like 'a%'")
                .evaluate({}), false);
            assert.strictEqual(parser.parse("'%' like '%'")
                .evaluate({}), true);
        });

        it('like_', function () {
            assert.strictEqual(parser.parse("'a' like '_'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' like '_bc'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' like 'a_c'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' like 'ab_'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' like '__c'")
                .evaluate({}), true);
            assert.strictEqual(parser.parse("'abc' like '_c'")
                .evaluate({}), false);
            assert.strictEqual(parser.parse("'_' like '_'")
                .evaluate({}), true);
        });

        it("jsonpath expr", function() {
            assert.strictEqual(parser.parse("`$.foo` = 'bar'")
                .evaluate({
                    foo: "bar"
                }), true);
            assert.strictEqual(parser.parse("`$.foo` = 'bar'")
                .evaluate({
                    foo: "baz"
                }), false);
        })

    });
});
