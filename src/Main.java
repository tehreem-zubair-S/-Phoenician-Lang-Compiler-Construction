import lexer.Lexer;
import lexer.Token;
import lexer.TokenType;
import parser.ASTNode;
import parser.ASTPrinter;
import parser.Interpreter;
import parser.Parser;
import parser.SemanticAnalyzer;
import parser.SymbolTable;
import parser.TACGenerator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final String VERSION = "1.2.0";
    private static final String DEFAULT_SOURCE_FILE = "program.phn";
    private static final Path PROJECT_ROOT = locateProjectRoot();

    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        printBanner();

        if (args.length > 0) {
            boolean success = compileFile(resolveSourcePath(args[0]));
            if (!success) {
                System.exit(1);
            }
            return;
        }

        runMenu();
    }

    private static void runMenu() {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            while (true) {
                printMenu();
                if (!scanner.hasNextLine()) {
                    compileFile(resolveSourcePath(DEFAULT_SOURCE_FILE));
                    return;
                }

                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        compileFile(resolveSourcePath(DEFAULT_SOURCE_FILE));
                        break;
                    case "2":
                        chooseAndRunFile(scanner);
                        break;
                    case "3":
                        runAllPrograms();
                        break;
                    case "4":
                        printKeywordRegistry();
                        break;
                    case "5":
                        System.out.println("Exiting.");
                        return;
                    default:
                        System.out.println("Invalid option. Please choose 1-5.");
                }
            }
        }
    }

    private static void chooseAndRunFile(Scanner scanner) {
        List<Path> files = listProgramFiles();
        if (files.isEmpty()) {
            System.out.println("No .phn files found in " + PROJECT_ROOT);
            return;
        }

        printSection("Available Phoenician Programs");
        for (int i = 0; i < files.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, PROJECT_ROOT.relativize(files.get(i)));
        }
        System.out.print("Select file number: ");

        if (!scanner.hasNextLine()) {
            return;
        }

        try {
            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (index < 0 || index >= files.size()) {
                System.out.println("Invalid file number.");
                return;
            }
            compileFile(files.get(index));
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Enter a number.");
        }
    }

    private static void runAllPrograms() {
        List<Path> files = listProgramFiles();
        if (files.isEmpty()) {
            System.out.println("No .phn files found in " + PROJECT_ROOT);
            return;
        }

        int passed = 0;
        int failed = 0;
        for (Path file : files) {
            if (compileFile(file)) {
                passed++;
            } else {
                failed++;
            }
        }

        printSection("Run All Summary");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
    }

    private static boolean compileFile(Path path) {
        if (!path.getFileName().toString().endsWith(".phn")) {
            System.out.println("Error: input file must have a .phn extension.");
            return false;
        }

        try {
            String source = Files.readString(path, StandardCharsets.UTF_8);
            return compileSource(source, path);
        } catch (IOException e) {
            System.out.println("Error reading file: " + path.toAbsolutePath());
            System.out.println("Details: " + e.getMessage());
            return false;
        }
    }

    private static boolean compileSource(String source, Path sourcePath) {
        printSection("Source File");
        System.out.println(sourcePath.toAbsolutePath());

        List<Token> tokens;
        printSection("Lexical Analysis / Tokenization");
        try {
            Lexer lexer = new Lexer(source);
            tokens = lexer.tokenize();
            printTokens(tokens);
            System.out.println("Lexical analysis: PASSED");
        } catch (Exception e) {
            System.out.println("Lexical analysis: FAILED");
            System.out.println("  " + e.getMessage());
            return false;
        }

        SymbolTable symbolTable = new SymbolTable();
        ASTNode root;

        printSection("Grammar Check / Parsing");
        try {
            Parser parser = new Parser(tokens, symbolTable);
            root = parser.parseProgram();

            if (parser.hasErrors()) {
                System.out.println("Grammar check: FAILED");
                for (String error : parser.getErrors()) {
                    System.out.println("  " + error);
                }
                return false;
            }
            System.out.println("Grammar check: PASSED");
        } catch (Exception e) {
            System.out.println("Grammar check: FAILED");
            System.out.println("  " + e.getMessage());
            return false;
        }

        printSection("Semantic Analysis");
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        if (!semanticAnalyzer.analyze(root)) {
            System.out.println("Semantic analysis: FAILED");
            for (String error : semanticAnalyzer.getErrors()) {
                System.out.println("  " + error);
            }
            return false;
        }
        semanticAnalyzer.printReport();

        printSection("Abstract Syntax Tree");
        System.out.print(new ASTPrinter().print(root));

        printSection("Three Address Code (TAC)");
        TACGenerator tacGenerator = new TACGenerator();
        tacGenerator.print(tacGenerator.generate(root));

        printSection("Execution Output");
        try {
            Interpreter interpreter = new Interpreter(symbolTable);
            interpreter.interpret(root);
            interpreter.printMemoryState();
        } catch (RuntimeException e) {
            System.out.println("Execution failed:");
            System.out.println("  " + e.getMessage());
            symbolTable.printSymbolTable();
            return false;
        }

        System.out.println("Execution: PASSED");
        return true;
    }

    private static void printTokens(List<Token> tokens) {
        System.out.printf("%-5s %-6s %-6s %-22s %-20s %-18s%n",
                "#", "Line", "Col", "Token Type", "Lexeme", "Code Points");
        System.out.println("--------------------------------------------------------------------------------");

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            String lexeme = token.getValue().isEmpty() ? "<EOF>" : token.getValue();
            System.out.printf("%-5d %-6d %-6d %-22s %-20s %-18s%n",
                    i + 1,
                    token.getLine(),
                    token.getColumn(),
                    token.getType(),
                    lexeme,
                    toCodePoints(token.getValue()));
        }
    }

    private static void printKeywordRegistry() {
        printSection("Phoenician Keyword Registry");
        System.out.printf("%-24s %-16s %-18s%n", "Token Type", "Symbol", "Code Points");
        System.out.println("--------------------------------------------------------------");

        for (TokenType type : TokenType.values()) {
            if (type.isKeyword()) {
                System.out.printf("%-24s %-16s %-18s%n",
                        type,
                        type.getSymbol(),
                        toCodePoints(type.getSymbol()));
            }
        }
    }

    private static List<Path> listProgramFiles() {
        try (Stream<Path> paths = Files.list(PROJECT_ROOT)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".phn"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static Path resolveSourcePath(String fileName) {
        Path requested = Path.of(fileName);
        if (requested.isAbsolute()) {
            return requested;
        }

        Path projectFile = PROJECT_ROOT.resolve(fileName);
        if (Files.exists(projectFile)) {
            return projectFile;
        }
        return Path.of("").toAbsolutePath().resolve(fileName);
    }

    private static Path locateProjectRoot() {
        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(currentDirectory.resolve("src").resolve("Main.java"))) {
            return currentDirectory;
        }

        Path classLocation = getClassLocation();
        Path cursor = classLocation;
        while (cursor != null) {
            if (Files.exists(cursor.resolve("src").resolve("Main.java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }

        Path oneDriveProject = Path.of(
                System.getProperty("user.home"),
                "OneDrive",
                "Documents",
                "Phoenician-Lang"
        );
        if (Files.exists(oneDriveProject.resolve("src").resolve("Main.java"))) {
            return oneDriveProject;
        }

        return currentDirectory;
    }

    private static Path getClassLocation() {
        try {
            return Path.of(Main.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()).toAbsolutePath().normalize();
        } catch (URISyntaxException | NullPointerException e) {
            return Path.of("").toAbsolutePath().normalize();
        }
    }

    private static String toCodePoints(String value) {
        if (value == null || value.isEmpty()) {
            return "-";
        }

        return value.codePoints()
                .mapToObj(codePoint -> String.format("U+%04X", codePoint))
                .collect(Collectors.joining(" "));
    }

    private static void printBanner() {
        System.out.println("Phoenician Core Compiler v" + VERSION);
        System.out.println("Project root: " + PROJECT_ROOT);
    }

    private static void printMenu() {
        printSection("Main Menu");
        System.out.println("1. Run default program.phn");
        System.out.println("2. Choose a .phn file");
        System.out.println("3. Run all .phn files");
        System.out.println("4. Print keyword registry");
        System.out.println("5. Exit");
        System.out.print("Select option: ");
    }

    private static void printSection(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }
}
