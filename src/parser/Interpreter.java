package parser;

import lexer.TokenType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private static final int MAX_LOOP_ITERATIONS = 1000;

    private final SymbolTable symbolTable;
    private final Map<String, ClassDeclNode> classes;
    private final Map<String, Object> localValues;
    private ObjectValue currentObject;

    public Interpreter(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.classes = new HashMap<>();
        this.localValues = new HashMap<>();
        this.currentObject = null;
    }

    public void interpret(ASTNode root) {
        evaluate(root);
    }

    public Object evaluate(ASTNode node) {
        if (node == null) {
            return null;
        }

        switch (node.getClass().getSimpleName()) {
            case "ProgramNode":
                Object lastValue = null;
                for (StatementNode statement : ((ProgramNode) node).statements) {
                    lastValue = evaluate(statement);
                }
                return lastValue;

            case "BlockNode":
                Object blockValue = null;
                for (StatementNode statement : ((BlockNode) node).statements) {
                    blockValue = evaluate(statement);
                }
                return blockValue;

            case "VarDeclNode":
                return evaluateVarDeclaration((VarDeclNode) node);

            case "ExpressionStatementNode":
                return evaluate(((ExpressionStatementNode) node).expression);

            case "PrintStmtNode":
                Object printValue = evaluate(((PrintStmtNode) node).value);
                System.out.println(formatValue(printValue));
                return printValue;

            case "IfStmtNode":
                IfStmtNode ifStmt = (IfStmtNode) node;
                if (isTruthy(evaluate(ifStmt.condition))) {
                    return evaluate(ifStmt.thenBlock);
                }
                return evaluate(ifStmt.elseBlock);

            case "WhileStmtNode":
                return evaluateWhile((WhileStmtNode) node);

            case "ForStmtNode":
                return evaluateFor((ForStmtNode) node);

            case "DoWhileStmtNode":
                return evaluateDoWhile((DoWhileStmtNode) node);

            case "ReturnStmtNode":
                ReturnStmtNode returnStmt = (ReturnStmtNode) node;
                return returnStmt.value == null ? null : evaluate(returnStmt.value);

            case "BreakStmtNode":
            case "ContinueStmtNode":
                throw runtimeError(node, "break/continue is not implemented yet.");

            case "PackageDeclNode":
            case "ImportDeclNode":
            case "FuncDeclNode":
            case "InterfaceDeclNode":
            case "MethodDeclNode":
            case "ConstructorDeclNode":
                return null;

            case "ClassDeclNode":
                ClassDeclNode classDecl = (ClassDeclNode) node;
                classes.put(classDecl.name, classDecl);
                return null;

            case "NumberNode":
                return ((NumberNode) node).value;

            case "StringNode":
                return ((StringNode) node).value;

            case "IdentifierNode":
                return evaluateIdentifier((IdentifierNode) node);

            case "AssignmentNode":
                return evaluateAssignment((AssignmentNode) node);

            case "BinaryOpNode":
                return evaluateBinary((BinaryOpNode) node);

            case "UnaryNode":
                return evaluateUnary((UnaryNode) node);

            case "ConditionalNode":
                ConditionalNode conditional = (ConditionalNode) node;
                return isTruthy(evaluate(conditional.condition))
                        ? evaluate(conditional.thenExpr)
                        : evaluate(conditional.elseExpr);

            case "ObjectCreationExpressionNode":
                return createObject((ObjectCreationExpressionNode) node);

            case "MethodCallNode":
                return callMethod((MethodCallNode) node);

            default:
                throw runtimeError(node, "Unsupported AST node: " + node.getClass().getSimpleName());
        }
    }

    private Object evaluateVarDeclaration(VarDeclNode node) {
        Object value = node.initializer == null ? defaultValueForType(node.type) : evaluate(node.initializer);

        if (symbolTable.lookup(node.name) == null) {
            symbolTable.addSymbol(node.name, node.type);
        }

        if (!symbolTable.setSymbolValue(node.name, value)) {
            throw runtimeError(node, "Could not assign variable '" + node.name + "'.");
        }
        return value;
    }

    private Object evaluateWhile(WhileStmtNode node) {
        Object lastValue = null;
        int iterations = 0;
        while (isTruthy(evaluate(node.condition))) {
            guardLoop(node, ++iterations);
            lastValue = evaluate(node.body);
        }
        return lastValue;
    }

    private Object evaluateFor(ForStmtNode node) {
        if (node.init != null) {
            evaluate(node.init);
        }

        Object lastValue = null;
        int iterations = 0;
        while (node.condition == null || isTruthy(evaluate(node.condition))) {
            guardLoop(node, ++iterations);
            lastValue = evaluate(node.body);
            if (node.update != null) {
                evaluate(node.update);
            }
        }
        return lastValue;
    }

    private Object evaluateDoWhile(DoWhileStmtNode node) {
        Object lastValue;
        int iterations = 0;
        do {
            guardLoop(node, ++iterations);
            lastValue = evaluate(node.body);
        } while (isTruthy(evaluate(node.condition)));
        return lastValue;
    }

    private Object evaluateIdentifier(IdentifierNode node) {
        if (node.name.equals(TokenType.TRUE.getSymbol())) {
            return true;
        }
        if (node.name.equals(TokenType.FALSE.getSymbol())) {
            return false;
        }
        if (node.name.equals(TokenType.NULL.getSymbol())) {
            return null;
        }

        if (localValues.containsKey(node.name)) {
            return localValues.get(node.name);
        }
        if (currentObject != null && currentObject.fields.containsKey(node.name)) {
            return currentObject.fields.get(node.name);
        }

        SymbolTable.Symbol symbol = symbolTable.lookup(node.name);
        if (symbol == null) {
            throw new RuntimeException("Undefined variable: " + node.name);
        }
        if (!symbol.isInitialized()) {
            throw runtimeError(node, "Variable '" + node.name + "' was declared but not initialized.");
        }
        return symbol.getValue();
    }

    private Object evaluateAssignment(AssignmentNode node) {
        if (!(node.left instanceof IdentifierNode)) {
            throw runtimeError(node, "Left side of assignment must be a variable name.");
        }

        String name = ((IdentifierNode) node.left).name;
        SymbolTable.Symbol symbol = symbolTable.lookup(name);
        boolean objectField = currentObject != null && currentObject.fields.containsKey(name);
        if (symbol == null && !objectField) {
            throw new RuntimeException("Undefined variable: " + name);
        }

        Object rightValue = evaluate(node.right);
        Object result;
        Object currentValue = objectField ? currentObject.fields.get(name) : symbol.getValue();

        if (node.operator.equals(TokenType.ASSIGN.getSymbol())) {
            result = rightValue;
        } else if (node.operator.equals(TokenType.PLUS_ASSIGN.getSymbol())) {
            result = add(currentValue, rightValue, node);
        } else if (node.operator.equals(TokenType.MINUS_ASSIGN.getSymbol())) {
            result = asNumber(currentValue, node) - asNumber(rightValue, node);
        } else if (node.operator.equals(TokenType.MULTIPLY_ASSIGN.getSymbol())) {
            result = asNumber(currentValue, node) * asNumber(rightValue, node);
        } else if (node.operator.equals(TokenType.DIVIDE_ASSIGN.getSymbol())) {
            result = divide(currentValue, rightValue, node);
        } else {
            throw runtimeError(node, "Unsupported assignment operator '" + node.operator + "'.");
        }

        if (objectField) {
            currentObject.fields.put(name, result);
        } else if (symbol != null) {
            symbolTable.setSymbolValue(name, result);
        } else {
            throw new RuntimeException("Undefined variable: " + name);
        }
        return result;
    }

    private Object createObject(ObjectCreationExpressionNode node) {
        ClassDeclNode classDecl = classes.get(node.className);
        if (classDecl == null) {
            throw runtimeError(node, "Undefined class '" + node.className + "'.");
        }

        ObjectValue object = new ObjectValue(node.className, classDecl);
        for (ClassMemberNode member : classDecl.members) {
            if (member instanceof FieldMemberNode) {
                FieldMemberNode field = (FieldMemberNode) member;
                object.fields.put(field.name, field.initializer == null
                        ? defaultValueForType(field.type)
                        : evaluateWithObject(object, Map.of(), field.initializer));
            }
        }

        ConstructorMemberNode constructor = findConstructor(classDecl);
        if (constructor != null) {
            Map<String, Object> arguments = bindArguments(constructor.params, node.arguments, node);
            evaluateWithObject(object, arguments, constructor.body);
        }
        return object;
    }

    private Object callMethod(MethodCallNode node) {
        Object target = evaluate(new IdentifierNode(node.object));
        if (!(target instanceof ObjectValue)) {
            throw runtimeError(node, "'" + node.object + "' is not an object.");
        }

        ObjectValue object = (ObjectValue) target;
        MethodMemberNode method = findMethod(object.classDecl, node.method);
        if (method == null) {
            throw runtimeError(node, "Undefined method '" + node.method + "' on class '" + object.className + "'.");
        }

        Map<String, Object> arguments = bindArguments(method.params, node.arguments, node);
        return evaluateWithObject(object, arguments, method.body);
    }

    private Object evaluateWithObject(ObjectValue object, Map<String, Object> values, ASTNode body) {
        ObjectValue previousObject = currentObject;
        Map<String, Object> previousValues = new HashMap<>(localValues);
        currentObject = object;
        localValues.clear();
        localValues.putAll(values);
        try {
            return evaluate(body);
        } finally {
            currentObject = previousObject;
            localValues.clear();
            localValues.putAll(previousValues);
        }
    }

    private Map<String, Object> bindArguments(List<ParamNode> params, List<ExpressionNode> args, ASTNode node) {
        if (params.size() != args.size()) {
            throw runtimeError(node, "Expected " + params.size() + " arguments but got " + args.size() + ".");
        }

        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < params.size(); i++) {
            values.put(params.get(i).getName(), evaluate(args.get(i)));
        }
        return values;
    }

    private ConstructorMemberNode findConstructor(ClassDeclNode classDecl) {
        for (ClassMemberNode member : classDecl.members) {
            if (member instanceof ConstructorMemberNode) {
                return (ConstructorMemberNode) member;
            }
        }
        return null;
    }

    private MethodMemberNode findMethod(ClassDeclNode classDecl, String name) {
        for (ClassMemberNode member : classDecl.members) {
            if (member instanceof MethodMemberNode && ((MethodMemberNode) member).name.equals(name)) {
                return (MethodMemberNode) member;
            }
        }
        return null;
    }

    private Object evaluateBinary(BinaryOpNode node) {
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);
        String op = node.operator;

        if (op.equals(TokenType.PLUS.getSymbol())) {
            return add(left, right, node);
        }
        if (op.equals(TokenType.MINUS.getSymbol())) {
            return asNumber(left, node) - asNumber(right, node);
        }
        if (op.equals(TokenType.MULTIPLY.getSymbol())) {
            return asNumber(left, node) * asNumber(right, node);
        }
        if (op.equals(TokenType.DIVIDE.getSymbol())) {
            return divide(left, right, node);
        }
        if (op.equals(TokenType.MODULO.getSymbol())) {
            int divisor = asNumber(right, node);
            if (divisor == 0) {
                throw runtimeError(node, "Modulo by zero.");
            }
            return asNumber(left, node) % divisor;
        }
        if (op.equals(TokenType.GREATER.getSymbol()) || op.equals(">")) {
            return asNumber(left, node) > asNumber(right, node);
        }
        if (op.equals(TokenType.LESS.getSymbol()) || op.equals("<")) {
            return asNumber(left, node) < asNumber(right, node);
        }
        if (op.equals(TokenType.GREATER_EQUAL.getSymbol())) {
            return asNumber(left, node) >= asNumber(right, node);
        }
        if (op.equals(TokenType.LESS_EQUAL.getSymbol())) {
            return asNumber(left, node) <= asNumber(right, node);
        }
        if (op.equals(TokenType.EQUAL.getSymbol())) {
            return valuesEqual(left, right);
        }
        if (op.equals(TokenType.NOT_EQUAL.getSymbol())) {
            return !valuesEqual(left, right);
        }
        if (op.equals(TokenType.LOGICAL_AND.getSymbol())) {
            return isTruthy(left) && isTruthy(right);
        }
        if (op.equals(TokenType.LOGICAL_OR.getSymbol())) {
            return isTruthy(left) || isTruthy(right);
        }

        throw runtimeError(node, "Unsupported binary operator '" + op + "'.");
    }

    private Object evaluateUnary(UnaryNode node) {
        Object value = evaluate(node.operand);
        String op = node.operator;

        if (op.equals(TokenType.MINUS.getSymbol())) {
            return -asNumber(value, node);
        }
        if (op.equals(TokenType.PLUS.getSymbol())) {
            return asNumber(value, node);
        }
        if (op.equals(TokenType.LOGICAL_NOT.getSymbol())) {
            return !isTruthy(value);
        }

        throw runtimeError(node, "Unsupported unary operator '" + op + "'.");
    }

    public void printMemoryState() {
        symbolTable.printSymbolTable();
    }

    private void guardLoop(ASTNode node, int iterations) {
        if (iterations > MAX_LOOP_ITERATIONS) {
            throw runtimeError(node, "Loop exceeded " + MAX_LOOP_ITERATIONS +
                    " iterations. Check the loop condition/update.");
        }
    }

    private Object add(Object left, Object right, ASTNode node) {
        if (left instanceof String || right instanceof String) {
            return formatValue(left) + formatValue(right);
        }
        return asNumber(left, node) + asNumber(right, node);
    }

    private Object divide(Object left, Object right, ASTNode node) {
        int divisor = asNumber(right, node);
        if (divisor == 0) {
            throw runtimeError(node, "Division by zero.");
        }
        return asNumber(left, node) / divisor;
    }

    private int asNumber(Object value, ASTNode node) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        throw runtimeError(node, "Expected a number but found '" + formatValue(value) + "'.");
    }

    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Integer) {
            return (Integer) value != 0;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        return true;
    }

    private boolean valuesEqual(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private Object defaultValueForType(String type) {
        return 0;
    }

    private String formatValue(Object value) {
        return value == null ? "null" : value.toString();
    }

    private RuntimeException runtimeError(ASTNode node, String message) {
        String location = node == null ? "unknown location" : node.location();
        return new RuntimeException("Runtime Error at " + location + ": " + message);
    }

    private static class ObjectValue {
        private final String className;
        private final ClassDeclNode classDecl;
        private final Map<String, Object> fields;

        private ObjectValue(String className, ClassDeclNode classDecl) {
            this.className = className;
            this.classDecl = classDecl;
            this.fields = new HashMap<>();
        }

        @Override
        public String toString() {
            return className + fields;
        }
    }
}
