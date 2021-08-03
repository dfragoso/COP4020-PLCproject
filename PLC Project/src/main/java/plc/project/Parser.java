package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        //source ::= field* method*
        //LET is a field and DEF method
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();

        while(match("LET")){
            Ast.Field field = parseField();
            fields.add(field);
        }
        while(match("DEF")){
            Ast.Method method = parseMethod();
            methods.add(method);
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        //field ::= 'LET' identifier ('=' expression)? ';' -- same as LET in statement
        //field ::= 'LET' identifier ':' identifier ('=' expression)? ';'
        String name;
        String typeName = new String();
        Optional<Ast.Expr> value = Optional.empty();
        if(match(Token.Type.IDENTIFIER)) {
            name = tokens.get(-1).getLiteral();

            //Only name, no other expression
            if (match(":")){
                if (match(Token.Type.IDENTIFIER)){
                    typeName = tokens.get(-1).getLiteral();
                }
            }

            if(match(";")){
                return new Ast.Field(name, Optional.empty());
            }
            //There is an expression in the declaration
            if(match("=")){
                value = Optional.of(parseExpression());
                if(!match(";")){
                    throw new ParseException("Missing a semicolon at the end", tokens.index);
                }
            }
            return new Ast.Field(name, typeName, value);

        }
        else{
            throw new ParseException("Invalid declaration syntax", tokens.index);
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        //method ::= 'DEF' identifier '(' (identifier (',' identifier)*)? ')' 'DO' statement* 'END'
        //method ::= 'DEF' identifier '(' (identifier ':' identifier (',' identifier ':' identifier)* )? ')' (':' identifier)? 'DO'
        String name = new String();
        List<String> parameters = new ArrayList<>();
        List<String> parameterTypeNames = new ArrayList<>();
        List<Ast.Stmt> statements = new ArrayList<>();
        Optional<String> returnTypeName = Optional.empty();
        if(match(Token.Type.IDENTIFIER)){
            name = tokens.get(-1).getLiteral();
        }
        if(match("(")){
            if(match(Token.Type.IDENTIFIER)){
                 String param = tokens.get(-1).getLiteral();
                 parameters.add(param);
                if(match(":")){
                    if(match(Token.Type.IDENTIFIER)){
                        String paramType = tokens.get(0).getLiteral();
                        parameterTypeNames.add(paramType);
                    }
                }
            }
            if(match(",")){
                while(match(Token.Type.IDENTIFIER)){
                    String param = tokens.get(-1).getLiteral();
                    parameters.add(param);
                    if(match(":")){
                        if(match(Token.Type.IDENTIFIER)){
                            String paramType = tokens.get(0).getLiteral();
                            parameterTypeNames.add(paramType);
                        }
                    }
                }
            }
        }
        if(!match(")")){
            throw new ParseException("Missing closing parenthesis", tokens.index);
        }
        if(match(":")){
            if(match(Token.Type.IDENTIFIER)){
                returnTypeName = Optional.of(tokens.get(-1).getLiteral());
            }
        }
        if(!match("DO")){
            throw new ParseException("Missing DO keyword", tokens.index);
        }
        while (true){
            if(peek("END")){
                break;
            }
            Ast.Stmt stmt = parseStatement();
            statements.add(stmt);
        }
        if(!match("END")){
            throw new ParseException("Missing END keyword", tokens.index);
        }
        return new Ast.Method(name, parameters, parameterTypeNames, returnTypeName, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
//    statement ::=
//            'LET' identifier ('=' expression)? ';' |
//            'IF' expression 'DO' statement* ('ELSE' statement*)? 'END' |
//            'FOR' identifier 'IN' expression 'DO' statement* 'END' |
//            'WHILE' expression 'DO' statement* 'END' |
//            'RETURN' expression ';' |
//             expression ('=' expression)? ';'
    public Ast.Stmt parseStatement() throws ParseException {
        if(match("LET")){
            return parseDeclarationStatement();
        }
        else if(match("IF")){
            return parseIfStatement();
        }
        else if(match("FOR")){
            return parseForStatement();
        }
        else if(match("WHILE")){
            return parseWhileStatement();
        }
        else if(match("RETURN")){
            return parseReturnStatement();
        }else{
            //expression ('=' expression)? ';'
            Ast.Expr expr = parseExpression();
            Ast.Stmt stmt = new Ast.Stmt.Expression(expr);
            if(match("=")){
                stmt = new Ast.Stmt.Assignment(expr, parseExpression());
            }
            if(!match(";")){
                throw new ParseException("Missing semicolon at the end of expression", tokens.index);
            }
            return stmt;
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        //'LET' identifier ('=' expression)? ';'
        //New:
        //'LET' identifier (':' identifier)? ('=' expression)? ';'
        //LET already matched in statement method
        Optional<String> typeName = Optional.empty();
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid declaration syntax", tokens.index);
        }
        String name = tokens.get(-1).getLiteral();
        if(match(":")){
           if(match(Token.Type.IDENTIFIER)){
               typeName = Optional.of(tokens.get(-1).getLiteral());
           }
        }
        Optional<Ast.Expr> value = Optional.empty();
        //There is an expression in the declaration
        if(match("=")){
            value = Optional.of(parseExpression());
        }
        if(!match(";")){
            throw new ParseException("Missing a semicolon at the end", tokens.index);
        }
        return new Ast.Stmt.Declaration(name, typeName, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        //'IF' expression 'DO' statement* ('ELSE' statement*)? 'END'
        Ast.Expr expr = parseExpression();
        if(!match("DO")){
            throw new ParseException("Invalid IF syntax, missing the DO", tokens.index);
        }
        List<Ast.Stmt> thenStatements = new ArrayList<>();
        List<Ast.Stmt> elseStatements = new ArrayList<>();
        while(true){
            if(peek("ELSE") || peek("END")){
                break;
            }
            Ast.Stmt stmt = parseStatement();
            thenStatements.add(stmt);
        }
        if(match("ELSE")){
            while(true){
                if(peek("END")){
                    break;
                }
                Ast.Stmt elseStmt = parseStatement();
                elseStatements.add(elseStmt);
            }
        }
        if(!match("END")){
            throw new ParseException("Invalid IF syntax, missing the END", tokens.index);
        }
        return new Ast.Stmt.If(expr, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        //'FOR' identifier 'IN' expression 'DO' statement* 'END'
        if(!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Invalid FOR statement syntax", tokens.index);
        }
        String name = tokens.get(-1).getLiteral();
        if(!match("IN")){
            throw new ParseException("Invalid FOR statement syntax, missing IN", tokens.index);
        }
        Ast.Expr expr = parseExpression();
        if(!match("DO")){
            throw new ParseException("Invalid FOR statement syntax, missing DO", tokens.index);
        }
        List<Ast.Stmt> stmtList = new ArrayList<>();
        while(true){
            if(peek("END")){
                break;
            }
            Ast.Stmt stmt = parseStatement();
            stmtList.add(stmt);
        }
        if(match("END")){
            return new Ast.Stmt.For(name, expr, stmtList);
        }
        else{
            throw new ParseException("Missing the END", tokens.index-1);
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        //'WHILE' expression 'DO' statement* 'END'
        Ast.Expr expr = parseExpression();
        if(match("DO")){
            List<Ast.Stmt> stmtList = new ArrayList<>();
            while(true){
                if(peek("END")){
                    break;
                }
                Ast.Stmt stmt = parseStatement();
                stmtList.add(stmt);
            }
            if(match("END")){
                return new Ast.Stmt.While(expr, stmtList);
            }
            else{
                throw new ParseException("Missing the END", tokens.index-1);
            }

        }else{
            throw new ParseException("Invalid while statement syntax", tokens.index);
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        //'RETURN' expression ';'
        if(match(";")){
            throw new ParseException("Return statement is missing the expression", tokens.index);
        }
        Ast.Expr expr = parseExpression();
        if(!match(";")){
            throw new ParseException("Missing a ;", tokens.index);
        }
        return new Ast.Stmt.Return(expr);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        //expression ::= logical_expression
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        //logical_expression ::= comparison_expression (('AND' | 'OR') comparison_expression)*
        Ast.Expr exprL = parseEqualityExpression();
        String operator = "";
        while(match("AND") || match("OR")){
            operator = tokens.get(-1).getLiteral();
            Ast.Expr exprR = parseEqualityExpression();
            exprL = new Ast.Expr.Binary(operator, exprL, exprR);
        }
        return exprL;

    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        //comparison_expression ::= additive_expression (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expression)*
        Ast.Expr exprL = parseAdditiveExpression();
        String operator = "";
        while(match("<") || match("<=") || match(">") || match(">=") || match("==") || match("!=")){
            operator = tokens.get(-1).getLiteral();
            Ast.Expr exprR = parseAdditiveExpression();
            exprL = new Ast.Expr.Binary(operator, exprL, exprR);
        }
        return exprL;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        //additive_expression ::= multiplicative_expression (('+' | '-') multiplicative_expression)*
        //return parseMultiplicativeExpression();
        Ast.Expr exprL = parseMultiplicativeExpression();
        String operator = "";
        while(match("+") || match("-")){
            operator = tokens.get(-1).getLiteral();
            Ast.Expr exprR = parseMultiplicativeExpression();
            exprL = new Ast.Expr.Binary(operator, exprL, exprR);
        }
        return exprL;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        //multiplicative_expression ::= secondary_expression ( ('*' | '/') secondary_expression )*
        Ast.Expr exprL = parseSecondaryExpression();
        String operator = "";
        while(match("*") || match("/")){
            operator = tokens.get(-1).getLiteral();
            Ast.Expr exprR = parseSecondaryExpression();
            exprL = new Ast.Expr.Binary(operator, exprL, exprR);
        }
        return exprL;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        // secondary_expression ::= primary_expression (  '.' identifier (   '(' (expression (',' expression)*)? ')'  )?  )*
        Ast.Expr expr = parsePrimaryExpression();
        while(match(".")){
           Ast.Expr primExpr = parsePrimaryExpression();
           if(primExpr instanceof Ast.Expr.Access){
               primExpr = new Ast.Expr.Access(Optional.of(expr), ((Ast.Expr.Access) primExpr).getName());
           }
           else if(primExpr instanceof Ast.Expr.Function){
               primExpr = new Ast.Expr.Function(Optional.of(expr), ((Ast.Expr.Function) primExpr).getName(), ((Ast.Expr.Function) primExpr).getArguments());
           }
           else{
               throw new ParseException("Invalid expression", tokens.index);
           }
           expr = primExpr;
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public String replaceHelper(String tobeReplaced){
        String finalString = tobeReplaced.substring(1, tobeReplaced.length()-1);
        //escape ::= '\' [bnrt'"\]
        finalString = finalString.replace("\\n", "\n");
        finalString = finalString.replace("\\b", "\b");
        finalString = finalString.replace("\\r", "\r");
        finalString = finalString.replace("\\t", "\t");
        finalString = finalString.replace("\\'", "\'");
        finalString = finalString.replace("\\\"", "\"");
        finalString = finalString.replace("\\\\", "\\");
        return finalString;
    }
    /*primary_expression ::=
    'NIL' | 'TRUE' | 'FALSE' |
    integer | decimal | character | string |
    '(' expression ')' |
    identifier ('(' (expression (',' expression)*)? ')')?
    */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if(match("TRUE")){
            return new Ast.Expr.Literal(Boolean.TRUE);
        }
        else if(match("NIL")){
            return new Ast.Expr.Literal(null);
        }
        else if(match("FALSE")){
            return new Ast.Expr.Literal(Boolean.FALSE);
        }
        else if(match(Token.Type.INTEGER)){
            BigInteger value = new BigInteger(tokens.get(-1).getLiteral());
            return new Ast.Expr.Literal(value);
        }
        else if(match(Token.Type.DECIMAL)){
            BigDecimal value = new BigDecimal(tokens.get(-1).getLiteral());
            return new Ast.Expr.Literal(value);
        }
        else if(match(Token.Type.CHARACTER)){
            //System.out.println(tokens.get(-1).getLiteral());
            /*1- Get the token value
            2- Remove the ' ' sorrounding the char
            3- Remove scape (\\)
            4- Create literal using Character class
            */
            String character = replaceHelper(tokens.get(-1).getLiteral());
            //System.out.println(character);
            Character value = new Character(character.charAt(0));
            return new Ast.Expr.Literal(value);
        }
        else if(match(Token.Type.STRING)){
            //System.out.println(tokens.get(-1).getLiteral());
            String finalString = replaceHelper(tokens.get(-1).getLiteral());
            return new Ast.Expr.Literal(finalString);
        }
        else if(match("(")){
            //'(' expression ')'
            Ast.Expr expr = parseExpression();
            if(!match(")")){
                throw new ParseException("Expected closing parenthesis", tokens.index);
            }
            return new Ast.Expr.Group(expr);
        }
        else if(match(Token.Type.IDENTIFIER)){
            //identifier ('(' (expression (',' expression)*)? ')')?
            String name = tokens.get(-1).getLiteral(); //String itself, the name of the indentifier
            if(match("(")){
                List<Ast.Expr> arguments = new ArrayList<>();
                if(match(")")){
                    return new Ast.Expr.Function(Optional.empty(), name, arguments);
                }
                arguments.add(parseExpression());
                while(match(",")){
                    arguments.add(parseExpression());
                }
                if(!match(")")){
                    throw new ParseException("Expected closing parenthesis", tokens.index);
                }
                return new Ast.Expr.Function(Optional.empty(), name, arguments);
            }
            return new Ast.Expr.Access(Optional.empty(),name);
        }

        else{
            throw new ParseException("Invalid Primary Exception", tokens.index);
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    // Peeking at tokens, one object at a time
    private boolean peek(Object... patterns) {
        //throw new UnsupportedOperationException();
        for(int i = 0; i < patterns.length; i++){
            if(!tokens.has(i)){
                return false;
            }else if(patterns[i] instanceof Token.Type){
                //True if token type I am looking for is the token I really have
                //This is a type comparison
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }else if(patterns[i] instanceof String){
                //Get token at index zero and go and compare with literal type
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }else{
                //Problem is not inside the parser
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    //Same matching as peek. Advances stream index (consume) the elements when matched
    private boolean match(Object... patterns) {
        //throw new UnsupportedOperationException();
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {
        // List of tokens
        private final List<Token> tokens;
        // Position within tokens
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        //checks if tokens has offset tokens remaining
        // Ask has before get !!!!
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        //Return token at offset position
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token position in the input, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
/*

source ::= field* method*
field ::= 'LET' identifier ('=' expression)? ';'
method ::= 'DEF' identifier '(' (identifier (',' identifier)*)? ')' 'DO' statement* 'END'
//Literals are enclosed in single quotes ' ' to generate a railroad diagram

//To do in part2b
statement ::=
    'LET' identifier ('=' expression)? ';' |
    'IF' expression 'DO' statement* ('ELSE' statement*)? 'END' |
    'FOR' identifier 'IN' expression 'DO' statement* 'END' |
    'WHILE' expression 'DO' statement* 'END' |
    'RETURN' expression ';' |
    expression ('=' expression)? ';'

//For part2a
expression ::= logical_expression

logical_expression ::= comparison_expression (('AND' | 'OR') comparison_expression)*
                            // Literals AND and OR are lexed as Token.Type.IDENTIFIER \\
comparison_expression ::= additive_expression (('<' | '<=' | '>' | '>=' | '==' | '!=') additive_expression)*
                                                    // Lexed as OPERATOR \\
additive_expression ::= multiplicative_expression (('+' | '-') multiplicative_expression)*
multiplicative_expression ::= secondary_expression (('*' | '/') secondary_expression)*

secondary_expression ::= primary_expression ('.' identifier ('(' (expression (',' expression)*)? ')')?)*

primary_expression ::=
    'NIL' | 'TRUE' | 'FALSE' |
    integer | decimal | character | string |
    '(' expression ')' |
    identifier ('(' (expression (',' expression)*)? ')')?

//Grammar for the lexer
identifier ::= [A-Za-z_] [A-Za-z0-9_]*
number ::= [+-]? [0-9]+ ('.' [0-9]+)?
character ::= ['] ([^'\\] | escape) [']
string ::= '"' ([^"\n\r\\] | escape)* '"'
escape ::= '\' [bnrt'"\]
operator ::= [<>!=] '='? | 'any character'

whitespace ::= [ \b\n\r\t]

* */