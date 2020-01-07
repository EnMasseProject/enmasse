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

func TestLex(t *testing.T) {
	testCases := []struct {
		expr  string
		expected []int

	}{
		{"1", []int{INTEGRAL}},
		{"1.0", []int{FLOAT}},

		{"-1", []int{INTEGRAL}},
		{"-1.0", []int{FLOAT}},

		{"'A'", []int{STRING}},
		{"'AB'", []int{STRING}},
		{"''", []int{STRING}},
		{"TRUE", []int{TRUE}},
		{"NULL", []int{NULL}},
		{"=", []int{'='}},
		{">", []int{'>'}},
		{">=", []int{GE}},
		{"<", []int{'<'}},
		{"<=", []int{LE}},
		{"!=", []int{NE}},
		{"1 = 1", []int{INTEGRAL, '=', INTEGRAL}},
		{"TRUE AND FALSE", []int{TRUE, AND, FALSE}},
		{"( TRUE )", []int{'(', TRUE, ')'}},
		{"(TRUE)", []int{'(', TRUE, ')'}},
	}

	for _, tc := range testCases {
		lexer := newLexer([]byte(tc.expr))
		fst := &FilterSymType{}


		toks := make([]int, 0)
		for {
			tok := lexer.Lex(fst)
			if tok == 0 {
				break
			}
			toks = append(toks, tok)
		}

		assert.NoErrorf(t, lexer.GetError(), "Unexpected expected for case : %s", tc.expr)
		assert.Equal(t, tc.expected, toks, "Unexpected tokens for case : %s", tc.expr)
	}





}