package parser;

import java.util.List;

public class ASTPrinter {
    public String print(ASTNode node) {
        StringBuilder builder = new StringBuilder();
        appendNode(builder, node, 0, null);
        return builder.toString();
    }

    private void appendNode(StringBuilder builder, ASTNode node, int indent, String label) {
        if (node == null) {
            appendLine(builder, indent, label, "null");
            return;
        }

        if (node instanceof ProgramNode) {
            appendLine(builder, indent, label, "Program");
            appendStatements(builder, ((ProgramNode) node).statements, indent + 1);
        } else if (node instanceof BlockNode) {
            appendLine(builder, indent, label, "Block");
            appendStatements(builder, ((BlockNode) node).statements, indent + 1);
        } else if (node instanceof VarDeclNode) {
            VarDeclNode varDecl = (VarDeclNode) node;
            appendLine(builder, indent, label, "VarDecl type=" + varDecl.type + " name=" + varDecl.name);
            appendNode(builder, varDecl.initializer, indent + 1, "initializer");
        } else if (node instanceof PrintStmtNode) {
            appendLine(builder, indent, label, "Print");
            appendNode(builder, ((PrintStmtNode) node).value, indent + 1, "value");
        } else if (node instanceof ExpressionStatementNode) {
            appendLine(builder, indent, label, "ExpressionStatement");
            appendNode(builder, ((ExpressionStatementNode) node).expression, indent + 1, "expression");
        } else if (node instanceof IfStmtNode) {
            IfStmtNode ifStmt = (IfStmtNode) node;
            appendLine(builder, indent, label, "If");
            appendNode(builder, ifStmt.condition, indent + 1, "condition");
            appendNode(builder, ifStmt.thenBlock, indent + 1, "then");
            appendNode(builder, ifStmt.elseBlock, indent + 1, "else");
        } else if (node instanceof WhileStmtNode) {
            WhileStmtNode whileStmt = (WhileStmtNode) node;
            appendLine(builder, indent, label, "While");
            appendNode(builder, whileStmt.condition, indent + 1, "condition");
            appendNode(builder, whileStmt.body, indent + 1, "body");
        } else if (node instanceof ForStmtNode) {
            ForStmtNode forStmt = (ForStmtNode) node;
            appendLine(builder, indent, label, "For");
            appendNode(builder, forStmt.init, indent + 1, "init");
            appendNode(builder, forStmt.condition, indent + 1, "condition");
            appendNode(builder, forStmt.update, indent + 1, "update");
            appendNode(builder, forStmt.body, indent + 1, "body");
        } else if (node instanceof ReturnStmtNode) {
            appendLine(builder, indent, label, "Return");
            appendNode(builder, ((ReturnStmtNode) node).value, indent + 1, "value");
        } else if (node instanceof ClassDeclNode) {
            ClassDeclNode classDecl = (ClassDeclNode) node;
            appendLine(builder, indent, label, "Class name=" + classDecl.name);
            appendLine(builder, indent + 1, "members", String.valueOf(classDecl.members.size()));
        } else if (node instanceof FuncDeclNode) {
            FuncDeclNode funcDecl = (FuncDeclNode) node;
            appendLine(builder, indent, label, "Function name=" + funcDecl.name + " returns=" + funcDecl.returnType);
            appendNode(builder, funcDecl.body, indent + 1, "body");
        } else if (node instanceof NumberNode) {
            appendLine(builder, indent, label, "Number " + ((NumberNode) node).value);
        } else if (node instanceof StringNode) {
            appendLine(builder, indent, label, "String \"" + ((StringNode) node).value + "\"");
        } else if (node instanceof IdentifierNode) {
            appendLine(builder, indent, label, "Identifier " + ((IdentifierNode) node).name);
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode binary = (BinaryOpNode) node;
            appendLine(builder, indent, label, "BinaryOp " + binary.operator);
            appendNode(builder, binary.left, indent + 1, "left");
            appendNode(builder, binary.right, indent + 1, "right");
        } else if (node instanceof AssignmentNode) {
            AssignmentNode assignment = (AssignmentNode) node;
            appendLine(builder, indent, label, "Assignment " + assignment.operator);
            appendNode(builder, assignment.left, indent + 1, "target");
            appendNode(builder, assignment.right, indent + 1, "value");
        } else if (node instanceof UnaryNode) {
            UnaryNode unary = (UnaryNode) node;
            appendLine(builder, indent, label, "Unary " + unary.operator);
            appendNode(builder, unary.operand, indent + 1, "operand");
        } else {
            appendLine(builder, indent, label, node.getClass().getSimpleName());
        }
    }

    private void appendStatements(StringBuilder builder, List<StatementNode> statements, int indent) {
        for (int i = 0; i < statements.size(); i++) {
            appendNode(builder, statements.get(i), indent, "statement[" + i + "]");
        }
    }

    private void appendLine(StringBuilder builder, int indent, String label, String value) {
        builder.append("  ".repeat(indent));
        if (label != null) {
            builder.append(label).append(": ");
        }
        builder.append(value).append(System.lineSeparator());
    }
}
