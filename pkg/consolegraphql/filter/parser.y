/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

 /* Based on original work from: https://github.com/xwb1989/sqlparser/blob/master/sql.y */

%{
package filter

func setExpressionTree(yylex interface{}, expr Expr) {
  yylex.(*lexer).expr = expr
}

func setOrderByTree(yylex interface{}, expr OrderBy) {
  yylex.(*lexer).orderBy = expr
}

%}

%union {
  bytes         []byte
  stringValue   StringVal
  integralValue	IntVal
  floatValue    FloatVal
  jsonPathValue JSONPathVal
  boolVal       BoolVal

  orderList     OrderBy
  order		*Order

  expr          Expr
  str           string
}

%type <expr> expression
%type <expr> condition
%type <str> compare
%type <expr> value
%type <expr> value_expression
%type <boolVal> boolean_value

%left <bytes> OR
%left <bytes> AND
%right <bytes> NOT


%left <bytes> '=' '<' '>' LE GE NE LIKE IS

%token <bytes> '('
%token <bytes> ')'

%token <stringValue> STRING
%token <integralValue> INTEGRAL
%token <floatValue> FLOAT
%token <jsonPathValue> JSON_PATH

%token <bytes> NULL TRUE FALSE

%token <bytes> START_EXPRESSION

%type <orderList> order_list
%type <order> order
%type <order> order
%type <str> asc_desc_opt

%token <bytes> ASC DESC
%token <bytes> ','

%token <bytes> START_ORDER_LIST

%start top


%%

top:
  START_EXPRESSION expression
  {
    setExpressionTree(Filterlex, $2)
  }
|
  START_ORDER_LIST order_list
  {
    setOrderByTree(Filterlex, $2)
  }

expression:
  condition
  {
    $$ = $1
  }
|  '(' expression ')'
  {
    $$ = NewBoolExpr($2)
  }
| expression AND expression
  {
    $$ = NewAndExpr($1,$3)
  }
| expression OR expression
  {
    $$ = NewOrExpr($1,$3)
  }
| NOT expression
  {
    $$ = NewNotExpr($2)
  }
| boolean_value
  {
    $$ = $1
  }

condition:
  value_expression compare value_expression
  {
    $$ = NewComparisonExpr($1, $2, $3)
  }
| value_expression LIKE value_expression
  {
    $$ = NewComparisonExpr($1, LikeStr, $3)
  }
| value_expression NOT LIKE value_expression
  {
    $$ = NewComparisonExpr($1, NotLikeStr, $4)
  }
| value_expression IS NULL
  {
    $$ = NewIsNullExpr($1, IsNull)
  }
| value_expression IS NOT NULL
  {
    $$ = NewIsNullExpr($1, IsNotNull)
  }


value_expression:
  value
  {
    $$ = $1
  }
| boolean_value
  {
    $$ = $1
  }

compare:
  '='
  {
    $$ = EqualStr
  }
| '<'
  {
    $$ = LessThanStr
  }
| '>'
  {
    $$ = GreaterThanStr
  }
| LE
  {
    $$ = LessEqualStr
  }
| GE
  {
    $$ = GreaterEqualStr
  }
| NE
  {
    $$ = NotEqualStr
  }

value:
  STRING
  {
    $$ = StringVal($1)
  }
| INTEGRAL
  {
    $$ = IntVal($1)
  }
| FLOAT
  {
    $$ = FloatVal($1)
  }
| JSON_PATH
  {
    $$ = $1
  }
| NULL
  {
    $$ = NewNullVal()
  }

boolean_value:
  TRUE
  {
    $$ = BoolVal(true)
  }
| FALSE
  {
    $$ = BoolVal(false)
  }

order_list:
  {
    $$ = NewEmptyOrderBy()
  }
| order
  {
    $$ = NewOrderBy($1)
  }
| order_list ',' order
  {
    $$ = append($1, $3)
  }

order:
  JSON_PATH asc_desc_opt
  {
    $$ = NewOrder($1, $2)
  }

asc_desc_opt:
  {
    $$ = AscScr
  }
| ASC
  {
    $$ = AscScr
  }
| DESC
  {
    $$ = DescScr
  }
