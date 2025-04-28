import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;

public class ScannerGUI {
    private JFrame frame;
    private JTextArea inputTextArea;
    private JTextArea symbolTableArea, tokenTableArea, errorConsoleArea;
    private JButton runButton, compileButton, scanButton, saveButton;
    private String lastDeclarationType;
    private JLabel statusBar;
    private StyleContext styleContext;
    private StyledDocument doc;
    private Style keywordStyle;
    private String lastTokenNeedingSemicolon = null;
    private JTextPane inputTextPane;
    private JTabbedPane tabbedPane;
    private TextLineNumber textLineNumber;

    // List of keywords to highlight
    private static final String[] KEYWORDS = {
        "int", "float", "double", "char", "string", "void",
        "if", "else", "for", "while", "do", "switch", "case", "default",
        "break", "continue", "return", "true", "false", "null","void","main()"
    };

    public ScannerGUI() {
        // Try Nimbus Look and Feel for a modern look
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // fallback to default
        }

        frame = new JFrame("W++ Lexical & Syntax Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setLayout(new BorderLayout(5, 5));

        // ===== MENU BAR =====
        JMenuBar menuBar = new JMenuBar();
        JButton newFile = new JButton("New File");
        saveButton = new JButton("Save As");
        JButton exit = new JButton("Exit");

        // Action listeners for menu items
        newFile.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(frame, 
                "Clear current content?", "New File", 
                JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                inputTextPane.setText("");
                tokenTableArea.setText("");
                symbolTableArea.setText("");
                errorConsoleArea.setText("");
            }
        });

        saveButton.addActionListener(e -> saveToFile());
        exit.addActionListener(e -> System.exit(0));

        menuBar.add(newFile);
        menuBar.add(saveButton);
        menuBar.add(exit);
        frame.setJMenuBar(menuBar);

        // ===== TOOLBAR =====
        JToolBar toolBar = new JToolBar();
        runButton = new JButton("Run");
        compileButton = new JButton("Compile");
        scanButton = new JButton("Scan & Analyze");

        // Toolbar button actions
        compileButton.addActionListener(e -> performCompilation());
        runButton.addActionListener(e -> JOptionPane.showMessageDialog(frame,
            "Run feature is under development.", "Info", JOptionPane.INFORMATION_MESSAGE));
        scanButton.addActionListener(e -> scanAndAnalyze());

        toolBar.add(runButton);
        toolBar.add(compileButton);
        toolBar.add(scanButton);
        frame.add(toolBar, BorderLayout.NORTH);

        // ===== TOP: SOURCE CODE AREA (in a panel) =====
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Source Code", TitledBorder.LEFT, TitledBorder.TOP));

        // Using JTextPane for syntax highlighting
        inputTextPane = new JTextPane();
        styleContext = new StyleContext();
        doc = new DefaultStyledDocument(styleContext);
        inputTextPane.setDocument(doc);
        inputTextPane.setFont(new Font("Monospaced", Font.PLAIN, 14));

        // Create syntax highlighting styles
        keywordStyle = styleContext.addStyle("KeywordStyle", null);
        StyleConstants.setForeground(keywordStyle, Color.BLUE);
        StyleConstants.setBold(keywordStyle, true);

        // Add document listener for real-time highlighting and status updates
        doc.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    highlightKeywords();
                    updateStatusBar();
                });
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    highlightKeywords();
                    updateStatusBar();
                });
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Plain text components don't fire these events
            }
        });

        inputTextArea = new JTextArea(); // Keep for compatibility
        JScrollPane inputScrollPane = new JScrollPane(inputTextPane);
        
        // Add line numbers to the editor
        textLineNumber = new TextLineNumber(inputTextPane);
        inputScrollPane.setRowHeaderView(textLineNumber);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // ===== BOTTOM: TABBED PANE =====
        tabbedPane = new FlatTabbedPane();

        // Create Token List Panel
        JPanel tokenPanel = new JPanel(new BorderLayout());
        tokenTableArea = new JTextArea();
        tokenTableArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        tokenTableArea.setEditable(false);
        tokenTableArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane tokenScrollPane = new JScrollPane(tokenTableArea);
        tokenPanel.add(tokenScrollPane, BorderLayout.CENTER);

        // Create Symbol Table Panel
        JPanel symbolPanel = new JPanel(new BorderLayout());
        symbolTableArea = new JTextArea();
        symbolTableArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        symbolTableArea.setEditable(false);
        symbolTableArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane symbolScrollPane = new JScrollPane(symbolTableArea);
        symbolPanel.add(symbolScrollPane, BorderLayout.CENTER);

        // Create Error Console Panel
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorConsoleArea = new JTextArea();
        errorConsoleArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        errorConsoleArea.setEditable(false);
        errorConsoleArea.setForeground(Color.RED);
        errorConsoleArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane errorScrollPane = new JScrollPane(errorConsoleArea);
        errorPanel.add(errorScrollPane, BorderLayout.CENTER);

        // Add tabs
        tabbedPane.addTab("Token List", tokenPanel);
        tabbedPane.addTab("Symbol Table", symbolPanel);
        tabbedPane.addTab("Error Console", errorPanel);

        // ===== MAIN SPLIT: Source Code (top) & Tabbed Pane (bottom) =====
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, tabbedPane);
        verticalSplit.setDividerLocation(350);
        verticalSplit.setResizeWeight(0.7); // Give more weight to the editor area
        frame.add(verticalSplit, BorderLayout.CENTER);

        // ===== STATUS BAR =====
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        statusBar = new JLabel(" Words: 0 | Characters: 0 | Lines: 1");
        statusBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        statusPanel.add(statusBar, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
        updateStatusBar();
    }

    private void updateStatusBar() {
        try {
            String text = doc.getText(0, doc.getLength());
            int lines = text.split("\n", -1).length;
            int chars = text.length();
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            statusBar.setText(String.format(" Words: %d | Characters: %d | Lines: %d", words, chars, lines));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void highlightKeywords() {
        try {
            String text = doc.getText(0, doc.getLength());
            doc.setCharacterAttributes(0, text.length(), styleContext.getStyle(StyleContext.DEFAULT_STYLE), true);

            for (String keyword : KEYWORDS) {
                Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keywordStyle, true);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void scanAndAnalyze() {
        errorConsoleArea.setText("");
        tokenTableArea.setText("");
        symbolTableArea.setText("");

        String input;
        try {
            input = doc.getText(0, doc.getLength());
            inputTextArea.setText(input);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return;
        }

        

        // Remove comments
        String commentRegex = "(?s)//[^\\r\\n]*|/\\*.*?\\*/";
        Pattern commentPattern = Pattern.compile(commentRegex);
        input = commentPattern.matcher(input).replaceAll("");

        String[] lines = input.split("\n");
        String regex = "\\b(int|float|double|char|string|if|else|for|while|do|break|continue|return|void)\\b" +
                      "|[+\\-*/%<>=!&|]{1,2}" +
                      "|\\d+(\\.\\d+)?" +
                      "|\'[^\']\'" +
                      "|\"([^\"]*)\"" +
                      "|[a-zA-Z_][a-zA-Z0-9_]*" +
                      "|[{}();,]|\\n";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        List<String> tokens = new ArrayList<>();
        List<Integer> tokenLines = new ArrayList<>();
        int currentLine = 1;

        while (matcher.find()) {
            String token = matcher.group();
            if (token.equals("\n")) {
                currentLine++;
                continue;
            }
            tokens.add(token);
            tokenLines.add(currentLine);
            
            // Token categorization
            if (token.matches("\\b(int|float|double|char|string|void)\\b")) {
                tokenTableArea.append("[DECLARATION]: " + token + " (Line " + currentLine + ")\n");
                lastDeclarationType = token;
            } else if (token.matches("\\b(if|else)\\b")) {
                tokenTableArea.append("[CONDITIONAL]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("\\b(for|while|do)\\b")) {
                tokenTableArea.append("[LOOP]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("\\b(break|continue|return)\\b")) {
                tokenTableArea.append("[CONTROL]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("[+\\-*/%<>=!&|]{1,2}")) {
                if (token.equals("==") || token.equals("!=") || token.equals("<") ||
                    token.equals(">") || token.equals("<=") || token.equals(">=")) {
                    tokenTableArea.append("[RELATIONAL_OPERATOR]: " + token + " (Line " + currentLine + ")\n");
                } else if (token.equals("+") || token.equals("-") || token.equals("*") ||
                           token.equals("/") || token.equals("%")) {
                    tokenTableArea.append("[ARITHMETIC_OPERATOR]: " + token + " (Line " + currentLine + ")\n");
                } else if (token.equals("=")) {
                    tokenTableArea.append("[ASSIGNMENT_OPERATOR]: " + token + " (Line " + currentLine + ")\n");
                } else if (token.equals("&&") || token.equals("||")) {
                    tokenTableArea.append("[LOGICAL_OPERATOR]: " + token + " (Line " + currentLine + ")\n");
                } else {
                    tokenTableArea.append("[OPERATOR]: " + token + " (Line " + currentLine + ")\n");
                }
            } else if (token.matches("\\d+(\\.\\d+)?")) {
                tokenTableArea.append("[NUMBER]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("\'[^\']\'")) {
                tokenTableArea.append("[CHAR_LITERAL]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("\"([^\"]*)\"")) {
                tokenTableArea.append("[STRING_LITERAL]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                tokenTableArea.append("[IDENTIFIER]: " + token + " (Line " + currentLine + ")\n");
            } else if (token.matches("[{}();,]")) {
                tokenTableArea.append("[SEPARATOR]: " + token + " (Line " + currentLine + ")\n");
            } else {
                tokenTableArea.append("[UNKNOWN]: " + token + " (Line " + currentLine + ")\n");
            }
        }

        
        //errorConsoleArea.setText("");
        // tokenTableArea.setText("");

        // Perform syntax analysis
        SyntaxAnalyzer analyzer = new SyntaxAnalyzer();
        SyntaxAnalyzer.analyzeSyntax(tokens, tokenLines, symbolTableArea, errorConsoleArea);
        SyntaxAnalyzer.analyze(input, errorConsoleArea); //ughhh
        tabbedPane.setSelectedIndex(0); // Show token list first // Show error console by default
 
         System.out.println("Symbol Table Contents: " + SyntaxAnalyzer.declaredVariables);
    }
  
    private void performCompilation() {
        scanAndAnalyze();
        if (errorConsoleArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                "Compilation successful. No syntax errors found.", 
                "Compilation Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            tabbedPane.setSelectedIndex(2); // Show error console
        }
    }

    private void saveToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save As");
        fileChooser.setFileFilter(new FileNameExtensionFilter("C++ Files", "cpp"));
        int userChoice = fileChooser.showSaveDialog(frame);

        if (userChoice == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".cpp")) {
                filePath += ".cpp";
                fileToSave = new File(filePath);
            }

            try {
                String content = doc.getText(0, doc.getLength());
                try (FileWriter writer = new FileWriter(fileToSave)) {
                    writer.write(content);
                    JOptionPane.showMessageDialog(frame,
                        "File saved successfully: " + filePath, "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException | BadLocationException e) {
                JOptionPane.showMessageDialog(frame,
                    "Error saving file: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScannerGUI());
    }

    class FlatTabbedPane extends JTabbedPane {
        public FlatTabbedPane() {
            super();
            setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
                private final Insets borderInsets = new Insets(0, 0, 0, 0);
                private final int cornerRadius = 10;

                @Override
                protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, 
                                             int x, int y, int w, int h, boolean isSelected) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                        RenderingHints.VALUE_ANTIALIAS_ON);

                    if (isSelected) {
                        g2d.setColor(new Color(70, 130, 180));
                        g2d.fillRoundRect(x, y, w, h + 5, cornerRadius, cornerRadius);
                        g2d.setColor(getBackground());
                        g2d.fillRect(x, y + h - 5, w, 5);
                    } else {
                        g2d.setColor(new Color(220, 220, 220));
                        g2d.fillRoundRect(x, y, w, h, cornerRadius, cornerRadius);
                    }
                    g2d.dispose();
                }

                @Override
                protected void paintTabBackground(Graphics g, int tabPlacement, 
                                                int tabIndex, int x, int y, 
                                                int w, int h, boolean isSelected) {
                    // Tab background is painted in the paintTabBorder method
                }

                @Override
                protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                    int width = tabPane.getWidth();
                    int height = tabPane.getHeight();
                    int x = 0;
                    int y = 0;

                    switch (tabPlacement) {
                        case TOP:
                            y = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                            height -= y;
                            break;
                        case BOTTOM:
                            height -= calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                            break;
                        case LEFT:
                            x = calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                            width -= x;
                            break;
                        case RIGHT:
                            width -= calculateTabAreaWidth(tabPlacement, runCount, maxTabWidth);
                            break;
                    }

                    g.setColor(new Color(70, 130, 180));
                    g.drawRect(x, y, width - 1, height - 1);
                }

                @Override
                protected Insets getContentBorderInsets(int tabPlacement) {
                    return borderInsets;
                }
            });

            setFont(new Font("SansSerif", Font.PLAIN, 12));
            setForeground(Color.BLACK);
            setBackground(Color.WHITE);
        }
    }
}

class TextLineNumber extends JPanel {
    private static final long serialVersionUID = 1L;
    private final static Color DEFAULT_BACKGROUND = new Color(235, 235, 235);
    private final static Color DEFAULT_FOREGROUND = Color.BLACK;
    private final static Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);
    private final static int MARGIN = 5;
    private final static int HEIGHT = Integer.MAX_VALUE - 1000000;
    private JTextComponent component;
    private int minimumDisplayDigits;
    private int lastDigits;
    private int lastHeight;
    private Font font;
    private Color foreground;
    private Color background;

    public TextLineNumber(JTextComponent component) {
        this(component, 3);
    }

    public TextLineNumber(JTextComponent component, int minimumDisplayDigits) {
        this.component = component;
        this.minimumDisplayDigits = minimumDisplayDigits;
        setFont(DEFAULT_FONT);
        setForeground(DEFAULT_FOREGROUND);
        setBackground(DEFAULT_BACKGROUND);
        setPreferredWidth();

        component.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                documentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                documentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                documentChanged();
            }
        });
    }

    private void documentChanged() {
        SwingUtilities.invokeLater(() -> {
            int width = getPreferredWidth();
            if (width != getPreferredSize().width) {
                setPreferredWidth();
            }
            repaint();
        });
    }

    private void setPreferredWidth() {
        Element root = component.getDocument().getDefaultRootElement();
        int lines = root.getElementCount();
        int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);
        if (lastDigits != digits) {
            lastDigits = digits;
            FontMetrics fontMetrics = getFontMetrics(getFont());
            int width = fontMetrics.charWidth('0') * digits + 2 * MARGIN;
            Dimension d = getPreferredSize();
            d.setSize(width, HEIGHT);
            setPreferredSize(d);
        }
    }

    private int getPreferredWidth() {
        Element root = component.getDocument().getDefaultRootElement();
        int lines = root.getElementCount();
        int digits = Math.max(String.valueOf(lines).length(), minimumDisplayDigits);
        FontMetrics fontMetrics = getFontMetrics(getFont());
        return fontMetrics.charWidth('0') * digits + 2 * MARGIN;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
        Rectangle clip = g.getClipBounds();
        int lineHeight = fontMetrics.getHeight();

        // Find the starting line number that is visible
        int linesOffset = 0;
        try {
            int startOffset = component.viewToModel2D(new Point(0, clip.y));
            int startLine = component.getDocument().getDefaultRootElement().getElementIndex(startOffset);
            linesOffset = startLine + 1;
        } catch (Exception e) {
            // Ignore
        }

        // Find the ending line number that is visible
        int endLine = 0;
        try {
            int endOffset = component.viewToModel2D(new Point(0, clip.y + clip.height));
            endLine = component.getDocument().getDefaultRootElement().getElementIndex(endOffset);
        } catch (Exception e) {
            endLine = component.getDocument().getDefaultRootElement().getElementCount() - 1;
        }

        // Draw the line numbers
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int line = linesOffset; line <= endLine + 1; line++) {
            String lineNumber = String.valueOf(line);
            int y = (line - linesOffset + 1) * lineHeight - fontMetrics.getDescent();
            int stringWidth = fontMetrics.stringWidth(lineNumber);
            int x = getSize().width - stringWidth - MARGIN;
            g2d.setColor(getForeground());
            g2d.drawString(lineNumber, x, y);
        }
    }
}
