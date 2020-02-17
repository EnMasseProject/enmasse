/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

//go:generate go run github.com/99designs/gqlgen generate --config console/console-server/src/main/resources/gqlgen.yml --verbose

import (
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/agent"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/filter"
	"strings"
)

type Resolver struct {
	AdminConfig  *v1beta2.AdminV1beta2Client
	CoreConfig   *v1beta1.EnmasseV1beta1Client
	Cache        *cache.MemdbCache
	GetCollector func(string) agent.Delegate
}

func (r *Resolver) Query() QueryResolver {
	return &queryResolver{r}
}

func (r *Resolver) Mutation() MutationResolver {
	return &mutationResolver{r}
}

type queryResolver struct{ *Resolver }

type mutationResolver struct{ *Resolver }

func CalcLowerUpper(offset *int, first *int, len int) (int, int) {
	lower := 0
	if offset != nil && *offset > 0 {
		lower = Min(*offset, len)
	}
	upper := len
	if first != nil && *first > 0 {
		upper = Min(lower+*first, len)
	}
	return lower, upper
}

func Min(x, y int) int {
	if x < y {
		return x
	}
	return y
}

func BuildFilter(f *string, orderedKeyElements ...string) (cache.ObjectFilter, string, error) {
	if f != nil && *f != "" {
		key := ""
		expression, err := filter.ParseFilterExpression(*f)
		if err != nil {
			return nil, "", err
		}
		if len(orderedKeyElements) > 0 {
			key = buildKey(expression, orderedKeyElements...)
		}
		return func(i interface{}) (match bool, cont bool, e error) {
			rv, e := expression.Eval(i)
			if e != nil {
				return false, false, e
			}
			return rv.(bool), true, nil
		}, key, nil
	}
	return nil, "", nil
}

// builds the longest possible hierarchical key, in key element order, that matches the filter.
// The filter tree traversal is pruned whenever boolean logic other from an "and" is encountered.
// (This allows the indices provided by hashicorp memdb to be used efficiently).
func buildKey(expression filter.Expr, orderedKeyElements ...string) string {
	keyElements := make([]string, 0)

	for i, keyElement := range orderedKeyElements {
		foundTerm := false

		doCmp := func(left, right filter.Expr,
			fltKeyElement func(string) bool,
			fltStrVal func(string) (string, bool)) bool {
			if jpv, jpok := left.(filter.JSONPathVal); jpok && fltKeyElement(jpv.JSONPathExpr) {
				if stv, stok := right.(filter.StringVal); stok {
					if sv, ok := fltStrVal(string(stv)); ok {
						keyElements = append(keyElements, sv)
						return true
					}
				}
			}
			return false
		}

		accept := func(e filter.Expr) bool {
			if foundTerm {
				return false
			}
			switch e.(type) {
			case filter.AndExpr:
				return true
			case filter.ComparisonExpr:
				return true
			}
			return false
		}
		visit := func(e filter.Expr) {
			if ce, ceok := e.(filter.ComparisonExpr); ceok {
				fltKeyElement := func(jp string) bool { return jp == keyElement }

				if ce.Operator == filter.EqualStr {
					fltStrVal := func(sv string) (string, bool) { return sv, true }
					if doCmp(ce.Left, ce.Right, fltKeyElement, fltStrVal) || doCmp(ce.Right, ce.Left, fltKeyElement, fltStrVal) {
						foundTerm = true
					}
				} else if ce.Operator == filter.LikeStr && i == len(orderedKeyElements)-1 {
					// Like eligible for last key element only
					fltStrVal := func(sv string) (string, bool) {
						// Allow only a single trailing % and no _
						return strings.TrimSuffix(sv, "%"),
							strings.Index(sv, "%") == len(sv)-1 && strings.Index(sv, "_") == -1
					}
					if doCmp(ce.Left, ce.Right, fltKeyElement, fltStrVal) || doCmp(ce.Right, ce.Left, fltKeyElement, fltStrVal) {
						foundTerm = true
					}
				}
			}
		}
		expression.Traverse(accept, visit)

		if !foundTerm {
			break
		}
	}

	if len(orderedKeyElements) > len(keyElements) {
		keyElements = append(keyElements, "")
	}

	return strings.Join(keyElements, "/")
}

func BuildOrderer(o *string) (func(interface{}) error, error) {
	if o != nil && *o != "" {
		orderby, err := filter.ParseOrderByExpression(*o)
		if err != nil {
			return nil, err
		}

		return orderby.Sort, err
	}
	return func(interface{}) error { return nil }, nil
}
