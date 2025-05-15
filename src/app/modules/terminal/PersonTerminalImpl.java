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
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
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
        File cwd = getCurrentDirectory();
        appendOutput("$ " + (cwd != null ? cwd.getAbsolutePath() : ""));
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
        manual.put("exit", "exit\nExit the terminal (hides the panel).");
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
        cmds.put("help", new Command("Show help for a command or list all commands.", this::handleHelp));
        cmds.put("man", new Command("Show the manual entry for a command.", this::handleMan));
        cmds.put("echo", new Command("Echo the input arguments.", this::handleEcho));
        cmds.put("clear", new Command("Clear the terminal output.", _ -> clearOutput()));
        cmds.put("exit", new Command("Exit the terminal (hides the panel).", _ -> parentFrame.setVisible(false)));
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
        manual.put("exit", "exit\nExit the terminal (hides the panel).");
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
            return;
        }
        String path = args.get(0);
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
            if (!newDir.exists() || !newDir.isDirectory() || !isSubdirectory(newDir)) {
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
            appendOutput("Usage: add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]");
            return;
        }
        String first = args.get(0), last = args.get(1), dob = args.get(2);
        String gov = args.size() > 3 ? args.get(3) : "";
        String stu = args.size() > 4 ? args.get(4) : "";
        var result = manager.addPersonFromFields(first, last, dob, gov, stu);
        if (result.success) {
            appendOutput("Person added.");
        } else {
            appendOutput("Error: " + result.errorMessage);
        }
    }
    private void handleEdit(List<String> args) {
        if (args.size() < 4) {
            appendOutput("Usage: edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]");
            return;
        }
        try {
            int idx = Integer.parseInt(args.get(0)) - 1;
            String first = args.get(1), last = args.get(2), dob = args.get(3);
            String gov = args.size() > 4 ? args.get(4) : "";
            String stu = args.size() > 5 ? args.get(5) : "";
            var result = manager.updatePersonFromFields(idx, first, last, dob, gov, stu);
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

    @Override
    public void updateUI() {
        super.updateUI();
        applyThemeRecursively(this);
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
}