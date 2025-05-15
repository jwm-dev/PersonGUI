import src.app.AppController;
import src.app.gui.Frame;
import src.app.gui.GUI;

import javax.swing.*;
import java.io.File;

/**
 * Application entry point and control logic for Person Manager.
 */
public class PersonApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppController appController = new AppController();
            // Load config from disk before any UI is created
            File configFile = new File("data/.config/config");
            appController.loadConfig(configFile);
            // Retrieve THEME property from config
            String themeName = appController.getThemeName();
            appController.loadTheme(themeName); // Ensure themeProps is up to date
            src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
            // --- Now safe to create Swing components ---
            Frame gui = new Frame(appController);
            appController.initModules(gui);
            GUI pgui = new GUI(gui, appController);
            appController.setGuiApi(pgui);
            gui.initializeUI(pgui);
            gui.setVisible(true);
        });
    }
}
