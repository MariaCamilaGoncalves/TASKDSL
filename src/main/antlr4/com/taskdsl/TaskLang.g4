grammar TaskLang;

// =========================================================
// TaskLang - DSL para gerenciamento de tarefas (task management)
// =========================================================

// ---------------- REGRAS SINTÁTICAS ----------------

program
    : statement* EOF
    ;

statement
    : varDecl
    | assignment
    | ifStmt
    | whileStmt
    | forStmt
    | printStmt
    | taskDecl
    | assignTaskStmt
    | completeStmt
    | listTasksStmt
    | block
    ;

block
    : '{' statement* '}'
    ;

type
    : INT
    | FLOAT
    | STRING
    ;

varDecl
    : type ID ('=' expr)? ';'
    ;

assignment
    : ID '=' expr ';'
    ;

ifStmt
    : IF '(' expr ')' block (ELSE block)?
    ;

whileStmt
    : WHILE '(' expr ')' block
    ;

// for (int i = 0; i < 10; i = i + 1) { ... }
forStmt
    : FOR '(' forInit ';' expr ';' ID '=' expr ')' block
    ;

forInit
    : type ID '=' expr
    | ID '=' expr
    ;

printStmt
    : PRINT '(' expr ')' ';'
    ;

// ---------------- COMANDOS DE DOMÍNIO (exclusivos da DSL) ----------------

// task "Escrever relatório" priority 2;
taskDecl
    : TASK expr PRIORITY expr ';'
    ;

// assign "Escrever relatório" to "Mariana";
assignTaskStmt
    : ASSIGN expr TO expr ';'
    ;

// complete "Escrever relatório";
completeStmt
    : COMPLETE expr ';'
    ;

// list tasks;
listTasksStmt
    : LIST TASKS ';'
    ;

// ---------------- EXPRESSÕES ----------------

expr
    : '(' expr ')'                                    # ParenExpr
    | op=('-' | '!') expr                              # UnaryExpr
    | expr op=('*' | '/' | '%') expr                   # MulDivExpr
    | expr op=('+' | '-') expr                         # AddSubExpr
    | expr op=('<' | '>' | '<=' | '>=') expr            # RelExpr
    | expr op=('==' | '!=') expr                       # EqExpr
    | expr op='&&' expr                                 # AndExpr
    | expr op='||' expr                                 # OrExpr
    | INT_LIT                                            # IntLitExpr
    | FLOAT_LIT                                          # FloatLitExpr
    | STRING_LIT                                         # StringLitExpr
    | TRUE                                                # TrueExpr
    | FALSE                                               # FalseExpr
    | ID                                                  # VarRefExpr
    ;

// ---------------- PALAVRAS-CHAVE ----------------

INT      : 'int';
FLOAT    : 'float';
STRING   : 'string';
IF       : 'if';
ELSE     : 'else';
WHILE    : 'while';
FOR      : 'for';
PRINT    : 'print';
TASK     : 'task';
PRIORITY : 'priority';
ASSIGN   : 'assign';
TO       : 'to';
COMPLETE : 'complete';
LIST     : 'list';
TASKS    : 'tasks';
TRUE     : 'true';
FALSE    : 'false';

// ---------------- LITERAIS E IDENTIFICADORES ----------------

FLOAT_LIT
    : DIGIT+ '.' DIGIT+
    ;

INT_LIT
    : DIGIT+
    ;

STRING_LIT
    : '"' ( ESC | ~["\\\r\n] )* '"'
    ;

fragment ESC
    : '\\' [btnr"\\]
    ;

fragment DIGIT
    : [0-9]
    ;

ID
    : [a-zA-Z_] [a-zA-Z_0-9]*
    ;

// ---------------- COMENTÁRIOS E ESPAÇOS ----------------

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

// Não há uma regra "pega-tudo" aqui de propósito: qualquer caractere que não
// combine com nenhum token acima faz o ANTLR emitir automaticamente um erro
// de reconhecimento de token ("token recognition error"), que é capturado
// pelo CompilerErrorListener registrado no Lexer (fase de Análise Léxica),
// já indicando a linha do caractere inválido.
