package src.app.modules.terminal;

import src.app.AppController;
import src.app.gui.Dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Handles the UI and event logic for the terminal panel.
 * Renamed from PersonTerminalPanel to PTerminalPanel
 */
public class PTerminalPanel extends JPanel {
    private final JTextArea outputArea;
    private final JTextField inputField;
    private final AppController manager;
    private final Dialogs operations;
    private final JFrame parentFrame;
    private PTerminalCommands commandExecutor;

    public PTerminalPanel(JFrame parentFrame, AppController manager, Dialogs operations) {
        super(new BorderLayout(5, 5));
        this.parentFrame = parentFrame;
        this.manager = manager;
        this.operations = operations;
        this.outputArea = new JTextArea(8, 40);
        this.inputField = new JTextField();
        this.outputArea.setEditable(false);
        this.outputArea.setFont(UIManager.getFont("TextArea.font"));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        JLabel promptLabel = new JLabel("$ ");
        promptLabel.setFont(UIManager.getFont("Label.font"));
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        printWelcome();
        inputField.addActionListener(_ -> {
            String cmdLine = inputField.getText().trim();
            inputField.setText("");
            if (!cmdLine.isEmpty() && commandExecutor != null) {
                commandExecutor.handleCommand(cmdLine);
            }
        });
    }

    public void setCommandExecutor(PTerminalCommands executor) {
        this.commandExecutor = executor;
    }

    public void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    public void clearOutput() {
        outputArea.setText("");
    }

    public void printWelcome() {
        appendOutput("PersonTerminal ready. Type 'help' for a list of commands.");
        if (commandExecutor != null) {
            File cwd = commandExecutor.getCurrentDirectory();
            appendOutput("$ " + (cwd != null ? cwd.getAbsolutePath() : ""));
        }
    }

    public void runScript(File scriptFile) {
        if (commandExecutor != null) {
            commandExecutor.runScript(scriptFile);
        }
    }

    // Remove themeAll, now handled globally

    public File getCurrentDirectory() {
        return commandExecutor != null ? commandExecutor.getCurrentDirectory() : null;
    }

    public void setCurrentDirectory(File dir) {
        if (commandExecutor != null) commandExecutor.setCurrentDirectory(dir);
    }

    public AppController getManager() { return manager; }
    public Dialogs getOperations() { return operations; }
    public JFrame getParentFrame() { return parentFrame; }
}
