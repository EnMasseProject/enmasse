/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestBuildFilter(t *testing.T) {
	testCases := []struct {
		name        string
		filter      string
		keyElements []string
		expected    string
	}{
		{"one element", "`$.metadata.namespace` = 'foo'", []string{"$.metadata.namespace"}, "foo"},
		{"reversed operands", "'foo' = `$.metadata.namespace`", []string{"$.metadata.namespace"}, "foo"},
		{"element and element", "`$.metadata.namespace` = 'foo' AND `$.metadata.name` = 'bar'", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/bar"},
		{"element and (element)", "`$.metadata.namespace` = 'foo' AND (`$.metadata.name` = 'bar')", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/bar"},
		{"element or element", "`$.metadata.namespace` = 'foo' OR `$.metadata.name` = 'bar'", []string{"$.metadata.namespace", "$.metadata.name"}, ""},
		{"element and (element or element)", "`$.metadata.namespace` = 'foo' AND (`$.metadata.name` = 'bar' OR `$.metadata.foo` = 'baz')", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/"},
		{"unexpected operand type", "`$.metadata.namespace` = 123", []string{"$.metadata.namespace"}, ""},
		{"missing leading element disallowed", "`$.metadata.name` = 'bar'", []string{"$.metadata.namespace", "$.metadata.name"}, ""},
		{"missing trailing element allowed", "`$.metadata.namespace` = 'foo'", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/"},
		{"duplicate elements", "`$.metadata.namespace` = 'foo' AND `$.metadata.namespace` LIKE 'fo%'", []string{"$.metadata.namespace"}, "foo"},

		{"like element", "`$.metadata.namespace` LIKE 'fo%'", []string{"$.metadata.namespace"}, "fo"},
		{"element and like element", "`$.metadata.namespace` = 'foo' AND `$.metadata.name` LIKE 'ba%'", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/ba"},
		{"leading like element disallowed", "`$.metadata.namespace` LIKE 'fo%' AND `$.metadata.name` = 'bar'", []string{"$.metadata.namespace", "$.metadata.name"}, ""},

		{"missing element", "`$.metadata.namespace` = 'foo'", []string{"$.metadata.namespace", "$.metadata.name"}, "foo/"},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			_, actual, err := BuildFilter(&tc.filter, tc.keyElements...)
			assert.NoError(t, err)
			assert.Equal(t, tc.expected, actual)

		})
	}
}
