
//line pkg/consolegraphql/filter/filter_lex.rl:1
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


//line pkg/consolegraphql/filter/filter_lex.go:20
var _filterlexer_actions []byte = []byte{
	0, 1, 2, 1, 3, 1, 6, 1, 7, 
	1, 8, 1, 9, 1, 10, 1, 11, 
	1, 12, 1, 13, 1, 14, 1, 15, 
	1, 16, 1, 17, 1, 18, 1, 19, 
	1, 20, 1, 21, 1, 22, 1, 23, 
	1, 24, 1, 25, 1, 26, 1, 27, 
	1, 28, 1, 29, 2, 0, 1, 2, 
	3, 4, 2, 3, 5, 
}

var _filterlexer_to_state_actions []byte = []byte{
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 53, 0, 0, 0, 0, 0, 0, 
}

var _filterlexer_from_state_actions []byte = []byte{
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 0, 0, 0, 0, 0, 0, 0, 
	0, 1, 0, 0, 0, 0, 0, 0, 
}

const filterlexer_start int = 25
const filterlexer_first_final int = 25
const filterlexer_error int = 0

const filterlexer_en_main int = 25


//line pkg/consolegraphql/filter/filter_lex.rl:22


type lexer struct {
    data []byte
    p, pe, cs int
    ts, te, act int
    expr Expr
    e error
}

func newLexer(data []byte) *lexer {
    lex := &lexer{
        data: data,
        pe: len(data),
    }
    
//line pkg/consolegraphql/filter/filter_lex.go:70
	{
	 lex.cs = filterlexer_start
	 lex.ts = 0
	 lex.te = 0
	 lex.act = 0
	}

//line pkg/consolegraphql/filter/filter_lex.rl:38
    return lex
}

func (lex *lexer) Lex(out *FilterSymType) int {
    eof := lex.pe
    tok := 0
    
//line pkg/consolegraphql/filter/filter_lex.go:86
	{
	var _acts int
	var _nacts uint

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

//line pkg/consolegraphql/filter/filter_lex.go:107
		}
	}

	switch  lex.cs {
	case 25:
		switch  lex.data[( lex.p)] {
		case 32:
			goto tr30;
		case 33:
			goto tr31;
		case 39:
			goto tr3;
		case 40:
			goto tr32;
		case 41:
			goto tr33;
		case 43:
			goto tr34;
		case 45:
			goto tr34;
		case 60:
			goto tr35;
		case 61:
			goto tr36;
		case 62:
			goto tr37;
		case 65:
			goto tr38;
		case 70:
			goto tr39;
		case 73:
			goto tr40;
		case 76:
			goto tr41;
		case 78:
			goto tr42;
		case 79:
			goto tr43;
		case 84:
			goto tr44;
		case 96:
			goto tr27;
		case 97:
			goto tr38;
		case 102:
			goto tr39;
		case 105:
			goto tr40;
		case 108:
			goto tr41;
		case 110:
			goto tr42;
		case 111:
			goto tr43;
		case 116:
			goto tr44;
		}
		switch {
		case  lex.data[( lex.p)] > 13:
			if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
				goto tr5;
			}
		case  lex.data[( lex.p)] >= 9:
			goto tr30;
		}
		goto tr1;
	case 0:
		goto _out
	case 1:
		if  lex.data[( lex.p)] == 61 {
			goto tr0;
		}
		goto tr1;
	case 2:
		if  lex.data[( lex.p)] == 39 {
			goto tr4;
		}
		goto tr3;
	case 26:
		if  lex.data[( lex.p)] == 39 {
			goto tr3;
		}
		goto tr45;
	case 3:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5;
		}
		goto tr1;
	case 27:
		if  lex.data[( lex.p)] == 46 {
			goto tr47;
		}
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5;
		}
		goto tr46;
	case 4:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr7;
		}
		goto tr6;
	case 28:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr7;
		}
		goto tr48;
	case 29:
		if  lex.data[( lex.p)] == 61 {
			goto tr50;
		}
		goto tr49;
	case 30:
		if  lex.data[( lex.p)] == 61 {
			goto tr52;
		}
		goto tr51;
	case 5:
		switch  lex.data[( lex.p)] {
		case 78:
			goto tr8;
		case 110:
			goto tr8;
		}
		goto tr1;
	case 6:
		switch  lex.data[( lex.p)] {
		case 68:
			goto tr9;
		case 100:
			goto tr9;
		}
		goto tr1;
	case 7:
		switch  lex.data[( lex.p)] {
		case 65:
			goto tr10;
		case 97:
			goto tr10;
		}
		goto tr1;
	case 8:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr11;
		case 108:
			goto tr11;
		}
		goto tr1;
	case 9:
		switch  lex.data[( lex.p)] {
		case 83:
			goto tr12;
		case 115:
			goto tr12;
		}
		goto tr1;
	case 10:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr13;
		case 101:
			goto tr13;
		}
		goto tr1;
	case 11:
		switch  lex.data[( lex.p)] {
		case 83:
			goto tr14;
		case 115:
			goto tr14;
		}
		goto tr1;
	case 12:
		switch  lex.data[( lex.p)] {
		case 73:
			goto tr15;
		case 105:
			goto tr15;
		}
		goto tr1;
	case 13:
		switch  lex.data[( lex.p)] {
		case 75:
			goto tr16;
		case 107:
			goto tr16;
		}
		goto tr1;
	case 14:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr17;
		case 101:
			goto tr17;
		}
		goto tr1;
	case 15:
		switch  lex.data[( lex.p)] {
		case 79:
			goto tr18;
		case 85:
			goto tr19;
		case 111:
			goto tr18;
		case 117:
			goto tr19;
		}
		goto tr1;
	case 16:
		switch  lex.data[( lex.p)] {
		case 84:
			goto tr20;
		case 116:
			goto tr20;
		}
		goto tr1;
	case 17:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr21;
		case 108:
			goto tr21;
		}
		goto tr1;
	case 18:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr22;
		case 108:
			goto tr22;
		}
		goto tr1;
	case 19:
		switch  lex.data[( lex.p)] {
		case 82:
			goto tr23;
		case 114:
			goto tr23;
		}
		goto tr1;
	case 20:
		switch  lex.data[( lex.p)] {
		case 82:
			goto tr24;
		case 114:
			goto tr24;
		}
		goto tr1;
	case 21:
		switch  lex.data[( lex.p)] {
		case 85:
			goto tr25;
		case 117:
			goto tr25;
		}
		goto tr1;
	case 22:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr26;
		case 101:
			goto tr26;
		}
		goto tr1;
	case 23:
		if  lex.data[( lex.p)] == 96 {
			goto tr28;
		}
		goto tr27;
	case 31:
		if  lex.data[( lex.p)] == 96 {
			goto tr54;
		}
		goto tr53;
	case 24:
		if  lex.data[( lex.p)] == 96 {
			goto tr27;
		}
		goto tr29;
	}

	tr1:  lex.cs = 0; goto _again
	tr31:  lex.cs = 1; goto _again
	tr3:  lex.cs = 2; goto _again
	tr34:  lex.cs = 3; goto _again
	tr47:  lex.cs = 4; goto _again
	tr38:  lex.cs = 5; goto _again
	tr8:  lex.cs = 6; goto _again
	tr39:  lex.cs = 7; goto _again
	tr10:  lex.cs = 8; goto _again
	tr11:  lex.cs = 9; goto _again
	tr12:  lex.cs = 10; goto _again
	tr40:  lex.cs = 11; goto _again
	tr41:  lex.cs = 12; goto _again
	tr15:  lex.cs = 13; goto _again
	tr16:  lex.cs = 14; goto _again
	tr42:  lex.cs = 15; goto _again
	tr18:  lex.cs = 16; goto _again
	tr19:  lex.cs = 17; goto _again
	tr21:  lex.cs = 18; goto _again
	tr43:  lex.cs = 19; goto _again
	tr44:  lex.cs = 20; goto _again
	tr24:  lex.cs = 21; goto _again
	tr25:  lex.cs = 22; goto _again
	tr27:  lex.cs = 23; goto _again
	tr54:  lex.cs = 24; goto _again
	tr0:  lex.cs = 25; goto f0
	tr2:  lex.cs = 25; goto f1
	tr6:  lex.cs = 25; goto f4
	tr9:  lex.cs = 25; goto f5
	tr13:  lex.cs = 25; goto f6
	tr14:  lex.cs = 25; goto f7
	tr17:  lex.cs = 25; goto f8
	tr20:  lex.cs = 25; goto f9
	tr22:  lex.cs = 25; goto f10
	tr23:  lex.cs = 25; goto f11
	tr26:  lex.cs = 25; goto f12
	tr29:  lex.cs = 25; goto f14
	tr30:  lex.cs = 25; goto f17
	tr32:  lex.cs = 25; goto f18
	tr33:  lex.cs = 25; goto f19
	tr36:  lex.cs = 25; goto f20
	tr45:  lex.cs = 25; goto f21
	tr46:  lex.cs = 25; goto f22
	tr48:  lex.cs = 25; goto f23
	tr49:  lex.cs = 25; goto f24
	tr50:  lex.cs = 25; goto f25
	tr51:  lex.cs = 25; goto f26
	tr52:  lex.cs = 25; goto f27
	tr53:  lex.cs = 25; goto f28
	tr4:  lex.cs = 26; goto f2
	tr5:  lex.cs = 27; goto f3
	tr7:  lex.cs = 28; goto _again
	tr35:  lex.cs = 29; goto _again
	tr37:  lex.cs = 30; goto _again
	tr28:  lex.cs = 31; goto f13

	f3: _acts = 3; goto execFuncs
	f5: _acts = 5; goto execFuncs
	f11: _acts = 7; goto execFuncs
	f9: _acts = 9; goto execFuncs
	f8: _acts = 11; goto execFuncs
	f12: _acts = 13; goto execFuncs
	f6: _acts = 15; goto execFuncs
	f10: _acts = 17; goto execFuncs
	f7: _acts = 19; goto execFuncs
	f20: _acts = 21; goto execFuncs
	f27: _acts = 23; goto execFuncs
	f25: _acts = 25; goto execFuncs
	f0: _acts = 27; goto execFuncs
	f18: _acts = 29; goto execFuncs
	f19: _acts = 31; goto execFuncs
	f17: _acts = 33; goto execFuncs
	f22: _acts = 35; goto execFuncs
	f23: _acts = 37; goto execFuncs
	f21: _acts = 39; goto execFuncs
	f28: _acts = 41; goto execFuncs
	f26: _acts = 43; goto execFuncs
	f24: _acts = 45; goto execFuncs
	f4: _acts = 47; goto execFuncs
	f14: _acts = 49; goto execFuncs
	f1: _acts = 51; goto execFuncs
	f2: _acts = 56; goto execFuncs
	f13: _acts = 59; goto execFuncs

execFuncs:
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		_acts++
		switch _filterlexer_actions[_acts - 1] {
		case 3:
//line NONE:1
 lex.te = ( lex.p)+1

		case 4:
//line pkg/consolegraphql/filter/filter_lex.rl:78
 lex.act = 3;
		case 5:
//line pkg/consolegraphql/filter/filter_lex.rl:84
 lex.act = 4;
		case 6:
//line pkg/consolegraphql/filter/filter_lex.rl:95
 lex.te = ( lex.p)+1
{ tok = AND; ( lex.p)++; goto _out
}
		case 7:
//line pkg/consolegraphql/filter/filter_lex.rl:96
 lex.te = ( lex.p)+1
{ tok = OR; ( lex.p)++; goto _out
}
		case 8:
//line pkg/consolegraphql/filter/filter_lex.rl:97
 lex.te = ( lex.p)+1
{ tok = NOT; ( lex.p)++; goto _out
}
		case 9:
//line pkg/consolegraphql/filter/filter_lex.rl:98
 lex.te = ( lex.p)+1
{ tok = LIKE; ( lex.p)++; goto _out
}
		case 10:
//line pkg/consolegraphql/filter/filter_lex.rl:99
 lex.te = ( lex.p)+1
{ tok = TRUE; ( lex.p)++; goto _out
}
		case 11:
//line pkg/consolegraphql/filter/filter_lex.rl:100
 lex.te = ( lex.p)+1
{ tok = FALSE; ( lex.p)++; goto _out
}
		case 12:
//line pkg/consolegraphql/filter/filter_lex.rl:101
 lex.te = ( lex.p)+1
{ tok = NULL; ( lex.p)++; goto _out
}
		case 13:
//line pkg/consolegraphql/filter/filter_lex.rl:102
 lex.te = ( lex.p)+1
{ tok = IS; ( lex.p)++; goto _out
}
		case 14:
//line pkg/consolegraphql/filter/filter_lex.rl:104
 lex.te = ( lex.p)+1
{ tok = '='; ( lex.p)++; goto _out
}
		case 15:
//line pkg/consolegraphql/filter/filter_lex.rl:107
 lex.te = ( lex.p)+1
{ tok = GE; ( lex.p)++; goto _out
}
		case 16:
//line pkg/consolegraphql/filter/filter_lex.rl:108
 lex.te = ( lex.p)+1
{ tok = LE; ( lex.p)++; goto _out
}
		case 17:
//line pkg/consolegraphql/filter/filter_lex.rl:109
 lex.te = ( lex.p)+1
{ tok = NE; ( lex.p)++; goto _out
}
		case 18:
//line pkg/consolegraphql/filter/filter_lex.rl:111
 lex.te = ( lex.p)+1
{ tok = '('; ( lex.p)++; goto _out
}
		case 19:
//line pkg/consolegraphql/filter/filter_lex.rl:112
 lex.te = ( lex.p)+1
{ tok = ')'; ( lex.p)++; goto _out
}
		case 20:
//line pkg/consolegraphql/filter/filter_lex.rl:114
 lex.te = ( lex.p)+1

		case 21:
//line pkg/consolegraphql/filter/filter_lex.rl:58
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
		case 22:
//line pkg/consolegraphql/filter/filter_lex.rl:68
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
		case 23:
//line pkg/consolegraphql/filter/filter_lex.rl:78
 lex.te = ( lex.p)
( lex.p)--
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            ( lex.p)++; goto _out
 }
		case 24:
//line pkg/consolegraphql/filter/filter_lex.rl:84
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
		case 25:
//line pkg/consolegraphql/filter/filter_lex.rl:105
 lex.te = ( lex.p)
( lex.p)--
{ tok = '>'; ( lex.p)++; goto _out
}
		case 26:
//line pkg/consolegraphql/filter/filter_lex.rl:106
 lex.te = ( lex.p)
( lex.p)--
{ tok = '<'; ( lex.p)++; goto _out
}
		case 27:
//line pkg/consolegraphql/filter/filter_lex.rl:58
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
		case 28:
//line pkg/consolegraphql/filter/filter_lex.rl:84
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
		case 29:
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
	
//line pkg/consolegraphql/filter/filter_lex.go:684
		}
	}
	goto _again

_again:
	_acts = int(_filterlexer_to_state_actions[ lex.cs])
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		_acts++
		switch _filterlexer_actions[_acts - 1] {
		case 0:
//line NONE:1
 lex.ts = 0

		case 1:
//line NONE:1
 lex.act = 0

//line pkg/consolegraphql/filter/filter_lex.go:703
		}
	}

	if  lex.cs == 0 {
		goto _out
	}
	if ( lex.p)++; ( lex.p) != ( lex.pe) {
		goto _resume
	}
	_test_eof: {}
	if ( lex.p) == eof {
		switch  lex.cs {
		case 2:
			goto tr2
		case 26:
			goto tr45
		case 27:
			goto tr46
		case 4:
			goto tr6
		case 28:
			goto tr48
		case 29:
			goto tr49
		case 30:
			goto tr51
		case 23:
			goto tr2
		case 31:
			goto tr53
		case 24:
			goto tr29
	}
	}

	_out: {}
	}

//line pkg/consolegraphql/filter/filter_lex.rl:118


    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

