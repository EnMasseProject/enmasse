%lex
%options flex case-insensitive
%%

\s*\n\s*                  /* ignore */

[\w]?\"(\\.|[^\\"])*\"    return 'STRING_LITERAL_D';
[\w]?\'(\\.|[^\\'])*\'    return 'STRING_LITERAL_S';
[\w]?\`(\\.|[^\\`])*\`    return 'JSON_PATH';

"("                       return 'LPAREN';
")"                       return 'RPAREN';

"not"                     return 'NOT';

"in"                      return 'IN';
">="                      return 'GTE';
"<="                      return 'LTE';
">"                       return 'GT';
"<"                       return 'LT';
"="                       return 'EQ';
"!="                      return 'NEQ';

"like"                    return 'LIKE';

"and"                     return 'AND';
"or"                      return 'OR';

[A-Za-z0-9_\-\.:]+        return 'IDENT';

\s+                       /* */
.                         return 'INVALID';
<<EOF>>                   return 'EOF';

/lex

/* operator associations and precedence */
%right OR AND
%left EQ LIKE IN GTE GT LTE LT
%left NOT

/* start symbol */
%start statement

%% /* language grammar */

statement
:   query EOF
{
return yy.booleanExpression($1);
}
;

query
:   LPAREN query RPAREN
{$$ = yy.booleanExpression($2)}
|   query AND query
{$$ = yy.logicExpression.createAnd($1, $3)}
|   query OR query
{$$ = yy.logicExpression.createOr($1, $3)}
|   NOT query
{$$ = yy.unaryExpression.createNot($2)}
|   literal EQ literal
{$$ = yy.binaryExpression.equalsExpression($1, $3)}
|   literal NEQ literal
{$$ = yy.binaryExpression.notEqualsExpression($1, $3)}
|   literal LIKE literal
{$$ = yy.binaryExpression.likeExpression($1, $3)}
|   literal GT literal
{$$ = yy.binaryExpression.greaterThanExpression($1, $3)}
|   literal GTE literal
{$$ = yy.binaryExpression.greaterThanEqualsExpression($1, $3)}
|   literal LT literal
{$$ = yy.binaryExpression.lessThanExpression($1, $3)}
|   literal LTE literal
{$$ = yy.binaryExpression.lessThanEqualsExpression($1, $3)}
;

literal
:   STRING_LITERAL_S
{$$ = yy.constantExpression.createString($1)}
|   STRING_LITERAL_D
{$$ = yy.constantExpression.createString($1)}
|   JSON_PATH
{$$ = yy.constantExpression.createJsonPath($1)}
;