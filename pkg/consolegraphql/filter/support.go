/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

//go:generate goyacc -o filter.go -p Filter filter.y

package filter

import (
	"fmt"
	"k8s.io/client-go/third_party/forked/golang/template"
	"k8s.io/client-go/util/jsonpath"
	"math"
	"reflect"
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
	IsNull               = "is null"
	IsNotNull            = "is not null"
)

func Parse(s string) (Expr, error) {
	lexer := newLexer([]byte(s))
	_ = FilterParse(lexer)
	return lexer.expr, lexer.e
}

type Expr interface {
	Eval(interface{}) (interface{}, error)
}

func NewBoolExpr(e Expr) Expr {
	return &boolExpr{e}
}

type boolExpr struct {
	Expr
}

func (e boolExpr) Eval(v interface{}) (interface{}, error) {
	result, err := e.Expr.Eval(v)
	if err != nil {
		return nil, err
	}
	switch rv := result.(type) {
	case bool:
		return rv, nil
	default:
		return false, nil
	}
}


func NewAndExpr(left, right Expr) Expr {
	return &andExpr{left, right}
}

type andExpr struct {
	Left, Right Expr
}

func (e andExpr) Eval(v interface{}) (interface{}, error) {
	left, err := e.Left.Eval(v)
	if err != nil {
		return nil, err
	}
	switch l := left.(type) {
	case bool:
		if l {
			right, err := e.Right.Eval(v)
			if err != nil {
				return nil, err
			}
			switch r := right.(type) {
			case bool:
				return r, nil
			default:
				return false, nil
			}
		}
	default:
	}

	return false, nil
}

func NewOrExpr(left, right Expr) Expr {
	return &orExpr{left, right}
}

type orExpr struct {
	Left, Right Expr
}

func (e orExpr) Eval(v interface{}) (interface{}, error) {
	left, err := e.Left.Eval(v)
	if err != nil {
		return nil, err
	}
	switch l := left.(type) {
	case bool:
		if l {
			return l, nil
		}
	default:
	}

	right, err := e.Right.Eval(v)
	if err != nil {
		return nil, err
	}
	switch r := right.(type) {
	case bool:
		return r, nil
	default:
		return false, nil
	}
}

func NewNotExpr(e Expr) Expr {
	return &notExpr{e}
}

type notExpr struct {
	Expr Expr
}

func (e notExpr) Eval(v interface{}) (interface{}, error) {
	val, err := e.Expr.Eval(v)
	if err != nil {
		return nil, err
	}
	switch rv := val.(type) {
	case bool:
		return !rv, nil
	default:
		return false, nil
	}
}


func NewIsNullExpr(expr Expr, operator string) Expr {
	return &isNullExpr{operator, expr}
}

type isNullExpr struct {
	Operator    string
	Expr Expr
}

func (e isNullExpr) Eval(v interface{}) (interface{}, error) {
	val, err := e.Expr.Eval(v)
	if err != nil {
		return nil, err
	}

	switch e.Operator {
	case IsNull:
		switch val.(type) {
		case nullVal:
			return true, nil
		default:
			return false, nil
		}
	case IsNotNull:
		switch val.(type) {
		case nullVal:
			return false, nil
		default:
			return true, nil
		}
	}
	return false, nil
}

func NewComparisonExpr(left Expr, operator string, right Expr) Expr {
	return &comparisonExpr{operator, left, right}
}

type comparisonExpr struct {
	Operator    string
	Left, Right Expr
}

func (e comparisonExpr) Eval(v interface{}) (interface{}, error) {

	left, err := e.Left.Eval(v)
	if err != nil {
		return nil, err
	}

	right, err := e.Right.Eval(v)
	if err != nil {
		return nil, err
	}

	left = widenNumeric(left, reflect.TypeOf(right))
	right = widenNumeric(right, reflect.TypeOf(left))

	right = intFloatPromotion(left, right)
	left = intFloatPromotion(right, left)

	switch l := left.(type) {
	case int:
		switch r := right.(type) {
		case int:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case int8:
		switch r := right.(type) {
		case int8:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case int16:
		switch r := right.(type) {
		case int16:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case int32:
		switch r := right.(type) {
		case int32:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case int64:
		switch r := right.(type) {
		case int64:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case uint:
		switch r := right.(type) {
		case uint:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case uint8:
		switch r := right.(type) {
		case uint8:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case uint16:
		switch r := right.(type) {
		case uint16:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case uint32:
		switch r := right.(type) {
		case uint32:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case uint64:
		switch r := right.(type) {
		case uint64:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}

	case float32:
		switch r := right.(type) {
		case float32:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case float64:
		switch r := right.(type) {
		case float64:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case GreaterThanStr:
				return l > r, nil
			case GreaterEqualStr:
				return l >= r, nil
			case LessThanStr:
				return l < r, nil
			case LessEqualStr:
				return l <= r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case bool:
		switch r := right.(type) {
		case bool:
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	case string:
		switch r := right.(type) {
		case string:
			// JMS selector syntax restricts string comparison to == and !=
			switch e.Operator {
			case EqualStr:
				return l == r, nil
			case NotEqualStr:
				return l != r, nil
			case LikeStr:
				return like(l, r), nil
			case NotLikeStr:
				return !like(l, r), nil
			default:
				return false, nil
			}
		default:
			return false, nil
		}
	default:
		return false, nil
	}
}

type BoolVal bool

func (e BoolVal) Eval(interface{}) (interface{}, error) {
	return bool(e), nil
}

type StringVal string


func (e StringVal) Eval(interface{}) (interface{}, error) {
	return string(e), nil
}

type IntVal int

func (e IntVal) Eval(interface{}) (interface{}, error) {
	return int(e), nil
}

type FloatVal float64

func (e FloatVal) Eval(interface{}) (interface{}, error) {
	return float64(e), nil
}

func NewNullVal() nullVal {
	return nullVal{}
}

type nullVal struct{}


func (e nullVal) Eval(interface{}) (interface{}, error) {
	return e, nil
}

func NewJSONPathVal(jsonPath *jsonpath.JSONPath) JSONPathVal {
	return JSONPathVal{
		jsonPath,
	}
}


type JSONPathVal struct {
	*jsonpath.JSONPath
}

func (e JSONPathVal) Eval(v interface{}) (interface{}, error) {
	jp := e.JSONPath
	results, err := jp.FindResults(v)
	if err != nil {
		return nil, err
	}
	if len(results) > 0 && len(results[0]) > 0 {
		value, b := template.PrintableValue(results[0][0])
		if (b) {
			return value, nil
		}
	}
	return NewNullVal(), nil
}

// Wrapping the JSON object and implementing Eval

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

func intFloatPromotion(l interface{}, r interface{}) interface{} {
	switch l.(type) {
	case float64:
		switch i := r.(type) {
		case int:
			r = float64(i)
		case int8:
			r = float64(i)
		case int16:
			r = float64(i)
		case int32:
			r = float64(i)
		case int64:
			r = float64(i)
		case uint:
			r = float64(i)
		case uint8:
			r = float64(i)
		case uint16:
			r = float64(i)
		case uint32:
			r = float64(i)
		case uint64:
			r = float64(i)
		default:
		}
	case float32:
		switch i := r.(type) {
		case int:
			r = float32(i)
		case int8:
			r = float32(i)
		case int16:
			r = float32(i)
		case int32:
			r = float32(i)
		case int64:
			r = float32(i)
		case uint:
			r = float32(i)
		case uint8:
			r = float32(i)
		case uint16:
			r = float32(i)
		case uint32:
			r = float32(i)
		case uint64:
			r = float32(i)
		default:
		}
	}

	return r
}

const MaxUint = ^uint(0)
const MaxInt = uint64(int(MaxUint >> 1))

func widenNumeric(s interface{}, dst reflect.Type) interface{} {
	srt := reflect.TypeOf(s)

	if dst == srt {
		return s
	}

	convert := false
	switch srt.Kind() {
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		switch dst.Kind() {
		case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
			convert = true
		case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
			// A positive signed value must be able to fit unsigned type of the same width
			convert = reflect.ValueOf(s).Int() >= 0
		}
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		switch dst.Kind() {
		case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
			convert = true
		case reflect.Int:
			convert = reflect.ValueOf(s).Uint() <= MaxInt
		case reflect.Int8:
			convert = reflect.ValueOf(s).Uint() <= math.MaxInt8
		case reflect.Int16:
			convert = reflect.ValueOf(s).Uint() <= math.MaxInt16
		case reflect.Int32:
			convert = reflect.ValueOf(s).Uint() <= math.MaxInt32
		case reflect.Int64:
			convert = reflect.ValueOf(s).Uint() <= math.MaxInt64
		}
	case reflect.Float32, reflect.Float64:
		switch dst.Kind() {
		case reflect.Float32, reflect.Float64:
			convert = true
		}
	}

	if convert && dst.Bits() >= srt.Bits() {
		return reflect.ValueOf(s).Convert(dst).Interface()
	} else {
		return s
	}
}
