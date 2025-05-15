package src.app.gui;

import src.app.modules.filter.PFilter;
import src.app.modules.list.PList;
import src.app.modules.terminal.PTerminal;
import src.app.modules.viewer.PViewer;

import javax.swing.*;
import java.util.Properties;

/**
 * Interface for interacting with the GUI layer of the application.
 * Provides access to main modules, theme management, and global GUI actions.
 */
public interface GuiAPI {
    JFrame getMainFrame();
    MainBar getMainBar();
    PList getListModule();
    PFilter getFilterModule();
    PTerminal getTerminalModule();
    PViewer getPersonModule();
    Dialogs getDialogsModule();

    /**
     * Reloads config and theme, and applies to the entire GUI.
     */
    void reloadConfigAndTheme();

    /**
     * Apply a new config (and theme) from a Properties object.
     */
    void applyConfigAndTheme(Properties props);

    /**
     * Show the main application window.
     */
    void show();

    /**
     * Hide the main application window.
     */
    void hide();

    /**
     * Recursively updates the UI of all components in the main window (for theme changes).
     */
    void refreshAllUI();
}
