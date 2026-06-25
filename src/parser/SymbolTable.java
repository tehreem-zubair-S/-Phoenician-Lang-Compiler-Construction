package parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    public enum ScopeType {
        GLOBAL,
        FUNCTION,
        CLASS,
        BLOCK
    }

    public static class Symbol {
        private final String name;
        private String type;
        private Object value;
        private boolean initialized;
        private boolean constant;
        private ScopeType scope;

        public Symbol(String name, String type) {
            this.name = name;
            this.type = type;
            this.value = null;
            this.initialized = false;
            this.constant = false;
            this.scope = ScopeType.GLOBAL;
        }

        public Symbol(String name, String type, Object value) {
            this(name, type);
            setValue(value);
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
            this.initialized = true;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        public boolean isConstant() {
            return constant;
        }

        public void setConstant(boolean constant) {
            this.constant = constant;
        }

        public ScopeType getScope() {
            return scope;
        }

        public void setScope(ScopeType scope) {
            this.scope = scope;
        }
    }

    public static class FunctionSymbol extends Symbol {
        private final List<Symbol> parameters;
        private String returnType;
        private boolean method;
        private String className;

        public FunctionSymbol(String name, String returnType) {
            super(name, "function");
            this.returnType = returnType;
            this.parameters = new ArrayList<>();
            this.method = false;
            this.className = null;
        }

        public List<Symbol> getParameters() {
            return parameters;
        }

        public void setParameters(List<Symbol> parameters) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
        }

        public void addParameter(Symbol parameter) {
            parameters.add(parameter);
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public boolean isMethod() {
            return method;
        }

        public void setMethod(boolean method) {
            this.method = method;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }
    }

    public static class ClassSymbol extends Symbol {
        private final List<Symbol> fields;
        private final List<FunctionSymbol> methods;
        private String superClass;
        private final List<String> interfaces;
        private boolean interfaceType;

        public ClassSymbol(String name) {
            super(name, "class");
            this.fields = new ArrayList<>();
            this.methods = new ArrayList<>();
            this.superClass = null;
            this.interfaces = new ArrayList<>();
            this.interfaceType = false;
        }

        public List<Symbol> getFields() {
            return fields;
        }

        public void addField(Symbol field) {
            fields.add(field);
        }

        public List<FunctionSymbol> getMethods() {
            return methods;
        }

        public void addMethod(FunctionSymbol method) {
            methods.add(method);
            method.setMethod(true);
            method.setClassName(getName());
        }

        public String getSuperClass() {
            return superClass;
        }

        public void setSuperClass(String superClass) {
            this.superClass = superClass;
        }

        public List<String> getInterfaces() {
            return interfaces;
        }

        public void addInterface(String interfaceName) {
            interfaces.add(interfaceName);
        }

        public boolean isInterface() {
            return interfaceType;
        }

        public void setInterface(boolean interfaceType) {
            this.interfaceType = interfaceType;
        }
    }

    private final Map<String, Symbol> symbols;
    private final Map<String, FunctionSymbol> functions;
    private final Map<String, ClassSymbol> classes;
    private final Deque<Map<String, Symbol>> scopeStack;
    private final List<String> errors;
    private String currentScope;

    public SymbolTable() {
        this.symbols = new LinkedHashMap<>();
        this.functions = new LinkedHashMap<>();
        this.classes = new LinkedHashMap<>();
        this.scopeStack = new ArrayDeque<>();
        this.errors = new ArrayList<>();
        this.currentScope = "global";
        startScope("global");
    }

    public void startScope(String scopeName) {
        scopeStack.push(new LinkedHashMap<>());
        currentScope = scopeName;
    }

    public void endScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
        currentScope = "global";
    }

    public boolean isGlobalScope() {
        return scopeStack.size() == 1;
    }

    public boolean addSymbol(String name, String type) {
        return addSymbol(name, type, null, false);
    }

    public boolean addSymbol(String name, String type, Object value) {
        return addSymbol(name, type, value, true);
    }

    private boolean addSymbol(String name, String type, Object value, boolean initialized) {
        Map<String, Symbol> currentScopeMap = scopeStack.peek();
        if (currentScopeMap != null && currentScopeMap.containsKey(name)) {
            errors.add("Symbol '" + name + "' already defined in current scope");
            return false;
        }

        Symbol symbol = new Symbol(name, type);
        symbol.setScope(getScopeType());
        if (initialized) {
            symbol.setValue(value);
        }

        if (currentScopeMap != null) {
            currentScopeMap.put(name, symbol);
        }
        symbols.put(name, symbol);
        return true;
    }

    public boolean addFunction(String name, String returnType) {
        if (functions.containsKey(name)) {
            errors.add("Function '" + name + "' already defined");
            return false;
        }
        FunctionSymbol function = new FunctionSymbol(name, returnType);
        function.setScope(ScopeType.GLOBAL);
        functions.put(name, function);
        return true;
    }

    public boolean addFunction(String name, String returnType, List<Symbol> params) {
        if (!addFunction(name, returnType)) {
            return false;
        }
        functions.get(name).setParameters(params);
        return true;
    }

    public boolean addClass(String name) {
        return addClass(name, null);
    }

    public boolean addClass(String name, String superClass) {
        if (classes.containsKey(name)) {
            errors.add("Class '" + name + "' already defined");
            return false;
        }
        ClassSymbol classSymbol = new ClassSymbol(name);
        classSymbol.setSuperClass(superClass);
        classes.put(name, classSymbol);
        return true;
    }

    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopeStack) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        return symbols.get(name);
    }

    public FunctionSymbol lookupFunction(String name) {
        return functions.get(name);
    }

    public ClassSymbol lookupClass(String name) {
        return classes.get(name);
    }

    public String getSymbolType(String name) {
        Symbol symbol = lookup(name);
        return symbol == null ? null : symbol.getType();
    }

    public Object getSymbolValue(String name) {
        Symbol symbol = lookup(name);
        return symbol == null ? null : symbol.getValue();
    }

    public boolean setSymbolValue(String name, Object value) {
        Symbol symbol = lookup(name);
        if (symbol == null) {
            errors.add("Symbol '" + name + "' not found");
            return false;
        }
        if (symbol.isConstant()) {
            errors.add("Cannot modify constant '" + name + "'");
            return false;
        }
        symbol.setValue(value);
        return true;
    }

    public boolean isInitialized(String name) {
        Symbol symbol = lookup(name);
        return symbol != null && symbol.isInitialized();
    }

    public Map<String, Symbol> getCurrentScopeSymbols() {
        return scopeStack.peek();
    }

    public Map<String, Symbol> getAllSymbols() {
        return symbols;
    }

    public Map<String, FunctionSymbol> getAllFunctions() {
        return functions;
    }

    public Map<String, ClassSymbol> getAllClasses() {
        return classes;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void printSymbolTable() {
        System.out.println("\n--- Symbol Table Status ---");
        System.out.printf("%-18s %-18s %-18s %-12s%n", "Name", "Type", "Value", "Scope");
        System.out.println("------------------------------------------------------------------");

        if (symbols.isEmpty() && functions.isEmpty() && classes.isEmpty()) {
            System.out.println("(empty)");
            return;
        }

        for (Symbol symbol : symbols.values()) {
            String value = symbol.getValue() == null ? "null" : symbol.getValue().toString();
            System.out.printf("%-18s %-18s %-18s %-12s%n",
                    symbol.getName(), symbol.getType(), value, symbol.getScope());
        }

        for (FunctionSymbol function : functions.values()) {
            System.out.printf("%-18s %-18s %-18s %-12s%n",
                    function.getName(), "function", function.getReturnType(), function.getScope());
        }

        for (ClassSymbol classSymbol : classes.values()) {
            System.out.printf("%-18s %-18s %-18s %-12s%n",
                    classSymbol.getName(), "class", classSymbol.getFields().size() + " fields", "GLOBAL");
        }
    }

    public void clear() {
        symbols.clear();
        functions.clear();
        classes.clear();
        scopeStack.clear();
        errors.clear();
        currentScope = "global";
        startScope("global");
    }

    private ScopeType getScopeType() {
        if (currentScope.startsWith("function_")) {
            return ScopeType.FUNCTION;
        }
        if (currentScope.startsWith("class_")) {
            return ScopeType.CLASS;
        }
        if (currentScope.startsWith("block_")) {
            return ScopeType.BLOCK;
        }
        return ScopeType.GLOBAL;
    }
}
