package lexer;

public enum TokenType {
    // ========== STORAGE SPECIFIERS ==========
    AUTO("\uD802\uDD06"),                                // 𐤆
    EXTERN("\uD802\uDD07\uD802\uDD05\uD802\uDD11"),      // 𐤇𐤅𐤑
    REGISTER("\uD802\uDD0E\uD802\uDD10\uD802\uDD13"),    // 𐤎𐤐𐤓
    STATIC("\uD802\uDD00\uD802\uDD0C\uD802\uDD15"),      // 𐤀𐤌𐤕
    NATIVE("\uD802\uDD00\uD802\uDD03\uD802\uDD0C"),      // 𐤀𐤃𐤌
    TRANSIENT("\uD802\uDD0F\uD802\uDD01\uD802\uDD13\uD802\uDD09"), // 𐤏𐤁𐤓𐤉

    // ========== TYPE SPECIFIERS (single char where possible) ==========
    INT("\uD802\uDD0D"),                                 // 𐤍
    CHAR("\uD802\uDD0E"),                                // 𐤎
    ENUM("\uD802\uDD0F\uD802\uDD03\uD802\uDD14"),        // 𐤏𐤃𐤔
    FLOAT("\uD802\uDD11\uD802\uDD10\uD802\uDD04"),       // 𐤑𐤐𐤄
    DOUBLE("\uD802\uDD12\uD802\uDD09\uD802\uDD10"),      // 𐤒𐤉𐤐
    LONG("\uD802\uDD00\uD802\uDD13\uD802\uDD0A"),        // 𐤀𐤓𐤊
    SHORT("\uD802\uDD12\uD802\uDD11\uD802\uDD13"),       // 𐤒𐤑𐤓
    SIGNED("\uD802\uDD00\uD802\uDD05\uD802\uDD15"),      // 𐤀𐤅𐤕
    UNSIGNED("\uD802\uDD01\uD802\uDD0B\uD802\uDD09"),    // 𐤁𐤋𐤉
    UNION("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD0F"), // 𐤀𐤇𐤃𐤏
    VOID("\uD802\uDD13\uD802\uDD09\uD802\uDD12"),        // 𐤓𐤉𐤒
    TYPEDEF("\uD802\uDD14\uD802\uDD0C\uD802\uDD04"),     // 𐤔𐤌𐤄
    CLASS("\uD802\uDD0F\uD802\uDD09\uD802\uDD0D"),       // 𐤏𐤉𐤍
    INTERFACE("\uD802\uDD05\uD802\uDD14\uD802\uDD09"),   // 𐤅𐤔𐤉
    STRUCT("\uD802\uDD01\uD802\uDD0D\uD802\uDD09\uD802\uDD15"), // 𐤁𐤍𐤉𐤕

    // ========== TYPE QUALIFIERS ==========
    CONST("\uD802\uDD0A\uD802\uDD01\uD802\uDD0F"),       // 𐤊𐤁𐤏
    VOLATILE("\uD802\uDD05\uD802\uDD0B\uD802\uDD00\uD802\uDD15"), // 𐤅𐤋𐤀𐤕
    FINAL("\uD802\uDD00\uD802\uDD07\uD802\uDD13"),       // 𐤀𐤇𐤓
    STRICTFP("\uD802\uDD12\uD802\uDD03\uD802\uDD14"),    // 𐤒𐤃𐤔
    SIZEOF("\uD802\uDD0A\uD802\uDD0C\uD802\uDD04"),      // 𐤊𐤌𐤄

    // ========== ACCESS MODIFIERS ==========
    PUBLIC("\uD802\uDD10\uD802\uDD15\uD802\uDD07"),      // 𐤐𐤕𐤇
    PRIVATE("\uD802\uDD0E\uD802\uDD02\uD802\uDD13"),     // 𐤎𐤂𐤓
    PROTECTED("\uD802\uDD14\uD802\uDD0C\uD802\uDD13"),   // 𐤔𐤌𐤓

    // ========== CONDITIONAL ==========
    IF("\uD802\uDD00"),                                  // 𐤀
    ELSE("\uD802\uDD01"),                                // 𐤁

    // ========== LOOPS ==========
    WHILE("\uD802\uDD02"),                               // 𐤂
    FOR("\uD802\uDD03"),                                 // 𐤃
    DO("\uD802\uDD0F\uD802\uDD14\uD802\uDD04"),          // 𐤏𐤔𐤄
    CONTINUE("\uD802\uDD0C"),                            // 𐤌

    // ========== JUMP ==========
    BREAK("\uD802\uDD0B"),                               // 𐤋
    RETURN("\uD802\uDD05"),                              // 𐤅
    GOTO("\uD802\uDD0B\uD802\uDD0A"),                    // 𐤋𐤊
    INSTANCEOF("\uD802\uDD0C\uD802\uDD03\uD802\uDD04"),  // 𐤌𐤃𐤄

    // ========== SWITCH ==========
    SWITCH("\uD802\uDD12\uD802\uDD10\uD802\uDD07"),      // 𐤒𐤐𐤇
    CASE("\uD802\uDD0F\uD802\uDD0B\uD802\uDD09"),        // 𐤏𐤋𐤉
    DEFAULT("\uD802\uDD05\uD802\uDD0B\uD802\uDD00"),     // 𐤅𐤋𐤀

    // ========== OOP ==========
    NEW("\uD802\uDD00\uD802\uDD01\uD802\uDD0D"),         // 𐤀𐤁𐤍
    SUPER("\uD802\uDD0F\uD802\uDD0B"),                   // 𐤏𐤋
    THIS("\uD802\uDD00\uD802\uDD0D\uD802\uDD0A"),        // 𐤀𐤍𐤊
    IMPLEMENTS("\uD802\uDD09\uD802\uDD10\uD802\uDD0F\uD802\uDD0C"), // 𐤉𐤐𐤏𐤌
    EXTENDS("\uD802\uDD09\uD802\uDD10\uD802\uDD0F\uD802\uDD15"),   // 𐤉𐤐𐤏𐤕
    CONSTRUCTOR("\uD802\uDD04\uD802\uDD09\uD802\uDD04"), // 𐤄𐤉𐤄
    METHOD("\uD802\uDD10\uD802\uDD0F\uD802\uDD0B"),      // 𐤐𐤏𐤋

    // ========== MODULE ==========
    PACKAGE("\uD802\uDD0F\uD802\uDD13\uD802\uDD01"),     // 𐤏𐤓𐤁
    IMPORT("\uD802\uDD09\uD802\uDD01\uD802\uDD00"),      // 𐤉𐤁𐤀

    // ========== EXCEPTION ==========
    TRY("\uD802\uDD0D\uD802\uDD0E\uD802\uDD04"),         // 𐤍𐤎𐤄
    CATCH("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD15"), // 𐤀𐤇𐤃𐤕
    FINALLY("\uD802\uDD00\uD802\uDD07\uD802\uDD13\uD802\uDD09"), // 𐤀𐤇𐤓𐤉
    THROW("\uD802\uDD13\uD802\uDD0C\uD802\uDD04"),       // 𐤓𐤌𐤄
    THROWS("\uD802\uDD13\uD802\uDD0C\uD802\uDD04\uD802\uDD15"), // 𐤓𐤌𐤄𐤕

    // ========== CONCURRENCY ==========
    SYNCHRONIZED("\uD802\uDD00\uD802\uDD07\uD802\uDD03\uD802\uDD0F\uD802\uDD0D"), // 𐤀𐤇𐤃𐤏𐤍

    // ========== I/O (Single chars) ==========
    PRINT("\uD802\uDD07"),                               // 𐤇
    INPUT("\uD802\uDD08"),                               // 𐤈

    // ========== LITERALS ==========
    TRUE("\uD802\uDD09"),                                // 𐤉
    FALSE("\uD802\uDD0A"),                               // 𐤊
    NULL("\uD802\uDD13\uD802\uDD09\uD802\uDD12\uD802\uDD12"), // 𐤓𐤉𐤒𐤒

    // ========== OPERATORS ==========
    ASSIGN("\uD802\uDD14"),                              // 𐤔
    EQUAL("\uD802\uDD14\uD802\uDD14"),                   // 𐤔𐤔
    NOT_EQUAL("\uD802\uDD0D\uD802\uDD14"),               // 𐤍𐤔
    GREATER("\uD802\uDD15"),                             // 𐤕
    LESS("\uD802\uDD15\uD802\uDD00"),                    // 𐤕𐤀
    GREATER_EQUAL("\uD802\uDD15\uD802\uDD14"),           // 𐤕𐤔
    LESS_EQUAL("\uD802\uDD00\uD802\uDD15"),              // 𐤀𐤕
    PLUS("\uD802\uDD10"),                                // 𐤐
    MINUS("\uD802\uDD11"),                               // 𐤑
    MULTIPLY("\uD802\uDD12"),                            // 𐤒
    DIVIDE("\uD802\uDD13"),                              // 𐤓
    MODULO("\uD802\uDD0F"),                              // 𐤏
    INCREMENT("\uD802\uDD10\uD802\uDD10"),               // 𐤐𐤐
    DECREMENT("\uD802\uDD11\uD802\uDD11"),               // 𐤑𐤑
    PLUS_ASSIGN("\uD802\uDD10\uD802\uDD14"),             // 𐤐𐤔
    MINUS_ASSIGN("\uD802\uDD11\uD802\uDD14"),            // 𐤑𐤔
    MULTIPLY_ASSIGN("\uD802\uDD12\uD802\uDD14"),         // 𐤒𐤔
    DIVIDE_ASSIGN("\uD802\uDD13\uD802\uDD14"),           // 𐤓𐤔
    LOGICAL_NOT("\uD802\uDD00\uD802\uDD0B"),             // 𐤀𐤋
    BITWISE_NOT("\uD802\uDD0D\uD802\uDD0F"),             // 𐤍𐤏
    LOGICAL_AND("\uD802\uDD00"),                         // 𐤀 (as AND)
    LOGICAL_OR("\uD802\uDD05"),                          // 𐤅 (as OR)
    QUESTION("?"),
    COLON(":"),

    // ========== DELIMITERS ==========
    LBRACE("{"),
    RBRACE("}"),
    LPAREN("("),
    RPAREN(")"),
    SEMICOLON(";"),
    DOT("."),
    COMMA(","),
    LT("<"),
    GT(">"),

    // ========== SPECIAL ==========
    IDENTIFIER(null),
    NUMBER(null),
    STRING(null),
    EOF(null),
    ERROR(null);

    private final String symbol;

    TokenType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isKeyword() {
        return symbol != null && !symbol.isEmpty() &&
                this != IDENTIFIER && this != NUMBER &&
                this != STRING &&
                this != EOF && this != ERROR &&
                this != QUESTION && this != COLON &&
                this != LBRACE && this != RBRACE &&
                this != LPAREN && this != RPAREN &&
                this != SEMICOLON && this != DOT &&
                this != COMMA && this != LT && this != GT;
    }
}
