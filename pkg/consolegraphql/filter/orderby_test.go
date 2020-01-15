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

func TestParseOrderBy(t *testing.T) {
	testCases := []struct {
		expr  string
		num   int
	}{
		{"", 0},
		{"`$.Foo`", 1},
		{"`$.Foo` ASC", 1},
		{"`$.Foo` DESC, `$.Boo`", 2},
	}

	for _, tc := range testCases {
		orderBy, err := ParseOrderByExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case : %s", tc.expr)
		assert.Equal(t, tc.num, len(orderBy), "Unexpected number of order clauses for case : %s", tc.expr)
	}
}


func TestEvalOrderBy(t *testing.T) {
	type Inner struct {
		Bif int
	}
	type Outer struct {
		Bar *Inner
	}

	testCases := []struct {
		name string
		expr  string
		data interface{}
		expected interface{}
	}{
		{"alpha order",
			"`$.Foo`",
			[]struct{Foo string } {{"a"}, {"v"}, {"b"}, {"z"},},
			[]struct {Foo string }{{"a"}, {"b"}, {"v"}, {"z"},},
		},
		{"alpha order - explicit ASC",
			"`$.Foo` ASC",
			[]struct{Foo string } {{"a"}, {"v"}, {"b"}, {"z"},},
			[]struct {Foo string }{{"a"}, {"b"}, {"v"}, {"z"},},
		},
		{"alpha order - DESC",
			"`$.Foo` DESC",
			[]struct{Foo string } {{"a"}, {"v"}, {"b"}, {"z"},},
			[]struct {Foo string }{{"z"}, {"v"}, {"b"}, {"a"},},
		},
		{"int order - DESC",
			"`$.Foo` DESC",
			[]struct{Foo int } {{4}, {2}, {8}, {1},},
			[]struct {Foo int }{{8}, {4}, {2}, {1},},
		},
		{"float order",
			"`$.Foo`",
			[]struct{Foo float64 } {{4}, {2}, {8}, {1},},
			[]struct {Foo float64 }{{1}, {2}, {4}, {8},},
		},
		{"uint order",
			"`$.Foo`",
			[]struct{Foo uint } {{4}, {2}, {8}, {1},},
			[]struct {Foo uint }{{1}, {2}, {4}, {8},},
		},
		{"nested",
			"`$.Bar.Bif`",
			[]Outer {{&Inner{4}}, {&Inner{2}}, {&Inner{8}}, {&Inner{1}},},
			[]Outer{{&Inner{1}}, {&Inner{2}}, {&Inner{4}}, {&Inner{8}},},
		},
		{"nested - null first semantics",
			"`$.Bar.Bif`",
			[]Outer {{&Inner{4}}, {nil}, {&Inner{8}}, {&Inner{1}},},
			[]Outer{{nil}, {&Inner{1}}, {&Inner{4}}, {&Inner{8}},},
		},
		{"two column ordering",
			"`$.Foo`, `$.Inner`",
			[]struct{
				Foo string
				Baz int
			} {
				{"a",   1},
				{"v",   4},
				{"a",   2},
				{"z",   9},
			},
			[]struct {
				Foo string
				Baz int
			}{
				{"a",   1},
				{"a",   2},
				{"v",   4},
				{"z",   9},
			}},
	}

	for _, tc := range testCases {
		orderBy, err := ParseOrderByExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case %s : %s", tc.name, tc.expr)
		err = orderBy.Sort(tc.data)
		assert.NoErrorf(t, err, "Unexpected error for case %s : %s", tc.name, tc.expr)
		assert.Equal(t, tc.expected, tc.data, "Unexpected results ordering for case %s : %s", tc.name, tc.expr)
	}
}
