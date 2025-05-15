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
}
