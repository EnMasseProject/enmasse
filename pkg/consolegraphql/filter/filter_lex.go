
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
)


//line filter_lex.go:18
const filterlexer_start int = 22
const filterlexer_first_final int = 22
const filterlexer_error int = 0

const filterlexer_en_main int = 22


//line filter_lex.rl:20


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
    
//line filter_lex.go:43
	{
	 lex.cs = filterlexer_start
	 lex.ts = 0
	 lex.te = 0
	 lex.act = 0
	}

//line filter_lex.rl:36
    return lex
}

func (lex *lexer) Lex(out *FilterSymType) int {
    eof := lex.pe
    tok := 0
    
//line filter_lex.go:59
	{
	if ( lex.p) == ( lex.pe) {
		goto _test_eof
	}
	switch  lex.cs {
	case 22:
		goto st_case_22
	case 0:
		goto st_case_0
	case 1:
		goto st_case_1
	case 2:
		goto st_case_2
	case 3:
		goto st_case_3
	case 23:
		goto st_case_23
	case 4:
		goto st_case_4
	case 24:
		goto st_case_24
	case 25:
		goto st_case_25
	case 26:
		goto st_case_26
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
	}
	goto st_out
tr0:
//line filter_lex.rl:71
 lex.te = ( lex.p)+1
{ tok = NE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr3:
//line filter_lex.rl:57
 lex.te = ( lex.p)+1
{ tok = STRING; out.stringValue = StringVal(lex.data[lex.ts+1:lex.te-1]); {( lex.p)++;  lex.cs = 22; goto _out } }
	goto st22
tr5:
//line filter_lex.rl:49
( lex.p) = ( lex.te) - 1
{ val, _ := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr8:
//line filter_lex.rl:58
 lex.te = ( lex.p)+1
{ tok = AND; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr12:
//line filter_lex.rl:63
 lex.te = ( lex.p)+1
{ tok = FALSE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr15:
//line filter_lex.rl:61
 lex.te = ( lex.p)+1
{ tok = LIKE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr18:
//line filter_lex.rl:60
 lex.te = ( lex.p)+1
{ tok = NOT; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr20:
//line filter_lex.rl:64
 lex.te = ( lex.p)+1
{ tok = NULL; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr21:
//line filter_lex.rl:59
 lex.te = ( lex.p)+1
{ tok = OR; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr24:
//line filter_lex.rl:62
 lex.te = ( lex.p)+1
{ tok = TRUE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr25:
//line filter_lex.rl:76
 lex.te = ( lex.p)+1

	goto st22
tr27:
//line filter_lex.rl:73
 lex.te = ( lex.p)+1
{ tok = '('; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr28:
//line filter_lex.rl:74
 lex.te = ( lex.p)+1
{ tok = ')'; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr31:
//line filter_lex.rl:66
 lex.te = ( lex.p)+1
{ tok = '='; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr39:
//line filter_lex.rl:49
 lex.te = ( lex.p)
( lex.p)--
{ val, _ := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr41:
//line filter_lex.rl:53
 lex.te = ( lex.p)
( lex.p)--
{ val, _ := strconv.ParseFloat(string(lex.data[lex.ts:lex.te]), 64);
            out.floatValue = FloatVal(val)
            tok = FLOAT;
            {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr42:
//line filter_lex.rl:68
 lex.te = ( lex.p)
( lex.p)--
{ tok = '<'; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr43:
//line filter_lex.rl:70
 lex.te = ( lex.p)+1
{ tok = LE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr44:
//line filter_lex.rl:67
 lex.te = ( lex.p)
( lex.p)--
{ tok = '>'; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
tr45:
//line filter_lex.rl:69
 lex.te = ( lex.p)+1
{ tok = GE; {( lex.p)++;  lex.cs = 22; goto _out }}
	goto st22
	st22:
//line NONE:1
 lex.ts = 0

		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof22
		}
	st_case_22:
//line NONE:1
 lex.ts = ( lex.p)

//line filter_lex.go:245
		switch  lex.data[( lex.p)] {
		case 32:
			goto tr25
		case 33:
			goto st1
		case 39:
			goto st2
		case 40:
			goto tr27
		case 41:
			goto tr28
		case 43:
			goto st3
		case 45:
			goto st3
		case 60:
			goto st25
		case 61:
			goto tr31
		case 62:
			goto st26
		case 65:
			goto st5
		case 70:
			goto st7
		case 76:
			goto st11
		case 78:
			goto st14
		case 79:
			goto st18
		case 84:
			goto st19
		}
		switch {
		case  lex.data[( lex.p)] > 13:
			if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
				goto tr4
			}
		case  lex.data[( lex.p)] >= 9:
			goto tr25
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
		switch  lex.data[( lex.p)] {
		case 10:
			goto st0
		case 39:
			goto tr3
		}
		goto st2
	st3:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof3
		}
	st_case_3:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr4
		}
		goto st0
tr4:
//line NONE:1
 lex.te = ( lex.p)+1

	goto st23
	st23:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof23
		}
	st_case_23:
//line filter_lex.go:333
		if  lex.data[( lex.p)] == 46 {
			goto st4
		}
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto tr4
		}
		goto tr39
	st4:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof4
		}
	st_case_4:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto st24
		}
		goto tr5
	st24:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof24
		}
	st_case_24:
		if 48 <=  lex.data[( lex.p)] &&  lex.data[( lex.p)] <= 57 {
			goto st24
		}
		goto tr41
	st25:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof25
		}
	st_case_25:
		if  lex.data[( lex.p)] == 61 {
			goto tr43
		}
		goto tr42
	st26:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof26
		}
	st_case_26:
		if  lex.data[( lex.p)] == 61 {
			goto tr45
		}
		goto tr44
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
			goto tr8
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
			goto tr12
		}
		goto st0
	st11:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof11
		}
	st_case_11:
		if  lex.data[( lex.p)] == 73 {
			goto st12
		}
		goto st0
	st12:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof12
		}
	st_case_12:
		if  lex.data[( lex.p)] == 75 {
			goto st13
		}
		goto st0
	st13:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof13
		}
	st_case_13:
		if  lex.data[( lex.p)] == 69 {
			goto tr15
		}
		goto st0
	st14:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof14
		}
	st_case_14:
		switch  lex.data[( lex.p)] {
		case 79:
			goto st15
		case 85:
			goto st16
		}
		goto st0
	st15:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof15
		}
	st_case_15:
		if  lex.data[( lex.p)] == 84 {
			goto tr18
		}
		goto st0
	st16:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof16
		}
	st_case_16:
		if  lex.data[( lex.p)] == 76 {
			goto st17
		}
		goto st0
	st17:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof17
		}
	st_case_17:
		if  lex.data[( lex.p)] == 76 {
			goto tr20
		}
		goto st0
	st18:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof18
		}
	st_case_18:
		if  lex.data[( lex.p)] == 82 {
			goto tr21
		}
		goto st0
	st19:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof19
		}
	st_case_19:
		if  lex.data[( lex.p)] == 82 {
			goto st20
		}
		goto st0
	st20:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof20
		}
	st_case_20:
		if  lex.data[( lex.p)] == 85 {
			goto st21
		}
		goto st0
	st21:
		if ( lex.p)++; ( lex.p) == ( lex.pe) {
			goto _test_eof21
		}
	st_case_21:
		if  lex.data[( lex.p)] == 69 {
			goto tr24
		}
		goto st0
	st_out:
	_test_eof22:  lex.cs = 22; goto _test_eof
	_test_eof1:  lex.cs = 1; goto _test_eof
	_test_eof2:  lex.cs = 2; goto _test_eof
	_test_eof3:  lex.cs = 3; goto _test_eof
	_test_eof23:  lex.cs = 23; goto _test_eof
	_test_eof4:  lex.cs = 4; goto _test_eof
	_test_eof24:  lex.cs = 24; goto _test_eof
	_test_eof25:  lex.cs = 25; goto _test_eof
	_test_eof26:  lex.cs = 26; goto _test_eof
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

	_test_eof: {}
	if ( lex.p) == eof {
		switch  lex.cs {
		case 23:
			goto tr39
		case 4:
			goto tr5
		case 24:
			goto tr41
		case 25:
			goto tr42
		case 26:
			goto tr44
		}
	}

	_out: {}
	}

//line filter_lex.rl:80


    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

