package src.app.gui;

import src.app.AppController;
import java.util.Properties;
import javax.swing.UIManager;

public class GUI implements GuiAPI {
    private final Frame frame;
    private final AppController appController;

    public GUI(Frame frame, AppController appController) {
        this.frame = frame;
        this.appController = appController;
    }

    @Override
    public void reloadConfigAndTheme() {
        // Only update the UI, do NOT call appController.reloadConfigAndTheme()!
        frame.getContentPane().removeAll();
        frame.setupMenuBar();
        frame.setupSplitPane();
        frame.removeBordersRecursively(frame);
        frame.getRootPane().setBorder(null);
        frame.applyThemeFromThemes();
        // --- Robust: update all windows, not just the main frame ---
        applyThemeEverywhere();
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (frame.getSplitPane() != null) frame.getSplitPane().setDividerLocation(appController.getSidebarWidth());
            if (frame.getRightSplit() != null) frame.getRightSplit().setDividerLocation(appController.getListTerminalDivider());
            if (frame.getFilterSplit() != null) frame.getFilterSplit().setDividerLocation(frame.getFilterSplit().getWidth() - appController.getFilterWidth());
            frame.setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    /**
     * Robustly applies the current theme to all open windows and components.
     */
    private void applyThemeEverywhere() {
        try {
            // Only use CloudyLookAndFeel
            UIManager.setLookAndFeel(new src.app.gui.CloudyLookAndFeel(appController.getThemeProperties()));
        } catch (Exception ignored) {}
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            javax.swing.SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    /**
     * Force a robust theme refresh after all UI is attached.
     */
    public void forceThemeRefresh() {
        applyThemeEverywhere();
        frame.revalidate();
        frame.repaint();
    }

    @Override
    public void applyConfigAndTheme(Properties props) {
        appController.setSidebarWidth(Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(appController.getSidebarWidth()))));
        appController.setFilterWidth(Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(appController.getFilterWidth()))));
        appController.setListTerminalDivider(Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(appController.getListTerminalDivider()))));
        appController.setWindowWidth(Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(appController.getWindowWidth()))));
        appController.setWindowHeight(Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(appController.getWindowHeight()))));
        appController.setThemeName(props.getProperty("THEME", appController.getThemeName()));
        appController.loadTheme(appController.getThemeName());
        reloadConfigAndTheme();
    }

    // Optionally, delegate other GuiAPI methods to frame as needed
    @Override
    public javax.swing.JFrame getMainFrame() { return frame; }
    @Override
    public MainBar getMainBar() { return frame.getMenuBarModule(); }
    @Override
    public src.app.modules.list.PList getListModule() { return frame.getListModule(); }
    @Override
    public src.app.modules.filter.PFilter getFilterModule() { return frame.getFilterModule(); }
    @Override
    public src.app.modules.terminal.PTerminal getTerminalModule() { return frame.getTerminalModule(); }
    @Override
    public src.app.modules.viewer.PViewer getPersonModule() { return frame.getPersonModule(); }
    @Override
    public Dialogs getDialogsModule() { return frame.getFileActions(); }
    @Override
    public void show() { frame.setVisible(true); }
    @Override
    public void hide() { frame.setVisible(false); }
    @Override
    public void refreshAllUI() { frame.refreshAllUI(); }
}
