
//line lex.rl:1
/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package filter

import (
        "fmt"
        "strconv"
        "strings"
        "k8s.io/client-go/util/jsonpath"
)


//line lex.go:20
var _filterlexer_actions []byte = []byte{
	0, 1, 2, 1, 3, 1, 6, 1, 7, 
	1, 8, 1, 9, 1, 10, 1, 11, 
	1, 12, 1, 13, 1, 14, 1, 15, 
	1, 16, 1, 17, 1, 18, 1, 19, 
	1, 20, 1, 21, 1, 22, 1, 23, 
	1, 24, 1, 25, 1, 26, 1, 27, 
	1, 28, 1, 29, 1, 30, 1, 31, 
	1, 32, 2, 0, 1, 2, 3, 4, 
	2, 3, 5, 
}

var _filterlexer_key_offsets []byte = []byte{
	0, 0, 1, 2, 4, 6, 10, 12, 
	14, 16, 18, 20, 22, 24, 26, 28, 
	30, 32, 34, 36, 40, 42, 44, 46, 
	48, 50, 52, 54, 55, 56, 88, 89, 
	92, 94, 95, 96, 
}

var _filterlexer_trans_keys []byte = []byte{
	61, 39, 48, 57, 48, 57, 78, 83, 
	110, 115, 68, 100, 67, 99, 69, 101, 
	83, 115, 67, 99, 65, 97, 76, 108, 
	83, 115, 69, 101, 83, 115, 73, 105, 
	75, 107, 69, 101, 79, 85, 111, 117, 
	84, 116, 76, 108, 76, 108, 82, 114, 
	82, 114, 85, 117, 69, 101, 96, 96, 
	32, 33, 39, 40, 41, 44, 60, 61, 
	62, 65, 68, 70, 73, 76, 78, 79, 
	84, 96, 97, 100, 102, 105, 108, 110, 
	111, 116, 9, 13, 43, 45, 48, 57, 
	39, 46, 48, 57, 48, 57, 61, 61, 
	96, 
}

var _filterlexer_single_lengths []byte = []byte{
	0, 1, 1, 0, 0, 4, 2, 2, 
	2, 2, 2, 2, 2, 2, 2, 2, 
	2, 2, 2, 4, 2, 2, 2, 2, 
	2, 2, 2, 1, 1, 26, 1, 1, 
	0, 1, 1, 1, 
}

var _filterlexer_range_lengths []byte = []byte{
	0, 0, 0, 1, 1, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 3, 0, 1, 
	1, 0, 0, 0, 
}

var _filterlexer_index_offsets []byte = []byte{
	0, 0, 2, 4, 6, 8, 13, 16, 
	19, 22, 25, 28, 31, 34, 37, 40, 
	43, 46, 49, 52, 57, 60, 63, 66, 
	69, 72, 75, 78, 80, 82, 112, 114, 
	117, 119, 121, 123, 
}

var _filterlexer_indicies []byte = []byte{
	0, 1, 4, 3, 5, 1, 7, 6, 
	8, 9, 8, 9, 1, 10, 10, 1, 
	11, 11, 1, 12, 12, 1, 13, 13, 
	1, 14, 14, 1, 15, 15, 1, 16, 
	16, 1, 17, 17, 1, 18, 18, 1, 
	19, 19, 1, 20, 20, 1, 21, 21, 
	1, 22, 22, 1, 23, 24, 23, 24, 
	1, 25, 25, 1, 26, 26, 1, 27, 
	27, 1, 28, 28, 1, 29, 29, 1, 
	30, 30, 1, 31, 31, 1, 33, 32, 
	32, 34, 35, 36, 3, 37, 38, 40, 
	41, 42, 43, 44, 45, 46, 47, 48, 
	49, 50, 51, 32, 44, 45, 46, 47, 
	48, 49, 50, 51, 35, 39, 5, 1, 
	3, 52, 54, 5, 53, 7, 55, 57, 
	56, 59, 58, 61, 60, 
}

var _filterlexer_trans_targs []byte = []byte{
	29, 0, 29, 2, 30, 31, 29, 32, 
	6, 7, 29, 29, 9, 10, 29, 12, 
	13, 14, 29, 29, 17, 18, 29, 20, 
	21, 29, 22, 29, 29, 25, 26, 29, 
	27, 35, 29, 29, 1, 29, 29, 3, 
	29, 33, 29, 34, 5, 8, 11, 15, 
	16, 19, 23, 24, 29, 29, 4, 29, 
	29, 29, 29, 29, 29, 28, 
}

var _filterlexer_trans_actions []byte = []byte{
	31, 0, 57, 0, 62, 3, 53, 0, 
	0, 0, 5, 21, 0, 0, 23, 0, 
	0, 0, 15, 19, 0, 0, 11, 0, 
	0, 9, 0, 17, 7, 0, 0, 13, 
	0, 65, 55, 39, 0, 33, 35, 0, 
	37, 0, 25, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 45, 41, 0, 43, 
	51, 29, 49, 27, 47, 0, 
}

var _filterlexer_to_state_actions []byte = []byte{
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 59, 0, 0, 
	0, 0, 0, 0, 
}

var _filterlexer_from_state_actions []byte = []byte{
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 1, 0, 0, 
	0, 0, 0, 0, 
}

var _filterlexer_eof_trans []byte = []byte{
	0, 0, 3, 0, 7, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 3, 35, 0, 53, 54, 
	56, 57, 59, 61, 
}

const filterlexer_start int = 29
const filterlexer_first_final int = 29
const filterlexer_error int = 0

const filterlexer_en_main int = 29


//line lex.rl:22


type lexer struct {
    data []byte
    p, pe, cs int
    ts, te, act int

    e error

    expr Expr
    orderBy OrderBy

    startToks []int
}

func newLexer(data []byte) *lexer {
    lex := &lexer{
        data: data,
        pe: len(data),
    }
    
//line lex.go:175
	{
	 lex.cs = filterlexer_start
	 lex.ts = 0
	 lex.te = 0
	 lex.act = 0
	}

//line lex.rl:43
    return lex
}

func (lex *lexer) Lex(out *FilterSymType) int {
    eof := lex.pe
    tok := 0

    // Produce starting token(s) to allow grammars with multiple start symbols.
    if len(lex.startToks) > 0 {
        tok = lex.startToks[0]
        lex.startToks = lex.startToks[1:]
        return tok;
    }

    
//line lex.go:199
	{
	var _klen int
	var _trans int
	var _acts int
	var _nacts uint
	var _keys int
	if ( lex.p) == ( lex.pe) {
		goto _test_eof
	}
	if  lex.cs == 0 {
		goto _out
	}
_resume:
	_acts = int(_filterlexer_from_state_actions[ lex.cs])
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		 _acts++
		switch _filterlexer_actions[_acts - 1] {
		case 2:
//line NONE:1
 lex.ts = ( lex.p)

//line lex.go:222
		}
	}

	_keys = int(_filterlexer_key_offsets[ lex.cs])
	_trans = int(_filterlexer_index_offsets[ lex.cs])

	_klen = int(_filterlexer_single_lengths[ lex.cs])
	if _klen > 0 {
		_lower := int(_keys)
		var _mid int
		_upper := int(_keys + _klen - 1)
		for {
			if _upper < _lower {
				break
			}

			_mid = _lower + ((_upper - _lower) >> 1)
			switch {
			case  lex.data[( lex.p)] < _filterlexer_trans_keys[_mid]:
				_upper = _mid - 1
			case  lex.data[( lex.p)] > _filterlexer_trans_keys[_mid]:
				_lower = _mid + 1
			default:
				_trans += int(_mid - int(_keys))
				goto _match
			}
		}
		_keys += _klen
		_trans += _klen
	}

	_klen = int(_filterlexer_range_lengths[ lex.cs])
	if _klen > 0 {
		_lower := int(_keys)
		var _mid int
		_upper := int(_keys + (_klen << 1) - 2)
		for {
			if _upper < _lower {
				break
			}

			_mid = _lower + (((_upper - _lower) >> 1) & ^1)
			switch {
			case  lex.data[( lex.p)] < _filterlexer_trans_keys[_mid]:
				_upper = _mid - 2
			case  lex.data[( lex.p)] > _filterlexer_trans_keys[_mid + 1]:
				_lower = _mid + 2
			default:
				_trans += int((_mid - int(_keys)) >> 1)
				goto _match
			}
		}
		_trans += _klen
	}

_match:
	_trans = int(_filterlexer_indicies[_trans])
_eof_trans:
	 lex.cs = int(_filterlexer_trans_targs[_trans])

	if _filterlexer_trans_actions[_trans] == 0 {
		goto _again
	}

	_acts = int(_filterlexer_trans_actions[_trans])
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		_acts++
		switch _filterlexer_actions[_acts-1] {
		case 3:
//line NONE:1
 lex.te = ( lex.p)+1

		case 4:
//line lex.rl:91
 lex.act = 3;
		case 5:
//line lex.rl:97
 lex.act = 4;
		case 6:
//line lex.rl:108
 lex.te = ( lex.p)+1
{ tok = AND; ( lex.p)++; goto _out
}
		case 7:
//line lex.rl:109
 lex.te = ( lex.p)+1
{ tok = OR; ( lex.p)++; goto _out
}
		case 8:
//line lex.rl:110
 lex.te = ( lex.p)+1
{ tok = NOT; ( lex.p)++; goto _out
}
		case 9:
//line lex.rl:111
 lex.te = ( lex.p)+1
{ tok = LIKE; ( lex.p)++; goto _out
}
		case 10:
//line lex.rl:112
 lex.te = ( lex.p)+1
{ tok = TRUE; ( lex.p)++; goto _out
}
		case 11:
//line lex.rl:113
 lex.te = ( lex.p)+1
{ tok = FALSE; ( lex.p)++; goto _out
}
		case 12:
//line lex.rl:114
 lex.te = ( lex.p)+1
{ tok = NULL; ( lex.p)++; goto _out
}
		case 13:
//line lex.rl:115
 lex.te = ( lex.p)+1
{ tok = IS; ( lex.p)++; goto _out
}
		case 14:
//line lex.rl:117
 lex.te = ( lex.p)+1
{ tok = ASC; ( lex.p)++; goto _out
}
		case 15:
//line lex.rl:118
 lex.te = ( lex.p)+1
{ tok = DESC; ( lex.p)++; goto _out
}
		case 16:
//line lex.rl:120
 lex.te = ( lex.p)+1
{ tok = '='; ( lex.p)++; goto _out
}
		case 17:
//line lex.rl:123
 lex.te = ( lex.p)+1
{ tok = GE; ( lex.p)++; goto _out
}
		case 18:
//line lex.rl:124
 lex.te = ( lex.p)+1
{ tok = LE; ( lex.p)++; goto _out
}
		case 19:
//line lex.rl:125
 lex.te = ( lex.p)+1
{ tok = NE; ( lex.p)++; goto _out
}
		case 20:
//line lex.rl:127
 lex.te = ( lex.p)+1
{ tok = '('; ( lex.p)++; goto _out
}
		case 21:
//line lex.rl:128
 lex.te = ( lex.p)+1
{ tok = ')'; ( lex.p)++; goto _out
}
		case 22:
//line lex.rl:130
 lex.te = ( lex.p)+1
{ tok = ','; ( lex.p)++; goto _out
}
		case 23:
//line lex.rl:132
 lex.te = ( lex.p)+1

		case 24:
//line lex.rl:71
 lex.te = ( lex.p)
( lex.p)--
{
            val, err := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            ( lex.p)++; goto _out
}
		case 25:
//line lex.rl:81
 lex.te = ( lex.p)
( lex.p)--
{
            val, err := strconv.ParseFloat(string(lex.data[lex.ts:lex.te]), 64);
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.floatValue = FloatVal(val)
            tok = FLOAT;
            ( lex.p)++; goto _out
}
		case 26:
//line lex.rl:91
 lex.te = ( lex.p)
( lex.p)--
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            ( lex.p)++; goto _out
 }
		case 27:
//line lex.rl:97
 lex.te = ( lex.p)
( lex.p)--
{
            val := "{" + strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1) + "}"
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse(val)
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath)
            ( lex.p)++; goto _out
 }
		case 28:
//line lex.rl:121
 lex.te = ( lex.p)
( lex.p)--
{ tok = '>'; ( lex.p)++; goto _out
}
		case 29:
//line lex.rl:122
 lex.te = ( lex.p)
( lex.p)--
{ tok = '<'; ( lex.p)++; goto _out
}
		case 30:
//line lex.rl:71
( lex.p) = ( lex.te) - 1
{
            val, err := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            ( lex.p)++; goto _out
}
		case 31:
//line lex.rl:97
( lex.p) = ( lex.te) - 1
{
            val := "{" + strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1) + "}"
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse(val)
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath)
            ( lex.p)++; goto _out
 }
		case 32:
//line NONE:1
	switch  lex.act {
	case 0:
	{ lex.cs = 0
goto _again
}
	case 3:
	{( lex.p) = ( lex.te) - 1

            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            ( lex.p)++; goto _out
 }
	case 4:
	{( lex.p) = ( lex.te) - 1

            val := "{" + strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1) + "}"
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse(val)
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath)
            ( lex.p)++; goto _out
 }
	}
	
//line lex.go:513
		}
	}

_again:
	_acts = int(_filterlexer_to_state_actions[ lex.cs])
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		_acts++
		switch _filterlexer_actions[_acts-1] {
		case 0:
//line NONE:1
 lex.ts = 0

		case 1:
//line NONE:1
 lex.act = 0

//line lex.go:531
		}
	}

	if  lex.cs == 0 {
		goto _out
	}
	( lex.p)++
	if ( lex.p) != ( lex.pe) {
		goto _resume
	}
	_test_eof: {}
	if ( lex.p) == eof {
		if _filterlexer_eof_trans[ lex.cs] > 0 {
			_trans = int(_filterlexer_eof_trans[ lex.cs] - 1)
			goto _eof_trans
		}
	}

	_out: {}
	}

//line lex.rl:136


    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

