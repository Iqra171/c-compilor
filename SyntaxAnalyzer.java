import java.util.*;
import java.util.regex.*;
import javax.swing.*;

public class SyntaxAnalyzer {
    private static final Set<String> cppKeywords = Set.of("int", "float", "double", "char", "bool", "if", "else",
            "long", "short", "unsigned", "true", "false", "string", "for", "while", "do", "switch", "case", "break", "continue",
            "return", "void", "struct", "class", "const", "static", "enum", "namespace", "using", "try", "catch", "throw");

    public static final Map<String, VariableInfo> declaredVariables = new HashMap<>();
    private static final Set<String> assignmentOperators = Set.of("=", "+=", "-=", "*=", "/=", "%=", "<<=", ">>=", "&=", "^=", "|=");
    private static boolean skipRemainingChecksForLine = false;

    static class VariableInfo {
        String type;
        boolean initialized;

        public VariableInfo(String type, boolean initialized) {
            this.type = type;
            this.initialized = initialized;
        }
    }

    private static void updateSymbolTableGUI(JTextArea symbolTableArea) {
        if (symbolTableArea == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("SYMBOL TABLE:\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-15s | %-10s | %-10s\n", "IDENTIFIER", "TYPE", "INITIALIZED"));
        sb.append("----------------------------------------\n");
            
        for (Map.Entry<String, VariableInfo> entry : declaredVariables.entrySet()) {
            sb.append(String.format("%-15s | %-10s | %-10s\n", 
                entry.getKey(), 
                entry.getValue().type, 
                entry.getValue().initialized ? "Yes" : "No"));
        }
           
        symbolTableArea.setText(sb.toString());
    }

    public static void analyzeSyntax(List<String> tokens, List<Integer> tokenLines, 
    JTextArea symbolTableArea, JTextArea errorConsoleArea) {
declaredVariables.clear(); // Clear previous analysis

// DEBUG: Print tokens to verify input
System.out.println("Tokens: " + tokens);

// Process tokens to populate declaredVariables
for (int i = 0; i < tokens.size(); i++) {
String token = tokens.get(i);
if (cppKeywords.contains(token)) { // Found a type keyword (e.g., "int")
if (i + 1 < tokens.size()) {
String nextToken = tokens.get(i + 1);
if (nextToken.matches("[a-zA-Z_][a-zA-Z0-9_]*")) { // Valid variable name
String varName = nextToken;
declaredVariables.put(varName, new VariableInfo(token, false));
System.out.println("DEBUG: Registered variable - " + token + " " + varName);
}
}
}
}

updateSymbolTableGUI(symbolTableArea); // Update GUI
}
  
    private static String assembleCode(List<String> tokens) {
        StringBuilder code = new StringBuilder();
        for (String token : tokens) {
            code.append(token).append(" ");
        }
        return code.toString();
    }

    public static void analyze(String code, JTextArea outputArea) {
    

        String processedCode = checkComments(code, outputArea);
        if (processedCode.equals("//ANALYSIS_TERMINATED_DUE_TO_UNCLOSED_COMMENT")) {
            return; // Exit early, don't perform further analysis
        }

        code = code.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        String[] lines = code.split("\\n");
        int lineNumber = 0;
        declaredVariables.clear();
    
        // Then continue with the rest of your analysis using the processed code
    
        // Add this line to check for main function issues
        checkMainFunction(code, outputArea);
        
    
        boolean expectingElse = false;
        for (String line : lines) {
            lineNumber++;
            skipRemainingChecksForLine = false;
            String trimmed = line.replaceAll("//.*$", "").trim();
            if (trimmed.isEmpty()) continue;
    
            checkMultipleDataTypes(trimmed, lineNumber, outputArea);
            if (skipRemainingChecksForLine) continue;
    
            checkStringArithmeticOperations(trimmed, lineNumber, outputArea);
            
            if (trimmed.matches("^\\s*else\\b.*") && !expectingElse) {
                outputArea.append("Line " + lineNumber + ": Error - 'else' without matching 'if'.\n");
            }
    
            if (!trimmed.matches("^\\s*else\\b.*")) {
                expectingElse = false;
            }
    
            checkKeywordCase(trimmed, lineNumber, outputArea);
            checkSemicolon(trimmed, lineNumber, outputArea);
            checkEmptyInitialization(trimmed, lineNumber, outputArea);
            checkIdentifiersWithoutKeywords(trimmed, lineNumber, outputArea);
            checkMultipleDeclarations(trimmed, lineNumber, outputArea);
            checkDeclaration(trimmed, lineNumber, outputArea);
            checkInitialization(trimmed, lineNumber, outputArea);
            // checkAssignmentOperators(trimmed, lineNumber, outputArea);
            // checkProblematicOperators(trimmed, lineNumber, outputArea);
            // checkSyntaxErrors(line, lineNumber, outputArea);
            // expectingElse = checkIfElseStatements(trimmed, lineNumber, outputArea) || expectingElse;
        }
    
        for (Map.Entry<String, VariableInfo> entry : declaredVariables.entrySet()) {
            if (!entry.getValue().initialized) {
                outputArea.append("Warning: Variable '" + entry.getKey() + "' is declared but never initialized.\n");
            }
        }
    }

    private static String checkComments(String code, JTextArea outputArea) {
        int lineNumber = 1;
        boolean inMultilineComment = false;
        int commentStartLine = 0;
        StringBuilder processedCode = new StringBuilder();
        
        for (int i = 0; i < code.length(); i++) {
            // Check for end of multiline comment
            if (inMultilineComment) {
                if (i < code.length() - 1 && code.charAt(i) == '*' && code.charAt(i + 1) == '/') {
                    inMultilineComment = false;
                    i++; // Skip the '/' character
                    continue;
                }
                
                // Check for nested comment start - error in C++
                if (i < code.length() - 1 && code.charAt(i) == '/' && code.charAt(i + 1) == '*') {
                    outputArea.append("Error at line " + lineNumber + ": Nested comments are not allowed in C++\n");
                    i++; // Skip the '*' character
                    
                    // Important: Don't change inMultilineComment state here,
                    // we're already in a comment so we just continue processing
                }
                
                // Count lines within comments
                if (code.charAt(i) == '\n') {
                    lineNumber++;
                    processedCode.append('\n'); // Keep newlines for line count consistency
                }
                continue;
            }
            
            // Check for start of multiline comment
            if (i < code.length() - 1 && code.charAt(i) == '/' && code.charAt(i + 1) == '*') {
                inMultilineComment = true;
                commentStartLine = lineNumber;
                i++; // Skip the '*' character
                continue;
            }
            
            // Check for single-line comment
            if (i < code.length() - 1 && code.charAt(i) == '/' && code.charAt(i + 1) == '/') {
                // Skip to end of line
                while (i < code.length() && code.charAt(i) != '\n') {
                    i++;
                }
                // Don't skip the newline itself
                if (i < code.length() && code.charAt(i) == '\n') {
                    processedCode.append('\n');
                    lineNumber++;
                }
                continue;
            }
            
            // Add character to processed code
            processedCode.append(code.charAt(i));
            
            // Count lines outside comments
            if (code.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        
        // Check if we ended with an unclosed multiline comment
        if (inMultilineComment) {
            // Only show the comment error
            outputArea.append("Error: Unterminated multi-line comment starting at line " + commentStartLine + "\n");
            
            // Return a special marker to indicate analysis should stop
            return "//ANALYSIS_TERMINATED_DUE_TO_UNCLOSED_COMMENT";
        }
        
        return processedCode.toString();
    }
 // Add this method to the SyntaxAnalyzer class
 private static void checkMainFunction(String code, JTextArea outputArea) {
    Pattern mainFunctionPattern = Pattern.compile("\\b(int|void)\\s+main\\s*\\([^)]*\\)");
    Matcher mainFunctionMatcher = mainFunctionPattern.matcher(code);
    
    int mainCount = 0;
    while (mainFunctionMatcher.find()) {
        mainCount++;
    }
    
    if (mainCount > 1) {
        outputArea.append("Error: Multiple main functions detected. A C++ program can have only one main function.\n");
        return;
    }
    // Check if main function exists with proper syntax
    boolean hasProperMain = Pattern.compile("\\b(int|void)\\s+main\\s*\\([^)]*\\)").matcher(code).find();
    if (!hasProperMain) {
        // Check if there's some attempt at defining main but with incorrect syntax
        boolean hasIncorrectMain = Pattern.compile("\\bmain\\s*\\)\\s*\\(|\\bmain\\s*[^(]*\\(|\\bmain\\s*\\([^)]*[^)]$").matcher(code).find();
        
        if (hasIncorrectMain) {
            outputArea.append("Error: Invalid main function syntax. Correct syntax is: int main() or int main(int argc, char* argv[])\n");
        } else {
            // No main function found at all
            boolean anyMainWord = Pattern.compile("\\bmain\\b").matcher(code).find();
            if (anyMainWord) {
                outputArea.append("Error: 'main' keyword found but not properly declared as a function. Use: int main() { ... }\n");
            } else {
                outputArea.append("Error: No main() function found.\n");
            }
        }
        return;
    }
    
    // Check for missing braces in main function
    Pattern mainPattern = Pattern.compile("\\b(int|void)\\s+main\\s*\\([^)]*\\)\\s*([{]?)");
    Matcher mainMatcher = mainPattern.matcher(code);
    if (mainMatcher.find()) {
        String openingBrace = mainMatcher.group(2);
        if (openingBrace == null || openingBrace.isEmpty()) {
            outputArea.append("Error: Missing opening brace '{' for main function.\n");
        } else {
            // Check for matching closing brace
            int startPos = mainMatcher.end();
            int braceCount = 1;
            boolean closingBraceFound = false;
            
            for (int i = startPos; i < code.length(); i++) {
                if (code.charAt(i) == '{') braceCount++;
                else if (code.charAt(i) == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        closingBraceFound = true;
                        break;
                    }
                }
            }
            
            if (!closingBraceFound) {
                outputArea.append("Error: Missing closing brace '}' for main function.\n");
            }
        }
    }
    Pattern mainBodyPattern = Pattern.compile("\\b(int|void)\\s+main\\s*\\([^)]*\\)\\s*\\{([\\s\\S]*?)\\}");
    Matcher mainBodyMatcher = mainBodyPattern.matcher(code);
    
    if (mainBodyMatcher.find()) {
        String mainBody = mainBodyMatcher.group(2);
        
        // Apply all the checks from analyze() method to the main body
        mainBody = mainBody.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        String[] lines = mainBody.split("\\n");
        int lineNumber = 0;
        Map<String, VariableInfo> mainFunctionVariables = new HashMap<>();
        
        boolean expectingElse = false;
        for (String line : lines) {
            lineNumber++;
            boolean skipRemainingChecksForLine = false;
            String trimmed = line.replaceAll("//.*$", "").trim();
            if (trimmed.isEmpty()) continue;
    
            // Apply all your existing checks
            checkMultipleDataTypes(trimmed, lineNumber, outputArea);
            if (skipRemainingChecksForLine) continue;
    
            checkStringArithmeticOperations(trimmed, lineNumber, outputArea);
            
            if (trimmed.matches("^\\s*else\\b.*") && !expectingElse) {
                outputArea.append("Main function line " + lineNumber + ": Error - 'else' without matching 'if'.\n");
            }
    
            if (!trimmed.matches("^\\s*else\\b.*")) {
                expectingElse = false;
            }
    
            checkKeywordCase(trimmed, lineNumber, outputArea);
            checkSemicolon(trimmed, lineNumber, outputArea);
            checkEmptyInitialization(trimmed, lineNumber, outputArea);
            checkIdentifiersWithoutKeywords(trimmed, lineNumber, outputArea);
            checkMultipleDeclarations(trimmed, lineNumber, outputArea);
            checkDeclaration(trimmed, lineNumber, outputArea);
            checkInitialization(trimmed, lineNumber, outputArea);
            checkAssignmentOperators(trimmed, lineNumber, outputArea);
            checkProblematicOperators(trimmed, lineNumber, outputArea);
            checkSyntaxErrors(line, lineNumber, outputArea);
            expectingElse = checkIfElseStatements(trimmed, lineNumber, outputArea) || expectingElse;
        }
    
        // Check for uninitialized variables in main function
        for (Map.Entry<String, VariableInfo> entry : mainFunctionVariables.entrySet()) {
            if (!entry.getValue().initialized) {
                outputArea.append("Warning in main function: Variable '" + entry.getKey() + "' is declared but never initialized.\n");
            }
        }
        
        // Check for missing return statement if main is declared as int
        if (mainBodyMatcher.group(1).equals("int") && !Pattern.compile("\\breturn\\b").matcher(mainBody).find()) {
            // outputArea.append("Warning: int main() function should have a return statement.\n");
        }
    }

} 
// no chnage till now
    private static void checkKeywordCase(String line, int lineNumber, JTextArea outputArea) {
        for (String kw : cppKeywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String match = matcher.group();
                if (!match.equals(kw)) {
                    outputArea.append("Line " + lineNumber + ": Incorrect keyword format -> '" + match + "' should be '" + kw + "'\n");
                }
            }
        }
    }
    

    private static boolean isStatementThatNeedsSemicolon(String line) {
        line = line.trim();
        
        // Check if this is a function definition (has parentheses followed by a brace)
        if (line.matches(".*\\)\\s*(\\{.*)?$") && 
        line.matches(".*\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*")) {
        return false;
    }
        return !(line.startsWith("if") ||
                line.startsWith("else") ||
                line.startsWith("while") ||
                line.startsWith("for") ||
                line.startsWith("do") ||
                line.endsWith("{") ||
                line.endsWith("}"));
    }

    private static void checkSemicolon(String line, int lineNumber, JTextArea outputArea) {
        // Skip preprocessor directives, empty lines, and comments
        if (line.trim().startsWith("#") || line.trim().isEmpty() || line.trim().startsWith("//")) {
            return;
        }
        
        // Skip function definitions (like int main() {...})
        if (line.matches(".*\\)\\s*\\{.*") && 
            line.matches(".*\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*")) {
            return;
        }
        
        // Original skipping conditions
        if (line.matches(".*\\{\\s*$") || line.matches(".*\\}\\s*$") ||
            line.matches("^\\s*if\\s*\\(.*\\).*") || line.matches("^\\s*else.*")) {
            return;
        }
        
        // Additional skipping for loop and while statements
        if (line.matches("^\\s*for\\s*\\(.*\\).*") || line.matches("^\\s*while\\s*\\(.*\\).*")) {
            return;
        }
    
        // If we get here and the line needs a semicolon but doesn't have one, flag it
        if (isStatementThatNeedsSemicolon(line) && !line.trim().endsWith(";")) {
            // Additional check: make sure it's not a function declaration
            if (!line.matches(".*\\)\\s*$")) {
                outputArea.append("Line " + lineNumber + ": Error - Missing semicolon.\n");
            }
        }
    }

    private static void checkMultipleDataTypes(String line, int lineNumber, JTextArea outputArea) {
        // Skip comments and preprocessor directives
        if (line.trim().startsWith("//") || line.trim().startsWith("#")) {
            return;
        }
        
        // Skip function bodies that contain multiple declarations
        if (line.contains("{") && line.contains("}")) {
            // This is likely a function with body on one line
            return;
        }
        
        // Skip separate variable declarations (int i=0; int j=0;)
        if (line.contains(";") && line.trim().split(";").length > 1) {
            boolean hasMultipleDeclarations = false;
            String[] statements = line.trim().split(";");
            
            for (String statement : statements) {
                if (statement.trim().isEmpty()) continue;
                
                List<String> typesInStatement = new ArrayList<>();
                for (String type : new String[]{"int", "float", "double", "char", "bool", "long", "short", 
                                               "unsigned", "signed", "string", "void", "auto", "size_t"}) {
                    Pattern pattern = Pattern.compile("\\b" + type + "\\b");
                    Matcher matcher = pattern.matcher(statement);
                    if (matcher.find()) {
                        typesInStatement.add(type);
                    }
                }
                
                // Check if this individual statement has multiple data types
                if (typesInStatement.size() > 1) {
                    hasMultipleDeclarations = true;
                    break;
                }
            }
            
            if (!hasMultipleDeclarations) {
                return;
            }
        }
    
        // Original check for multiple data types in a single declaration
        String[] types = {"int", "float", "double", "char", "bool", "long", "short",
                         "unsigned", "signed", "string", "void", "auto", "size_t"};
    
        List<String> foundTypes = new ArrayList<>();
        for (String type : types) {
            Pattern pattern = Pattern.compile("\\b" + type + "\\b");
            Matcher matcher = pattern.matcher(line);
    
            while (matcher.find()) {
                int position = matcher.start();
                if (line.substring(0, position).contains("//")) {
                    continue;
                }
                foundTypes.add(type);
            }
        }
    
        if (foundTypes.size() > 1) {
            // Check for valid combinations
            if (foundTypes.size() == 2) {
                if (foundTypes.contains("long") && foundTypes.contains("long")) {
                    return;
                }
                if (foundTypes.contains("unsigned") && foundTypes.contains("int")) {
                    return;
                }
                if (foundTypes.contains("unsigned") && foundTypes.contains("short")) {
                    return;
                }
                if ((foundTypes.contains("unsigned") && foundTypes.contains("long"))) {
                    return;
                }
            }
    
            outputArea.append("Line " + lineNumber + ": Error - Multiple data types in single declaration: " +
                             String.join(", ", foundTypes) + "\n");
            skipRemainingChecksForLine = true;
        }
    }

    private static void checkEmptyInitialization(String line, int lineNumber, JTextArea outputArea) {
        if (line.matches(".*=\\s*;") || line.matches(".*=[^;]*;\\s*$") && line.matches(".*=\\s*;\\s*$")) {
            outputArea.append("Line " + lineNumber + ": Syntax error - empty initialization or assignment (missing right-hand side).\n");
        }
    }

    private static void checkSyntaxErrors(String line, int lineNumber, JTextArea outputArea) {
        boolean inStringLiteral = false;
        boolean inCharLiteral = false;
        boolean inComment = false;
        boolean inPreprocessor = line.trim().startsWith("#");

        Map<Character, Set<String>> validContexts = new HashMap<>();
        validContexts.put('!', Set.of("operator", "conditional"));
        validContexts.put('@', Set.of("preprocessor", "annotation"));
        validContexts.put('#', Set.of("preprocessor", "stringizing"));
        validContexts.put('$', Set.of("identifier"));
        validContexts.put('%', Set.of("operator", "formatstring"));
        validContexts.put('^', Set.of("operator"));
        validContexts.put('&', Set.of("operator", "reference", "address"));
        validContexts.put('*', Set.of("operator", "pointer", "dereference", "multiplication"));
        validContexts.put('(', Set.of("grouping", "call", "precedence"));
        validContexts.put(')', Set.of("grouping", "call", "precedence"));
        validContexts.put('_', Set.of("identifier"));
         validContexts.put('+', Set.of("operator", "addition", "increment"));
         validContexts.put('-', Set.of("operator", "subtraction", "decrement", "negative"));
        validContexts.put('=', Set.of("assignment", "comparison", "initialization"));
        validContexts.put(':', Set.of("label", "scope", "ternary", "foreach"));
        validContexts.put(';', Set.of("statement_end"));
        validContexts.put('{', Set.of("block_start"));
        validContexts.put('}', Set.of("block_end"));
        validContexts.put('[', Set.of("array"));
        validContexts.put(']', Set.of("array"));
        validContexts.put('<', Set.of("comparison", "template", "stream"));
        validContexts.put('>', Set.of("comparison", "template", "stream"));
        validContexts.put('?', Set.of("ternary"));
        validContexts.put(',', Set.of("separator"));
        validContexts.put('.', Set.of("member", "decimal"));
        validContexts.put('/', Set.of("operator", "division", "comment"));
        validContexts.put('\\', Set.of("escape", "line_continuation"));
        validContexts.put('|', Set.of("operator", "bitwise"));
        validContexts.put('`', Set.of());
        validContexts.put('~', Set.of("operator", "destructor", "bitwise"));

        if (line.contains(":") && !line.matches(".*\\bfor\\s*\\(.*:.*\\).*")) {
            outputArea.append("Line " + lineNumber + ": Unexpected colon detected. Check syntax.\n");
        }

        if (line.contains("@") && !inStringLiteral && !inCharLiteral && !inComment) {
            outputArea.append("Line " + lineNumber + ": Unexpected '@' symbol detected. This is not standard C++ syntax.\n");
        }

        if (line.contains("$") && !inStringLiteral && !inCharLiteral && !inComment) {
            outputArea.append("Line " + lineNumber + ": Unexpected '$' symbol detected. This is not standard C++ syntax.\n");
        }

        if (line.contains("`") && !inStringLiteral && !inCharLiteral && !inComment) {
            outputArea.append("Line " + lineNumber + ": Unexpected '`' symbol detected. This is not standard C++ syntax.\n");
        }

        Pattern strayPunctuation = Pattern.compile(
            "(?<![\"'\\w\\s\\\\])" +    // Not preceded by word, space, \ or quote
            "([!#%^&*\\[\\]|~]|(?<!\\+)\\+(?!\\+)|(?<!-)\\-(?!-))" + // Match allowed punctuations; + only if not part of ++, - only if not part of --
            "(?![\"'\\w\\s\\\\])"       // Not followed by word, space, \ or quote
        );
        Matcher strayMatcher = strayPunctuation.matcher(line);
        if (strayMatcher.find() && !inStringLiteral && !inCharLiteral && !inComment) {
            outputArea.append("Line " + lineNumber + ": Unexpected stray character '" + strayMatcher.group(1) + "' detected. Check syntax.\n");
        }

        Pattern invalidSequence = Pattern.compile("(?<![=<>!&|+-])([#%^&*+-])\\1{2,}(?![=<>!&|+-])");
        Matcher invalidSequenceMatcher = invalidSequence.matcher(line);
        if (invalidSequenceMatcher.find() && !inStringLiteral && !inCharLiteral && !inComment) {
            outputArea.append("Line " + lineNumber + ": Invalid sequence of special characters '" +
                             invalidSequenceMatcher.group(0) + "' detected. Check syntax.\n");
        }

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && (i == 0 || line.charAt(i-1) != '\\') && !inCharLiteral && !inComment) {
                inStringLiteral = !inStringLiteral;
            } else if (c == '\'' && (i == 0 || line.charAt(i-1) != '\\') && !inStringLiteral && !inComment) {
                inCharLiteral = !inCharLiteral;
            } else if (i < line.length() - 1 && c == '/' && line.charAt(i+1) == '/' && !inStringLiteral && !inCharLiteral) {
                inComment = true;
            }

            if (inStringLiteral || inCharLiteral || inComment) {
                continue;
            }

            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && validContexts.containsKey(c)) {
                if (c == '!' && i < line.length() - 1 && !isValidNotOperatorContext(line, i, outputArea)) {
                    outputArea.append("Line " + lineNumber + ": Unexpected '!' symbol in this context. Check syntax.\n");
                } else if (c == '#' && !inPreprocessor && i > 0 && !isValidStringizingOperator(line, i)) {
                    outputArea.append("Line " + lineNumber + ": Unexpected '#' symbol outside preprocessor directive. Check syntax.\n");
                } else if (c == '$' && !isValidInIdentifier(line, i)) {
                    outputArea.append("Line " + lineNumber + ": '$' is not standard in C++ identifiers. Check syntax.\n");
                }
            }
        }
    }

    private static boolean isValidNotOperatorContext(String line, int position, JTextArea outputArea) {
        return position < line.length() - 1 &&
               (line.charAt(position + 1) == '=' ||
                Character.isLetterOrDigit(line.charAt(position + 1)) ||
                line.charAt(position + 1) == '(' ||
                line.charAt(position + 1) == ' ');
    }

    private static boolean isValidStringizingOperator(String line, int position) {
        return line.trim().startsWith("#define") &&
               position > line.indexOf("#define") + 7;
    }

    private static boolean isValidInIdentifier(String line, int position) {
        if (position > 0 && position < line.length() - 1) {
            return Character.isLetterOrDigit(line.charAt(position - 1)) ||
                   line.charAt(position - 1) == '_' ||
                   Character.isLetterOrDigit(line.charAt(position + 1)) ||
                   line.charAt(position + 1) == '_';
        }
        return false;
    }

    private static void checkIdentifiersWithoutKeywords(String line, int lineNumber, JTextArea outputArea) {
        if (line.matches("^\\s*(int|void|float|double|char|bool|long|short|unsigned)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\(.*")) {
            return;
        }
      
        if (line.matches("^\\s*(int|float|double|char|bool|long|short|unsigned)\\s+.*")) {
            return;
        }

        if (line.matches("^\\s*(if|else)\\s*.*")) {
            return;
        }

        if (line.contains("main")) {
            Pattern mainPattern = Pattern.compile("\\bmain\\b");
            Matcher mainMatcher = mainPattern.matcher(line);
            if (mainMatcher.find()) {
                // Replace 'main' with a placeholder to avoid triggering identifier warnings
                line = line.replaceAll("\\bmain\\b", "MAIN_FUNCTION_PLACEHOLDER");
            }
        }

        Pattern identPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = identPattern.matcher(line);

        while (matcher.find()) {
            String identifier = matcher.group(1);
            if (cppKeywords.contains(identifier)) continue;

            if (!declaredVariables.containsKey(identifier)) {
                outputArea.append("Line " + lineNumber + ": Identifier '" + identifier + "' used without declaration.\n");
            }
        }
    }

    private static boolean isValidVariableName(String name, int lineNumber, JTextArea outputArea) {
        // Variable name should already be extracted without = or value
        
        if (cppKeywords.contains(name)) {
            outputArea.append("Line " + lineNumber + ": Cannot use reserved keyword '" + name + "' as variable name.\n");
            return false;
        }
        
        if (!name.matches("^[a-zA-Z_].*")) {
            outputArea.append("Line " + lineNumber + ": Variable name '" + name + "' must begin with a letter or underscore.\n");
            return false;
        }
        
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            outputArea.append("Line " + lineNumber + ": Variable name '" + name + "' contains invalid characters. Only letters, digits, and underscores are allowed.\n");
            return false;
        }
        
        return true;
    }


    private static void checkMultipleDeclarations(String line, int lineNumber, JTextArea outputArea) {
        for (String type : new String[]{"int", "float", "double", "char", "bool", "long", "long long", "short", "unsigned int", "string"}) {
            Pattern pattern = Pattern.compile("^\\s*" + type + "\\s+([^;]+);");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String declarations = matcher.group(1);
                String[] vars = declarations.split(",");

                if (vars.length <= 1 && !declarations.contains(",")) {
                    return;
                }

                for (String var : vars) {
                    var = var.trim();
                    if (var.contains("=")) {
                        String[] parts = var.split("=", 2);
                        String varName = parts[0].trim();
                        String value = parts[1].trim();

                        if (!isValidVariableName(varName, lineNumber, outputArea)) {
                            continue;
                        }

                        if (declaredVariables.containsKey(varName)) {
                            // outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' is already declared.\n");
                        } else {
                            declaredVariables.put(varName, new VariableInfo(type, true));
                            if (!isValidValue(type, value, outputArea)) {
                                outputArea.append("Line " + lineNumber + ": Invalid initialization value for variable '" + varName + "' of type " + type + ".\n");
                            }
                        }
                    } else {
                        String varName = var.trim();
                        if (!isValidVariableName(varName, lineNumber, outputArea)) {
                            continue;
                        }

                        if (declaredVariables.containsKey(varName)) {
                            // outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' is already declared.\n");
                        } else {
                            declaredVariables.put(varName, new VariableInfo(type, false));
                        }
                    }
                }
                return;
            }
        }
    }

    private static void checkDeclaration(String line, int lineNumber, JTextArea outputArea) {
        if (line.contains(",")) {
            return;
        }
    
        // First check for malformed declarations like "int;" without variable name
        Pattern malformedPattern = Pattern.compile("^\\s*(int|float|double|char|bool|long long|long|short|unsigned int|string)\\s*;\\s*$");
        Matcher malformedMatcher = malformedPattern.matcher(line);
        
        if (malformedMatcher.find()) {
            String dataType = malformedMatcher.group(1);
            outputArea.append("Line " + lineNumber + ": Error - Declaration of '" + dataType + "' without variable name.\n");
            return;
        }
    
        // Modified pattern to handle declarations with or without initialization
        Pattern pattern = Pattern.compile("^\\s*(int|float|double|char|bool|long long|long|short|unsigned int|string)\\s+(\\w+)\\s*(=\\s*\\S+)?\\s*;\\s*$");
        Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            String varType = matcher.group(1);
            String varName = matcher.group(2);
            boolean initialized = matcher.group(3) != null; // Check if initialization is present
    
            System.out.println("DEBUG: Found declaration - " + varType + " " + varName); 
    
            if (!isValidVariableName(varName, lineNumber, outputArea)) {
                return;  
            }
    
            if (declaredVariables.containsKey(varName)) {
                // outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' is already declared.\n");
            } else {
                declaredVariables.put(varName, new VariableInfo(varType, initialized));
                System.out.println("DEBUG: Added to symbol table - " + varName + " (initialized: " + initialized + ")");
            }
        }
    }

    private static void checkInitialization(String line, int lineNumber, JTextArea outputArea) {
        if (line.contains(",")) {
            return;
        }

        Pattern pattern = Pattern.compile("^\\s*(int|float|double|char|bool|long long|long|short|unsigned int|string)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^;]+);\\s*$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String type = matcher.group(1);
            String varName = matcher.group(2);
            String value = matcher.group(3).trim();

            if (!isValidVariableName(varName, lineNumber, outputArea)) {
                return;
            }

            if (declaredVariables.containsKey(varName)) {
                // outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' is already declared.\n");
            } else {
                declaredVariables.put(varName, new VariableInfo(type, true));
                if (!isValidValue(type, value, outputArea)) {
                    outputArea.append("Line " + lineNumber + ": Invalid initialization value for type " + type + ".\n");
                }
            }
        }
    }

    private static void checkAssignmentOperators(String line, int lineNumber, JTextArea outputArea) {
        for (String op : assignmentOperators) {
            Pattern pattern = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\" + op + "\\s*(.+);\\s*$");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String varName = matcher.group(1);
                String value = matcher.group(2).trim();

                if (!declaredVariables.containsKey(varName)) {
                    outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used before declaration.\n");
                } else {
                    VariableInfo info = declaredVariables.get(varName);
                    if (!op.equals("=") && !info.initialized) {
                        outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used in " + op + " before initialization.\n");
                    }

                    info.initialized = true;
                    if (containsNestedAssignment(value)) {
                        outputArea.append("Line " + lineNumber + ": Complex nested assignment detected. This may lead to confusion: " + value + "\n");
                    }

                    if (!isValidValue(info.type, value, outputArea)) {
                        outputArea.append("Line " + lineNumber + ": Invalid value for variable of type " + info.type + ".\n");
                    }
                }
                return;
            }
        }
    }

    private static boolean containsNestedAssignment(String value) {
        for (String op : assignmentOperators) {
            if (value.contains(op)) {
                return true;
            }
        }
        return false;
    }

    private static void checkProblematicOperators(String line, int lineNumber, JTextArea outputArea) {
        // Original checks
        if (line.matches(".*\\+\\+\\+.*") || line.matches(".*---.*")) {
            outputArea.append("Line " + lineNumber + ": Syntax error - invalid multiple increment/decrement operators.\n");
        }
    
        if (line.matches(".*\\+\\+--.*") || line.matches(".*--\\+\\+.*")) {
            outputArea.append("Line " + lineNumber + ": Confusing operator sequence detected (++-- or --++). This may lead to unexpected behavior.\n");
        }
    
        if (line.matches(".*[=]\\s*-\\s*-\\s*-\\s*-.*")) {
            outputArea.append("Line " + lineNumber + ": Misleading sequence of unary minus operators. This could be parsed incorrectly.\n");
        }
    
        if (line.matches(".*\\*&.*") || line.matches(".*&\\*.*")) {
            outputArea.append("Line " + lineNumber + ": Potentially invalid operator combination (*& or &*).\n");
        }
    
        if (line.matches(".*\\*\\*.*") || line.matches(".*/\\*.*") || line.matches(".*/\\+.*") ||
            line.matches(".*\\+/.*") || line.matches(".*\\+-.*") || line.matches(".*-\\+.*")) {
            outputArea.append("Line " + lineNumber + ": Invalid or confusing consecutive arithmetic operators detected.\n");
        }
        
        // First check for valid increment/decrement operations
        boolean foundValidIncDec = false;
        
        // Check for valid post-increment/decrement (most common)
        Pattern postIncDec = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*(\\+\\+|--)");
        Matcher postMatch = postIncDec.matcher(line);
        
        while (postMatch.find()) {
            foundValidIncDec = true;
            String varName = postMatch.group(1);
            
            // Inline validation logic
            if (!declaredVariables.containsKey(varName)) {
                outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used with increment/decrement operator before declaration.\n");
            } else {
                VariableInfo info = declaredVariables.get(varName);
                if (!info.initialized) {
                    outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used with increment/decrement operator before initialization.\n");
                    // Mark as initialized since it's being assigned a value
                    info.initialized = true;
                }
                
                // Check if the variable type is compatible with increment/decrement
                if (!info.type.equals("int") && !info.type.equals("long") && !info.type.equals("float") &&
                    !info.type.equals("double") && !info.type.equals("short") && !info.type.equals("byte")) {
                    outputArea.append("Line " + lineNumber + ": Increment/decrement operator used on non-numeric type '" + info.type + "'.\n");
                }
            }
        }
        
        // Check for valid pre-increment/decrement
        Pattern preIncDec = Pattern.compile("(\\+\\+|--)\\s*([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher preMatch = preIncDec.matcher(line);
        
        while (preMatch.find()) {
            foundValidIncDec = true;
            String varName = preMatch.group(2);
            
            // Inline validation logic
            if (!declaredVariables.containsKey(varName)) {
                outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used with increment/decrement operator before declaration.\n");
            } else {
                VariableInfo info = declaredVariables.get(varName);
                if (!info.initialized) {
                    outputArea.append("Line " + lineNumber + ": Variable '" + varName + "' used with increment/decrement operator before initialization.\n");
                    // Mark as initialized since it's being assigned a value
                    info.initialized = true;
                }
                
                // Check if the variable type is compatible with increment/decrement
                if (!info.type.equals("int") && !info.type.equals("long") && !info.type.equals("float") &&
                    !info.type.equals("double") && !info.type.equals("short") && !info.type.equals("byte")) {
                    outputArea.append("Line " + lineNumber + ": Increment/decrement operator used on non-numeric type '" + info.type + "'.\n");
                }
            }
        }
        
        // Only check for standalone increment/decrement if we didn't find valid ones
        if (!foundValidIncDec && (line.contains("++") || line.contains("--"))) {
            // Skip for loops as they commonly use these operators
            if (line.contains("for(") || line.contains("for (")) {
                return;
            }
            
            // Check for increment/decrement at the beginning of the line
            String trimmedLine = line.trim();
            if ((trimmedLine.startsWith("++") || trimmedLine.startsWith("--"))) {
                // If it starts with ++ or -- but doesn't have a variable after it
                if (!trimmedLine.matches("(\\+\\+|--)\\s*[a-zA-Z_].*")) {
                    outputArea.append("Line " + lineNumber + ": Increment/decrement operator missing a variable.\n");
                }
            } 
            // Check for standalone operators elsewhere
            else if (line.matches(".*\\+\\+\\s*;.*") || line.matches(".*--\\s*;.*")) {
                outputArea.append("Line " + lineNumber + ": Potentially invalid increment/decrement operation.\n");
            }
        }
        
        // Check for dangling operators
        Pattern danglingOp = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*([+\\-*/&|^%])\\s*;");
        Matcher danglingMatch = danglingOp.matcher(line);
        
        if (danglingMatch.find()) {
            String op = danglingMatch.group(2);
            outputArea.append("Line " + lineNumber + ": Incomplete expression with dangling operator '" + op + "'.\n");
        }
    }

    private static void checkStringArithmeticOperations(String line, int lineNumber, JTextArea outputArea) {
        Pattern pattern = Pattern.compile("(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b)\\s*([+\\-*/])\\s*(\\b[a-zA-Z_][a-zA-Z0-9_]*\\b|\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            String leftOperand = matcher.group(1);
            String operator = matcher.group(2);
            String rightOperand = matcher.group(3);

            if (declaredVariables.containsKey(leftOperand) &&
                declaredVariables.get(leftOperand).type.equals("string")) {
                boolean rightIsNumeric = false;
                if (rightOperand.matches("-?\\d+(\\.\\d+)?")) {
                    rightIsNumeric = true;
                } else if (declaredVariables.containsKey(rightOperand)) {
                    String rightType = declaredVariables.get(rightOperand).type;
                    if (rightType.equals("int") || rightType.equals("float") ||
                        rightType.equals("double") || rightType.equals("long") ||
                        rightType.equals("short") || rightType.equals("unsigned int") ||
                        rightType.equals("long long")) {
                        rightIsNumeric = true;
                    }
                }

                if (rightIsNumeric) {
                    outputArea.append("Line " + lineNumber + ": Error - Invalid arithmetic operation: string " +
                                     operator + " numeric value is not allowed.\n");
                }
            }

            if (declaredVariables.containsKey(rightOperand) &&
                declaredVariables.get(rightOperand).type.equals("string")) {
                boolean leftIsNumeric = false;
                if (leftOperand.matches("-?\\d+(\\.\\d+)?")) {
                    leftIsNumeric = true;
                } else if (declaredVariables.containsKey(leftOperand)) {
                    String leftType = declaredVariables.get(leftOperand).type;
                    if (leftType.equals("int") || leftType.equals("float") ||
                        leftType.equals("double") || leftType.equals("long") ||
                        leftType.equals("short") || leftType.equals("unsigned int") ||
                        leftType.equals("long long")) {
                        leftIsNumeric = true;
                    }
                }

                if (leftIsNumeric) {
                    outputArea.append("Line " + lineNumber + ": Error - Invalid arithmetic operation: numeric value " +
                                     operator + " string is not allowed.\n");
                }
            }
        }
    }

    private static boolean checkIfElseStatements(String line, int lineNumber, JTextArea outputArea) {
        boolean foundIf = false;
        line = line.trim(); // Clean whitespace
    
        // Check for 'if' with proper parentheses
        if (line.matches("^if\\s*\\(.*\\)\\s*.*")) {
            foundIf = true;
            String condition = line.replaceAll("^if\\s*\\((.*)\\).*", "$1");
            checkConditionVariables(condition, lineNumber, outputArea);
    
            if (condition.trim().isEmpty()) {
                outputArea.append("Line " + lineNumber + ": Error - Empty condition in if statement.\n");
            }
    
            // Now check braces only if needed
            if (!line.contains("{") && !line.matches("^if\\s*\\(.*\\)\\s*[^;{]+;\\s*$")) {
                outputArea.append("Line " + lineNumber + ": Warning - Missing opening brace in if statement.\n");
            }
        }
        // Check for 'else if'
        else if (line.matches("^else\\s+if\\s*\\(.*\\)\\s*.*")) {
            foundIf = true;
            String condition = line.replaceAll("^else\\s+if\\s*\\((.*)\\).*", "$1");
            checkConditionVariables(condition, lineNumber, outputArea);
    
            if (condition.trim().isEmpty()) {
                outputArea.append("Line " + lineNumber + ": Error - Empty condition in else-if statement.\n");
            }
    
            if (!line.contains("{") && !line.matches("^else\\s+if\\s*\\(.*\\)\\s*[^;{]+;\\s*$")) {
                outputArea.append("Line " + lineNumber + ": Warning - Missing opening brace in else-if statement.\n");
            }
        }
        // Check for 'else'
        else if (line.matches("^else\\s*(\\{)?\\s*$")) {
            if (!line.contains("{")) {
                outputArea.append("Line " + lineNumber + ": Warning - Missing opening brace in else statement.\n");
            }
        }
        // Single line 'else' statement
        else if (line.matches("^else\\s+[^;{]+;\\s*$")) {
            outputArea.append("Line " + lineNumber + ": Single-line else statement detected without braces.\n");
        }
        // Bad syntax: if without parentheses
        else if (line.matches("^if\\s+[^\\(].*")) {
            outputArea.append("Line " + lineNumber + ": Syntax error - 'if' missing parentheses.\n");
        }
    
        return foundIf;
    }
    
    
    private static void checkConditionVariables(String condition, int lineNumber, JTextArea outputArea) {
        String cleanedCondition = condition.replaceAll("\"[^\"]*\"", "STRINGLITERAL")
                                         .replaceAll("\'[^\']*\'", "CHARLITERAL");

        Pattern identPattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b(?!\\s*\\()");
        Matcher matcher = identPattern.matcher(cleanedCondition);

        Set<String> checkedVariables = new HashSet<>();
        while (matcher.find()) {
            String identifier = matcher.group(1);
            if (checkedVariables.contains(identifier)) continue;
            checkedVariables.add(identifier);

            if (cppKeywords.contains(identifier)) continue;
            if (identifier.equals("true") || identifier.equals("false") || identifier.equals("null")) continue;

            if (!declaredVariables.containsKey(identifier)) {
                outputArea.append("Line " + lineNumber + ": Condition uses undeclared variable '" + identifier + "'.\n");
            } else if (!declaredVariables.get(identifier).initialized) {
                outputArea.append("Line " + lineNumber + ": Condition uses uninitialized variable '" + identifier + "'.\n");
            }
        }

        Pattern memberPattern = Pattern.compile("(\\b[a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.\\s*([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher memberMatcher = memberPattern.matcher(cleanedCondition);

        while (memberMatcher.find()) {
            String objectName = memberMatcher.group(1);
            if (cppKeywords.contains(objectName) ||
                objectName.equals("true") || objectName.equals("false") || objectName.equals("null")) {
                continue;
            }

            if (!declaredVariables.containsKey(objectName)) {
                outputArea.append("Line " + lineNumber + ": Condition uses undeclared object '" + objectName + "'.\n");
            } else if (!declaredVariables.get(objectName).initialized) {
                outputArea.append("Line " + lineNumber + ": Condition uses uninitialized object '" + objectName + "'.\n");
            }
        }
    }

    private static boolean isValidValue(String type, String value, JTextArea outputArea) {
        if (value.trim().isEmpty()) {
            return false;
        }

        if (declaredVariables.containsKey(value)) {
            VariableInfo info = declaredVariables.get(value);
            if (!info.initialized) {
                return false;
            }
            return isTypeCompatible(type, info.type);
        }

        if (value.contains("+") || value.contains("-") ||
            value.contains("*") || value.contains("/") ||
            value.contains("%")) {
            return true;
        }

        if (type.equals("bool") && (value.equals("true") || value.equals("false"))) {
            return true;
        }

        switch (type) {
            case "int":
            case "long":
            case "short":
            case "unsigned int":
                return value.matches("-?\\d+");
            case "long long":
                return value.matches("-?\\d+[lL]?");
            case "float":
            case "double":
                return value.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fF]?");
            case "char":
                return value.matches("'.'");
            case "bool":
                return value.equals("true") || value.equals("false") ||
                       value.equals("0") || value.equals("1");
            case "string":
                if (value.matches("\".*\"")) {
                    return true;
                }
                if (declaredVariables.containsKey(value)) {
                    return declaredVariables.get(value).type.equals("string");
                }
                return false;
            default:
                return false;
        }
    }

    private static boolean isTypeCompatible(String targetType, String sourceType) {
        if (targetType.equals(sourceType)) {
            return true;
        }

        if (targetType.equals("double") && (sourceType.equals("int") || sourceType.equals("float") ||
                                          sourceType.equals("long") || sourceType.equals("short") ||
                                          sourceType.equals("long long") || sourceType.equals("unsigned int"))) {
            return true;
        }

        if (targetType.equals("float") && (sourceType.equals("int") || sourceType.equals("short") ||
                                         sourceType.equals("long") || sourceType.equals("unsigned int"))) {
            return true;
        }

        if (targetType.equals("long long") && (sourceType.equals("int") || sourceType.equals("short") ||
                                             sourceType.equals("long") || sourceType.equals("unsigned int"))) {
            return true;
        }

        if (targetType.equals("long") && (sourceType.equals("int") || sourceType.equals("short") ||
                                        sourceType.equals("unsigned int"))) {
            return true;
        }

        if (targetType.equals("int") && (sourceType.equals("short"))) {
            return true;
        }

        if (targetType.equals("unsigned int") && (sourceType.equals("short"))) {
            return true;
        }

        if (targetType.equals("bool") && (sourceType.equals("int") || sourceType.equals("short") ||
                                        sourceType.equals("long") || sourceType.equals("long long") ||
                                        sourceType.equals("unsigned int"))) {
            return true;
        }

        if (targetType.equals("string") && sourceType.equals("char")) {
            return true;
        }

        if (targetType.equals("string") && (sourceType.equals("int") || sourceType.equals("float") ||
                                          sourceType.equals("double") || sourceType.equals("long") ||
                                          sourceType.equals("short") || sourceType.equals("unsigned int") ||
                                          sourceType.equals("long long") || sourceType.equals("bool"))) {
            return false;
        }

        return false;
    }
}
