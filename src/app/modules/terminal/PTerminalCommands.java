package src.app.modules.terminal;

import src.app.AppController;
import src.app.gui.Dialogs;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.function.Consumer;

/**
 * Handles command definitions, registry, and execution for the terminal.
 * Renamed from PersonTerminalCommands to PTerminalCommands
 */
public class PTerminalCommands {
    private final PTerminalPanel panel;
    private final Map<String, Command> commandMap;
    private boolean scriptRunning = false;
    private final File rootDir = new File("data");
    private File currentDir = rootDir;
    private String motd = "PersonTerminal ready. Type 'help' for a list of commands.";

    public PTerminalCommands(PTerminalPanel panel, AppController manager, Dialogs operations) {
        this.panel = panel;
        this.commandMap = buildCommands();
    }

    public void handleCommand(String cmdLine) {
        panel.appendOutput("> " + cmdLine);
        List<String> tokens = tokenizeCommand(cmdLine);
        if (tokens.isEmpty()) return;
        String cmd = tokens.remove(0).toLowerCase();
        Command command = commandMap.get(cmd);
        if (command != null) {
            try {
                command.action.accept(tokens);
            } catch (Exception e) {
                panel.appendOutput("Error executing command: " + e.getMessage());
            }
        } else {
            panel.appendOutput("Unknown command: " + cmd);
        }
    }

    public void runScript(File scriptFile) {
        if (scriptRunning) {
            panel.appendOutput("Error: A script is already running");
            return;
        }
        scriptRunning = true;
        panel.appendOutput("Running script: " + scriptFile.getName());
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                panel.appendOutput("$ " + line);
                handleCommand(line);
            }
            panel.appendOutput("Script execution complete: " + scriptFile.getName());
        } catch (Exception e) {
            panel.appendOutput("Error reading script file: " + e.getMessage());
        } finally {
            scriptRunning = false;
        }
    }

    private Map<String, Command> buildCommands() {
        Map<String, Command> cmds = new LinkedHashMap<>();
        // --- Command manual entries ---
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
        // Example: help command
        cmds.put("help", new Command("Show help for a command or list all commands", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Available commands: " + String.join(", ", cmds.keySet()));
                panel.appendOutput("Type 'man <command>' to see the manual entry for a command.");
            } else {
                String cmdName = args.get(0).toLowerCase();
                String entry = manual.get(cmdName);
                if (entry != null) {
                    panel.appendOutput(entry);
                } else {
                    panel.appendOutput("No manual entry for command: " + cmdName);
                }
            }
        }));
        cmds.put("man", new Command("Show the manual entry for a command: man <command>", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: man <command>");
                return;
            }
            String cmdName = args.get(0).toLowerCase();
            String entry = manual.get(cmdName);
            if (entry != null) {
                panel.appendOutput(entry);
            } else {
                panel.appendOutput("No manual entry for command: " + cmdName);
            }
        }));
        cmds.put("echo", new Command("Echo the input arguments", args -> {
            panel.appendOutput(String.join(" ", args));
        }));
        cmds.put("clear", new Command("Clear the terminal output", _ -> {
            panel.clearOutput();
        }));
        cmds.put("exit", new Command("Exit the terminal (hides the panel)", _ -> {
            panel.setVisible(false);
            panel.appendOutput("Terminal hidden. Use menu to reopen.");
        }));
        cmds.put("pwd", new Command("Print working directory", _ -> {
            try {
                String rel = rootDir.getCanonicalFile().toPath().relativize(currentDir.getCanonicalFile().toPath()).toString();
                panel.appendOutput("/data" + (rel.isEmpty() ? "" : "/" + rel));
            } catch (Exception e) {
                panel.appendOutput("/data");
            }
        }));
        cmds.put("ls", new Command("List files in the current directory or a subdirectory", args -> {
            File dir = currentDir;
            if (!args.isEmpty()) {
                File candidate = new File(currentDir, args.get(0));
                try { candidate = candidate.getCanonicalFile(); } catch (Exception ignored) {}
                if (candidate.exists() && candidate.isDirectory() && isSubdirectory(candidate)) {
                    dir = candidate;
                } else {
                    panel.appendOutput("No such directory: " + args.get(0));
                    return;
                }
            }
            String[] files = dir.list();
            if (files != null) {
                Arrays.sort(files);
                for (String file : files) {
                    File f = new File(dir, file);
                    panel.appendOutput((f.isDirectory() ? file + "/" : file));
                }
            } else {
                panel.appendOutput("Unable to list files.");
            }
        }));
        cmds.put("cd", new Command("Change directory (within /data only)", args -> {
            if (args.isEmpty()) {
                currentDir = rootDir;
                panel.appendOutput("Changed to /data");
                return;
            }
            String path = args.get(0);
            File newDir;
            if (path.equals("..")) {
                newDir = currentDir.getParentFile();
                if (newDir == null || !isSubdirectory(newDir)) {
                    panel.appendOutput("Already at /data");
                    return;
                }
            } else if (path.equals(".")) {
                newDir = currentDir;
            } else {
                newDir = new File(currentDir, path);
            }
            try {
                newDir = newDir.getCanonicalFile();
                // Fix: Only relativize if both paths are of the same type (both relative or both absolute)
                if (!newDir.exists() || !newDir.isDirectory() || !isSubdirectory(newDir)) {
                    panel.appendOutput("No such directory: " + path);
                    return;
                }
                currentDir = newDir;
                String rel;
                try {
                    rel = rootDir.getCanonicalFile().toPath().relativize(currentDir.toPath()).toString();
                } catch (Exception e) {
                    rel = currentDir.getName();
                }
                panel.appendOutput("Changed to /data" + (rel.isEmpty() ? "" : "/" + rel));
            } catch (Exception e) {
                panel.appendOutput("Error: " + e.getMessage());
            }
        }));
        // --- People database commands ---
        cmds.put("list", new Command("List all people in the database", _ -> {
            var people = panel.getManager().getPeople();
            if (people.isEmpty()) {
                panel.appendOutput("No people in the database.");
            } else {
                for (int i = 0; i < people.size(); i++) {
                    var p = people.get(i);
                    panel.appendOutput((i+1) + ". " + p.getFirstName() + " " + p.getLastName());
                }
            }
        }));
        cmds.put("count", new Command("Show the number of people in the database", _ -> {
            var people = panel.getManager().getPeople();
            panel.appendOutput("People in database: " + people.size());
        }));
        cmds.put("find", new Command("Find people by name (case-insensitive)", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: find <name>");
                return;
            }
            String search = args.get(0).toLowerCase();
            var people = panel.getManager().getPeople();
            boolean found = false;
            for (int i = 0; i < people.size(); i++) {
                var p = people.get(i);
                if (p.getFirstName().toLowerCase().contains(search) || p.getLastName().toLowerCase().contains(search)) {
                    panel.appendOutput((i+1) + ". " + p.getFirstName() + " " + p.getLastName());
                    found = true;
                }
            }
            if (!found) panel.appendOutput("No matches found for: " + search);
        }));
        cmds.put("info", new Command("Show detailed info for a person by index", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: info <index>");
                return;
            }
            try {
                int idx = Integer.parseInt(args.get(0)) - 1;
                var people = panel.getManager().getPeople();
                if (idx < 0 || idx >= people.size()) {
                    panel.appendOutput("Invalid index.");
                    return;
                }
                var p = people.get(idx);
                panel.appendOutput("First Name: " + p.getFirstName());
                panel.appendOutput("Last Name: " + p.getLastName());
                panel.appendOutput("DOB: " + (p.getDOB() != null ? p.getDOB().toString() : ""));
                if (p instanceof src.person.RegisteredPerson reg) {
                    panel.appendOutput("GovID: " + reg.getGovID());
                    if (p instanceof src.person.OCCCPerson occc) {
                        panel.appendOutput("StudentID: " + occc.getStudentID());
                    }
                }
            } catch (Exception e) {
                panel.appendOutput("Invalid index or error: " + e.getMessage());
            }
        }));
        cmds.put("add", new Command("Add a person: add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]", args -> {
            if (args.size() < 3) {
                panel.appendOutput("Usage: add <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]");
                return;
            }
            String first = args.get(0), last = args.get(1), dob = args.get(2);
            String gov = args.size() > 3 ? args.get(3) : "";
            String stu = args.size() > 4 ? args.get(4) : "";
            var result = panel.getManager().addPersonFromFields(first, last, dob, gov, stu);
            if (result.success) {
                panel.appendOutput("Person added.");
            } else {
                panel.appendOutput("Error: " + result.errorMessage);
            }
        }));
        cmds.put("edit", new Command("Edit a person: edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]", args -> {
            if (args.size() < 4) {
                panel.appendOutput("Usage: edit <index> <FirstName> <LastName> <DOB MM/DD/YYYY> [GovID] [StudentID]");
                return;
            }
            try {
                int idx = Integer.parseInt(args.get(0)) - 1;
                String first = args.get(1), last = args.get(2), dob = args.get(3);
                String gov = args.size() > 4 ? args.get(4) : "";
                String stu = args.size() > 5 ? args.get(5) : "";
                var result = panel.getManager().updatePersonFromFields(idx, first, last, dob, gov, stu);
                if (result.success) {
                    panel.appendOutput("Person updated.");
                } else {
                    panel.appendOutput("Error: " + result.errorMessage);
                }
            } catch (Exception e) {
                panel.appendOutput("Invalid index or error: " + e.getMessage());
            }
        }));
        cmds.put("delete", new Command("Delete a person: delete <index>", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: delete <index>");
                return;
            }
            try {
                int idx = Integer.parseInt(args.get(0)) - 1;
                var people = panel.getManager().getPeople();
                if (idx < 0 || idx >= people.size()) {
                    panel.appendOutput("Invalid index.");
                    return;
                }
                boolean ok = panel.getManager().deletePersonByIndex(idx);
                if (ok) {
                    panel.appendOutput("Person deleted.");
                } else {
                    panel.appendOutput("Delete failed.");
                }
            } catch (Exception e) {
                panel.appendOutput("Invalid index or error: " + e.getMessage());
            }
        }));
        cmds.put("indexof", new Command("Get the index of a person by name: indexof <FirstName> <LastName>", args -> {
            if (args.size() < 2) {
                panel.appendOutput("Usage: indexof <FirstName> <LastName>");
                return;
            }
            String first = args.get(0).toLowerCase();
            String last = args.get(1).toLowerCase();
            var people = panel.getManager().getPeople();
            boolean found = false;
            for (int i = 0; i < people.size(); i++) {
                var p = people.get(i);
                if (p.getFirstName().toLowerCase().equals(first) && p.getLastName().toLowerCase().equals(last)) {
                    panel.appendOutput("Index: " + (i + 1));
                    found = true;
                    break;
                }
            }
            if (!found) {
                panel.appendOutput("Person not found: " + args.get(0) + " " + args.get(1));
            }
        }));
        // --- Config commands ---
        cmds.put("set", new Command("Set a config property: set <KEY> <VALUE>", args -> {
            if (args.size() < 2) {
                panel.appendOutput("Usage: set <KEY> <VALUE>");
                return;
            }
            String key = args.get(0);
            String value = args.get(1);
            var app = panel.getManager();
            switch (key.toUpperCase()) {
                case "THEME" -> app.setThemeName(value);
                case "SIDEBAR_WIDTH" -> app.setSidebarWidth(Integer.parseInt(value));
                case "FILTER_WIDTH" -> app.setFilterWidth(Integer.parseInt(value));
                case "LIST_TERMINAL_DIVIDER" -> app.setListTerminalDivider(Double.parseDouble(value));
                case "WINDOW_WIDTH" -> app.setWindowWidth(Integer.parseInt(value));
                case "WINDOW_HEIGHT" -> app.setWindowHeight(Integer.parseInt(value));
                default -> panel.appendOutput("Unknown config key: " + key);
            }
            panel.appendOutput("Set " + key + " = " + value);
        }));
        cmds.put("get", new Command("Get a config property: get <KEY>", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: get <KEY>");
                return;
            }
            String key = args.get(0);
            var app = panel.getManager();
            String value = switch (key.toUpperCase()) {
                case "THEME" -> app.getThemeName();
                case "SIDEBAR_WIDTH" -> String.valueOf(app.getSidebarWidth());
                case "FILTER_WIDTH" -> String.valueOf(app.getFilterWidth());
                case "LIST_TERMINAL_DIVIDER" -> String.valueOf(app.getListTerminalDivider());
                case "WINDOW_WIDTH" -> String.valueOf(app.getWindowWidth());
                case "WINDOW_HEIGHT" -> String.valueOf(app.getWindowHeight());
                default -> null;
            };
            if (value != null) {
                panel.appendOutput(key + " = " + value);
            } else {
                panel.appendOutput("Unknown config key: " + key);
            }
        }));
        cmds.put("saveconfig", new Command("Save the current config to disk", _ -> {
            var app = panel.getManager();
            app.saveConfig(new java.io.File("data/.config/config"));
            panel.appendOutput("Config saved.");
        }));
        cmds.put("cat", new Command("Display the contents of a file in the current directory: cat <filename>", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: cat <filename>");
                return;
            }
            String filename = args.get(0);
            File file = new File(currentDir, filename);
            try {
                file = file.getCanonicalFile();
                if (!file.exists() || !file.isFile() || !isSubdirectory(file.getParentFile())) {
                    panel.appendOutput("File not found or not allowed: " + filename);
                    return;
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        panel.appendOutput(line);
                    }
                }
            } catch (Exception e) {
                panel.appendOutput("Error reading file: " + e.getMessage());
            }
        }));
        cmds.put("grep", new Command("Search for a pattern in the current People list or a file: grep <pattern> [file]", args -> {
            if (args.isEmpty()) {
                panel.appendOutput("Usage: grep <pattern> [file]");
                return;
            }
            String pattern = args.get(0).toLowerCase();
            if (args.size() == 1) {
                // Search People list
                var people = panel.getManager().getPeople();
                boolean found = false;
                for (int i = 0; i < people.size(); i++) {
                    var p = people.get(i);
                    String personStr = p.getFirstName() + " " + p.getLastName();
                    if (personStr.toLowerCase().contains(pattern) || (p.getDOB() != null && p.getDOB().toString().toLowerCase().contains(pattern))) {
                        panel.appendOutput((i+1) + ". " + personStr);
                        found = true;
                    }
                }
                if (!found) panel.appendOutput("No matches found in People list for: " + pattern);
            } else {
                // Search file
                String filename = args.get(1);
                File file = new File(currentDir, filename);
                try {
                    file = file.getCanonicalFile();
                    if (!file.exists() || !file.isFile() || !isSubdirectory(file.getParentFile())) {
                        panel.appendOutput("File not found or not allowed: " + filename);
                        return;
                    }
                    boolean found = false;
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        int lineNum = 1;
                        while ((line = reader.readLine()) != null) {
                            if (line.toLowerCase().contains(pattern)) {
                                panel.appendOutput(lineNum + ": " + line);
                                found = true;
                            }
                            lineNum++;
                        }
                    }
                    if (!found) panel.appendOutput("No matches found in file for: " + pattern);
                } catch (Exception e) {
                    panel.appendOutput("Error reading file: " + e.getMessage());
                }
            }
        }));
        cmds.put("config", new Command("Show the current config or the default config: config [-d]", args -> {
            try {
                File configFile;
                if (!args.isEmpty() && args.get(0).equals("-d")) {
                    configFile = new File("data/.config/config.default");
                } else {
                    configFile = new File("data/.config/config");
                }
                configFile = configFile.getCanonicalFile();
                if (!configFile.exists() || !isSubdirectory(configFile.getParentFile())) {
                    panel.appendOutput("Config file not found: " + configFile.getName());
                    return;
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        panel.appendOutput(line);
                    }
                }
            } catch (Exception e) {
                panel.appendOutput("Error reading config: " + e.getMessage());
            }
        }));
        cmds.put("motd", new Command("Show, set, or reset the terminal message of the day: motd [new message|-r|--reset]", args -> {
            // Clear the last '> motd ...' line from the output
            panel.clearOutput();
            if (!args.isEmpty() && (args.get(0).equals("-r") || args.get(0).equals("--reset"))) {
                motd = "PersonTerminal ready. Type 'help' for a list of commands.";
                panel.appendOutput("MOTD reset to default.");
            } else if (args.isEmpty()) {
                panel.appendOutput(motd);
            } else {
                motd = String.join(" ", args);
                panel.appendOutput("MOTD updated.");
            }
        }));
        // ... Add more commands here, following the pattern ...
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

    // Add a public getter for commandMap
    public Map<String, Command> getCommandMap() {
        return commandMap;
    }

    // Add public getter and setter for currentDir
    public File getCurrentDirectory() {
        return currentDir;
    }
    public void setCurrentDirectory(File dir) {
        if (dir != null && isSubdirectory(dir) && dir.isDirectory()) {
            currentDir = dir;
        }
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

    public static class Command {
        public final String description;
        public final Consumer<List<String>> action;
        public Command(String description, Consumer<List<String>> action) {
            this.description = description;
            this.action = action;
        }
    }
}
