grammar MesoQL;

options {
    caseInsensitive = true;
}

// ============================================================
// Parser Rules
// ============================================================

query
    : searchClause whereClause outputClause* EOF
    ;

searchClause
    : SEARCH source
    ;

// Fixed for Phase 1; designed for extension
source
    : STORM_EVENTS
    | FORECAST_DISCUSSIONS
    ;

whereClause
    : WHERE semanticClause filterClause*
    ;

// SEMANTIC is required ; every MesoQL query is a hybrid query
semanticClause
    : SEMANTIC LPAREN STRING RPAREN
    ;

filterClause
    : AND filter
    ;

filter
    : field IN LPAREN stringList RPAREN                         # inFilter
    | field BETWEEN numericLiteral AND numericLiteral           # betweenFilter
    | field comparisonOp literal                                # comparisonFilter
    ;

// Output clauses ; all optional, any order
outputClause
    : synthesizeClause
    | clusterClause
    | explainClause
    | limitClause
    ;

synthesizeClause
    : SYNTHESIZE STRING
    ;

clusterClause
    : CLUSTER BY THEME
    ;

explainClause
    : EXPLAIN
    ;

limitClause
    : LIMIT INTEGER
    ;

// ============================================================
// Shared Rules
// ============================================================

stringList
    : STRING (COMMA STRING)*
    ;

comparisonOp
    : EQ | NEQ | GT | LT | GTE | LTE
    ;

literal
    : STRING
    | numericLiteral
    ;

numericLiteral
    : INTEGER
    | DECIMAL
    ;

field
    : ID
    ;

// ============================================================
// Lexer Rules ; Keywords
// ============================================================

SEARCH               : 'search' ;
WHERE                : 'where' ;
AND                  : 'and' ;
SEMANTIC             : 'semantic' ;
IN                   : 'in' ;
BETWEEN              : 'between' ;
SYNTHESIZE           : 'synthesize' ;
CLUSTER              : 'cluster' ;
BY                   : 'by' ;
THEME                : 'theme' ;
EXPLAIN              : 'explain' ;
LIMIT                : 'limit' ;

// Sources
STORM_EVENTS         : 'storm_events' ;
FORECAST_DISCUSSIONS : 'forecast_discussions' ;

// ============================================================
// Lexer Rules ; Operators and Delimiters
// ============================================================

GTE     : '>=' ;
LTE     : '<=' ;
NEQ     : '!=' ;
GT      : '>' ;
LT      : '<' ;
EQ      : '=' ;

LPAREN  : '(' ;
RPAREN  : ')' ;
COMMA   : ',' ;

// ============================================================
// Lexer Rules ; Literals and Identifiers
// ============================================================

INTEGER : DIGIT+ ;
DECIMAL : DIGIT+ '.' DIGIT+ ;
STRING  : '"' (~["\r\n] | '\\"')* '"' ;
ID      : [a-zA-Z_][a-zA-Z_0-9]* ;

WS      : [ \t\r\n]+ -> skip ;

fragment DIGIT : [0-9] ;
