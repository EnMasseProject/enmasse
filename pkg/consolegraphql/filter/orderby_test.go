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

func TestOrderBy(t *testing.T) {

	testCases := []struct {
		expr  string
		num   int
	}{
		{"`$.Foo`", 1},
		{"`$.Foo` ASC", 1},
		{"`$.Foo` DESC, `$.Boo`", 2},
	}

	for _, tc := range testCases {
		clauses , err := ParseOrderByExpression(tc.expr)
		assert.NoErrorf(t, err, "Unexpected error for case : %s", tc.expr)
		assert.Equal(t, tc.num, len(clauses), "Unexpected number of order by clauses for case : %s", tc.expr)
	}
}
