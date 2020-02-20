
//line pkg/consolegraphql/filter/lex.rl:1
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


//line pkg/consolegraphql/filter/lex.go:20
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

const filterlexer_start int = 29
const filterlexer_first_final int = 29
const filterlexer_error int = 0

const filterlexer_en_main int = 29


//line pkg/consolegraphql/filter/lex.rl:22


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
    
//line pkg/consolegraphql/filter/lex.go:78
	{
	 lex.cs = filterlexer_start
	 lex.ts = 0
	 lex.te = 0
	 lex.act = 0
	}

//line pkg/consolegraphql/filter/lex.rl:43
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

    
//line pkg/consolegraphql/filter/lex.go:102
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

//line pkg/consolegraphql/filter/lex.go:123
		}
	}

	switch  lex.cs {
	case 29:
		switch  lex.data[( lex.p)] {
		case 32:
			goto tr35;
		case 33:
			goto tr36;
		case 39:
			goto tr3;
		case 40:
			goto tr37;
		case 41:
			goto tr38;
		case 44:
			goto tr40;
		case 60:
			goto tr41;
		case 61:
			goto tr42;
		case 62:
			goto tr43;
		case 65:
			goto tr44;
		case 68:
			goto tr45;
		case 70:
			goto tr46;
		case 73:
			goto tr47;
		case 76:
			goto tr48;
		case 78:
			goto tr49;
		case 79:
			goto tr50;
		case 84:
			goto tr51;
		case 96:
			goto tr32;
		case 97:
			goto tr44;
		case 100:
			goto tr45;
		case 102:
			goto tr46;
		case 105:
			goto tr47;
		case 108:
			goto tr48;
		case 110:
			goto tr49;
		case 111:
			goto tr50;
		case 116:
			goto tr51;
		}
		switch {
		case  lex.data[( lex.p)] < 43:
			if 9 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 13 {
				goto tr35;
			}
		case  lex.data[( lex.p)] > 45:
			if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
				goto tr5;
			}
		default:
			goto tr39;
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
	case 30:
		if  lex.data[( lex.p)] == 39 {
			goto tr3;
		}
		goto tr52;
	case 3:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5;
		}
		goto tr1;
	case 31:
		if  lex.data[( lex.p)] == 46 {
			goto tr54;
		}
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5;
		}
		goto tr53;
	case 4:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr7;
		}
		goto tr6;
	case 32:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr7;
		}
		goto tr55;
	case 33:
		if  lex.data[( lex.p)] == 61 {
			goto tr57;
		}
		goto tr56;
	case 34:
		if  lex.data[( lex.p)] == 61 {
			goto tr59;
		}
		goto tr58;
	case 5:
		switch  lex.data[( lex.p)] {
		case 78:
			goto tr8;
		case 83:
			goto tr9;
		case 110:
			goto tr8;
		case 115:
			goto tr9;
		}
		goto tr1;
	case 6:
		switch  lex.data[( lex.p)] {
		case 68:
			goto tr10;
		case 100:
			goto tr10;
		}
		goto tr1;
	case 7:
		switch  lex.data[( lex.p)] {
		case 67:
			goto tr11;
		case 99:
			goto tr11;
		}
		goto tr1;
	case 8:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr12;
		case 101:
			goto tr12;
		}
		goto tr1;
	case 9:
		switch  lex.data[( lex.p)] {
		case 83:
			goto tr13;
		case 115:
			goto tr13;
		}
		goto tr1;
	case 10:
		switch  lex.data[( lex.p)] {
		case 67:
			goto tr14;
		case 99:
			goto tr14;
		}
		goto tr1;
	case 11:
		switch  lex.data[( lex.p)] {
		case 65:
			goto tr15;
		case 97:
			goto tr15;
		}
		goto tr1;
	case 12:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr16;
		case 108:
			goto tr16;
		}
		goto tr1;
	case 13:
		switch  lex.data[( lex.p)] {
		case 83:
			goto tr17;
		case 115:
			goto tr17;
		}
		goto tr1;
	case 14:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr18;
		case 101:
			goto tr18;
		}
		goto tr1;
	case 15:
		switch  lex.data[( lex.p)] {
		case 83:
			goto tr19;
		case 115:
			goto tr19;
		}
		goto tr1;
	case 16:
		switch  lex.data[( lex.p)] {
		case 73:
			goto tr20;
		case 105:
			goto tr20;
		}
		goto tr1;
	case 17:
		switch  lex.data[( lex.p)] {
		case 75:
			goto tr21;
		case 107:
			goto tr21;
		}
		goto tr1;
	case 18:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr22;
		case 101:
			goto tr22;
		}
		goto tr1;
	case 19:
		switch  lex.data[( lex.p)] {
		case 79:
			goto tr23;
		case 85:
			goto tr24;
		case 111:
			goto tr23;
		case 117:
			goto tr24;
		}
		goto tr1;
	case 20:
		switch  lex.data[( lex.p)] {
		case 84:
			goto tr25;
		case 116:
			goto tr25;
		}
		goto tr1;
	case 21:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr26;
		case 108:
			goto tr26;
		}
		goto tr1;
	case 22:
		switch  lex.data[( lex.p)] {
		case 76:
			goto tr27;
		case 108:
			goto tr27;
		}
		goto tr1;
	case 23:
		switch  lex.data[( lex.p)] {
		case 82:
			goto tr28;
		case 114:
			goto tr28;
		}
		goto tr1;
	case 24:
		switch  lex.data[( lex.p)] {
		case 82:
			goto tr29;
		case 114:
			goto tr29;
		}
		goto tr1;
	case 25:
		switch  lex.data[( lex.p)] {
		case 85:
			goto tr30;
		case 117:
			goto tr30;
		}
		goto tr1;
	case 26:
		switch  lex.data[( lex.p)] {
		case 69:
			goto tr31;
		case 101:
			goto tr31;
		}
		goto tr1;
	case 27:
		if  lex.data[( lex.p)] == 96 {
			goto tr33;
		}
		goto tr32;
	case 35:
		if  lex.data[( lex.p)] == 96 {
			goto tr61;
		}
		goto tr60;
	case 28:
		if  lex.data[( lex.p)] == 96 {
			goto tr32;
		}
		goto tr34;
	}

	tr1:  lex.cs = 0; goto _again
	tr36:  lex.cs = 1; goto _again
	tr3:  lex.cs = 2; goto _again
	tr39:  lex.cs = 3; goto _again
	tr54:  lex.cs = 4; goto _again
	tr44:  lex.cs = 5; goto _again
	tr8:  lex.cs = 6; goto _again
	tr9:  lex.cs = 7; goto _again
	tr45:  lex.cs = 8; goto _again
	tr12:  lex.cs = 9; goto _again
	tr13:  lex.cs = 10; goto _again
	tr46:  lex.cs = 11; goto _again
	tr15:  lex.cs = 12; goto _again
	tr16:  lex.cs = 13; goto _again
	tr17:  lex.cs = 14; goto _again
	tr47:  lex.cs = 15; goto _again
	tr48:  lex.cs = 16; goto _again
	tr20:  lex.cs = 17; goto _again
	tr21:  lex.cs = 18; goto _again
	tr49:  lex.cs = 19; goto _again
	tr23:  lex.cs = 20; goto _again
	tr24:  lex.cs = 21; goto _again
	tr26:  lex.cs = 22; goto _again
	tr50:  lex.cs = 23; goto _again
	tr51:  lex.cs = 24; goto _again
	tr29:  lex.cs = 25; goto _again
	tr30:  lex.cs = 26; goto _again
	tr32:  lex.cs = 27; goto _again
	tr61:  lex.cs = 28; goto _again
	tr0:  lex.cs = 29; goto f0
	tr2:  lex.cs = 29; goto f1
	tr6:  lex.cs = 29; goto f4
	tr10:  lex.cs = 29; goto f5
	tr11:  lex.cs = 29; goto f6
	tr14:  lex.cs = 29; goto f7
	tr18:  lex.cs = 29; goto f8
	tr19:  lex.cs = 29; goto f9
	tr22:  lex.cs = 29; goto f10
	tr25:  lex.cs = 29; goto f11
	tr27:  lex.cs = 29; goto f12
	tr28:  lex.cs = 29; goto f13
	tr31:  lex.cs = 29; goto f14
	tr34:  lex.cs = 29; goto f16
	tr35:  lex.cs = 29; goto f19
	tr37:  lex.cs = 29; goto f20
	tr38:  lex.cs = 29; goto f21
	tr40:  lex.cs = 29; goto f22
	tr42:  lex.cs = 29; goto f23
	tr52:  lex.cs = 29; goto f24
	tr53:  lex.cs = 29; goto f25
	tr55:  lex.cs = 29; goto f26
	tr56:  lex.cs = 29; goto f27
	tr57:  lex.cs = 29; goto f28
	tr58:  lex.cs = 29; goto f29
	tr59:  lex.cs = 29; goto f30
	tr60:  lex.cs = 29; goto f31
	tr4:  lex.cs = 30; goto f2
	tr5:  lex.cs = 31; goto f3
	tr7:  lex.cs = 32; goto _again
	tr41:  lex.cs = 33; goto _again
	tr43:  lex.cs = 34; goto _again
	tr33:  lex.cs = 35; goto f15

	f3: _acts = 3; goto execFuncs
	f5: _acts = 5; goto execFuncs
	f13: _acts = 7; goto execFuncs
	f11: _acts = 9; goto execFuncs
	f10: _acts = 11; goto execFuncs
	f14: _acts = 13; goto execFuncs
	f8: _acts = 15; goto execFuncs
	f12: _acts = 17; goto execFuncs
	f9: _acts = 19; goto execFuncs
	f6: _acts = 21; goto execFuncs
	f7: _acts = 23; goto execFuncs
	f23: _acts = 25; goto execFuncs
	f30: _acts = 27; goto execFuncs
	f28: _acts = 29; goto execFuncs
	f0: _acts = 31; goto execFuncs
	f20: _acts = 33; goto execFuncs
	f21: _acts = 35; goto execFuncs
	f22: _acts = 37; goto execFuncs
	f19: _acts = 39; goto execFuncs
	f25: _acts = 41; goto execFuncs
	f26: _acts = 43; goto execFuncs
	f24: _acts = 45; goto execFuncs
	f31: _acts = 47; goto execFuncs
	f29: _acts = 49; goto execFuncs
	f27: _acts = 51; goto execFuncs
	f4: _acts = 53; goto execFuncs
	f16: _acts = 55; goto execFuncs
	f1: _acts = 57; goto execFuncs
	f2: _acts = 62; goto execFuncs
	f15: _acts = 65; goto execFuncs

execFuncs:
	_nacts = uint(_filterlexer_actions[_acts]); _acts++
	for ; _nacts > 0; _nacts-- {
		_acts++
		switch _filterlexer_actions[_acts - 1] {
		case 3:
//line NONE:1
 lex.te = ( lex.p)+1

		case 4:
//line pkg/consolegraphql/filter/lex.rl:91
 lex.act = 3;
		case 5:
//line pkg/consolegraphql/filter/lex.rl:97
 lex.act = 4;
		case 6:
//line pkg/consolegraphql/filter/lex.rl:108
 lex.te = ( lex.p)+1
{ tok = AND; ( lex.p)++; goto _out
}
		case 7:
//line pkg/consolegraphql/filter/lex.rl:109
 lex.te = ( lex.p)+1
{ tok = OR; ( lex.p)++; goto _out
}
		case 8:
//line pkg/consolegraphql/filter/lex.rl:110
 lex.te = ( lex.p)+1
{ tok = NOT; ( lex.p)++; goto _out
}
		case 9:
//line pkg/consolegraphql/filter/lex.rl:111
 lex.te = ( lex.p)+1
{ tok = LIKE; ( lex.p)++; goto _out
}
		case 10:
//line pkg/consolegraphql/filter/lex.rl:112
 lex.te = ( lex.p)+1
{ tok = TRUE; ( lex.p)++; goto _out
}
		case 11:
//line pkg/consolegraphql/filter/lex.rl:113
 lex.te = ( lex.p)+1
{ tok = FALSE; ( lex.p)++; goto _out
}
		case 12:
//line pkg/consolegraphql/filter/lex.rl:114
 lex.te = ( lex.p)+1
{ tok = NULL; ( lex.p)++; goto _out
}
		case 13:
//line pkg/consolegraphql/filter/lex.rl:115
 lex.te = ( lex.p)+1
{ tok = IS; ( lex.p)++; goto _out
}
		case 14:
//line pkg/consolegraphql/filter/lex.rl:117
 lex.te = ( lex.p)+1
{ tok = ASC; ( lex.p)++; goto _out
}
		case 15:
//line pkg/consolegraphql/filter/lex.rl:118
 lex.te = ( lex.p)+1
{ tok = DESC; ( lex.p)++; goto _out
}
		case 16:
//line pkg/consolegraphql/filter/lex.rl:120
 lex.te = ( lex.p)+1
{ tok = '='; ( lex.p)++; goto _out
}
		case 17:
//line pkg/consolegraphql/filter/lex.rl:123
 lex.te = ( lex.p)+1
{ tok = GE; ( lex.p)++; goto _out
}
		case 18:
//line pkg/consolegraphql/filter/lex.rl:124
 lex.te = ( lex.p)+1
{ tok = LE; ( lex.p)++; goto _out
}
		case 19:
//line pkg/consolegraphql/filter/lex.rl:125
 lex.te = ( lex.p)+1
{ tok = NE; ( lex.p)++; goto _out
}
		case 20:
//line pkg/consolegraphql/filter/lex.rl:127
 lex.te = ( lex.p)+1
{ tok = '('; ( lex.p)++; goto _out
}
		case 21:
//line pkg/consolegraphql/filter/lex.rl:128
 lex.te = ( lex.p)+1
{ tok = ')'; ( lex.p)++; goto _out
}
		case 22:
//line pkg/consolegraphql/filter/lex.rl:130
 lex.te = ( lex.p)+1
{ tok = ','; ( lex.p)++; goto _out
}
		case 23:
//line pkg/consolegraphql/filter/lex.rl:132
 lex.te = ( lex.p)+1

		case 24:
//line pkg/consolegraphql/filter/lex.rl:71
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
//line pkg/consolegraphql/filter/lex.rl:81
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
//line pkg/consolegraphql/filter/lex.rl:91
 lex.te = ( lex.p)
( lex.p)--
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            ( lex.p)++; goto _out
 }
		case 27:
//line pkg/consolegraphql/filter/lex.rl:97
 lex.te = ( lex.p)
( lex.p)--
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1)
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse("{" + val + "}")
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath, val)
            ( lex.p)++; goto _out
 }
		case 28:
//line pkg/consolegraphql/filter/lex.rl:121
 lex.te = ( lex.p)
( lex.p)--
{ tok = '>'; ( lex.p)++; goto _out
}
		case 29:
//line pkg/consolegraphql/filter/lex.rl:122
 lex.te = ( lex.p)
( lex.p)--
{ tok = '<'; ( lex.p)++; goto _out
}
		case 30:
//line pkg/consolegraphql/filter/lex.rl:71
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
//line pkg/consolegraphql/filter/lex.rl:97
( lex.p) = ( lex.te) - 1
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1)
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse("{" + val + "}")
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath, val)
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

            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1)
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse("{" + val + "}")
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath, val)
            ( lex.p)++; goto _out
 }
	}
	
//line pkg/consolegraphql/filter/lex.go:767
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

//line pkg/consolegraphql/filter/lex.go:786
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
		case 30:
			goto tr52
		case 31:
			goto tr53
		case 4:
			goto tr6
		case 32:
			goto tr55
		case 33:
			goto tr56
		case 34:
			goto tr58
		case 27:
			goto tr2
		case 35:
			goto tr60
		case 28:
			goto tr34
	}
	}

	_out: {}
	}

//line pkg/consolegraphql/filter/lex.rl:136


    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

