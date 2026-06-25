package parser;

import java.util.*;

/**
 * Abstract Syntax Tree (AST) Nodes for Phoenician Language
 * Contains all node types needed for the complete CFG
 *
 * All node classes are in this single file for simplicity
 */
public abstract class ASTNode {
    private int line = -1;
    private int column = -1;

    public void setSourceLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String location() {
        if (line < 0 || column < 0) {
            return "unknown location";
        }
        return "line " + line + ", column " + column;
    }
}

// ========== PROGRAM ==========

class ProgramNode extends ASTNode {
    public List<StatementNode> statements;

    public ProgramNode(List<StatementNode> statements) {
        this.statements = statements;
    }
}

// ========== STATEMENTS ==========

abstract class StatementNode extends ASTNode {}

class ExpressionStatementNode extends StatementNode {
    public ExpressionNode expression;

    public ExpressionStatementNode(ExpressionNode expression) {
        this.expression = expression;
    }
}

// ========== DECLARATIONS ==========

class PackageDeclNode extends StatementNode {
    public String name;

    public PackageDeclNode(String name) {
        this.name = name;
    }
}

class ImportDeclNode extends StatementNode {
    public String name;
    public boolean isStarImport;

    public ImportDeclNode(String name, boolean isStarImport) {
        this.name = name;
        this.isStarImport = isStarImport;
    }
}

class VarDeclNode extends StatementNode {
    public List<String> storageSpecs;
    public List<String> qualifiers;
    public String type;
    public String name;
    public ExpressionNode initializer;

    public VarDeclNode(String type, String name, ExpressionNode initializer) {
        this.storageSpecs = new ArrayList<>();
        this.qualifiers = new ArrayList<>();
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    public VarDeclNode(List<String> storageSpecs, List<String> qualifiers,
                       String type, String name, ExpressionNode initializer) {
        this.storageSpecs = storageSpecs != null ? storageSpecs : new ArrayList<>();
        this.qualifiers = qualifiers != null ? qualifiers : new ArrayList<>();
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }
}

class FuncDeclNode extends StatementNode {
    public List<String> storageSpecs;
    public List<String> qualifiers;
    public String returnType;
    public String name;
    public List<ParamNode> params;
    public List<String> throwsList;
    public BlockNode body;

    public FuncDeclNode(List<String> storageSpecs, List<String> qualifiers,
                        String returnType, String name, List<ParamNode> params,
                        List<String> throwsList, BlockNode body) {
        this.storageSpecs = storageSpecs != null ? storageSpecs : new ArrayList<>();
        this.qualifiers = qualifiers != null ? qualifiers : new ArrayList<>();
        this.returnType = returnType;
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
        this.throwsList = throwsList != null ? throwsList : new ArrayList<>();
        this.body = body;
    }
}

// ========== OOP DECLARATIONS ==========

class ClassDeclNode extends StatementNode {
    public String accessModifier;
    public String name;
    public String superClass;
    public List<String> interfaces;
    public List<ClassMemberNode> members;

    public ClassDeclNode(String accessModifier, String name, String superClass,
                         List<String> interfaces, List<ClassMemberNode> members) {
        this.accessModifier = accessModifier;
        this.name = name;
        this.superClass = superClass;
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        this.members = members != null ? members : new ArrayList<>();
    }
}

class InterfaceDeclNode extends StatementNode {
    public String accessModifier;
    public String name;
    public List<String> extendedInterfaces;
    public List<InterfaceMemberNode> members;

    public InterfaceDeclNode(String accessModifier, String name,
                             List<String> extendedInterfaces,
                             List<InterfaceMemberNode> members) {
        this.accessModifier = accessModifier;
        this.name = name;
        this.extendedInterfaces = extendedInterfaces != null ? extendedInterfaces : new ArrayList<>();
        this.members = members != null ? members : new ArrayList<>();
    }
}

class ObjectDeclNode extends StatementNode {
    public String className;
    public List<ExpressionNode> arguments;
    public String target;

    public ObjectDeclNode(String className, List<ExpressionNode> arguments, String target) {
        this.className = className;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
        this.target = target;
    }
}

// ========== CLASS MEMBERS ==========

abstract class ClassMemberNode extends ASTNode {}

class FieldMemberNode extends ClassMemberNode {
    public List<String> modifiers;
    public String type;
    public String name;
    public ExpressionNode initializer;

    public FieldMemberNode(List<String> modifiers, String type, String name, ExpressionNode initializer) {
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }
}

class MethodMemberNode extends ClassMemberNode {
    public List<String> modifiers;
    public String returnType;
    public String name;
    public List<ParamNode> params;
    public List<String> throwsList;
    public BlockNode body;

    public MethodMemberNode(List<String> modifiers, String returnType, String name,
                            List<ParamNode> params, List<String> throwsList, BlockNode body) {
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
        this.returnType = returnType;
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
        this.throwsList = throwsList != null ? throwsList : new ArrayList<>();
        this.body = body;
    }
}

class ConstructorMemberNode extends ClassMemberNode {
    public List<String> modifiers;
    public String name;
    public List<ParamNode> params;
    public List<String> throwsList;
    public BlockNode body;

    public ConstructorMemberNode(List<String> modifiers, String name,
                                 List<ParamNode> params, List<String> throwsList, BlockNode body) {
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
        this.throwsList = throwsList != null ? throwsList : new ArrayList<>();
        this.body = body;
    }
}

class InnerClassMemberNode extends ClassMemberNode {
    public ClassDeclNode innerClass;

    public InnerClassMemberNode(ClassDeclNode innerClass) {
        this.innerClass = innerClass;
    }
}

// ========== INTERFACE MEMBERS ==========

abstract class InterfaceMemberNode extends ASTNode {}

class InterfaceMethodMemberNode extends InterfaceMemberNode {
    public String returnType;
    public String name;
    public List<ParamNode> params;

    public InterfaceMethodMemberNode(String returnType, String name, List<ParamNode> params) {
        this.returnType = returnType;
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
    }
}

// ========== METHOD/CONSTRUCTOR DECLARATIONS ==========

class MethodDeclNode extends StatementNode {
    public List<String> modifiers;
    public String returnType;
    public String name;
    public List<ParamNode> params;
    public List<String> throwsList;
    public BlockNode body;

    public MethodDeclNode(List<String> modifiers, String returnType, String name,
                          List<ParamNode> params, List<String> throwsList, BlockNode body) {
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
        this.returnType = returnType;
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
        this.throwsList = throwsList != null ? throwsList : new ArrayList<>();
        this.body = body;
    }
}

class ConstructorDeclNode extends StatementNode {
    public List<String> modifiers;
    public String name;
    public List<ParamNode> params;
    public List<String> throwsList;
    public BlockNode body;

    public ConstructorDeclNode(List<String> modifiers, String name,
                               List<ParamNode> params, List<String> throwsList, BlockNode body) {
        this.modifiers = modifiers != null ? modifiers : new ArrayList<>();
        this.name = name;
        this.params = params != null ? params : new ArrayList<>();
        this.throwsList = throwsList != null ? throwsList : new ArrayList<>();
        this.body = body;
    }
}

// ========== PARAMETERS ==========

class ParamNode {
    public String type;
    public String name;

    public ParamNode(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String getType() { return type; }
    public String getName() { return name; }
}

// ========== CONTROL FLOW ==========

class IfStmtNode extends StatementNode {
    public ExpressionNode condition;
    public BlockNode thenBlock;
    public BlockNode elseBlock;

    public IfStmtNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }
}

class WhileStmtNode extends StatementNode {
    public ExpressionNode condition;
    public BlockNode body;

    public WhileStmtNode(ExpressionNode condition, BlockNode body) {
        this.condition = condition;
        this.body = body;
    }
}

class ForStmtNode extends StatementNode {
    public StatementNode init;
    public ExpressionNode condition;
    public ExpressionNode update;
    public BlockNode body;

    public ForStmtNode(StatementNode init, ExpressionNode condition,
                       ExpressionNode update, BlockNode body) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }
}

class DoWhileStmtNode extends StatementNode {
    public BlockNode body;
    public ExpressionNode condition;

    public DoWhileStmtNode(BlockNode body, ExpressionNode condition) {
        this.body = body;
        this.condition = condition;
    }
}

class SwitchStmtNode extends StatementNode {
    public ExpressionNode expression;
    public List<CaseClauseNode> cases;
    public DefaultClauseNode defaultClause;

    public SwitchStmtNode(ExpressionNode expression, List<CaseClauseNode> cases,
                          DefaultClauseNode defaultClause) {
        this.expression = expression;
        this.cases = cases != null ? cases : new ArrayList<>();
        this.defaultClause = defaultClause;
    }
}

class CaseClauseNode extends ASTNode {
    public ExpressionNode expression;
    public List<StatementNode> statements;

    public CaseClauseNode(ExpressionNode expression, List<StatementNode> statements) {
        this.expression = expression;
        this.statements = statements != null ? statements : new ArrayList<>();
    }
}

class DefaultClauseNode extends ASTNode {
    public List<StatementNode> statements;

    public DefaultClauseNode(List<StatementNode> statements) {
        this.statements = statements != null ? statements : new ArrayList<>();
    }
}

// ========== JUMP STATEMENTS ==========

class BreakStmtNode extends StatementNode {}

class ContinueStmtNode extends StatementNode {}

class GotoStmtNode extends StatementNode {
    public String label;

    public GotoStmtNode(String label) {
        this.label = label;
    }
}

class ReturnStmtNode extends StatementNode {
    public ExpressionNode value;

    public ReturnStmtNode(ExpressionNode value) {
        this.value = value;
    }
}

// ========== I/O STATEMENTS ==========

class PrintStmtNode extends StatementNode {
    public ExpressionNode value;

    public PrintStmtNode(ExpressionNode value) {
        this.value = value;
    }
}

class InputStmtNode extends StatementNode {
    public String target;
    public String type;

    public InputStmtNode(String target, String type) {
        this.target = target;
        this.type = type;
    }
}

// ========== EXCEPTION HANDLING ==========

class TryStmtNode extends StatementNode {
    public BlockNode tryBlock;
    public List<CatchClauseNode> catches;
    public FinallyClauseNode finallyClause;

    public TryStmtNode(BlockNode tryBlock, List<CatchClauseNode> catches,
                       FinallyClauseNode finallyClause) {
        this.tryBlock = tryBlock;
        this.catches = catches != null ? catches : new ArrayList<>();
        this.finallyClause = finallyClause;
    }
}

class CatchClauseNode extends ASTNode {
    public String type;
    public String name;
    public BlockNode block;

    public CatchClauseNode(String type, String name, BlockNode block) {
        this.type = type;
        this.name = name;
        this.block = block;
    }
}

class FinallyClauseNode extends ASTNode {
    public BlockNode block;

    public FinallyClauseNode(BlockNode block) {
        this.block = block;
    }
}

class ThrowStmtNode extends StatementNode {
    public ExpressionNode expression;

    public ThrowStmtNode(ExpressionNode expression) {
        this.expression = expression;
    }
}

// ========== SYNCHRONIZED ==========

class SynchronizedStmtNode extends StatementNode {
    public ExpressionNode lock;
    public BlockNode block;

    public SynchronizedStmtNode(ExpressionNode lock, BlockNode block) {
        this.lock = lock;
        this.block = block;
    }
}

// ========== BLOCK ==========

class BlockNode extends StatementNode {
    public List<StatementNode> statements;

    public BlockNode(List<StatementNode> statements) {
        this.statements = statements != null ? statements : new ArrayList<>();
    }
}

// ========== EXPRESSIONS ==========

abstract class ExpressionNode extends ASTNode {}

class NumberNode extends ExpressionNode {
    public int value;

    public NumberNode(int value) {
        this.value = value;
    }
}

class StringNode extends ExpressionNode {
    public String value;

    public StringNode(String value) {
        this.value = value;
    }
}

class IdentifierNode extends ExpressionNode {
    public String name;

    public IdentifierNode(String name) {
        this.name = name;
    }
}

class BinaryOpNode extends ExpressionNode {
    public ExpressionNode left;
    public String operator;
    public ExpressionNode right;

    public BinaryOpNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

class AssignmentNode extends ExpressionNode {
    public ExpressionNode left;
    public String operator;
    public ExpressionNode right;

    public AssignmentNode(ExpressionNode left, String operator, ExpressionNode right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }
}

class ConditionalNode extends ExpressionNode {
    public ExpressionNode condition;
    public ExpressionNode thenExpr;
    public ExpressionNode elseExpr;

    public ConditionalNode(ExpressionNode condition, ExpressionNode thenExpr, ExpressionNode elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
    }
}

class MethodCallNode extends ExpressionNode {
    public String object;
    public String method;
    public List<ExpressionNode> arguments;

    public MethodCallNode(String object, String method, List<ExpressionNode> arguments) {
        this.object = object;
        this.method = method;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }
}

class SizeofNode extends ExpressionNode {
    public String typeName;
    public ExpressionNode expression;
    public boolean isType;

    public SizeofNode(String typeName) {
        this.typeName = typeName;
        this.isType = true;
    }

    public SizeofNode(ExpressionNode expression) {
        this.expression = expression;
        this.isType = false;
    }
}

class InstanceofNode extends ExpressionNode {
    public ExpressionNode expression;
    public String typeName;

    public InstanceofNode(ExpressionNode expression, String typeName) {
        this.expression = expression;
        this.typeName = typeName;
    }
}

class UnaryNode extends ExpressionNode {
    public String operator;
    public ExpressionNode operand;

    public UnaryNode(String operator, ExpressionNode operand) {
        this.operator = operator;
        this.operand = operand;
    }
}
class ObjectCreationExpressionNode extends ExpressionNode {
    public String className;
    public List<ExpressionNode> arguments;

    public ObjectCreationExpressionNode(String className, List<ExpressionNode> arguments) {
        this.className = className;
        this.arguments = arguments != null ? arguments : new ArrayList<>();
    }
}
