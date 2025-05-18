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
        // Force all JPopupMenus to be heavyweight for reliable event handling on Linux
        javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        SwingUtilities.invokeLater(() -> {
            AppController appController = new AppController();
            File configFile = new File("data/.config/config");
            appController.loadConfig(configFile);
            String themeName = appController.getThemeName();
            appController.loadTheme(themeName); 
            src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
            Frame frame = new Frame(appController);
            appController.initModules(frame);
            GUI gui = new GUI(frame, appController);
            appController.setGuiApi(gui);
            frame.initializeUI(gui);
            frame.setVisible(true);
        });
    }
}
