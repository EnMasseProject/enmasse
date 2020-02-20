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
    %% write init;
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

    %%{
        squote      = "'";
        not_squote      = [^'];
        dble_squote      = "''";
        squoted_string = squote (not_squote | dble_squote)* squote;

        bquote      = "`";
        not_bquote      = [^`];
        dble_bquote      = "```";
        bquoted_string = bquote (not_bquote | dble_bquote)* bquote;

        float_num =  [+\-]? digit+ '.'  digit+;
        integral_num =  [+\-]? digit+;
        main := |*
            integral_num => {
            val, err := strconv.Atoi(string(lex.data[lex.ts:lex.te]));
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.integralValue = IntVal(val)
            tok = INTEGRAL;
            fbreak;};

            float_num => {
            val, err := strconv.ParseFloat(string(lex.data[lex.ts:lex.te]), 64);
            if err != nil {
                // Should not happen as we've already sanitized the input
                panic(err)
            }
            out.floatValue = FloatVal(val)
            tok = FLOAT;
            fbreak;};

            squoted_string => {
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "''", "'", -1)
            tok = STRING
            out.stringValue = StringVal(val)
            fbreak; };

            bquoted_string => {
            val := strings.Replace(string(lex.data[lex.ts+1:lex.te-1]), "```", "`", -1)
            tok = JSON_PATH
            jsonPath := jsonpath.New("filter expr").AllowMissingKeys(true)
            err := jsonPath.Parse("{" + val + "}")
            if err != nil {
                lex.e = err
            }
            out.jsonPathValue = NewJSONPathVal(jsonPath, val)
            fbreak; };

            /AND/i => { tok = AND; fbreak;};
            /OR/i => { tok = OR; fbreak;};
            /NOT/i => { tok = NOT; fbreak;};
            /LIKE/i => { tok = LIKE; fbreak;};
            /TRUE/i => { tok = TRUE; fbreak;};
            /FALSE/i => { tok = FALSE; fbreak;};
            /NULL/i => { tok = NULL; fbreak;};
            /IS/i => { tok = IS; fbreak;};

            /ASC/i => { tok = ASC; fbreak;};
            /DESC/i => { tok = DESC; fbreak;};

            '=' => { tok = '='; fbreak;};
            '>' => { tok = '>'; fbreak;};
            '<' => { tok = '<'; fbreak;};
            '>=' => { tok = GE; fbreak;};
            '<=' => { tok = LE; fbreak;};
            '!=' => { tok = NE; fbreak;};

            '(' => { tok = '('; fbreak;};
            ')' => { tok = ')'; fbreak;};

            ',' => { tok = ','; fbreak;};

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

