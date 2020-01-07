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

func TestLexSimpleLexemes(t *testing.T) {

	testCases := []struct {
		expr     string
		expected []int
	}{
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

		assert.NoErrorf(t, lexer.GetError(), "Unexpected error for case : %s", tc.expr)
		assert.Equal(t, tc.expected, toks, "Unexpected tokens for case : %s", tc.expr)
	}
}

func TestLexValueCarryingLexemes(t *testing.T) {

	testCases := []struct {
		expr           string
		expected       int
		validatingFunc func(*FilterSymType)
	}{
		{"1", INTEGRAL,
			func(fst *FilterSymType) { assert.Equal(t, IntVal(1), fst.integralValue) },
		},
		{"1.0", FLOAT,
			func(fst *FilterSymType) { assert.Equal(t, FloatVal(1.0), fst.floatValue) },
		},

		{"-1", INTEGRAL,
			func(fst *FilterSymType) { assert.Equal(t, IntVal(-1), fst.integralValue) },
		},
		{"-1.0", FLOAT,
			func(fst *FilterSymType) { assert.Equal(t, FloatVal(-1.0), fst.floatValue) },
		},

		{"'A'", STRING,
			func(fst *FilterSymType) { assert.Equal(t, StringVal("A"), fst.stringValue) },
		},
		{"'AB'", STRING,
			func(fst *FilterSymType) { assert.Equal(t, StringVal("AB"), fst.stringValue) },
		},
		{"''", STRING,
			func(fst *FilterSymType) { assert.Equal(t, StringVal(""), fst.stringValue) },
		},
		{"'a cow said ''moo!'''", STRING,
			func(fst *FilterSymType) { assert.Equal(t, StringVal("a cow said 'moo!'"), fst.stringValue) },
		},
		{"''''", STRING,
			func(fst *FilterSymType) { assert.Equal(t, StringVal("'"), fst.stringValue) },
		},
	}

	for _, tc := range testCases {
		lexer := newLexer([]byte(tc.expr))
		fst := &FilterSymType{}

		tok := lexer.Lex(fst)
		if tc.validatingFunc != nil {
			tc.validatingFunc(fst)
		}


		assert.NoErrorf(t, lexer.GetError(), "Unexpected error for case : %s", tc.expr)
		assert.Equal(t, tc.expected, tok, "Unexpected token for case : %s", tc.expr)
	}

}