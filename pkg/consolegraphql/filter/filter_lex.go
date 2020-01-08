
//line filter_lex.rl:1
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


//line filter_lex.go:20
const filterlexer_start int = 25
const filterlexer_first_final int = 25
const filterlexer_error int = 0

const filterlexer_en_main int = 25


//line filter_lex.rl:22


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
    
//line filter_lex.go:45
	{
	 lex.cs = filterlexer_start
	 lex.ts = 0
	 lex.te = 0
	 lex.act = 0
	}

//line filter_lex.rl:38
    return lex
}

func (lex *lexer) Lex(out *FilterSymType) int {
    eof := lex.pe
    tok := 0
    
//line filter_lex.go:61
	{
	if ( lex.p) == ( lex.pe) {
		goto _test_eof
	}
	switch  lex.cs {
	case 25:
		goto st_case_25
	case 0:
		goto st_case_0
	case 1:
		goto st_case_1
	case 2:
		goto st_case_2
	case 26:
		goto st_case_26
	case 3:
		goto st_case_3
	case 27:
		goto st_case_27
	case 4:
		goto st_case_4
	case 28:
		goto st_case_28
	case 29:
		goto st_case_29
	case 30:
		goto st_case_30
	case 5:
		goto st_case_5
	case 6:
		goto st_case_6
	case 7:
		goto st_case_7
	case 8:
		goto st_case_8
	case 9:
		goto st_case_9
	case 10:
		goto st_case_10
	case 11:
		goto st_case_11
	case 12:
		goto st_case_12
	case 13:
		goto st_case_13
	case 14:
		goto st_case_14
	case 15:
		goto st_case_15
	case 16:
		goto st_case_16
	case 17:
		goto st_case_17
	case 18:
		goto st_case_18
	case 19:
		goto st_case_19
	case 20:
		goto st_case_20
	case 21:
		goto st_case_21
	case 22:
		goto st_case_22
	case 23:
		goto st_case_23
	case 31:
		goto st_case_31
	case 24:
		goto st_case_24
	}
	goto st_out
tr0:
//line filter_lex.rl:109
 lex.te = ( lex.p)+1
{ tok = NE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr2:
//line NONE:1
	switch  lex.act {
	case 0:
	{{goto st0 }}
	case 3:
	{( lex.p) = ( lex.te) - 1

            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            {( lex.p)++;  lex.cs = 25; goto _out } }
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
            {( lex.p)++;  lex.cs = 25; goto _out } }
	}
	
	goto st25
tr6:
//line filter_lex.rl:58
( lex.p) = ( lex.te) - 1
{
            val, err := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr9:
//line filter_lex.rl:95
 lex.te = ( lex.p)+1
{ tok = AND; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr13:
//line filter_lex.rl:100
 lex.te = ( lex.p)+1
{ tok = FALSE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr14:
//line filter_lex.rl:102
 lex.te = ( lex.p)+1
{ tok = IS; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr17:
//line filter_lex.rl:98
 lex.te = ( lex.p)+1
{ tok = LIKE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr20:
//line filter_lex.rl:97
 lex.te = ( lex.p)+1
{ tok = NOT; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr22:
//line filter_lex.rl:101
 lex.te = ( lex.p)+1
{ tok = NULL; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr23:
//line filter_lex.rl:96
 lex.te = ( lex.p)+1
{ tok = OR; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr26:
//line filter_lex.rl:99
 lex.te = ( lex.p)+1
{ tok = TRUE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr29:
//line filter_lex.rl:84
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
            {( lex.p)++;  lex.cs = 25; goto _out } }
	goto st25
tr30:
//line filter_lex.rl:114
 lex.te = ( lex.p)+1

	goto st25
tr32:
//line filter_lex.rl:111
 lex.te = ( lex.p)+1
{ tok = '('; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr33:
//line filter_lex.rl:112
 lex.te = ( lex.p)+1
{ tok = ')'; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr36:
//line filter_lex.rl:104
 lex.te = ( lex.p)+1
{ tok = '='; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr45:
//line filter_lex.rl:78
 lex.te = ( lex.p)
( lex.p)--
{
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            {( lex.p)++;  lex.cs = 25; goto _out } }
	goto st25
tr46:
//line filter_lex.rl:58
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
            {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr48:
//line filter_lex.rl:68
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
            {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr49:
//line filter_lex.rl:106
 lex.te = ( lex.p)
( lex.p)--
{ tok = '<'; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr50:
//line filter_lex.rl:108
 lex.te = ( lex.p)+1
{ tok = LE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr51:
//line filter_lex.rl:105
 lex.te = ( lex.p)
( lex.p)--
{ tok = '>'; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr52:
//line filter_lex.rl:107
 lex.te = ( lex.p)+1
{ tok = GE; {( lex.p)++;  lex.cs = 25; goto _out }}
	goto st25
tr53:
//line filter_lex.rl:84
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
            {( lex.p)++;  lex.cs = 25; goto _out } }
	goto st25
	st25:
//line NONE:1
 lex.ts = 0

//line NONE:1
 lex.act = 0

		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof25
		}
	st_case_25:
//line NONE:1
 lex.ts = ( lex.p)

//line filter_lex.go:341
		switch  lex.data[( lex.p)] {
		case 32:
			goto tr30
		case 33:
			goto st1
		case 39:
			goto st2
		case 40:
			goto tr32
		case 41:
			goto tr33
		case 43:
			goto st3
		case 45:
			goto st3
		case 60:
			goto st29
		case 61:
			goto tr36
		case 62:
			goto st30
		case 65:
			goto st5
		case 70:
			goto st7
		case 73:
			goto st11
		case 76:
			goto st12
		case 78:
			goto st15
		case 79:
			goto st19
		case 84:
			goto st20
		case 96:
			goto st23
		}
		switch {
		case  lex.data[( lex.p)] > 13:
			if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
				goto tr5
			}
		case  lex.data[( lex.p)] >= 9:
			goto tr30
		}
		goto st0
st_case_0:
	st0:
		 lex.cs = 0
		goto _out
	st1:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof1
		}
	st_case_1:
		if  lex.data[( lex.p)] == 61 {
			goto tr0
		}
		goto st0
	st2:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof2
		}
	st_case_2:
		if  lex.data[( lex.p)] == 39 {
			goto tr4
		}
		goto st2
tr4:
//line NONE:1
 lex.te = ( lex.p)+1

//line filter_lex.rl:78
 lex.act = 3;
	goto st26
	st26:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof26
		}
	st_case_26:
//line filter_lex.go:423
		if  lex.data[( lex.p)] == 39 {
			goto st2
		}
		goto tr45
	st3:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof3
		}
	st_case_3:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5
		}
		goto st0
tr5:
//line NONE:1
 lex.te = ( lex.p)+1

	goto st27
	st27:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof27
		}
	st_case_27:
//line filter_lex.go:447
		if  lex.data[( lex.p)] == 46 {
			goto st4
		}
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr5
		}
		goto tr46
	st4:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof4
		}
	st_case_4:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto st28
		}
		goto tr6
	st28:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof28
		}
	st_case_28:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto st28
		}
		goto tr48
	st29:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof29
		}
	st_case_29:
		if  lex.data[( lex.p)] == 61 {
			goto tr50
		}
		goto tr49
	st30:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof30
		}
	st_case_30:
		if  lex.data[( lex.p)] == 61 {
			goto tr52
		}
		goto tr51
	st5:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof5
		}
	st_case_5:
		if  lex.data[( lex.p)] == 78 {
			goto st6
		}
		goto st0
	st6:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof6
		}
	st_case_6:
		if  lex.data[( lex.p)] == 68 {
			goto tr9
		}
		goto st0
	st7:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof7
		}
	st_case_7:
		if  lex.data[( lex.p)] == 65 {
			goto st8
		}
		goto st0
	st8:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof8
		}
	st_case_8:
		if  lex.data[( lex.p)] == 76 {
			goto st9
		}
		goto st0
	st9:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof9
		}
	st_case_9:
		if  lex.data[( lex.p)] == 83 {
			goto st10
		}
		goto st0
	st10:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof10
		}
	st_case_10:
		if  lex.data[( lex.p)] == 69 {
			goto tr13
		}
		goto st0
	st11:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof11
		}
	st_case_11:
		if  lex.data[( lex.p)] == 83 {
			goto tr14
		}
		goto st0
	st12:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof12
		}
	st_case_12:
		if  lex.data[( lex.p)] == 73 {
			goto st13
		}
		goto st0
	st13:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof13
		}
	st_case_13:
		if  lex.data[( lex.p)] == 75 {
			goto st14
		}
		goto st0
	st14:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof14
		}
	st_case_14:
		if  lex.data[( lex.p)] == 69 {
			goto tr17
		}
		goto st0
	st15:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof15
		}
	st_case_15:
		switch  lex.data[( lex.p)] {
		case 79:
			goto st16
		case 85:
			goto st17
		}
		goto st0
	st16:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof16
		}
	st_case_16:
		if  lex.data[( lex.p)] == 84 {
			goto tr20
		}
		goto st0
	st17:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof17
		}
	st_case_17:
		if  lex.data[( lex.p)] == 76 {
			goto st18
		}
		goto st0
	st18:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof18
		}
	st_case_18:
		if  lex.data[( lex.p)] == 76 {
			goto tr22
		}
		goto st0
	st19:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof19
		}
	st_case_19:
		if  lex.data[( lex.p)] == 82 {
			goto tr23
		}
		goto st0
	st20:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof20
		}
	st_case_20:
		if  lex.data[( lex.p)] == 82 {
			goto st21
		}
		goto st0
	st21:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof21
		}
	st_case_21:
		if  lex.data[( lex.p)] == 85 {
			goto st22
		}
		goto st0
	st22:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof22
		}
	st_case_22:
		if  lex.data[( lex.p)] == 69 {
			goto tr26
		}
		goto st0
	st23:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof23
		}
	st_case_23:
		if  lex.data[( lex.p)] == 96 {
			goto tr28
		}
		goto st23
tr28:
//line NONE:1
 lex.te = ( lex.p)+1

//line filter_lex.rl:84
 lex.act = 4;
	goto st31
	st31:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof31
		}
	st_case_31:
//line filter_lex.go:677
		if  lex.data[( lex.p)] == 96 {
			goto st24
		}
		goto tr53
	st24:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof24
		}
	st_case_24:
		if  lex.data[( lex.p)] == 96 {
			goto st23
		}
		goto tr29
	st_out:
	_test_eof25:  lex.cs = 25; goto _test_eof
	_test_eof1:  lex.cs = 1; goto _test_eof
	_test_eof2:  lex.cs = 2; goto _test_eof
	_test_eof26:  lex.cs = 26; goto _test_eof
	_test_eof3:  lex.cs = 3; goto _test_eof
	_test_eof27:  lex.cs = 27; goto _test_eof
	_test_eof4:  lex.cs = 4; goto _test_eof
	_test_eof28:  lex.cs = 28; goto _test_eof
	_test_eof29:  lex.cs = 29; goto _test_eof
	_test_eof30:  lex.cs = 30; goto _test_eof
	_test_eof5:  lex.cs = 5; goto _test_eof
	_test_eof6:  lex.cs = 6; goto _test_eof
	_test_eof7:  lex.cs = 7; goto _test_eof
	_test_eof8:  lex.cs = 8; goto _test_eof
	_test_eof9:  lex.cs = 9; goto _test_eof
	_test_eof10:  lex.cs = 10; goto _test_eof
	_test_eof11:  lex.cs = 11; goto _test_eof
	_test_eof12:  lex.cs = 12; goto _test_eof
	_test_eof13:  lex.cs = 13; goto _test_eof
	_test_eof14:  lex.cs = 14; goto _test_eof
	_test_eof15:  lex.cs = 15; goto _test_eof
	_test_eof16:  lex.cs = 16; goto _test_eof
	_test_eof17:  lex.cs = 17; goto _test_eof
	_test_eof18:  lex.cs = 18; goto _test_eof
	_test_eof19:  lex.cs = 19; goto _test_eof
	_test_eof20:  lex.cs = 20; goto _test_eof
	_test_eof21:  lex.cs = 21; goto _test_eof
	_test_eof22:  lex.cs = 22; goto _test_eof
	_test_eof23:  lex.cs = 23; goto _test_eof
	_test_eof31:  lex.cs = 31; goto _test_eof
	_test_eof24:  lex.cs = 24; goto _test_eof

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

//line filter_lex.rl:118


    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

