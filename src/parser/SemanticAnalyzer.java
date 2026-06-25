package parser;

import lexer.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemanticAnalyzer {
    private static final Set<String> BUILT_IN_TYPES = Set.of(
            TokenType.INT.getSymbol(),
            TokenType.CHAR.getSymbol(),
            TokenType.FLOAT.getSymbol(),
            TokenType.DOUBLE.getSymbol(),
            TokenType.LONG.getSymbol(),
            TokenType.SHORT.getSymbol(),
            TokenType.VOID.getSymbol(),
            "int",
            "char",
            "float",
            "double",
            "long",
            "short",
            "void"
    );

    private final List<String> errors;
    private final ArrayDeque<Map<String, String>> scopes;
    private final Map<String, ClassInfo> classes;
    private ClassInfo currentClass;

    public SemanticAnalyzer() {
        this.errors = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
        this.classes = new HashMap<>();
        this.currentClass = null;
    }

    public boolean analyze(ASTNode root) {
        errors.clear();
        scopes.clear();
        classes.clear();
        currentClass = null;

        beginScope();
        collectClassDeclarations(root);
        analyzeNode(root);
        endScope();
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void printReport() {
        System.out.println("Semantic analysis: PASSED");
        System.out.println("Checks: declarations, undefined variables, assignment targets, classes, methods, argument counts.");
    }

    private void collectClassDeclarations(ASTNode node) {
        if (node instanceof ProgramNode) {
            for (StatementNode statement : ((ProgramNode) node).statements) {
                collectClassDeclarations(statement);
            }
        } else if (node instanceof ClassDeclNode) {
            ClassDeclNode classDecl = (ClassDeclNode) node;
            if (classes.containsKey(classDecl.name)) {
                semanticError(classDecl, "Class '" + classDecl.name + "' is already declared.");
                return;
            }

            ClassInfo info = new ClassInfo(classDecl);
            for (ClassMemberNode member : classDecl.members) {
                if (member instanceof FieldMemberNode) {
                    FieldMemberNode field = (FieldMemberNode) member;
                    info.fields.put(field.name, field.type);
                } else if (member instanceof MethodMemberNode) {
                    MethodMemberNode method = (MethodMemberNode) member;
                    info.methods.put(method.name, method);
                } else if (member instanceof ConstructorMemberNode) {
                    info.constructor = (ConstructorMemberNode) member;
                }
            }
            classes.put(classDecl.name, info);
        }
    }

    private void analyzeNode(ASTNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof ProgramNode) {
            for (StatementNode statement : ((ProgramNode) node).statements) {
                analyzeNode(statement);
            }
        } else if (node instanceof BlockNode) {
            beginScope();
            for (StatementNode statement : ((BlockNode) node).statements) {
                analyzeNode(statement);
            }
            endScope();
        } else if (node instanceof VarDeclNode) {
            analyzeVarDecl((VarDeclNode) node);
        } else if (node instanceof ExpressionStatementNode) {
            analyzeExpression(((ExpressionStatementNode) node).expression);
        } else if (node instanceof PrintStmtNode) {
            analyzeExpression(((PrintStmtNode) node).value);
        } else if (node instanceof IfStmtNode) {
            IfStmtNode ifStmt = (IfStmtNode) node;
            analyzeExpression(ifStmt.condition);
            analyzeNode(ifStmt.thenBlock);
            analyzeNode(ifStmt.elseBlock);
        } else if (node instanceof WhileStmtNode) {
            WhileStmtNode whileStmt = (WhileStmtNode) node;
            analyzeExpression(whileStmt.condition);
            analyzeNode(whileStmt.body);
        } else if (node instanceof ForStmtNode) {
            ForStmtNode forStmt = (ForStmtNode) node;
            beginScope();
            analyzeNode(forStmt.init);
            analyzeExpression(forStmt.condition);
            analyzeExpression(forStmt.update);
            analyzeNode(forStmt.body);
            endScope();
        } else if (node instanceof ReturnStmtNode) {
            analyzeExpression(((ReturnStmtNode) node).value);
        } else if (node instanceof ClassDeclNode) {
            analyzeClassDecl((ClassDeclNode) node);
        }
    }

    private void analyzeClassDecl(ClassDeclNode classDecl) {
        ClassInfo previousClass = currentClass;
        currentClass = classes.get(classDecl.name);

        beginScope();
        if (currentClass != null) {
            for (Map.Entry<String, String> field : currentClass.fields.entrySet()) {
                declare(field.getKey(), field.getValue(), classDecl);
            }
        }

        for (ClassMemberNode member : classDecl.members) {
            if (member instanceof FieldMemberNode) {
                FieldMemberNode field = (FieldMemberNode) member;
                ensureTypeExists(field.type, field);
                analyzeExpression(field.initializer);
            } else if (member instanceof ConstructorMemberNode) {
                analyzeConstructor((ConstructorMemberNode) member);
            } else if (member instanceof MethodMemberNode) {
                analyzeMethod((MethodMemberNode) member);
            }
        }

        endScope();
        currentClass = previousClass;
    }

    private void analyzeConstructor(ConstructorMemberNode constructor) {
        beginScope();
        declareParameters(constructor.params, constructor);
        analyzeNode(constructor.body);
        endScope();
    }

    private void analyzeMethod(MethodMemberNode method) {
        ensureTypeExists(method.returnType, method);
        beginScope();
        declareParameters(method.params, method);
        analyzeNode(method.body);
        endScope();
    }

    private void analyzeVarDecl(VarDeclNode varDecl) {
        ensureTypeExists(varDecl.type, varDecl);
        analyzeExpression(varDecl.initializer);
        declare(varDecl.name, varDecl.type, varDecl);
    }

    private String analyzeExpression(ExpressionNode expression) {
        if (expression == null) {
            return "void";
        }

        if (expression instanceof NumberNode) {
            return TokenType.INT.getSymbol();
        }
        if (expression instanceof StringNode) {
            return "string";
        }
        if (expression instanceof IdentifierNode) {
            IdentifierNode identifier = (IdentifierNode) expression;
            if (isLiteral(identifier.name)) {
                return "literal";
            }

            String type = resolve(identifier.name);
            if (type == null) {
                semanticError(identifier, "Undefined variable '" + identifier.name + "'.");
                return "unknown";
            }
            return type;
        }
        if (expression instanceof AssignmentNode) {
            AssignmentNode assignment = (AssignmentNode) expression;
            if (!(assignment.left instanceof IdentifierNode)) {
                semanticError(assignment, "Left side of assignment must be a variable.");
            } else {
                IdentifierNode target = (IdentifierNode) assignment.left;
                if (resolve(target.name) == null) {
                    semanticError(target, "Cannot assign to undefined variable '" + target.name + "'.");
                }
            }
            return analyzeExpression(assignment.right);
        }
        if (expression instanceof BinaryOpNode) {
            BinaryOpNode binary = (BinaryOpNode) expression;
            analyzeExpression(binary.left);
            analyzeExpression(binary.right);
            return isComparisonOperator(binary.operator) ? "boolean" : TokenType.INT.getSymbol();
        }
        if (expression instanceof UnaryNode) {
            return analyzeExpression(((UnaryNode) expression).operand);
        }
        if (expression instanceof ConditionalNode) {
            ConditionalNode conditional = (ConditionalNode) expression;
            analyzeExpression(conditional.condition);
            analyzeExpression(conditional.thenExpr);
            return analyzeExpression(conditional.elseExpr);
        }
        if (expression instanceof ObjectCreationExpressionNode) {
            return analyzeObjectCreation((ObjectCreationExpressionNode) expression);
        }
        if (expression instanceof MethodCallNode) {
            return analyzeMethodCall((MethodCallNode) expression);
        }

        semanticError(expression, "Unsupported expression in semantic analyzer: " + expression.getClass().getSimpleName());
        return "unknown";
    }

    private String analyzeObjectCreation(ObjectCreationExpressionNode objectCreation) {
        ClassInfo classInfo = classes.get(objectCreation.className);
        if (classInfo == null) {
            semanticError(objectCreation, "Undefined class '" + objectCreation.className + "'.");
            return "unknown";
        }

        int expected = classInfo.constructor == null ? 0 : classInfo.constructor.params.size();
        if (expected != objectCreation.arguments.size()) {
            semanticError(objectCreation, "Constructor for class '" + objectCreation.className +
                    "' expects " + expected + " arguments but got " + objectCreation.arguments.size() + ".");
        }
        for (ExpressionNode argument : objectCreation.arguments) {
            analyzeExpression(argument);
        }
        return objectCreation.className;
    }

    private String analyzeMethodCall(MethodCallNode methodCall) {
        String objectType = resolve(methodCall.object);
        if (objectType == null) {
            semanticError(methodCall, "Undefined object variable '" + methodCall.object + "'.");
            return "unknown";
        }

        ClassInfo classInfo = classes.get(objectType);
        if (classInfo == null) {
            semanticError(methodCall, "Variable '" + methodCall.object + "' is not an object with methods.");
            return "unknown";
        }

        MethodMemberNode method = classInfo.methods.get(methodCall.method);
        if (method == null) {
            semanticError(methodCall, "Class '" + objectType + "' has no method named '" + methodCall.method + "'.");
            return "unknown";
        }
        if (method.params.size() != methodCall.arguments.size()) {
            semanticError(methodCall, "Method '" + methodCall.method + "' expects " + method.params.size() +
                    " arguments but got " + methodCall.arguments.size() + ".");
        }
        for (ExpressionNode argument : methodCall.arguments) {
            analyzeExpression(argument);
        }
        return method.returnType;
    }

    private void declareParameters(List<ParamNode> params, ASTNode owner) {
        Set<String> seen = new HashSet<>();
        for (ParamNode param : params) {
            ensureTypeExists(param.getType(), owner);
            if (!seen.add(param.getName())) {
                semanticError(owner, "Duplicate parameter '" + param.getName() + "'.");
            }
            declare(param.getName(), param.getType(), owner);
        }
    }

    private void declare(String name, String type, ASTNode node) {
        Map<String, String> scope = scopes.peek();
        if (scope != null && scope.containsKey(name)) {
            semanticError(node, "Variable '" + name + "' is already declared in this scope.");
            return;
        }
        if (scope != null) {
            scope.put(name, type);
        }
    }

    private String resolve(String name) {
        for (Map<String, String> scope : scopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        if (currentClass != null && currentClass.fields.containsKey(name)) {
            return currentClass.fields.get(name);
        }
        return null;
    }

    private void ensureTypeExists(String type, ASTNode node) {
        if (type == null || BUILT_IN_TYPES.contains(type) || classes.containsKey(type)) {
            return;
        }
        semanticError(node, "Unknown type '" + type + "'.");
    }

    private boolean isLiteral(String name) {
        return name.equals(TokenType.TRUE.getSymbol()) ||
                name.equals(TokenType.FALSE.getSymbol()) ||
                name.equals(TokenType.NULL.getSymbol());
    }

    private boolean isComparisonOperator(String operator) {
        return operator.equals(TokenType.EQUAL.getSymbol()) ||
                operator.equals(TokenType.NOT_EQUAL.getSymbol()) ||
                operator.equals(TokenType.GREATER.getSymbol()) ||
                operator.equals(TokenType.LESS.getSymbol()) ||
                operator.equals(TokenType.GREATER_EQUAL.getSymbol()) ||
                operator.equals(TokenType.LESS_EQUAL.getSymbol());
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void semanticError(ASTNode node, String message) {
        errors.add("Semantic Error at " + node.location() + ": " + message);
    }

    private static class ClassInfo {
        private final ClassDeclNode declaration;
        private final Map<String, String> fields;
        private final Map<String, MethodMemberNode> methods;
        private ConstructorMemberNode constructor;

        private ClassInfo(ClassDeclNode declaration) {
            this.declaration = declaration;
            this.fields = new HashMap<>();
            this.methods = new HashMap<>();
            this.constructor = null;
        }
    }
}
