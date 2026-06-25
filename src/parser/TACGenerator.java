package parser;

import lexer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TACGenerator {
    private final List<String> code;
    private int tempCounter;
    private int labelCounter;

    public TACGenerator() {
        this.code = new ArrayList<>();
        this.tempCounter = 0;
        this.labelCounter = 0;
    }

    public List<String> generate(ASTNode root) {
        code.clear();
        tempCounter = 0;
        labelCounter = 0;
        emitNode(root);
        return new ArrayList<>(code);
    }

    public void print(List<String> instructions) {
        if (instructions.isEmpty()) {
            System.out.println("(no TAC generated)");
            return;
        }

        System.out.printf("%-5s %s%n", "#", "Instruction");
        System.out.println("----------------------------------------");
        for (int i = 0; i < instructions.size(); i++) {
            System.out.printf("%-5d %s%n", i + 1, instructions.get(i));
        }
    }

    private void emitNode(ASTNode node) {
        if (node == null) {
            return;
        }

        if (node instanceof ProgramNode) {
            for (StatementNode statement : ((ProgramNode) node).statements) {
                emitNode(statement);
            }
        } else if (node instanceof BlockNode) {
            for (StatementNode statement : ((BlockNode) node).statements) {
                emitNode(statement);
            }
        } else if (node instanceof VarDeclNode) {
            VarDeclNode varDecl = (VarDeclNode) node;
            if (varDecl.initializer != null) {
                code.add(varDecl.name + " = " + emitExpression(varDecl.initializer));
            } else {
                code.add(varDecl.name + " = 0");
            }
        } else if (node instanceof ExpressionStatementNode) {
            emitExpression(((ExpressionStatementNode) node).expression);
        } else if (node instanceof PrintStmtNode) {
            code.add("print " + emitExpression(((PrintStmtNode) node).value));
        } else if (node instanceof IfStmtNode) {
            emitIf((IfStmtNode) node);
        } else if (node instanceof WhileStmtNode) {
            emitWhile((WhileStmtNode) node);
        } else if (node instanceof ForStmtNode) {
            emitFor((ForStmtNode) node);
        } else if (node instanceof ReturnStmtNode) {
            ReturnStmtNode returnStmt = (ReturnStmtNode) node;
            code.add("return " + (returnStmt.value == null ? "" : emitExpression(returnStmt.value)));
        } else if (node instanceof ClassDeclNode) {
            ClassDeclNode classDecl = (ClassDeclNode) node;
            code.add("# class " + classDecl.name);
        }
    }

    private void emitIf(IfStmtNode ifStmt) {
        String elseLabel = newLabel();
        String endLabel = newLabel();
        String condition = emitExpression(ifStmt.condition);

        code.add("ifFalse " + condition + " goto " + elseLabel);
        emitNode(ifStmt.thenBlock);
        code.add("goto " + endLabel);
        code.add(elseLabel + ":");
        emitNode(ifStmt.elseBlock);
        code.add(endLabel + ":");
    }

    private void emitWhile(WhileStmtNode whileStmt) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        code.add(startLabel + ":");
        String condition = emitExpression(whileStmt.condition);
        code.add("ifFalse " + condition + " goto " + endLabel);
        emitNode(whileStmt.body);
        code.add("goto " + startLabel);
        code.add(endLabel + ":");
    }

    private void emitFor(ForStmtNode forStmt) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        emitNode(forStmt.init);
        code.add(startLabel + ":");
        if (forStmt.condition != null) {
            code.add("ifFalse " + emitExpression(forStmt.condition) + " goto " + endLabel);
        }
        emitNode(forStmt.body);
        if (forStmt.update != null) {
            emitExpression(forStmt.update);
        }
        code.add("goto " + startLabel);
        code.add(endLabel + ":");
    }

    private String emitExpression(ExpressionNode expression) {
        if (expression == null) {
            return "";
        }
        if (expression instanceof NumberNode) {
            return String.valueOf(((NumberNode) expression).value);
        }
        if (expression instanceof StringNode) {
            return "\"" + ((StringNode) expression).value + "\"";
        }
        if (expression instanceof IdentifierNode) {
            return ((IdentifierNode) expression).name;
        }
        if (expression instanceof BinaryOpNode) {
            BinaryOpNode binary = (BinaryOpNode) expression;
            String left = emitExpression(binary.left);
            String right = emitExpression(binary.right);
            String temp = newTemp();
            code.add(temp + " = " + left + " " + operatorName(binary.operator) + " " + right);
            return temp;
        }
        if (expression instanceof AssignmentNode) {
            AssignmentNode assignment = (AssignmentNode) expression;
            String target = emitExpression(assignment.left);
            String value = emitExpression(assignment.right);
            if ("𐤔".equals(assignment.operator)) {
                code.add(target + " = " + value);
            } else {
                String temp = newTemp();
                code.add(temp + " = " + target + " " + operatorName(assignment.operator) + " " + value);
                code.add(target + " = " + temp);
            }
            return target;
        }
        if (expression instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) expression;
            String operand = emitExpression(unary.operand);
            String temp = newTemp();
            code.add(temp + " = " + operatorName(unary.operator) + " " + operand);
            return temp;
        }
        if (expression instanceof ConditionalNode) {
            return emitConditional((ConditionalNode) expression);
        }
        if (expression instanceof ObjectCreationExpressionNode) {
            ObjectCreationExpressionNode objectCreation = (ObjectCreationExpressionNode) expression;
            String args = objectCreation.arguments.stream()
                    .map(this::emitExpression)
                    .collect(Collectors.joining(", "));
            String temp = newTemp();
            code.add(temp + " = new " + objectCreation.className + "(" + args + ")");
            return temp;
        }
        if (expression instanceof MethodCallNode) {
            MethodCallNode call = (MethodCallNode) expression;
            String args = call.arguments.stream()
                    .map(this::emitExpression)
                    .collect(Collectors.joining(", "));
            String temp = newTemp();
            code.add(temp + " = call " + call.object + "." + call.method + "(" + args + ")");
            return temp;
        }

        String temp = newTemp();
        code.add(temp + " = <unsupported " + expression.getClass().getSimpleName() + ">");
        return temp;
    }

    private String emitConditional(ConditionalNode conditional) {
        String result = newTemp();
        String elseLabel = newLabel();
        String endLabel = newLabel();

        code.add("ifFalse " + emitExpression(conditional.condition) + " goto " + elseLabel);
        code.add(result + " = " + emitExpression(conditional.thenExpr));
        code.add("goto " + endLabel);
        code.add(elseLabel + ":");
        code.add(result + " = " + emitExpression(conditional.elseExpr));
        code.add(endLabel + ":");
        return result;
    }

    private String newTemp() {
        tempCounter++;
        return "t" + tempCounter;
    }

    private String newLabel() {
        labelCounter++;
        return "L" + labelCounter;
    }

    private String operatorName(String operator) {
        if (operator.equals(TokenType.PLUS.getSymbol())) return "ADD";
        if (operator.equals(TokenType.MINUS.getSymbol())) return "SUB";
        if (operator.equals(TokenType.MULTIPLY.getSymbol())) return "MUL";
        if (operator.equals(TokenType.DIVIDE.getSymbol())) return "DIV";
        if (operator.equals(TokenType.MODULO.getSymbol())) return "MOD";
        if (operator.equals(TokenType.EQUAL.getSymbol())) return "EQ";
        if (operator.equals(TokenType.NOT_EQUAL.getSymbol())) return "NE";
        if (operator.equals(TokenType.GREATER.getSymbol())) return "GT";
        if (operator.equals(TokenType.LESS.getSymbol())) return "LT";
        if (operator.equals(TokenType.GREATER_EQUAL.getSymbol())) return "GE";
        if (operator.equals(TokenType.LESS_EQUAL.getSymbol())) return "LE";
        if (operator.equals(TokenType.LOGICAL_AND.getSymbol())) return "AND";
        if (operator.equals(TokenType.LOGICAL_OR.getSymbol())) return "OR";
        if (operator.equals(TokenType.LOGICAL_NOT.getSymbol())) return "NOT";
        return operator;
    }
}
