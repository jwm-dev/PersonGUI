package src.app.modules.terminal;

import src.app.AppController;
import src.app.gui.Dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PersonTerminal extends JPanel implements PTerminal {
    private final PTerminalPanel panel;
    private final PTerminalCommands commands;

    public PersonTerminal(JFrame parentFrame, AppController manager, Dialogs operations) {
        super(new BorderLayout());
        this.panel = new PTerminalPanel(parentFrame, manager, operations);
        this.commands = new PTerminalCommands(panel, manager, operations);
        panel.setCommandExecutor(commands);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void executeCommand(String command) {
        commands.handleCommand(command);
    }

    @Override
    public void runScript(File scriptFile) {
        panel.runScript(scriptFile);
    }

    // Remove themeAll, now handled globally

    @Override
    public File getCurrentDirectory() {
        return panel.getCurrentDirectory();
    }

    @Override
    public JComponent getPanel() {
        return panel;
    }
}
