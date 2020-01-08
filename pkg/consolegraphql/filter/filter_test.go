/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package filter

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestParseFilterBooleanExprValid(t *testing.T) {

	testCases := []struct {
		expr  string
	}{
		{"1 = 1"},
		{"TRUE = TRUE"},
		{"FALSE = TRUE"},
		{"FALSE = TRUE AND TRUE = FALSE OR TRUE"},
		{"( TRUE = TRUE )"},
		{"( TRUE )"},
	}

	for _, tc := range testCases {
		_, err := ParseFilterExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case : %s", tc.expr)
	}
}

func TestParseFilterValueExprValid(t *testing.T) {

	testCases := []struct {
		expr  string
	}{
		{"1 > 1"},
		{"1 >= 1"},
		{"2 < 1"},
		{"2 != 1"},
		{"'a' LIKE 'b'"},
	}

	for _, tc := range testCases {
		_, err := ParseFilterExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case : %s", tc.expr)
	}
}

func TestFilterEval(t *testing.T) {

	obj := struct {
		FooStr string
		FooInt int
		FooInt32 int32
		FooInt64 int64
		FooUint uint
	}{"Bar", 10, 11, 12, 13}

	testCases := []struct {
		expr     string
		expected bool
	}{
		{"1 = 1", true},
		{"1 = 2", false},
		{"1 != 2", true},

		{"2 > 1", true},
		{"2 > 2", false},
		{"2 >= 1", true},
		{"2 >= 2", true},

		{"2 < 1", false},
		{"2 < 2", false},
		{"2 <= 1", false},
		{"2 <= 2", true},

		{"1.0 = 1.0", true},
		{"1.0 = 1.0", true},
		{"1.0 = 0.1", false},
		{"1.0 != 0.1", true},
		{"1.1 > 1.0", true},

		{"1 = 1.0", true},
		{"1.0 != 2", true},
		{"2.0 >= 1", true},
		{"-1.9 > -2.0", true},

		{"'A' = 'A'", true},
		{"'A' = 'B'", false},
		{"'A' != 'B'", true},

		{"'B' > 'A'", false},  // String comparison is restricted to = and !=.

		{"'a' LIKE 'a'", true},
		{"'a' LIKE 'b'", false},
		{"'a' LIKE ''", false},
		{"'a' LIKE '%'", true},

		{"'abcd' LIKE 'a%'", true},
		{"'abcd' LIKE 'a%d'", true},
		{"'abcdef' LIKE 'a%c%f'", true},

		{"'abcd' LIKE 'a__d'", true},
		{"'abcd' LIKE 'a_d'", false},

		{"'abcd' LIKE '.*'", false},
		{"'.*' LIKE '.*'", true},

		{"'a' NOT LIKE 'b'", true},

		{"NULL IS NULL", true},
		{"'a' IS NULL", false},
		{"'' IS NULL", false},
		{"0 IS NULL", false},
		{"'a' IS NOT NULL", true},

		{"TRUE = TRUE", true},
		{"TRUE = FALSE", false},
		{"TRUE != FALSE", true},
		{"TRUE", true},
		{"FALSE", false},

		{"NULL = NULL", false},
		{"NULL = 1", false},
		{"NULL = 'NULL'", false},

		{"FALSE AND FALSE", false},
		{"TRUE AND FALSE", false},
		{"FALSE AND TRUE", false},
		{"TRUE AND TRUE", true},

		{"FALSE OR FALSE", false},
		{"TRUE OR FALSE", true},
		{"TRUE OR TRUE", true},

		{"TRUE OR TRUE", true},

		{"NOT (TRUE)", false},
		{"NOT (FALSE)", true},

		{"`$.FooStr` = 'Bar'", true},
		{"`$.FooStr` != 'Bar'", false},
		{"`$.FooStr` = `$.FooStr`", true},

		{"`$.FooInt` = 10", true},
		{"`$.FooInt32` = 11", true},
		{"`$.FooInt64` = 12", true},
		{"`$.FooUint` = 13", true},

		{"`$.NonExistentNode` != 'Bar'", false},
		{"`$.FooStr.NonExistentSubNode` != 'Bar'", false},

		{"`$.FooInt` IS NOT NULL", true},
		{"`$.NonExistentNode` IS NULL", true},
		{"`$.FooStr.NonExistentSubNode` IS NULL", true},
	}

	for _, tc := range testCases {
		expr, err := ParseFilterExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case : %s", tc.expr)
		assert.NotNil(t, expr, "Expected an expression")
		if expr != nil {
			actual, _ := expr.Eval(obj)
			assert.Equal(t, tc.expected, actual, "Unexpected result for case : %s", tc.expr)
		}
	}
}
