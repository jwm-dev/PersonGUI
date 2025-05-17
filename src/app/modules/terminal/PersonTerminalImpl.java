package src.app.modules.terminal;

import src.app.AppController;
import src.app.dialogs.Dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;

/**
 * Implementation of the PTerminal API, combining UI and command logic.
 */
public class PersonTerminalImpl extends JPanel implements PTerminal {
    private final JTextArea outputArea;
    private final JTextField inputField;
    private final AppController manager;
    private final Dialogs operations;
    private final JFrame parentFrame;
    private final Map<String, Command> commandMap;
    private boolean scriptRunning = false;
    private final File rootDir = new File("data");
    private File currentDir = rootDir;
    private String motd = "PersonTerminal ready. Type 'help' for a list of commands.";
    private final JLabel promptLabel;
    private final JLabel cwdLabel;
    private JPanel inputPanel;
    private JScrollPane scrollPane;

    public PersonTerminalImpl(JFrame parentFrame, AppController manager, Dialogs operations) {
        super(new BorderLayout(5, 5));
        setBackground(UIManager.getColor("Terminal.background"));
        setForeground(UIManager.getColor("Terminal.foreground"));
        this.parentFrame = parentFrame;
        this.manager = manager;
        this.operations = operations;
        this.outputArea = new JTextArea(8, 40);
        this.inputField = new JTextField();
        this.outputArea.setEditable(false);
        this.outputArea.setFont(UIManager.getFont("TextArea.font"));
        this.outputArea.setBackground(UIManager.getColor("Terminal.background"));
        this.outputArea.setForeground(UIManager.getColor("Terminal.foreground"));
        this.outputArea.setLineWrap(false); // Disable line wrapping for ASCII art
        this.outputArea.setWrapStyleWord(false);
        this.outputArea.setTabSize(8); // Set tab size to 8 for ASCII art
        // Use Monospaced font if available in base Java
        this.outputArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Add cwdLabel at the top
        cwdLabel = new JLabel();
        cwdLabel.setFont(UIManager.getFont("Label.font"));
        cwdLabel.setForeground(UIManager.getColor("Terminal.accent"));
        cwdLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        updateCwdLabel();
        add(cwdLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.setBackground(UIManager.getColor("Terminal.background"));
        inputPanel.setForeground(UIManager.getColor("Terminal.foreground"));
        promptLabel = new JLabel("$ ");
        promptLabel.setFont(UIManager.getFont("Label.font"));
        promptLabel.setForeground(UIManager.getColor("Terminal.accent"));
        inputField.setBackground(UIManager.getColor("Terminal.background"));
        inputField.setForeground(UIManager.getColor("Terminal.foreground"));
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        this.commandMap = buildCommands();
        printWelcome();
        inputField.addActionListener(_ -> {
            String cmdLine = inputField.getText().trim();
            inputField.setText("");
            if (!cmdLine.isEmpty()) {
                handleCommand(cmdLine);
            }
        });
    }

    public void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public void clearOutput() {
        outputArea.setText("");
    }

    public void printWelcome() {
        appendOutput(motd);
        updateCwdLabel();
    }

    @Override
    public void executeCommand(String command) {
        handleCommand(command);
    }

    @Override
    public void runScript(File scriptFile) {
        if (scriptRunning) {
            appendOutput("Error: A script is already running");
            return;
        }
        scriptRunning = true;
        appendOutput("Running script: " + scriptFile.getName());
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                appendOutput("$ " + line);
                handleCommand(line);
            }
            appendOutput("Script execution complete: " + scriptFile.getName());
        } catch (Exception e) {
            appendOutput("Error reading script file: " + e.getMessage());
        } finally {
            scriptRunning = false;
        }
    }

    @Override
    public File getCurrentDirectory() {
        return currentDir;
    }

    @Override
    public JComponent getPanel() {
        return this;
    }

    public AppController getManager() { return manager; }
    public Dialogs getOperations() { return operations; }
    public JFrame getParentFrame() { return parentFrame; }

    @Override
    public JTextArea getOutputArea() {
        return outputArea;
    }

    @Override
    public JTextField getInputField() {
        return inputField;
    }

    @Override
    public JLabel getPromptLabel() {
        return promptLabel;
    }

    // Command handling logic (merged from PTerminalCommands)
    private void handleCommand(String cmdLine) {
        appendOutput("> " + cmdLine);
        List<String> tokens = tokenizeCommand(cmdLine);
        if (tokens.isEmpty()) return;
        String cmd = tokens.remove(0).toLowerCase();
        Command command = commandMap.get(cmd);
        if (command != null) {
            try {
                command.action.accept(tokens);
            } catch (Exception e) {
                appendOutput("Error executing command: " + e.getMessage());
            }
        } else {
            appendOutput("Unknown command: " + cmd);
        }
    }

    private Map<String, Command> buildCommands() {
        Map<String, Command> cmds = new LinkedHashMap<>();
        Map<String, String> manual = new HashMap<>();
        manual.put("help", "help [command]\nShow help for a command or list all commands.");
        manual.put("man", "man <command>\nShow the manual entry for a command.");
        manual.put("echo", "echo <text>\nEcho the input arguments.");
        manual.put("clear", "clear\nClear the terminal output.");
        manual.put("exit", "exit\nExit the terminal (kills the panel).");
        manual.put("pwd", "pwd\nPrint working directory (relative to /data).");
        manual.put("ls", "ls [dir]\nList files in the current directory or a subdirectory.");
        manual.put("cd", "cd <dir>\nChange directory (within /data only).");
        manual.put("list", "list\nList all people in the database.");
        manual.put("count", "count\nShow the number of people in the database.");
        manual.put("find", "find <name>\nFind people by name (case-insensitive).");
        manual.put("info", "info <index>\nShow detailed info for a person by index.");
        manual.put("add", "add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]\nAdd a person.");
        manual.put("edit", "edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]\nEdit a person.");
        manual.put("delete", "delete <index>\nDelete a person.");
        manual.put("indexof", "indexof <FirstName> <LastName>\nGet the index of a person by name.");
        manual.put("set", "set <KEY> <VALUE>\nSet a config property (THEME, SIDEBAR_WIDTH, etc.).");
        manual.put("get", "get <KEY>\nGet a config property value.");
        manual.put("saveconfig", "saveconfig\nSave the current config to disk.");
        manual.put("cat", "cat <filename>\nDisplay the contents of a file in the current directory.");
        manual.put("grep", "grep <pattern> [file]\nSearch for a pattern in the current People list or a file.");
        manual.put("config", "config [-d]\nShow the current config or the default config.");
        manual.put("motd", "motd [new message|-r|--reset]\nShow, set, or reset the terminal message of the day.");
        manual.put("mkdir", "mkdir <dir>\nCreate a new directory (not starting with a dot).");
        manual.put("rmdir", "rmdir <dir>\nRemove an empty directory (not starting with a dot).");
        manual.put("touch", "touch <file>\nCreate an empty file (not starting with a dot).");
        manual.put("rm", "rm <file>\nRemove a file (not starting with a dot).");
        manual.put("easteregg", "easteregg\nPlay the Star Wars ASCII movie. Press 'q' or Ctrl+C to quit.");
        cmds.put("help", new Command("Show help for a command or list all commands.", this::handleHelp));
        cmds.put("man", new Command("Show the manual entry for a command.", this::handleMan));
        cmds.put("echo", new Command("Echo the input arguments.", this::handleEcho));
        cmds.put("clear", new Command("Clear the terminal output.", _ -> clearOutput()));
        cmds.put("exit", new Command("Exit the terminal (closes the app).", _ -> System.exit(0)));
        cmds.put("pwd", new Command("Print working directory (relative to /data).", _ -> printWorkingDirectory()));
        cmds.put("ls", new Command("List files in the current directory or a subdirectory.", this::handleLs));
        cmds.put("cd", new Command("Change directory (within /data only).", this::handleCd));
        cmds.put("list", new Command("List all people in the database.", _ -> listPeople()));
        cmds.put("count", new Command("Show the number of people in the database.", _ -> countPeople()));
        cmds.put("find", new Command("Find people by name (case-insensitive).", this::handleFind));
        cmds.put("info", new Command("Show detailed info for a person by index.", this::handleInfo));
        cmds.put("add", new Command("Add a person.", this::handleAdd));
        cmds.put("edit", new Command("Edit a person.", this::handleEdit));
        cmds.put("delete", new Command("Delete a person.", this::handleDelete));
        cmds.put("indexof", new Command("Get the index of a person by name.", this::handleIndexOf));
        cmds.put("set", new Command("Set a config property (THEME, SIDEBAR_WIDTH, etc.).", this::handleSet));
        cmds.put("get", new Command("Get a config property value.", this::handleGet));
        cmds.put("saveconfig", new Command("Save the current config to disk.", _ -> saveConfig()));
        cmds.put("cat", new Command("Display the contents of a file in the current directory: cat <filename>", this::handleCat));
        cmds.put("grep", new Command("Search for a pattern in the current People list or a file: grep <pattern> [file]", this::handleGrep));
        cmds.put("config", new Command("Show the current config or the default config: config [-d]", this::handleConfig));
        cmds.put("motd", new Command("Show, set, or reset the terminal message of the day: motd [new message|-r|--reset]", this::handleMotd));
        cmds.put("mkdir", new Command("Create a new directory (not starting with a dot).", this::handleMkdir));
        cmds.put("rmdir", new Command("Remove an empty directory (not starting with a dot).", this::handleRmdir));
        cmds.put("touch", new Command("Create an empty file (not starting with a dot).", this::handleTouch));
        cmds.put("rm", new Command("Remove a file (not starting with a dot).", this::handleRm));
        cmds.put("easteregg", new Command("Play the Star Wars ASCII movie.", this::handleEasterEgg));
        return cmds;
    }

    private List<String> tokenizeCommand(String command) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        for (char c : command.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (currentToken.length() > 0) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                }
            } else {
                currentToken.append(c);
            }
        }
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        return tokens;
    }

    // Helper to check if a directory is within /data
    private boolean isSubdirectory(File dir) {
        try {
            String rootPath = rootDir.getCanonicalPath();
            String dirPath = dir.getCanonicalPath();
            return dirPath.equals(rootPath) || dirPath.startsWith(rootPath + File.separator);
        } catch (Exception e) {
            return false;
        }
    }

    // Command class (from PTerminalCommands)
    private static class Command {
        public final Consumer<List<String>> action;
        public Command(String description, Consumer<List<String>> action) {
            this.action = action;
        }
    }

    // --- Command handler implementations ---
    private void handleHelp(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Available commands: " + String.join(", ", commandMap.keySet()));
            appendOutput("Type 'man <command>' to see the manual entry for a command.");
        } else {
            handleMan(args);
        }
    }
    private void handleMan(List<String> args) {
        Map<String, String> manual = new HashMap<>();
        manual.put("help", "help [command]\nShow help for a command or list all commands.");
        manual.put("man", "man <command>\nShow the manual entry for a command.");
        manual.put("echo", "echo <text>\nEcho the input arguments.");
        manual.put("clear", "clear\nClear the terminal output.");
        manual.put("exit", "exit\nExit the terminal (kills the panel).");
        manual.put("pwd", "pwd\nPrint working directory (relative to /data).");
        manual.put("ls", "ls [dir]\nList files in the current directory or a subdirectory.");
        manual.put("cd", "cd <dir>\nChange directory (within /data only).");
        manual.put("list", "list\nList all people in the database.");
        manual.put("count", "count\nShow the number of people in the database.");
        manual.put("find", "find <name>\nFind people by name (case-insensitive).");
        manual.put("info", "info <index>\nShow detailed info for a person by index.");
        manual.put("add", "add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]\nAdd a person.");
        manual.put("edit", "edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]\nEdit a person.");
        manual.put("delete", "delete <index>\nDelete a person.");
        manual.put("indexof", "indexof <FirstName> <LastName>\nGet the index of a person by name.");
        manual.put("set", "set <KEY> <VALUE>\nSet a config property (THEME, SIDEBAR_WIDTH, etc.).");
        manual.put("get", "get <KEY>\nGet a config property value.");
        manual.put("saveconfig", "saveconfig\nSave the current config to disk.");
        manual.put("cat", "cat <filename>\nDisplay the contents of a file in the current directory.");
        manual.put("grep", "grep <pattern> [file]\nSearch for a pattern in the current People list or a file.");
        manual.put("config", "config [-d]\nShow the current config or the default config.");
        manual.put("motd", "motd [new message|-r|--reset]\nShow, set, or reset the terminal message of the day.");
        manual.put("mkdir", "mkdir <dir>\nCreate a new directory (not starting with a dot).");
        manual.put("rmdir", "rmdir <dir>\nRemove an empty directory (not starting with a dot).");
        manual.put("touch", "touch <file>\nCreate an empty file (not starting with a dot).");
        manual.put("rm", "rm <file>\nRemove a file (not starting with a dot).");
        manual.put("easteregg", "easteregg\nPlay the Star Wars ASCII movie. Press 'q' or Ctrl+C to quit.");
        if (args.isEmpty()) {
            appendOutput("Usage: man <command>");
            return;
        }
        String cmdName = args.get(0).toLowerCase();
        String entry = manual.get(cmdName);
        if (entry != null) {
            appendOutput(entry);
        } else {
            appendOutput("No manual entry for command: " + cmdName);
        }
    }
    private void handleEcho(List<String> args) {
        appendOutput(String.join(" ", args));
    }
    private void handleLs(List<String> args) {
        File dir = currentDir;
        if (!args.isEmpty()) {
            File candidate = new File(currentDir, args.get(0));
            try { candidate = candidate.getCanonicalFile(); } catch (Exception ignored) {}
            if (candidate.exists() && candidate.isDirectory() && isSubdirectory(candidate)) {
                dir = candidate;
            } else {
                appendOutput("No such directory: " + args.get(0));
                return;
            }
        }
        String[] files = dir.list();
        if (files != null) {
            Arrays.sort(files);
            for (String file : files) {
                if (file.startsWith(".")) continue; // Skip hidden files/directories
                File f = new File(dir, file);
                appendOutput((f.isDirectory() ? file + "/" : file));
            }
        } else {
            appendOutput("Unable to list files.");
        }
    }
    private void handleCd(List<String> args) {
        if (args.isEmpty()) {
            currentDir = rootDir;
            appendOutput("Changed to /data");
            updateCwdLabel();
            return;
        }
        String path = args.get(0);
        if (path.startsWith(".")) {
            appendOutput("No such directory: " + path);
            return;
        }
        File newDir;
        if (path.equals("..")) {
            newDir = currentDir.getParentFile();
            if (newDir == null || !isSubdirectory(newDir)) {
                appendOutput("Already at /data");
                return;
            }
        } else if (path.equals(".")) {
            newDir = currentDir;
        } else {
            newDir = new File(currentDir, path);
        }
        try {
            newDir = newDir.getCanonicalFile();
            if (!newDir.exists() || !newDir.isDirectory() || !isSubdirectory(newDir) || newDir.getName().startsWith(".")) {
                appendOutput("No such directory: " + path);
                return;
            }
            currentDir = newDir;
            String rel;
            try {
                rel = rootDir.getCanonicalFile().toPath().relativize(currentDir.toPath()).toString();
            } catch (Exception e) {
                rel = currentDir.getName();
            }
            appendOutput("Changed to /data" + (rel.isEmpty() ? "" : "/" + rel));
            updateCwdLabel();
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage());
        }
    }
    private void handleFind(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: find <name>");
            return;
        }
        String search = args.get(0).toLowerCase();
        var people = manager.getPeople();
        boolean found = false;
        for (int i = 0; i < people.size(); i++) {
            var p = people.get(i);
            if (p.getFirstName().toLowerCase().contains(search) || p.getLastName().toLowerCase().contains(search)) {
                appendOutput((i+1) + ". " + p.getFirstName() + " " + p.getLastName());
                found = true;
            }
        }
        if (!found) appendOutput("No matches found for: " + search);
    }
    private void handleInfo(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: info <index>");
            return;
        }
        try {
            int idx = Integer.parseInt(args.get(0)) - 1;
            var people = manager.getPeople();
            if (idx < 0 || idx >= people.size()) {
                appendOutput("Invalid index.");
                return;
            }
            var p = people.get(idx);
            appendOutput("First Name: " + p.getFirstName());
            appendOutput("Last Name: " + p.getLastName());
            appendOutput("DOB: " + (p.getDOB() != null ? p.getDOB().toString() : ""));
            if (p instanceof src.person.RegisteredPerson reg) {
                appendOutput("GovID: " + reg.getGovID());
                if (p instanceof src.person.OCCCPerson occc) {
                    appendOutput("StudentID: " + occc.getStudentID());
                }
            }
        } catch (Exception e) {
            appendOutput("Invalid index or error: " + e.getMessage());
        }
    }
    private void handleAdd(List<String> args) {
        if (args.size() < 3) {
            appendOutput("Usage: add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID] [Description] [Tags]");
            return;
        }
        String first = args.get(0), last = args.get(1), dob = args.get(2);
        String gov = args.size() > 3 ? args.get(3) : "";
        String stu = args.size() > 4 ? args.get(4) : "";
        String desc = args.size() > 5 ? args.get(5) : "";
        String tags = args.size() > 6 ? args.get(6) : "";
        var result = manager.addPersonFromFields(first, last, dob, gov, stu, desc, tags);
        if (result.success) {
            appendOutput("Person added.");
        } else {
            appendOutput("Error: " + result.errorMessage);
        }
    }
    private void handleEdit(List<String> args) {
        if (args.size() < 4) {
            appendOutput("Usage: edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID] [Description] [Tags]");
            return;
        }
        try {
            int idx = Integer.parseInt(args.get(0)) - 1;
            String first = args.get(1), last = args.get(2), dob = args.get(3);
            String gov = args.size() > 4 ? args.get(4) : "";
            String stu = args.size() > 5 ? args.get(5) : "";
            String desc = args.size() > 6 ? args.get(6) : "";
            String tags = args.size() > 7 ? args.get(7) : "";
            var result = manager.updatePersonFromFields(idx, first, last, dob, gov, stu, desc, tags);
            if (result.success) {
                appendOutput("Person updated.");
            } else {
                appendOutput("Error: " + result.errorMessage);
            }
        } catch (Exception e) {
            appendOutput("Invalid index or error: " + e.getMessage());
        }
    }
    private void handleDelete(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: delete <index>");
            return;
        }
        try {
            int idx = Integer.parseInt(args.get(0)) - 1;
            var people = manager.getPeople();
            if (idx < 0 || idx >= people.size()) {
                appendOutput("Invalid index.");
                return;
            }
            boolean ok = manager.deletePersonByIndex(idx);
            if (ok) {
                appendOutput("Person deleted.");
            } else {
                appendOutput("Delete failed.");
            }
        } catch (Exception e) {
            appendOutput("Invalid index or error: " + e.getMessage());
        }
    }
    private void handleIndexOf(List<String> args) {
        if (args.size() < 2) {
            appendOutput("Usage: indexof <FirstName> <LastName>");
            return;
        }
        String first = args.get(0).toLowerCase();
        String last = args.get(1).toLowerCase();
        var people = manager.getPeople();
        boolean found = false;
        for (int i = 0; i < people.size(); i++) {
            var p = people.get(i);
            if (p.getFirstName().toLowerCase().equals(first) && p.getLastName().toLowerCase().equals(last)) {
                appendOutput("Index: " + (i + 1));
                found = true;
                break;
            }
        }
        if (!found) {
            appendOutput("Person not found: " + args.get(0) + " " + args.get(1));
        }
    }
    private void handleSet(List<String> args) {
        if (args.size() < 2) {
            appendOutput("Usage: set <KEY> <VALUE>");
            return;
        }
        String key = args.get(0);
        String value = args.get(1);
        switch (key.toUpperCase()) {
            case "THEME" -> manager.setThemeName(value);
            case "SIDEBAR_WIDTH" -> manager.setSidebarWidth(Integer.parseInt(value));
            case "FILTER_WIDTH" -> manager.setFilterWidth(Integer.parseInt(value));
            case "LIST_TERMINAL_DIVIDER" -> manager.setListTerminalDivider(Double.parseDouble(value));
            case "WINDOW_WIDTH" -> manager.setWindowWidth(Integer.parseInt(value));
            case "WINDOW_HEIGHT" -> manager.setWindowHeight(Integer.parseInt(value));
            default -> {
                appendOutput("Unknown config key: " + key);
                return;
            }
        }
        appendOutput("Set " + key + " = " + value);
    }
    private void handleGet(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: get <KEY>");
            return;
        }
        String key = args.get(0);
        String value = switch (key.toUpperCase()) {
            case "THEME" -> manager.getThemeName();
            case "SIDEBAR_WIDTH" -> String.valueOf(manager.getSidebarWidth());
            case "FILTER_WIDTH" -> String.valueOf(manager.getFilterWidth());
            case "LIST_TERMINAL_DIVIDER" -> String.valueOf(manager.getListTerminalDivider());
            case "WINDOW_WIDTH" -> String.valueOf(manager.getWindowWidth());
            case "WINDOW_HEIGHT" -> String.valueOf(manager.getWindowHeight());
            default -> null;
        };
        if (value != null) {
            appendOutput(key + " = " + value);
        } else {
            appendOutput("Unknown config key: " + key);
        }
    }
    private void saveConfig() {
        manager.saveConfig(new java.io.File("data/.config/config"));
        appendOutput("Config saved.");
    }
    private void printWorkingDirectory() {
        try {
            String rel = rootDir.getCanonicalFile().toPath().relativize(currentDir.getCanonicalFile().toPath()).toString();
            appendOutput("/data" + (rel.isEmpty() ? "" : "/" + rel));
        } catch (Exception e) {
            appendOutput("/data");
        }
    }
    private void listPeople() {
        var people = manager.getPeople();
        if (people.isEmpty()) {
            appendOutput("No people in the database.");
        } else {
            for (int i = 0; i < people.size(); i++) {
                var p = people.get(i);
                appendOutput((i+1) + ". " + p.getFirstName() + " " + p.getLastName());
            }
        }
    }
    private void countPeople() {
        var people = manager.getPeople();
        appendOutput("People in database: " + people.size());
    }

    // --- Additional command handlers ---
    private void handleCat(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: cat <filename>");
            return;
        }
        String filename = args.get(0);
        File file = new File(currentDir, filename);
        try {
            file = file.getCanonicalFile();
            if (!file.exists() || !file.isFile() || !isSubdirectory(file.getParentFile())) {
                appendOutput("File not found or not allowed: " + filename);
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendOutput(line);
                }
            }
        } catch (Exception e) {
            appendOutput("Error reading file: " + e.getMessage());
        }
    }

    private void handleGrep(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: grep <pattern> [file]");
            return;
        }
        String pattern = args.get(0).toLowerCase();
        if (args.size() == 1) {
            // Search People list
            var people = manager.getPeople();
            boolean found = false;
            for (int i = 0; i < people.size(); i++) {
                var p = people.get(i);
                String personStr = p.getFirstName() + " " + p.getLastName();
                if (personStr.toLowerCase().contains(pattern) || (p.getDOB() != null && p.getDOB().toString().toLowerCase().contains(pattern))) {
                    appendOutput((i+1) + ". " + personStr);
                    found = true;
                }
            }
            if (!found) appendOutput("No matches found in People list for: " + pattern);
        } else {
            // Search file
            String filename = args.get(1);
            File file = new File(currentDir, filename);
            try {
                file = file.getCanonicalFile();
                if (!file.exists() || !file.isFile() || !isSubdirectory(file.getParentFile())) {
                    appendOutput("File not found or not allowed: " + filename);
                    return;
                }
                boolean found = false;
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    int lineNum = 1;
                    while ((line = reader.readLine()) != null) {
                        if (line.toLowerCase().contains(pattern)) {
                            appendOutput(lineNum + ": " + line);
                            found = true;
                        }
                        lineNum++;
                    }
                }
                if (!found) appendOutput("No matches found in file for: " + pattern);
            } catch (Exception e) {
                appendOutput("Error reading file: " + e.getMessage());
            }
        }
    }

    private void handleConfig(List<String> args) {
        try {
            File configFile;
            if (!args.isEmpty() && args.get(0).equals("-d")) {
                configFile = new File("data/.config/config.default");
            } else {
                configFile = new File("data/.config/config");
            }
            configFile = configFile.getCanonicalFile();
            if (!configFile.exists() || !isSubdirectory(configFile.getParentFile())) {
                appendOutput("Config file not found: " + configFile.getName());
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendOutput(line);
                }
            }
        } catch (Exception e) {
            appendOutput("Error reading config: " + e.getMessage());
        }
    }

    private void handleMotd(List<String> args) {
        clearOutput();
        if (!args.isEmpty() && (args.get(0).equals("-r") || args.get(0).equals("--reset"))) {
            motd = "PersonTerminal ready. Type 'help' for a list of commands.";
            appendOutput("MOTD reset to default.");
        } else if (args.isEmpty()) {
            appendOutput(motd);
        } else {
            motd = String.join(" ", args);
            appendOutput("MOTD updated.");
        }
    }

    private void updateCwdLabel() {
        try {
            String rel = rootDir.getCanonicalFile().toPath().relativize(currentDir.getCanonicalFile().toPath()).toString();
            cwdLabel.setText("/data" + (rel.isEmpty() ? "" : "/" + rel));
        } catch (Exception e) {
            cwdLabel.setText("/data");
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyThemeRecursively(this);
        if (cwdLabel != null) {
            cwdLabel.setFont(UIManager.getFont("Label.font"));
            cwdLabel.setForeground(UIManager.getColor("Terminal.accent"));
        }
    }
    private void applyThemeRecursively(Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(UIManager.getColor("Terminal.background"));
            comp.setForeground(UIManager.getColor("Terminal.foreground"));
        } else if (comp instanceof JLabel) {
            comp.setForeground(UIManager.getColor("Terminal.accent"));
        } else if (comp instanceof JTextArea) {
            comp.setBackground(UIManager.getColor("Terminal.background"));
            comp.setForeground(UIManager.getColor("Terminal.foreground"));
        } else if (comp instanceof JTextField) {
            comp.setBackground(UIManager.getColor("Terminal.background"));
            comp.setForeground(UIManager.getColor("Terminal.foreground"));
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(UIManager.getColor("Terminal.background"));
            comp.setForeground(UIManager.getColor("Terminal.foreground"));
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }

    private void handleMkdir(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: mkdir <dir>");
            return;
        }
        String name = args.get(0);
        if (name.startsWith(".")) {
            appendOutput("Invalid directory name: " + name);
            return;
        }
        File dir = new File(currentDir, name);
        try {
            dir = dir.getCanonicalFile();
            if (!isSubdirectory(dir.getParentFile())) {
                appendOutput("Not allowed: " + name);
                return;
            }
            if (dir.exists()) {
                appendOutput("Directory already exists: " + name);
                return;
            }
            if (dir.mkdir()) {
                appendOutput("Directory created: " + name);
            } else {
                appendOutput("Failed to create directory: " + name);
            }
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage());
        }
    }
    private void handleRmdir(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: rmdir <dir>");
            return;
        }
        String name = args.get(0);
        if (name.startsWith(".")) {
            appendOutput("Invalid directory name: " + name);
            return;
        }
        File dir = new File(currentDir, name);
        try {
            dir = dir.getCanonicalFile();
            if (!isSubdirectory(dir.getParentFile())) {
                appendOutput("Not allowed: " + name);
                return;
            }
            if (!dir.exists() || !dir.isDirectory()) {
                appendOutput("No such directory: " + name);
                return;
            }
            String[] files = dir.list();
            if (files != null && files.length > 0) {
                appendOutput("Directory not empty: " + name);
                return;
            }
            if (dir.delete()) {
                appendOutput("Directory removed: " + name);
            } else {
                appendOutput("Failed to remove directory: " + name);
            }
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage());
        }
    }
    private void handleTouch(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: touch <file>");
            return;
        }
        String name = args.get(0);
        if (name.startsWith(".")) {
            appendOutput("Invalid file name: " + name);
            return;
        }
        File file = new File(currentDir, name);
        try {
            file = file.getCanonicalFile();
            if (!isSubdirectory(file.getParentFile())) {
                appendOutput("Not allowed: " + name);
                return;
            }
            if (file.exists()) {
                appendOutput("File already exists: " + name);
                return;
            }
            if (file.createNewFile()) {
                appendOutput("File created: " + name);
            } else {
                appendOutput("Failed to create file: " + name);
            }
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage());
        }
    }
    private void handleRm(List<String> args) {
        if (args.isEmpty()) {
            appendOutput("Usage: rm <file>");
            return;
        }
        String name = args.get(0);
        if (name.startsWith(".")) {
            appendOutput("Invalid file name: " + name);
            return;
        }
        File file = new File(currentDir, name);
        try {
            file = file.getCanonicalFile();
            if (!isSubdirectory(file.getParentFile())) {
                appendOutput("Not allowed: " + name);
                return;
            }
            if (!file.exists() || !file.isFile()) {
                appendOutput("No such file: " + name);
                return;
            }
            if (file.delete()) {
                appendOutput("File removed: " + name);
            } else {
                appendOutput("Failed to remove file: " + name);
            }
        } catch (Exception e) {
            appendOutput("Error: " + e.getMessage());
        }
    }
    private volatile Thread easterEggThread;
    private void handleEasterEgg(List<String> args) {
        if (easterEggThread != null && easterEggThread.isAlive()) {
            appendOutput("Easter egg is already playing!");
            return;
        }
        easterEggThread = new Thread(() -> {
            try {
                File file = new File("data/.assets/easter_egg.txt");
                if (!file.exists()) {
                    appendOutput("easter_egg.txt not found.");
                    return;
                }
                AsciiMoviePanel asciiPanel = new AsciiMoviePanel();
                asciiPanel.setBackground(outputArea.getBackground());
                asciiPanel.setForeground(outputArea.getForeground());
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    remove(cwdLabel);
                    remove(inputPanel);
                    remove(scrollPane);
                    add(asciiPanel, BorderLayout.CENTER);
                    revalidate();
                    repaint();
                });
                inputField.setEditable(false);
                inputField.setText("");
                asciiPanel.setFocusable(true);
                java.awt.event.KeyListener kl = new java.awt.event.KeyAdapter() {
                    @Override
                    public void keyPressed(java.awt.event.KeyEvent e) {
                        if (e.getKeyChar() == 'q' || e.getKeyCode() == java.awt.event.KeyEvent.VK_Q ||
                            (e.getKeyCode() == java.awt.event.KeyEvent.VK_C && e.isControlDown())) {
                            easterEggThread.interrupt();
                        }
                    }
                };
                asciiPanel.addKeyListener(kl);
                asciiPanel.requestFocusInWindow();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String lookahead = null;
                    while (true) {
                        String line = (lookahead != null) ? lookahead : reader.readLine();
                        lookahead = null;
                        if (line == null) break;
                        if (!line.matches("\\d+")) continue;
                        int delay = Integer.parseInt(line);
                        java.util.List<String> frameLines = new java.util.ArrayList<>();
                        // Skip exactly N blank lines, but if a non-blank is found, treat as frame content
                        int blanksSkipped = 0;
                        while (blanksSkipped < delay) {
                            reader.mark(4096);
                            String skipLine = reader.readLine();
                            if (skipLine == null) break;
                            if (skipLine.trim().isEmpty()) {
                                blanksSkipped++;
                            } else {
                                // Not blank, part of frame
                                reader.reset();
                                break;
                            }
                        }
                        // Collect all lines until next delay or EOF
                        while (true) {
                            reader.mark(4096);
                            String next = reader.readLine();
                            if (next == null) break;
                            if (next.matches("\\d+")) {
                                lookahead = next;
                                break;
                            }
                            frameLines.add(next);
                        }
                        java.util.List<String> frameCopy = new java.util.ArrayList<>(frameLines);
                        javax.swing.SwingUtilities.invokeLater(() -> asciiPanel.setFrame(frameCopy));
                        Thread.sleep(delay * 50L);
                        if (lookahead == null) break;
                    }
                } catch (InterruptedException ex) {
                    // interrupted by user
                } finally {
                    javax.swing.SwingUtilities.invokeAndWait(() -> {
                        remove(asciiPanel);
                        add(cwdLabel, BorderLayout.NORTH);
                        add(scrollPane, BorderLayout.CENTER);
                        add(inputPanel, BorderLayout.SOUTH);
                        revalidate();
                        repaint();
                        inputField.setEditable(true);
                        inputField.requestFocusInWindow();
                    });
                }
            } catch (Exception e) {
                appendOutput("Error playing easter egg: " + e.getMessage());
            }
        });
        easterEggThread.start();
    }

    // Custom panel for ASCII movie animation
    class AsciiMoviePanel extends JPanel {
        private java.util.List<String> frame = java.util.Collections.emptyList();
        private final Font font = new Font("Monospaced", Font.PLAIN, 14);
        public void setFrame(java.util.List<String> frame) {
            this.frame = frame;
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (frame == null || frame.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(getForeground());
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int charW = fm.charWidth('W');
            int charH = fm.getHeight();
            int frameRows = frame.size();
            int frameCols = frame.stream().mapToInt(String::length).max().orElse(0);
            // Center vertically and horizontally, but never negative
            int y0 = Math.max(0, (getHeight() - frameRows * charH) / 2) + fm.getAscent();
            int x0 = Math.max(0, (getWidth() - frameCols * charW) / 2);
            // Only render lines that fit in the panel
            for (int y = 0; y < frameRows && (y0 + y * charH) < getHeight(); y++) {
                String row = frame.get(y);
                for (int x = 0; x < row.length() && (x0 + x * charW) < getWidth(); x++) {
                    char c = row.charAt(x);
                    g2.drawString(String.valueOf(c), x0 + x * charW, y0 + y * charH);
                }
            }
        }
    }
}