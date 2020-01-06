/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

//go:generate goyacc -o filter.go -p Filter filter.y

package filter

import (
	"fmt"
	"regexp"
	"strings"
)

const (
	EqualStr             = "="
	LessThanStr          = "<"
	GreaterThanStr       = ">"
	LessEqualStr         = "<="
	GreaterEqualStr      = ">="
	NotEqualStr          = "!="
	LikeStr              = "like"
	NotLikeStr           = "not like"
)

func Parse(s string) (Expr, error) {
	lexer := newLexer([]byte(s))
	_ = FilterParse(lexer)
	return lexer.expr, lexer.e
}

type Expr interface {
	Eval() interface{}
}

func NewBoolExpr(e Expr) Expr {
	return &boolExpr{e}
}

type boolExpr struct {
	Expr
}

func (e boolExpr) Eval() interface{} {
	switch v := e.Expr.Eval().(type) {
	case bool:
		return v
	default:
		return false
	}
}


func NewAndExpr(left, right Expr) Expr {
	return &andExpr{left, right}
}

type andExpr struct {
	Left, Right Expr
}

func (e andExpr) Eval() interface{} {
	switch l := e.Left.Eval().(type) {
	case bool:
		if l {
			switch r := e.Right.Eval().(type) {
			case bool:
				return r
			default:
				return false
			}
		}
	default:
	}

	return false
}

func NewOrExpr(left, right Expr) Expr {
	return &orExpr{left, right}
}

type orExpr struct {
	Left, Right Expr
}

func (e orExpr) Eval() interface{} {
	switch l := e.Left.Eval().(type) {
	case bool:
		if (l) {
			return l
		}
	default:
	}

	switch r := e.Right.Eval().(type) {
	case bool:
		return r
	default:
		return false
	}
}

func NewNotExpr(e Expr) Expr {
	return &notExpr{e}
}

type notExpr struct {
	Expr Expr
}

func (e notExpr) Eval() interface{} {
	switch v := e.Expr.Eval().(type) {
	case bool:
		return !v
	default:
		return false
	}
}

func NewComparisonExpr(left Expr, operator string, right Expr) Expr {
	return &comparisonExpr{operator, left, right}
}

type comparisonExpr struct {
	Operator    string
	Left, Right Expr
}

func (e comparisonExpr) Eval() interface{} {
	promoteIntToFloat := func(l interface{}, r interface{}) interface{} {
		if _, lf := l.(float64); lf {
			if _, ri := r.(int); ri {
				r = float64(r.(int))
			}
		}
		return r
	}

	left := e.Left.Eval()
	right := e.Right.Eval()

	right = promoteIntToFloat(left, right)
	left = promoteIntToFloat(right, left)

	switch l := left.(type) {
	case int:
		switch r := right.(type) {
		case int:
			switch e.Operator {
			case EqualStr:
				return l == r
			case NotEqualStr:
				return l != r
			case GreaterThanStr:
				return l > r
			case GreaterEqualStr:
				return l >= r
			case LessThanStr:
				return l < r
			case LessEqualStr:
				return l <= r
			default:
				return false
			}
		default:
			return false
		}
	case float64:
		switch r := right.(type) {
		case float64:
			switch e.Operator {
			case EqualStr:
				return l == r
			case NotEqualStr:
				return l != r
			case GreaterThanStr:
				return l > r
			case GreaterEqualStr:
				return l >= r
			case LessThanStr:
				return l < r
			case LessEqualStr:
				return l <= r
			default:
				return false
			}
		default:
			return false
		}
	case bool:
		switch r := right.(type) {
		case bool:
			switch e.Operator {
			case EqualStr:
				return l == r
			case NotEqualStr:
				return l != r
			default:
				return false
			}
		default:
			return false
		}
	case string:
		switch r := right.(type) {
		case string:
			// JMS selector syntax restricts string comparison to == and !=
			switch e.Operator {
			case EqualStr:
				return l == r
			case NotEqualStr:
				return l != r
			case LikeStr:
				return like(l, r)
			case NotLikeStr:
				return !like(l, r)
			default:
				return false
			}
		default:
			return false
		}
	default:
		return false
	}
}

type BoolVal bool

func (e BoolVal) Eval() interface{} {
	return bool(e)
}

type StringVal string


func (e StringVal) Eval() interface{} {
	return string(e)
}

type IntVal int

func (e IntVal) Eval() interface{} {
	return int(e)
}

type FloatVal float64

func (e FloatVal) Eval() interface{} {
	return float64(e)
}

func NewNullVal() nullVal {
	return nullVal{}
}

type nullVal struct{}


func (e nullVal) Eval() interface{} {
	return e
}

func like(str, likePattern string) bool {
	reEscapedLikePattern := regexp.QuoteMeta(likePattern)
	re := "^" + strings.ReplaceAll(strings.ReplaceAll(reEscapedLikePattern, "%", ".*"), "_", ".") + "$"
	matched, err := regexp.MatchString(re, str)
	if err != nil {
		// Should never happen
		panic(fmt.Errorf("failed to compile LIKE expr '%s' into valid regexp : %s", likePattern, err))
	}
	return matched
}
