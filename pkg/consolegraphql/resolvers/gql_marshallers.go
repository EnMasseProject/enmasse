/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package resolvers

import (
	"fmt"
	"github.com/99designs/gqlgen/graphql"
	"io"
)

func MarshalCertBytes(s string) graphql.Marshaler {
	return graphql.WriterFunc(func(w io.Writer) {
		_, _ = fmt.Fprintf(w, "%s", s)
	})
}

func UnmarshalCertBytes(v interface{}) (string, error) {
	switch v := v.(type) {
	case string:
		return v, nil
	case *string:
		return *v, nil
	case []byte:
		return string(v), nil
	default:
		return "", fmt.Errorf("%T is not string", v)
	}
}
