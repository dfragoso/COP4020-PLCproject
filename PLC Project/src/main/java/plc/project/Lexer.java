package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid or missing.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        /*
        * char by char
        * */
        //match first char, then match in while until done
        List<Token> tokens = new ArrayList<>();

        while (chars.has(0)){
            if(match("[ \b\n\r\t]")){
                chars.skip();
            }else {
                tokens.add(lexToken());
            }
        }
        //throw new UnsupportedOperationException();
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        if(peek("[A-Za-z_]")) {
            return lexIdentifier();
        }
        else if(peek("[']")){
            return lexCharacter();
        }
        else if (peek("[+\\-]", "[0-9]") ||peek("[0-9]")){
            return lexNumber();
        }
        else if(peek("[\"]")){
            return lexString();
        }
        else{
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        //throw new UnsupportedOperationException();
        //identifier ::= [A-Za-z_] [A-Za-z0-9_-]*
        //If I am inside the function, is because the first character matched the start of the identifier
        //need to advance the index, so match to advance the first character
        match("[A-Za-z_]");
        //matching the end until the end
        while(match("[A-Za-z0-9_-]"));
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        //number ::= [+\-]? [0-9]+ ('.' [0-9]+)?
        //INTEGER, DECIMAL
        //throw new UnsupportedOperationException();
        match("[+\\-]");
        while(match("[0-9]"));
        if(match("[.]")){
            if(peek("[0-9]")){
                while(match("[0-9]"));
                return chars.emit(Token.Type.DECIMAL);
            }
            else{
                throw new ParseException("Invalid decimal", chars.index);
            }
        }else{
            return chars.emit(Token.Type.INTEGER);
        }
    }

    public Token lexCharacter() {
        //character ::= ['] ([^'\n\r\\] | escape) [']
        /*
        b n r t ' " \
        line ending: \n \r
        Escape characters are also supported starting with a backslash (\), which must be followed by one of bnrt'"\
        (and are considered one character). The character cannot be a single quote ('), since that ends a character literal,
        or a line ending (\n/\r), to avoid character literals spanning multiple lines.
        Examples: 'c', '\n'
        Non-Examples: '', 'abc' (note these should throw ParseExceptions with the index at the missing/invalid character)
        * */
        match("[']");
        if(match("[']")){
            throw new ParseException("Invalid token", chars.index);
        }
        if(peek("\\\\")){
           lexEscape();
        }
        else if(match("[^\'\n\r\\\\]"));
        if (match("[']")) {
            return chars.emit(Token.Type.CHARACTER);
        } else {
            throw new ParseException("Invalid token", chars.index);
        }
    }

    public Token lexString() {
        //string ::= '"' ([^"\n\r\\] | escape)* '"'
        match("[\"]");
        while(match("[^\"\n\r]")) {
            if (peek("\\\\")) {
                lexEscape();
            }
        }
        if (match("[\"]")) {
            return chars.emit(Token.Type.STRING);
        } else {
            throw new ParseException("Invalid token", chars.index);
        }
        //throw new UnsupportedOperationException();
    }

    public void lexEscape() {
        //escape ::= '\' [bnrt'"\\]
        match("\\\\");
        if(!match("[bnrt'\"\\\\]")){
            throw new ParseException("Invalid escape character", chars.index);
        }
        //throw new UnsupportedOperationException();
    }

    public Token lexOperator() {
        //operator ::= [<>!=] '='? | 'any character'
        //throw new UnsupportedOperationException();
        if(match("[<>!=]")){
            match("=");
        }else{
            match(".");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    //Examines a sequence of parameters to determine if they match the next set of elements
    //Does not advance stream index even of they are matched
    //Lexer: examines characters
    //Parser: examines tokens
    public boolean peek(String... patterns) {
        //pass regex as a pattern
        for(int i = 0; i<patterns.length; i++){
            //If no longer have chars or not a string
            //If the characters match, then match match the pattern
            if(!chars.has(i) ||
               !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        //throw new UnsupportedOperationException();
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    //Performs the same matching
    //Advances stream index the elements when matched
    // Completes any additional consumptions steps
    // Lexer and Parser implementations are logically identical, in both cases the appropriate stream, Character or Token is advanced
    public boolean match(String... patterns) {
        //call the peek method, if true:
        // advance char stream in fifo style
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        //throw new UnsupportedOperationException();
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    //Provides sequential character delivery to the lexer
        //input: the src string
        //index: position within source
        //length: size of current token
    public static final class CharStream {
        private final String input;
        private int index = 0;
        private int length = 0;
        public CharStream(String input) {
            this.input = input;
        }
        //Use this methods to deal with the char stream!
        //checks if input has offset characters remaining
        //ask has before get!
        public boolean has(int offset) {
            return index + offset < input.length();
        }
        //returns the char at offset position
        public char get(int offset) {
            return input.charAt(index + offset);
        }
        //move to the next char position in the input
        public void advance() {
            index++;
            length++;
        }
        //resets the size of the current token to 0. used with advance
        public void skip() {
            length = 0;
        }
        //instantiate the current token
        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
/*
\n
"this is a newline
within a string"
String str = new String("this \n newline"); Literal newline char in the string
String str = ne String("This is a different \\n representation of newline"); //"This is a different \n representation of newline"
 */