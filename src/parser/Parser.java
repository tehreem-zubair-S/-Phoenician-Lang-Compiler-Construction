package parser;

import lexer.*;
import java.util.*;

/**
 * Recursive Descent Parser for Phoenician Language
 *
 * Implements the complete CFG with:
 * - 52 Reserved Keywords
 * - OOP Features (Class, Object, Method, Constructor)
 * - Error Recovery
 */
public class Parser {

    private final List<Token> tokens;
    private int position;
    private Token currentToken;
    private SymbolTable symbolTable;
    private final List<String> errors;
    private boolean hasErrors;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.errors = new ArrayList<>();
        this.hasErrors = false;
        this.symbolTable = new SymbolTable();
        if (!tokens.isEmpty()) {
            this.currentToken = tokens.get(0);
        } else {
            this.currentToken = new Token(TokenType.EOF, "", 1, 1);
        }
    }

    public Parser(List<Token> tokens, SymbolTable symbolTable) {
        this(tokens);
        this.symbolTable = symbolTable;
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    // ========== HELPER METHODS ==========

    private void advance() {
        position++;
        if (position < tokens.size()) {
            currentToken = tokens.get(position);
        } else {
            currentToken = new Token(TokenType.EOF, "",
                    currentToken.getLine(), currentToken.getColumn());
        }
    }

    private void expect(TokenType type) throws Exception {
        if (currentToken.getType() == type) {
            advance();
        } else {
            String error = "Syntax Error at line " + currentToken.getLine() +
                    ", column " + currentToken.getColumn() +
                    ": expected " + describeToken(type) +
                    " but found " + describeToken(currentToken);
            errors.add(error);
            hasErrors = true;
            throw new Exception(error);
        }
    }

    private String describeToken(TokenType type) {
        String symbol = type.getSymbol();
        if (symbol == null || symbol.isEmpty()) {
            return type.toString();
        }
        return type + " ('" + symbol + "')";
    }

    private String describeToken(Token token) {
        String value = token.getValue();
        if (value == null || value.isEmpty()) {
            value = "<EOF>";
        }
        return token.getType() + " ('" + value + "')";
    }

    private <T extends ASTNode> T mark(T node, int line, int column) {
        if (node != null) {
            node.setSourceLocation(line, column);
        }
        return node;
    }

    private boolean match(TokenType type) {
        if (currentToken.getType() == type) {
            advance();
            return true;
        }
        return false;
    }

    private boolean isTypeSpec() {
        TokenType type = currentToken.getType();
        return type == TokenType.INT || type == TokenType.CHAR ||
                type == TokenType.FLOAT || type == TokenType.DOUBLE ||
                type == TokenType.LONG || type == TokenType.SHORT ||
                type == TokenType.SIGNED || type == TokenType.UNSIGNED ||
                type == TokenType.VOID || type == TokenType.ENUM ||
                type == TokenType.STRUCT || type == TokenType.UNION ||
                type == TokenType.CLASS || type == TokenType.INTERFACE ||
                type == TokenType.TYPEDEF ||
                (type == TokenType.IDENTIFIER && !isKeywordIdentifier());
    }

    private boolean startsDeclaration() {
        if (!isTypeSpec()) {
            return false;
        }
        if (currentToken.getType() != TokenType.IDENTIFIER) {
            return true;
        }
        return peekNextType() == TokenType.IDENTIFIER;
    }

    private boolean isKeywordIdentifier() {
        String value = currentToken.getValue();
        String[] keywords = {
                "𐤀𐤋𐤐", "𐤁𐤏𐤃", "𐤂𐤌𐤋", "𐤃𐤋𐤕", "𐤅𐤅𐤅", "𐤋𐤁𐤓", "𐤌𐤌𐤊",
                "𐤋𐤊𐤕", "𐤒𐤐𐤇", "𐤏𐤋𐤉", "𐤅𐤋𐤀", "𐤏𐤔𐤄", "𐤀𐤕𐤍", "𐤇𐤅𐤑",
                "𐤎𐤐𐤓", "𐤀𐤌𐤕", "𐤀𐤋𐤐", "𐤍𐤅𐤍", "𐤎𐤓𐤉", "𐤏𐤃𐤔", "𐤑𐤐𐤄",
                "𐤒𐤉𐤐", "𐤀𐤓𐤊", "𐤒𐤑𐤓", "𐤀𐤅𐤕", "𐤁𐤋𐤉", "𐤀𐤇𐤃", "𐤓𐤉𐤒",
                "𐤔𐤌𐤄", "𐤁𐤍𐤉𐤕", "𐤅𐤔𐤉", "𐤊𐤁𐤏", "𐤏𐤁𐤓", "𐤀𐤇𐤓", "𐤒𐤃𐤔",
                "𐤊𐤌𐤄", "𐤐𐤕𐤇", "𐤎𐤂𐤓", "𐤔𐤌𐤓", "𐤀𐤃𐤌", "𐤉𐤐𐤏", "𐤉𐤁𐤀",
                "𐤌𐤃𐤄", "𐤏𐤋", "𐤀𐤍𐤊", "𐤍𐤎𐤄", "𐤓𐤌𐤄", "𐤓𐤌𐤄𐤕"
        };
        for (String kw : keywords) {
            if (value.equals(kw)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAccessModifier() {
        TokenType type = currentToken.getType();
        return type == TokenType.PUBLIC || type == TokenType.PRIVATE ||
                type == TokenType.PROTECTED || type == TokenType.STATIC;
    }

    private boolean isStorageSpecifier() {
        TokenType type = currentToken.getType();
        return type == TokenType.AUTO || type == TokenType.EXTERN ||
                type == TokenType.REGISTER || type == TokenType.STATIC ||
                type == TokenType.NATIVE || type == TokenType.TRANSIENT;
    }

    private boolean isTypeQualifier() {
        TokenType type = currentToken.getType();
        return type == TokenType.CONST || type == TokenType.VOLATILE ||
                type == TokenType.FINAL || type == TokenType.STRICTFP;
    }

    private boolean isAssignmentOp() {
        TokenType type = currentToken.getType();
        return type == TokenType.ASSIGN ||
                type == TokenType.PLUS_ASSIGN ||
                type == TokenType.MINUS_ASSIGN ||
                type == TokenType.MULTIPLY_ASSIGN ||
                type == TokenType.DIVIDE_ASSIGN;
    }

    private TokenType peekNextType() {
        if (position + 1 < tokens.size()) {
            return tokens.get(position + 1).getType();
        }
        return TokenType.EOF;
    }

    private void synchronize() {
        while (currentToken.getType() != TokenType.EOF) {
            if (currentToken.getType() == TokenType.SEMICOLON) {
                advance();
                return;
            }
            switch (currentToken.getType()) {
                case CLASS: case INTERFACE: case IF: case WHILE:
                case FOR: case DO: case SWITCH: case RETURN:
                case BREAK: case CONTINUE: case GOTO:
                case TRY: case THROW: case SYNCHRONIZED:
                case PUBLIC: case PRIVATE: case PROTECTED:
                case STATIC: case LBRACE:
                    return;
                default:
                    advance();
            }
        }
    }

    // ========== GRAMMAR RULES ==========

    public ASTNode parseProgram() throws Exception {
        List<StatementNode> statements = new ArrayList<>();
        while (currentToken.getType() != TokenType.EOF) {
            try {
                int line = currentToken.getLine();
                int column = currentToken.getColumn();
                StatementNode stmt = parseStatement();
                if (stmt != null) {
                    mark(stmt, line, column);
                    statements.add(stmt);
                }
            } catch (Exception e) {
                if (!errors.contains(e.getMessage())) {
                    errors.add(e.getMessage());
                }
                hasErrors = true;
                synchronize();
            }
        }
        return mark(new ProgramNode(statements), 1, 1);
    }

    private StatementNode parseStatement() throws Exception {
        TokenType type = currentToken.getType();

        if (type == TokenType.PACKAGE) {
            return parsePackageDecl();
        }
        if (type == TokenType.IMPORT) {
            return parseImportDecl();
        }
        if (type == TokenType.CLASS || type == TokenType.STRUCT) {
            return parseClassDecl();
        }
        if (type == TokenType.INTERFACE) {
            return parseInterfaceDecl();
        }
        if (isAccessModifier()) {
            return parseMethodOrConstructorDecl();
        }
        if (isStorageSpecifier()) {
            return parseVarOrFuncDecl();
        }
        if (startsDeclaration()) {
            return parseVarOrFuncDecl();
        }

        switch (type) {
            case IF: return parseIfStmt();
            case WHILE: return parseWhileStmt();
            case FOR: return parseForStmt();
            case DO: return parseDoWhileStmt();
            case SWITCH: return parseSwitchStmt();
            case BREAK: return parseBreakStmt();
            case CONTINUE: return parseContinueStmt();
            case GOTO: return parseGotoStmt();
            case RETURN: return parseReturnStmt();
            case PRINT: return parsePrintStmt();
            case INPUT: return parseInputStmt();
            case NEW: return parseObjectDecl();
            case TRY: return parseTryStmt();
            case THROW: return parseThrowStmt();
            case SYNCHRONIZED: return parseSynchronizedStmt();
            case LBRACE: return parseBlock();
            default:
                ExpressionNode expr = parseExpression();
                expect(TokenType.SEMICOLON);
            return mark(new ExpressionStatementNode(expr), expr.getLine(), expr.getColumn());
        }
    }

    // ========== DECLARATIONS ==========

    private PackageDeclNode parsePackageDecl() throws Exception {
        expect(TokenType.PACKAGE);
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.SEMICOLON);
        return new PackageDeclNode(name);
    }

    private ImportDeclNode parseImportDecl() throws Exception {
        expect(TokenType.IMPORT);
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        boolean isStarImport = false;
        if (match(TokenType.DOT)) {
            if (match(TokenType.MULTIPLY)) {
                isStarImport = true;
            } else {
                while (currentToken.getType() == TokenType.IDENTIFIER ||
                        currentToken.getType() == TokenType.DOT) {
                    if (currentToken.getType() == TokenType.IDENTIFIER) {
                        name += "." + currentToken.getValue();
                    }
                    advance();
                }
            }
        }
        expect(TokenType.SEMICOLON);
        return new ImportDeclNode(name, isStarImport);
    }

    private StatementNode parseVarOrFuncDecl() throws Exception {
        List<String> storageSpecs = new ArrayList<>();
        while (isStorageSpecifier()) {
            storageSpecs.add(currentToken.getValue());
            advance();
        }

        List<String> qualifiers = new ArrayList<>();
        while (isTypeQualifier()) {
            qualifiers.add(currentToken.getValue());
            advance();
        }

        String type = parseTypeSpec();
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);

        if (currentToken.getType() == TokenType.LPAREN) {
            advance();
            List<ParamNode> params = parseParamList();
            expect(TokenType.RPAREN);

            List<String> throwsList = new ArrayList<>();
            if (currentToken.getType() == TokenType.THROWS) {
                advance();
                throwsList.add(parseTypeSpec());
                while (match(TokenType.COMMA)) {
                    throwsList.add(parseTypeSpec());
                }
            }

            BlockNode body = parseBlock();
            symbolTable.addFunction(name, type);
            for (ParamNode param : params) {
                symbolTable.addSymbol(param.getName(), param.getType());
            }
            return new FuncDeclNode(storageSpecs, qualifiers, type, name, params, throwsList, body);
        } else {
            ExpressionNode init = null;
            if (match(TokenType.ASSIGN)) {
                init = parseExpression();
            }
            expect(TokenType.SEMICOLON);
            symbolTable.addSymbol(name, type);
            return new VarDeclNode(storageSpecs, qualifiers, type, name, init);
        }
    }

    private StatementNode parseMethodOrConstructorDecl() throws Exception {
        List<String> modifiers = new ArrayList<>();
        while (isAccessModifier()) {
            modifiers.add(currentToken.getValue());
            advance();
        }

        boolean isConstructor = false;
        String returnType = null;
        String name;

        if (isTypeSpec()) {
            returnType = parseTypeSpec();
            name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
        } else {
            name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
            isConstructor = true;
        }

        expect(TokenType.LPAREN);
        List<ParamNode> params = parseParamList();
        expect(TokenType.RPAREN);

        List<String> throwsList = new ArrayList<>();
        if (currentToken.getType() == TokenType.THROWS) {
            advance();
            throwsList.add(parseTypeSpec());
            while (match(TokenType.COMMA)) {
                throwsList.add(parseTypeSpec());
            }
        }

        BlockNode body = parseBlock();

        if (isConstructor) {
            return new ConstructorDeclNode(modifiers, name, params, throwsList, body);
        } else {
            symbolTable.addFunction(name, returnType);
            for (ParamNode param : params) {
                symbolTable.addSymbol(param.getName(), param.getType());
            }
            return new MethodDeclNode(modifiers, returnType, name, params, throwsList, body);
        }
    }

    private String parseTypeSpec() throws Exception {
        if (!isTypeSpec()) {
            throw new Exception("Expected type specifier at line " + currentToken.getLine());
        }
        String type = currentToken.getValue();
        advance();
        return type;
    }

    private List<ParamNode> parseParamList() throws Exception {
        List<ParamNode> params = new ArrayList<>();
        if (currentToken.getType() == TokenType.RPAREN) {
            return params;
        }
        params.add(parseParamDecl());
        while (match(TokenType.COMMA)) {
            params.add(parseParamDecl());
        }
        return params;
    }

    private ParamNode parseParamDecl() throws Exception {
        String type = parseTypeSpec();
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        return new ParamNode(type, name);
    }

    // ========== OOP ==========

    private ClassDeclNode parseClassDecl() throws Exception {
        String accessModifier = null;
        if (isAccessModifier()) {
            accessModifier = currentToken.getValue();
            advance();
        }
        if (currentToken.getType() == TokenType.CLASS || currentToken.getType() == TokenType.STRUCT) {
            advance();
        } else {
            expect(TokenType.CLASS);
        }
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);

        String superClass = null;
        if (currentToken.getType() == TokenType.EXTENDS) {
            advance();
            superClass = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
        }

        List<String> interfaces = new ArrayList<>();
        if (currentToken.getType() == TokenType.IMPLEMENTS) {
            advance();
            interfaces.add(currentToken.getValue());
            expect(TokenType.IDENTIFIER);
            while (match(TokenType.COMMA)) {
                interfaces.add(currentToken.getValue());
                expect(TokenType.IDENTIFIER);
            }
        }

        expect(TokenType.LBRACE);
        List<ClassMemberNode> members = parseClassBody();
        expect(TokenType.RBRACE);

        symbolTable.addClass(name, superClass);
        return new ClassDeclNode(accessModifier, name, superClass, interfaces, members);
    }

    private List<ClassMemberNode> parseClassBody() throws Exception {
        List<ClassMemberNode> members = new ArrayList<>();
        while (currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            members.add(parseClassMember());
        }
        return members;
    }

    private ClassMemberNode parseClassMember() throws Exception {
        if (currentToken.getType() == TokenType.CLASS) {
            ClassDeclNode innerClass = parseClassDecl();
            return new InnerClassMemberNode(innerClass);
        }

        List<String> modifiers = new ArrayList<>();
        while (isAccessModifier() || currentToken.getType() == TokenType.STATIC) {
            modifiers.add(currentToken.getValue());
            advance();
        }

        if (currentToken.getType() == TokenType.CONSTRUCTOR) {
            advance();
            expect(TokenType.LPAREN);
            List<ParamNode> params = parseParamList();
            expect(TokenType.RPAREN);
            BlockNode body = parseBlock();
            return new ConstructorMemberNode(modifiers, "constructor", params, new ArrayList<>(), body);
        }

        if (currentToken.getType() == TokenType.METHOD) {
            advance();
            String name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
            expect(TokenType.LPAREN);
            List<ParamNode> params = parseParamList();
            expect(TokenType.RPAREN);
            String returnType = parseTypeSpec();
            BlockNode body = parseBlock();
            return new MethodMemberNode(modifiers, returnType, name, params, new ArrayList<>(), body);
        }

        if (isTypeSpec()) {
            String type = parseTypeSpec();
            String name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);

            if (currentToken.getType() == TokenType.LPAREN) {
                advance();
                List<ParamNode> params = parseParamList();
                expect(TokenType.RPAREN);

                List<String> throwsList = new ArrayList<>();
                if (currentToken.getType() == TokenType.THROWS) {
                    advance();
                    throwsList.add(parseTypeSpec());
                    while (match(TokenType.COMMA)) {
                        throwsList.add(parseTypeSpec());
                    }
                }

                BlockNode body = parseBlock();
                return new MethodMemberNode(modifiers, type, name, params, throwsList, body);
            } else {
                ExpressionNode init = null;
                if (match(TokenType.ASSIGN)) {
                    init = parseExpression();
                }
                expect(TokenType.SEMICOLON);
                return new FieldMemberNode(modifiers, type, name, init);
            }
        } else {
            // Constructor
            String name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
            expect(TokenType.LPAREN);
            List<ParamNode> params = parseParamList();
            expect(TokenType.RPAREN);

            List<String> throwsList = new ArrayList<>();
            if (currentToken.getType() == TokenType.THROWS) {
                advance();
                throwsList.add(parseTypeSpec());
                while (match(TokenType.COMMA)) {
                    throwsList.add(parseTypeSpec());
                }
            }

            BlockNode body = parseBlock();
            return new ConstructorMemberNode(modifiers, name, params, throwsList, body);
        }
    }

    private InterfaceDeclNode parseInterfaceDecl() throws Exception {
        String accessModifier = null;
        if (isAccessModifier()) {
            accessModifier = currentToken.getValue();
            advance();
        }
        expect(TokenType.INTERFACE);
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);

        List<String> extendedInterfaces = new ArrayList<>();
        if (currentToken.getType() == TokenType.EXTENDS) {
            advance();
            extendedInterfaces.add(currentToken.getValue());
            expect(TokenType.IDENTIFIER);
            while (match(TokenType.COMMA)) {
                extendedInterfaces.add(currentToken.getValue());
                expect(TokenType.IDENTIFIER);
            }
        }

        expect(TokenType.LBRACE);
        List<InterfaceMemberNode> members = parseInterfaceBody();
        expect(TokenType.RBRACE);
        return new InterfaceDeclNode(accessModifier, name, extendedInterfaces, members);
    }

    private List<InterfaceMemberNode> parseInterfaceBody() throws Exception {
        List<InterfaceMemberNode> members = new ArrayList<>();
        while (currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            String type = parseTypeSpec();
            String name = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
            expect(TokenType.LPAREN);
            List<ParamNode> params = parseParamList();
            expect(TokenType.RPAREN);
            expect(TokenType.SEMICOLON);
            members.add(new InterfaceMethodMemberNode(type, name, params));
        }
        return members;
    }

    private ObjectDeclNode parseObjectDecl() throws Exception {
        expect(TokenType.NEW);
        String className = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.LPAREN);
        List<ExpressionNode> args = parseArgList();
        expect(TokenType.RPAREN);

        String target = null;
        if (match(TokenType.ASSIGN)) {
            target = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
        }
        return new ObjectDeclNode(className, args, target);
    }

    private List<ExpressionNode> parseArgList() throws Exception {
        List<ExpressionNode> args = new ArrayList<>();
        if (currentToken.getType() == TokenType.RPAREN) {
            return args;
        }
        args.add(parseExpression());
        while (match(TokenType.COMMA)) {
            args.add(parseExpression());
        }
        return args;
    }

    // ========== CONTROL FLOW ==========

    private IfStmtNode parseIfStmt() throws Exception {
        expect(TokenType.IF);
        expect(TokenType.LPAREN);
        ExpressionNode condition = parseExpression();
        expect(TokenType.RPAREN);
        BlockNode thenBlock = parseBlock();
        BlockNode elseBlock = null;
        if (currentToken.getType() == TokenType.ELSE) {
            advance();
            elseBlock = parseBlock();
        }
        return new IfStmtNode(condition, thenBlock, elseBlock);
    }

    private WhileStmtNode parseWhileStmt() throws Exception {
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        ExpressionNode condition = parseExpression();
        expect(TokenType.RPAREN);
        BlockNode body = parseBlock();
        return new WhileStmtNode(condition, body);
    }

    private ForStmtNode parseForStmt() throws Exception {
        expect(TokenType.FOR);
        expect(TokenType.LPAREN);

        StatementNode init = null;
        if (currentToken.getType() != TokenType.SEMICOLON) {
            if (startsDeclaration() || isStorageSpecifier()) {
                init = parseVarOrFuncDecl();
            } else {
                ExpressionNode expr = parseExpression();
                init = new ExpressionStatementNode(expr);
                expect(TokenType.SEMICOLON);
            }
        } else {
            advance();
        }

        ExpressionNode condition = null;
        if (currentToken.getType() != TokenType.SEMICOLON) {
            condition = parseExpression();
        }
        expect(TokenType.SEMICOLON);

        ExpressionNode update = null;
        if (currentToken.getType() != TokenType.RPAREN) {
            update = parseExpression();
        }
        expect(TokenType.RPAREN);

        BlockNode body = parseBlock();
        return new ForStmtNode(init, condition, update, body);
    }

    private DoWhileStmtNode parseDoWhileStmt() throws Exception {
        expect(TokenType.DO);
        BlockNode body = parseBlock();
        expect(TokenType.WHILE);
        expect(TokenType.LPAREN);
        ExpressionNode condition = parseExpression();
        expect(TokenType.RPAREN);
        expect(TokenType.SEMICOLON);
        return new DoWhileStmtNode(body, condition);
    }

    private SwitchStmtNode parseSwitchStmt() throws Exception {
        expect(TokenType.SWITCH);
        expect(TokenType.LPAREN);
        ExpressionNode expr = parseExpression();
        expect(TokenType.RPAREN);
        expect(TokenType.LBRACE);

        List<CaseClauseNode> cases = new ArrayList<>();
        DefaultClauseNode defaultClause = null;

        while (currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            if (currentToken.getType() == TokenType.CASE) {
                cases.add(parseCaseClause());
            } else if (currentToken.getType() == TokenType.DEFAULT) {
                defaultClause = parseDefaultClause();
            } else {
                advance();
            }
        }
        expect(TokenType.RBRACE);
        return new SwitchStmtNode(expr, cases, defaultClause);
    }

    private CaseClauseNode parseCaseClause() throws Exception {
        expect(TokenType.CASE);
        ExpressionNode expr = parseExpression();
        expect(TokenType.COLON);
        List<StatementNode> statements = new ArrayList<>();
        while (currentToken.getType() != TokenType.CASE &&
                currentToken.getType() != TokenType.DEFAULT &&
                currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            statements.add(parseStatement());
        }
        return new CaseClauseNode(expr, statements);
    }

    private DefaultClauseNode parseDefaultClause() throws Exception {
        expect(TokenType.DEFAULT);
        expect(TokenType.COLON);
        List<StatementNode> statements = new ArrayList<>();
        while (currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            statements.add(parseStatement());
        }
        return new DefaultClauseNode(statements);
    }

    // ========== JUMP STATEMENTS ==========

    private BreakStmtNode parseBreakStmt() throws Exception {
        expect(TokenType.BREAK);
        expect(TokenType.SEMICOLON);
        return new BreakStmtNode();
    }

    private ContinueStmtNode parseContinueStmt() throws Exception {
        expect(TokenType.CONTINUE);
        expect(TokenType.SEMICOLON);
        return new ContinueStmtNode();
    }

    private GotoStmtNode parseGotoStmt() throws Exception {
        expect(TokenType.GOTO);
        String label = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.SEMICOLON);
        return new GotoStmtNode(label);
    }

    private ReturnStmtNode parseReturnStmt() throws Exception {
        expect(TokenType.RETURN);
        ExpressionNode expr = null;
        if (currentToken.getType() != TokenType.SEMICOLON) {
            expr = parseExpression();
        }
        expect(TokenType.SEMICOLON);
        return new ReturnStmtNode(expr);
    }

    private PrintStmtNode parsePrintStmt() throws Exception {
        expect(TokenType.PRINT);
        expect(TokenType.LPAREN);
        ExpressionNode value = parseExpression();
        expect(TokenType.RPAREN);
        expect(TokenType.SEMICOLON);
        return new PrintStmtNode(value);
    }

    private InputStmtNode parseInputStmt() throws Exception {
        expect(TokenType.INPUT);
        expect(TokenType.LPAREN);
        String target = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.RPAREN);
        expect(TokenType.SEMICOLON);
        return new InputStmtNode(target, "unknown");
    }

    // ========== EXCEPTION HANDLING ==========

    private TryStmtNode parseTryStmt() throws Exception {
        expect(TokenType.TRY);
        BlockNode tryBlock = parseBlock();

        List<CatchClauseNode> catches = new ArrayList<>();
        while (currentToken.getType() == TokenType.CATCH) {
            catches.add(parseCatchClause());
        }

        FinallyClauseNode finallyClause = null;
        if (currentToken.getType() == TokenType.FINALLY) {
            advance();
            finallyClause = new FinallyClauseNode(parseBlock());
        }
        return new TryStmtNode(tryBlock, catches, finallyClause);
    }

    private CatchClauseNode parseCatchClause() throws Exception {
        expect(TokenType.CATCH);
        expect(TokenType.LPAREN);
        String type = parseTypeSpec();
        String name = currentToken.getValue();
        expect(TokenType.IDENTIFIER);
        expect(TokenType.RPAREN);
        BlockNode block = parseBlock();
        return new CatchClauseNode(type, name, block);
    }

    private ThrowStmtNode parseThrowStmt() throws Exception {
        expect(TokenType.THROW);
        ExpressionNode expr = parseExpression();
        expect(TokenType.SEMICOLON);
        return new ThrowStmtNode(expr);
    }

    private SynchronizedStmtNode parseSynchronizedStmt() throws Exception {
        expect(TokenType.SYNCHRONIZED);
        expect(TokenType.LPAREN);
        ExpressionNode expr = parseExpression();
        expect(TokenType.RPAREN);
        BlockNode block = parseBlock();
        return new SynchronizedStmtNode(expr, block);
    }

    // ========== BLOCK ==========

    private BlockNode parseBlock() throws Exception {
        expect(TokenType.LBRACE);
        symbolTable.startScope("block_" + position);

        List<StatementNode> statements = new ArrayList<>();
        while (currentToken.getType() != TokenType.RBRACE &&
                currentToken.getType() != TokenType.EOF) {
            try {
                statements.add(parseStatement());
            } catch (Exception e) {
                if (!errors.contains(e.getMessage())) {
                    errors.add(e.getMessage());
                }
                hasErrors = true;
                synchronize();
            }
        }
        expect(TokenType.RBRACE);
        symbolTable.endScope();
        return new BlockNode(statements);
    }

    // ========== EXPRESSIONS ==========

    private ExpressionNode parseExpression() throws Exception {
        return parseAssignmentExpr();
    }

    private ExpressionNode parseAssignmentExpr() throws Exception {
        ExpressionNode left = parseConditionalExpr();
        if (isAssignmentOp()) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseAssignmentExpr();
            return mark(new AssignmentNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseConditionalExpr() throws Exception {
        ExpressionNode expr = parseLogicalOrExpr();
        if (match(TokenType.QUESTION)) {
            ExpressionNode thenExpr = parseExpression();
            expect(TokenType.COLON);
            ExpressionNode elseExpr = parseConditionalExpr();
            return mark(new ConditionalNode(expr, thenExpr, elseExpr), expr.getLine(), expr.getColumn());
        }
        return expr;
    }

    private ExpressionNode parseLogicalOrExpr() throws Exception {
        ExpressionNode left = parseLogicalAndExpr();
        while (currentToken.getType() == TokenType.LOGICAL_OR) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseLogicalAndExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseLogicalAndExpr() throws Exception {
        ExpressionNode left = parseEqualityExpr();
        while (currentToken.getType() == TokenType.LOGICAL_AND) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseEqualityExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseEqualityExpr() throws Exception {
        ExpressionNode left = parseRelationalExpr();
        while (currentToken.getType() == TokenType.EQUAL ||
                currentToken.getType() == TokenType.NOT_EQUAL) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseRelationalExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    /**
     * ✅ FIXED: parseRelationalExpr() method
     */
    private ExpressionNode parseRelationalExpr() throws Exception {
        ExpressionNode left = parseAdditiveExpr();
        while (currentToken.getType() == TokenType.GREATER ||
                currentToken.getType() == TokenType.LESS ||
                currentToken.getType() == TokenType.GREATER_EQUAL ||
                currentToken.getType() == TokenType.LESS_EQUAL) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseAdditiveExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseAdditiveExpr() throws Exception {
        ExpressionNode left = parseMultiplicativeExpr();
        while (currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseMultiplicativeExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseMultiplicativeExpr() throws Exception {
        ExpressionNode left = parseUnaryExpr();
        while (currentToken.getType() == TokenType.MULTIPLY ||
                currentToken.getType() == TokenType.DIVIDE ||
                currentToken.getType() == TokenType.MODULO) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode right = parseUnaryExpr();
            left = mark(new BinaryOpNode(left, op, right), left.getLine(), left.getColumn());
        }
        return left;
    }

    private ExpressionNode parseUnaryExpr() throws Exception {
        if (currentToken.getType() == TokenType.INCREMENT ||
                currentToken.getType() == TokenType.DECREMENT ||
                currentToken.getType() == TokenType.LOGICAL_NOT ||
                currentToken.getType() == TokenType.BITWISE_NOT ||
                currentToken.getType() == TokenType.PLUS ||
                currentToken.getType() == TokenType.MINUS) {
            String op = currentToken.getValue();
            advance();
            ExpressionNode operand = parseUnaryExpr();
            return mark(new UnaryNode(op, operand), operand.getLine(), operand.getColumn());
        }
        return parsePrimaryExpr();
    }

    private ExpressionNode parsePrimaryExpr() throws Exception {
        TokenType type = currentToken.getType();

        if (type == TokenType.NUMBER) {
            int line = currentToken.getLine();
            int column = currentToken.getColumn();
            int value = Integer.parseInt(currentToken.getValue());
            advance();
            return mark(new NumberNode(value), line, column);
        }

        if (type == TokenType.STRING) {
            int line = currentToken.getLine();
            int column = currentToken.getColumn();
            String value = currentToken.getValue();
            advance();
            return mark(new StringNode(value), line, column);
        }

        if (type == TokenType.TRUE || type == TokenType.FALSE || type == TokenType.NULL) {
            int line = currentToken.getLine();
            int column = currentToken.getColumn();
            String value = currentToken.getValue();
            advance();
            return mark(new IdentifierNode(value), line, column);
        }

        if (type == TokenType.IDENTIFIER) {
            int line = currentToken.getLine();
            int column = currentToken.getColumn();
            String name = currentToken.getValue();
            advance();
            if (match(TokenType.DOT)) {
                String method = currentToken.getValue();
                expect(TokenType.IDENTIFIER);
                expect(TokenType.LPAREN);
                List<ExpressionNode> args = parseArgList();
                expect(TokenType.RPAREN);
                return mark(new MethodCallNode(name, method, args), line, column);
            }
            return mark(new IdentifierNode(name), line, column);
        }

        if (type == TokenType.LPAREN) {
            advance();
            ExpressionNode expr = parseExpression();
            expect(TokenType.RPAREN);
            return expr;
        }

        if (type == TokenType.NEW) {
            // Parse object creation as expression
            expect(TokenType.NEW);
            String className = currentToken.getValue();
            expect(TokenType.IDENTIFIER);
            expect(TokenType.LPAREN);
            List<ExpressionNode> args = parseArgList();
            expect(TokenType.RPAREN);
            return mark(new ObjectCreationExpressionNode(className, args), currentToken.getLine(), currentToken.getColumn());
        }

        throw new Exception("Syntax Error at line " + currentToken.getLine() +
                ", column " + currentToken.getColumn() +
                ": unexpected " + describeToken(currentToken));
    }
}
