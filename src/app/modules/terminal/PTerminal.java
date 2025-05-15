package src.app.modules.terminal;

import javax.swing.*;
import java.io.File;

/**
 * API interface for the Terminal module.
 */
public interface PTerminal {
    void executeCommand(String command);
    void runScript(File scriptFile);
    File getCurrentDirectory();
    JComponent getPanel();

    /**
     * Returns the JTextArea used for terminal output.
     */
    JTextArea getOutputArea();

    /**
     * Returns the JTextField used for terminal input.
     */
    JTextField getInputField();

    /**
     * Returns the JLabel used for the prompt (e.g., "$ ").
     */
    JLabel getPromptLabel();
}
