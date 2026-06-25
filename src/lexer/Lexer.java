package lexer;

import java.util.*;

public class Lexer {

    private String source;
    private int position;
    private int line;
    private int column;
    private List<Token> tokens;

    // ========== MULTI-CHARACTER TOKENS ==========
    private static final Map<String, TokenType> MULTI_CHAR_TOKENS = new LinkedHashMap<>();
    static {
        // 2-character operators (must come before single-char matches)
        MULTI_CHAR_TOKENS.put("\uD802\uDD14\uD802\uDD14", TokenType.EQUAL);        // 𐤔𐤔 (==)
        MULTI_CHAR_TOKENS.put("\uD802\uDD0D\uD802\uDD14", TokenType.NOT_EQUAL);    // 𐤍𐤔 (!=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD15\uD802\uDD14", TokenType.GREATER_EQUAL); // 𐤕𐤔 (>=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD15\uD802\uDD00", TokenType.LESS);         // 𐤕𐤀 (<)
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD15", TokenType.LESS_EQUAL);   // 𐤀𐤕 (<=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD10\uD802\uDD14", TokenType.PLUS_ASSIGN);  // 𐤐𐤔 (+=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD11\uD802\uDD14", TokenType.MINUS_ASSIGN); // 𐤑𐤔 (-=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD12\uD802\uDD14", TokenType.MULTIPLY_ASSIGN); // 𐤒𐤔 (*=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD13\uD802\uDD14", TokenType.DIVIDE_ASSIGN); // 𐤓𐤔 (/=)
        MULTI_CHAR_TOKENS.put("\uD802\uDD10\uD802\uDD10", TokenType.INCREMENT);    // 𐤐𐤐 (++)
        MULTI_CHAR_TOKENS.put("\uD802\uDD11\uD802\uDD11", TokenType.DECREMENT);    // 𐤑𐤑 (--)
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD0B", TokenType.LOGICAL_NOT);  // 𐤀𐤋 (!)
        MULTI_CHAR_TOKENS.put("\uD802\uDD0D\uD802\uDD0F", TokenType.BITWISE_NOT);  // 𐤍𐤏 (~)

        // ===== KEYWORDS (multi-character) =====
        // Storage
        MULTI_CHAR_TOKENS.put("\uD802\uDD07\uD802\uDD05\uD802\uDD11", TokenType.EXTERN); // 𐤇𐤅𐤑
        MULTI_CHAR_TOKENS.put("\uD802\uDD0E\uD802\uDD10\uD802\uDD13", TokenType.REGISTER); // 𐤎𐤐𐤓
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD0C\uD802\uDD15", TokenType.STATIC); // 𐤀𐤌𐤕
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD03\uD802\uDD0C", TokenType.NATIVE); // 𐤀𐤃𐤌
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD01\uD802\uDD13\uD802\uDD09", TokenType.TRANSIENT); // 𐤏𐤁𐤓𐤉

        // Types
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD03\uD802\uDD14", TokenType.ENUM);  // 𐤏𐤃𐤔
        MULTI_CHAR_TOKENS.put("\uD802\uDD11\uD802\uDD10\uD802\uDD04", TokenType.FLOAT); // 𐤑𐤐𐤄
        MULTI_CHAR_TOKENS.put("\uD802\uDD12\uD802\uDD09\uD802\uDD10", TokenType.DOUBLE); // 𐤒𐤉𐤐
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD13\uD802\uDD0A", TokenType.LONG);  // 𐤀𐤓𐤊
        MULTI_CHAR_TOKENS.put("\uD802\uDD12\uD802\uDD11\uD802\uDD13", TokenType.SHORT); // 𐤒𐤑𐤓
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD05\uD802\uDD15", TokenType.SIGNED); // 𐤀𐤅𐤕
        MULTI_CHAR_TOKENS.put("\uD802\uDD01\uD802\uDD0B\uD802\uDD09", TokenType.UNSIGNED); // 𐤁𐤋𐤉
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD0F", TokenType.UNION); // 𐤀𐤇𐤃𐤏
        MULTI_CHAR_TOKENS.put("\uD802\uDD13\uD802\uDD09\uD802\uDD12", TokenType.VOID); // 𐤓𐤉𐤒
        MULTI_CHAR_TOKENS.put("\uD802\uDD14\uD802\uDD0C\uD802\uDD04", TokenType.TYPEDEF); // 𐤔𐤌𐤄
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD09\uD802\uDD0D", TokenType.CLASS); // 𐤏𐤉𐤍
        MULTI_CHAR_TOKENS.put("\uD802\uDD05\uD802\uDD14\uD802\uDD09", TokenType.INTERFACE); // 𐤅𐤔𐤉
        MULTI_CHAR_TOKENS.put("\uD802\uDD01\uD802\uDD0D\uD802\uDD09\uD802\uDD15", TokenType.STRUCT); // 𐤁𐤍𐤉𐤕

        // Qualifiers
        MULTI_CHAR_TOKENS.put("\uD802\uDD0A\uD802\uDD01\uD802\uDD0F", TokenType.CONST); // 𐤊𐤁𐤏
        MULTI_CHAR_TOKENS.put("\uD802\uDD05\uD802\uDD0B\uD802\uDD00\uD802\uDD15", TokenType.VOLATILE); // 𐤅𐤋𐤀𐤕
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD07\uD802\uDD13", TokenType.FINAL); // 𐤀𐤇𐤓
        MULTI_CHAR_TOKENS.put("\uD802\uDD12\uD802\uDD03\uD802\uDD14", TokenType.STRICTFP); // 𐤒𐤃𐤔
        MULTI_CHAR_TOKENS.put("\uD802\uDD0A\uD802\uDD0C\uD802\uDD04", TokenType.SIZEOF); // 𐤊𐤌𐤄

        // Access Modifiers
        MULTI_CHAR_TOKENS.put("\uD802\uDD10\uD802\uDD15\uD802\uDD07", TokenType.PUBLIC); // 𐤐𐤕𐤇
        MULTI_CHAR_TOKENS.put("\uD802\uDD0E\uD802\uDD02\uD802\uDD13", TokenType.PRIVATE); // 𐤎𐤂𐤓
        MULTI_CHAR_TOKENS.put("\uD802\uDD14\uD802\uDD0C\uD802\uDD13", TokenType.PROTECTED); // 𐤔𐤌𐤓

        // Loops (multi-char)
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD14\uD802\uDD04", TokenType.DO);    // 𐤏𐤔𐤄

        // Jump (multi-char)
        MULTI_CHAR_TOKENS.put("\uD802\uDD0B\uD802\uDD0A", TokenType.GOTO);  // 𐤋𐤊
        MULTI_CHAR_TOKENS.put("\uD802\uDD0C\uD802\uDD03\uD802\uDD04", TokenType.INSTANCEOF); // 𐤌𐤃𐤄

        // Switch
        MULTI_CHAR_TOKENS.put("\uD802\uDD12\uD802\uDD10\uD802\uDD07", TokenType.SWITCH); // 𐤒𐤐𐤇
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD0B\uD802\uDD09", TokenType.CASE);  // 𐤏𐤋𐤉
        MULTI_CHAR_TOKENS.put("\uD802\uDD05\uD802\uDD0B\uD802\uDD00", TokenType.DEFAULT); // 𐤅𐤋𐤀

        // OOP
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD01\uD802\uDD0D", TokenType.NEW);   // 𐤀𐤁𐤍
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD0B", TokenType.SUPER);            // 𐤏𐤋
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD0D\uD802\uDD0A", TokenType.THIS); // 𐤀𐤍𐤊
        MULTI_CHAR_TOKENS.put("\uD802\uDD09\uD802\uDD10\uD802\uDD0F\uD802\uDD0C", TokenType.IMPLEMENTS); // 𐤉𐤐𐤏𐤌
        MULTI_CHAR_TOKENS.put("\uD802\uDD09\uD802\uDD10\uD802\uDD0F\uD802\uDD15", TokenType.EXTENDS);   // 𐤉𐤐𐤏𐤕
        MULTI_CHAR_TOKENS.put("\uD802\uDD04\uD802\uDD09\uD802\uDD04", TokenType.CONSTRUCTOR); // 𐤄𐤉𐤄
        MULTI_CHAR_TOKENS.put("\uD802\uDD10\uD802\uDD0F\uD802\uDD0B", TokenType.METHOD); // 𐤐𐤏𐤋

        // Module
        MULTI_CHAR_TOKENS.put("\uD802\uDD0F\uD802\uDD13\uD802\uDD01", TokenType.PACKAGE); // 𐤏𐤓𐤁
        MULTI_CHAR_TOKENS.put("\uD802\uDD09\uD802\uDD01\uD802\uDD00", TokenType.IMPORT); // 𐤉𐤁𐤀

        // Exception
        MULTI_CHAR_TOKENS.put("\uD802\uDD0D\uD802\uDD0E\uD802\uDD04", TokenType.TRY);   // 𐤍𐤎𐤄
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD15", TokenType.CATCH); // 𐤀𐤇𐤃𐤕
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD07\uD802\uDD13\uD802\uDD09", TokenType.FINALLY); // 𐤀𐤇𐤓𐤉
        MULTI_CHAR_TOKENS.put("\uD802\uDD13\uD802\uDD0C\uD802\uDD04", TokenType.THROW); // 𐤓𐤌𐤄
        MULTI_CHAR_TOKENS.put("\uD802\uDD13\uD802\uDD0C\uD802\uDD04\uD802\uDD15", TokenType.THROWS); // 𐤓𐤌𐤄𐤕

        // Concurrency
        MULTI_CHAR_TOKENS.put("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD0F\uD802\uDD0D", TokenType.SYNCHRONIZED); // 𐤀𐤇𐤃𐤏𐤍

        // Literals (multi-char)
        MULTI_CHAR_TOKENS.put("\uD802\uDD13\uD802\uDD09\uD802\uDD12\uD802\uDD12", TokenType.NULL); // 𐤓𐤉𐤒𐤒
    }

    // ========== SINGLE CHARACTER TOKENS ==========
    private static final Map<String, TokenType> SINGLE_CHAR_TOKENS = new HashMap<>();
    static {
        // ASCII Delimiters
        SINGLE_CHAR_TOKENS.put("{", TokenType.LBRACE);
        SINGLE_CHAR_TOKENS.put("}", TokenType.RBRACE);
        SINGLE_CHAR_TOKENS.put("(", TokenType.LPAREN);
        SINGLE_CHAR_TOKENS.put(")", TokenType.RPAREN);
        SINGLE_CHAR_TOKENS.put(";", TokenType.SEMICOLON);
        SINGLE_CHAR_TOKENS.put(".", TokenType.DOT);
        SINGLE_CHAR_TOKENS.put(",", TokenType.COMMA);
        SINGLE_CHAR_TOKENS.put("?", TokenType.QUESTION);
        SINGLE_CHAR_TOKENS.put(":", TokenType.COLON);
        SINGLE_CHAR_TOKENS.put("<", TokenType.LT);
        SINGLE_CHAR_TOKENS.put(">", TokenType.GT);

        // Single Phoenician characters (operators)
        SINGLE_CHAR_TOKENS.put("\uD802\uDD10", TokenType.PLUS);       // 𐤐
        SINGLE_CHAR_TOKENS.put("\uD802\uDD11", TokenType.MINUS);      // 𐤑
        SINGLE_CHAR_TOKENS.put("\uD802\uDD12", TokenType.MULTIPLY);   // 𐤒
        SINGLE_CHAR_TOKENS.put("\uD802\uDD13", TokenType.DIVIDE);     // 𐤓
        SINGLE_CHAR_TOKENS.put("\uD802\uDD14", TokenType.ASSIGN);     // 𐤔
        SINGLE_CHAR_TOKENS.put("\uD802\uDD15", TokenType.GREATER);    // 𐤕
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0F", TokenType.MODULO);     // 𐤏
        SINGLE_CHAR_TOKENS.put("\uD802\uDD00", TokenType.LOGICAL_AND); // 𐤀 (as AND)
        SINGLE_CHAR_TOKENS.put("\uD802\uDD05", TokenType.LOGICAL_OR);  // 𐤅 (as OR)

        // ===== KEYWORDS (single character) =====
        SINGLE_CHAR_TOKENS.put("\uD802\uDD06", TokenType.AUTO);       // 𐤆
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0D", TokenType.INT);        // 𐤍
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0E", TokenType.CHAR);       // 𐤎
        SINGLE_CHAR_TOKENS.put("\uD802\uDD07", TokenType.PRINT);      // 𐤇
        SINGLE_CHAR_TOKENS.put("\uD802\uDD08", TokenType.INPUT);      // 𐤈
        SINGLE_CHAR_TOKENS.put("\uD802\uDD09", TokenType.TRUE);       // 𐤉
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0A", TokenType.FALSE);      // 𐤊
        SINGLE_CHAR_TOKENS.put("\uD802\uDD00", TokenType.IF);         // 𐤀
        SINGLE_CHAR_TOKENS.put("\uD802\uDD01", TokenType.ELSE);       // 𐤁
        SINGLE_CHAR_TOKENS.put("\uD802\uDD02", TokenType.WHILE);      // 𐤂
        SINGLE_CHAR_TOKENS.put("\uD802\uDD03", TokenType.FOR);        // 𐤃
        SINGLE_CHAR_TOKENS.put("\uD802\uDD05", TokenType.RETURN);     // 𐤅
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0B", TokenType.BREAK);      // 𐤋
        SINGLE_CHAR_TOKENS.put("\uD802\uDD0C", TokenType.CONTINUE);   // 𐤌
    }

    public Lexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
    }

    public List<Token> tokenize() throws Exception {
        while (position < source.length()) {
            // Use codePointAt to handle surrogate pairs (Phoenician characters)
            int codePoint = source.codePointAt(position);
            String currentCharStr = new String(Character.toChars(codePoint));

            if (codePoint == '\uFEFF') {
                position += Character.charCount(codePoint);
                continue;
            }

            // Skip whitespace
            if (Character.isWhitespace(codePoint)) {
                if (codePoint == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
                position += Character.charCount(codePoint);
                continue;
            }

            if (codePoint == '/' && peekNextCodePoint() == '/') {
                skipLineComment();
                continue;
            }

            // Check for multi-character tokens first (longest match)
            boolean matched = false;
            for (Map.Entry<String, TokenType> entry : MULTI_CHAR_TOKENS.entrySet()) {
                String tokenStr = entry.getKey();
                if (source.startsWith(tokenStr, position)) {
                    addToken(entry.getValue(), tokenStr);
                    position += tokenStr.length();
                    column += tokenStr.codePointCount(0, tokenStr.length());
                    matched = true;
                    break;
                }
            }
            if (matched) continue;

            // Check for single character tokens
            if (SINGLE_CHAR_TOKENS.containsKey(currentCharStr)) {
                addToken(SINGLE_CHAR_TOKENS.get(currentCharStr), currentCharStr);
                position += Character.charCount(codePoint);
                column++;
                continue;
            }

            // Check for numbers
            if (Character.isDigit(codePoint)) {
                tokenizeNumber();
                continue;
            }

            // Check for identifiers (a-z, A-Z, _)
            // Exclude Phoenician characters (Unicode range U+10900-U+1091F)
            boolean isPhoenician = (codePoint >= 0x10900 && codePoint <= 0x1091F);
            if (Character.isLetter(codePoint) && !isPhoenician) {
                tokenizeIdentifier();
                continue;
            }

            // Check for strings
            if (codePoint == '"') {
                tokenizeString();
                continue;
            }

            // Error
            throw new Exception("Unknown character '" + currentCharStr + "' at line " + line + ", column " + column);
        }

        addToken(TokenType.EOF, "");
        return tokens;
    }

    private void tokenizeNumber() {
        StringBuilder num = new StringBuilder();
        int startColumn = column;
        while (position < source.length() && Character.isDigit(source.charAt(position))) {
            num.append(source.charAt(position));
            position++;
            column++;
        }
        addToken(TokenType.NUMBER, num.toString(), startColumn);
    }

    private void tokenizeIdentifier() {
        StringBuilder ident = new StringBuilder();
        int startColumn = column;
        while (position < source.length() &&
                (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) {
            ident.append(source.charAt(position));
            position++;
            column++;
        }
        addToken(TokenType.IDENTIFIER, ident.toString(), startColumn);
    }

    private void tokenizeString() throws Exception {
        StringBuilder str = new StringBuilder();
        int startColumn = column;
        position++;
        column++;

        while (position < source.length() && source.charAt(position) != '"') {
            if (source.charAt(position) == '\n') {
                throw new Exception("Unclosed string at line " + line);
            }
            str.append(source.charAt(position));
            position++;
            column++;
        }

        if (position >= source.length()) {
            throw new Exception("Unclosed string at line " + line);
        }

        position++;
        column++;
        addToken(TokenType.STRING, str.toString(), startColumn);
    }

    private int peekNextCodePoint() {
        int nextPosition = position + Character.charCount(source.codePointAt(position));
        if (nextPosition >= source.length()) {
            return -1;
        }
        return source.codePointAt(nextPosition);
    }

    private void skipLineComment() {
        while (position < source.length() && source.charAt(position) != '\n') {
            position++;
            column++;
        }
    }

    private void addToken(TokenType type, String value) {
        tokens.add(new Token(type, value, line, column));
    }

    private void addToken(TokenType type, String value, int startColumn) {
        tokens.add(new Token(type, value, line, startColumn));
    }
}
