package src.app.gui;

import src.app.AppController;
import src.app.dialogs.Dialogs;

import java.util.Properties;

public class GUI implements GuiAPI {
    private final Frame frame;
    private final AppController appController;

    public GUI(Frame frame, AppController appController) {
        this.frame = frame;
        this.appController = appController;
    }

    @Override
    public void reloadConfigAndTheme() {
        src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
    }

    /**
     * Force a robust theme refresh after all UI is attached.
     */
    public void forceThemeRefresh() {
        src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
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
}
