# TaskLang — Compilador/Interpretador para uma DSL de Gerenciamento de Tarefas

TaskLang é uma linguagem de domínio específico (DSL) para descrever fluxos simples de
gerenciamento de tarefas (criação, atribuição a responsáveis e conclusão), combinada com
os recursos de uma linguagem de programação básica (variáveis tipadas, `if/else`, `while`,
`for`). O projeto implementa um **compilador/interpretador completo**, construído com
**Java + ANTLR4**, passando pelas quatro fases clássicas: análise léxica, análise sintática
(com geração de AST), análise semântica (tabela de símbolos + checagem de tipos) e um
backend que **interpreta a AST diretamente** (tree-walking interpreter).

---

## 1. Estrutura do projeto

```
taskdsl/
├── pom.xml
├── README.md
├── src/main/antlr4/com/taskdsl/TaskLang.g4     # gramática ANTLR4 (léxico + sintático)
├── src/main/java/com/taskdsl/
│   ├── Main.java                                # orquestra as 4 fases
│   ├── ASTBuilder.java                           # parse tree (ANTLR) -> AST própria
│   ├── ast/                                      # nós da AST (Node, Type, Expr, Stmt)
│   ├── errors/CompilerErrorListener.java         # erros léxicos/sintáticos com linha
│   ├── semantic/                                 # SymbolTable, Symbol, SemanticAnalyzer
│   └── interpreter/                              # Environment, Task, Interpreter (backend)
└── examples/
    ├── validos/           # programas .tsk que devem rodar com sucesso
    └── invalidos/         # programas .tsk que devem falhar (léxico / sintático / semântico)
```

## 2. Requisitos

- Java 11 ou superior (testado com JDK 21)
- Maven 3.6+ (com acesso à internet, para baixar `antlr4-runtime` e o plugin `antlr4-maven-plugin`
  do Maven Central na primeira execução)

## 3. Como compilar

Na raiz do projeto (onde está o `pom.xml`):

```bash
mvn clean package
```

Esse comando:
1. Executa o plugin `antlr4-maven-plugin`, que lê `TaskLang.g4` e gera automaticamente
   `TaskLangLexer`, `TaskLangParser`, `TaskLangVisitor` e `TaskLangBaseVisitor` em
   `target/generated-sources/antlr4/com/taskdsl/`.
2. Compila todo o código Java (gerado + escrito à mão).
3. Empacota tudo (incluindo o runtime do ANTLR) em um único JAR executável:
   `target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar`.

## 4. Como executar

```bash
java -jar target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar examples/validos/exemplo1_fundamentos.tsk
```

Troque o caminho do arquivo `.tsk` pelo programa que quiser rodar. O programa:
- imprime a saída dos comandos `print`, `task`, `assign`, `complete` e `list tasks`;
- em caso de erro léxico, sintático ou semântico, imprime as mensagens no `stderr`,
  indicando sempre **a linha do problema**, e encerra sem executar nada
  (códigos de saída: `2` = léxico/sintático, `3` = semântico, `4` = erro em tempo de execução).

### Testando os exemplos com erro

```bash
java -jar target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar examples/invalidos/erro_lexico.tsk
java -jar target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar examples/invalidos/erro_sintatico.tsk
java -jar target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar examples/invalidos/erro_semantico_tipos.tsk
java -jar target/taskdsl-compiler-1.0.0-jar-with-dependencies.jar examples/invalidos/erro_semantico_escopo.tsk
```

Cada um desses arquivos foi criado propositalmente para disparar um tipo de erro diferente
(veja os comentários dentro de cada `.tsk`).

---

## 5. A gramática da linguagem

### 5.1 Tipos primitivos

| Tipo     | Exemplo de literal      |
|----------|--------------------------|
| `int`    | `42`, `-3`                |
| `float`  | `3.14`, `0.5`              |
| `string` | `"texto entre aspas"`     |

Não há conversão implícita de `string` para número nem de número para `string`; a única
conversão automática permitida é **`int` → `float`** (ex.: declarar `float f = 10;` é válido).

### 5.2 Variáveis

```
int contador = 0;
float media;              // sem inicializador: assume valor padrão (0.0)
string nome = "Mariana";

contador = contador + 1;  // atribuição
```

Variáveis têm **escopo de bloco**: uma variável declarada dentro de `{ ... }` (corpo de
`if`, `while`, `for`) deixa de existir ao final do bloco.

### 5.3 Estruturas de controle

```
if (contador > 10) {
    print("alto");
} else {
    print("baixo");
}

while (contador < 5) {
    contador = contador + 1;
}

for (int i = 0; i < 5; i = i + 1) {
    print(i);
}
```

A condição de `if`, `while` e `for` **deve ser uma expressão booleana**, ou seja, resultado
de um operador relacional (`<`, `>`, `<=`, `>=`, `==`, `!=`) ou lógico (`&&`, `||`, `!`).
Não é permitido usar diretamente um `int` como condição (isso é verificado na análise
semântica). Não existe um tipo `bool` declarável em variáveis — booleano é sempre o
resultado momentâneo de uma comparação.

### 5.4 Operadores

- Aritméticos: `+` `-` `*` `/` `%` (também usados para concatenar `string + string`)
- Relacionais: `<` `>` `<=` `>=` `==` `!=`
- Lógicos: `&&` `||` `!`
- Unário: `-expr`, `!expr`

### 5.5 Comandos exclusivos do domínio (o que faz do TaskLang uma DSL de tarefas)

| Comando                          | Significado                                              |
|-----------------------------------|-----------------------------------------------------------|
| `task <string> priority <int>;`   | Cria uma tarefa com nome e prioridade (ou atualiza a prioridade se a tarefa já existir) |
| `assign <string> to <string>;`    | Designa uma tarefa já criada a um responsável              |
| `complete <string>;`              | Marca uma tarefa como concluída                            |
| `list tasks;`                     | Imprime todas as tarefas cadastradas e seu status          |

Exemplo:

```
task "Escrever relatório" priority 2;
assign "Escrever relatório" to "Mariana";
complete "Escrever relatório";
list tasks;
```

Regras semânticas específicas desses comandos:
- o nome da tarefa e o responsável devem ser expressões do tipo `string`;
- a prioridade deve ser do tipo `int`;
- `assign`/`complete` em uma tarefa que nunca foi criada com `task` gera um **erro em
  tempo de execução** (a tarefa não existe).

### 5.6 Comando utilitário

```
print(<expr>);
```
Imprime o valor de qualquer expressão (`int`, `float`, `string` ou booleano).

### 5.7 Gramática resumida (EBNF simplificado)

```
program        -> statement* EOF
statement      -> varDecl | assignment | ifStmt | whileStmt | forStmt
                 | printStmt | taskDecl | assignTaskStmt | completeStmt
                 | listTasksStmt | block
block          -> '{' statement* '}'
type           -> 'int' | 'float' | 'string'
varDecl        -> type ID ('=' expr)? ';'
assignment     -> ID '=' expr ';'
ifStmt         -> 'if' '(' expr ')' block ('else' block)?
whileStmt      -> 'while' '(' expr ')' block
forStmt        -> 'for' '(' forInit ';' expr ';' ID '=' expr ')' block
forInit        -> type ID '=' expr | ID '=' expr
taskDecl       -> 'task' expr 'priority' expr ';'
assignTaskStmt -> 'assign' expr 'to' expr ';'
completeStmt   -> 'complete' expr ';'
listTasksStmt  -> 'list' 'tasks' ';'
printStmt      -> 'print' '(' expr ')' ';'
expr           -> expr ('*'|'/'|'%') expr
                 | expr ('+'|'-') expr
                 | expr ('<'|'>'|'<='|'>=') expr
                 | expr ('=='|'!=') expr
                 | expr '&&' expr | expr '||' expr
                 | ('-'|'!') expr | '(' expr ')'
                 | INT_LIT | FLOAT_LIT | STRING_LIT | 'true' | 'false' | ID
```

A gramática completa (com precedência de operadores resolvida pela ordem das alternativas,
como o ANTLR4 exige) está em [`src/main/antlr4/com/taskdsl/TaskLang.g4`](src/main/antlr4/com/taskdsl/TaskLang.g4).

---

## 6. As quatro fases do compilador

1. **Análise Léxica** (`TaskLangLexer`, gerado pelo ANTLR): transforma o texto-fonte em
   tokens. Qualquer caractere que não corresponda a nenhum token válido faz o ANTLR emitir
   automaticamente um "token recognition error"; um `CompilerErrorListener` customizado,
   registrado no Lexer, captura esse e outros erros léxicos e imprime a linha exata do
   problema.

2. **Análise Sintática** (`TaskLangParser`, gerado pelo ANTLR): valida se a sequência de
   tokens corresponde à gramática. Erros de sintaxe (parênteses/chaves não fechados,
   `;` faltando, etc.) também são capturados pelo mesmo tipo de listener, com a linha do
   erro. Em seguida, `ASTBuilder` (um `TaskLangBaseVisitor`) percorre a árvore de parse do
   ANTLR e constrói a **AST própria** do projeto (pacote `com.taskdsl.ast`), desacoplada
   das classes geradas pelo ANTLR.

3. **Análise Semântica** (`SemanticAnalyzer`): percorre a AST usando uma **tabela de
   símbolos com escopo em pilha** (`SymbolTable`) para:
   - impedir o uso de variáveis não declaradas;
   - impedir redeclaração de variáveis no mesmo escopo;
   - checar tipos em declarações, atribuições e operações (ex.: **proíbe somar `string`
     com `int`**, exige tipos corretos nos comandos `task`/`assign`/`complete`, exige que
     condições de `if`/`while`/`for` sejam booleanas).

   Todos os erros encontrados são acumulados e exibidos de uma só vez ao final da análise.

4. **Backend**: um **interpretador tree-walking** (`Interpreter`) executa a AST diretamente
   (sem gerar código intermediário), mantendo em memória o valor das variáveis
   (`Environment`, também com escopo em pilha) e o estado das tarefas criadas (`Task`).

---

## 7. Casos de teste incluídos

| Arquivo                                             | Deve...                                                            |
|------------------------------------------------------|----------------------------------------------------------------------|
| `examples/validos/exemplo1_fundamentos.tsk`          | Rodar com sucesso (tipos, `if/else`, `while`, comandos de domínio)   |
| `examples/validos/exemplo2_lacos_floats.tsk`         | Rodar com sucesso (`for`, `float`, concatenação de string)           |
| `examples/validos/exemplo3_operadores.tsk`           | Rodar com sucesso (operadores relacionais, `&&`, `||`, `!`, `if` aninhado, unário `-`) |
| `examples/invalidos/erro_lexico.tsk`                 | Falhar na análise léxica (caractere `@` inválido)                    |
| `examples/invalidos/erro_sintatico.tsk`              | Falhar na análise sintática (`;` e `)` faltando)                     |
| `examples/invalidos/erro_semantico_tipos.tsk`        | Falhar na análise semântica (soma `string + int`, tipos errados nos comandos de domínio, condição de `if` não booleana) |
| `examples/invalidos/erro_semantico_escopo.tsk`       | Falhar na análise semântica (variável redeclarada, variável não declarada, uso fora do escopo) |

---