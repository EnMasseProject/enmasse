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
)

%%{
    machine filterlexer;
    write data;
    access lex.;
    variable p lex.p;
    variable pe lex.pe;
}%%

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
    %% write init;
    return lex
}

func (lex *lexer) Lex(out *FilterSymType) int {
    eof := lex.pe
    tok := 0
    %%{
        squote      = "'";
        not_squote      = [^'];
        dble_squote      = "''";
        newline      = "\n";
        quoted_string = squote (not_squote | dble_squote)* squote;
        float_num =  [+\-]? digit+ '.'  digit+;
        integral_num =  [+\-]? digit+;
        main := |*
            integral_num => { val, _ := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            fbreak;};

            float_num => { val, _ := strconv.ParseFloat(string(lex.data[lex.ts:lex.te]), 64);
            out.floatValue = FloatVal(val)
            tok = FLOAT;
            fbreak;};

            quoted_string => {
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            fbreak; };

            'AND' => { tok = AND; fbreak;};
            'OR' => { tok = OR; fbreak;};
            'NOT' => { tok = NOT; fbreak;};
            'LIKE' => { tok = LIKE; fbreak;};
            'TRUE' => { tok = TRUE; fbreak;};
            'FALSE' => { tok = FALSE; fbreak;};
            'NULL' => { tok = NULL; fbreak;};

            '=' => { tok = '='; fbreak;};
            '>' => { tok = '>'; fbreak;};
            '<' => { tok = '<'; fbreak;};
            '>=' => { tok = GE; fbreak;};
            '<=' => { tok = LE; fbreak;};
            '!=' => { tok = NE; fbreak;};

            '(' => { tok = '('; fbreak;};
            ')' => { tok = ')'; fbreak;};

            space;
        *|;

         write exec;
    }%%

    return tok;
}

func (lex *lexer) Error(e string) {
    lex.e = fmt.Errorf("error: %s", e)
}

func (lex *lexer) GetError() error {
    return lex.e
}

