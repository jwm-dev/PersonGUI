import src.app.AppController;
import src.app.gui.Dialogs;
import src.app.gui.Frame;
import src.app.modules.terminal.PTerminal;
import src.app.modules.terminal.PersonTerminal;
import src.app.gui.GUI;

import javax.swing.*;
import java.io.File;

/**
 * Application entry point and control logic for Person Manager.
 */
public class PersonApp {
    public static void main(String[] args) {
        // Remove initial Metal LAF set, always use CloudyLookAndFeel
        // Check for script argument to run in batch mode
        if (args.length > 0) {
            if (args[0].equals("-script") && args.length > 1) {
                runScriptInBatchMode(args[1]);
                return;
            } else if (args[0].endsWith(".ppl") || args[0].endsWith(".txt")) {
                runScriptInBatchMode(args[0]);
                return;
            } else {
                System.out.println("Usage: java PersonApp [-script scriptfile.txt]");
                System.out.println("  or:  java PersonApp scriptfile.txt");
                System.out.println("No GUI will be displayed when running in script mode.");
            }
        }
        // Launch GUI normally if no script argument is provided
        SwingUtilities.invokeLater(() -> {
            AppController appController = new AppController();
            // Load config from disk before any UI is created
            File configFile = new File("data/.config/config");
            appController.loadConfig(configFile);
            // Retrieve THEME property from config
            String themeName = appController.getThemeName();
            appController.loadTheme(themeName); // Ensure themeProps is up to date
            // Always set LookAndFeel to CloudyLookAndFeel with loaded theme properties before any UI is created
            try {
                UIManager.setLookAndFeel(new src.app.gui.CloudyLookAndFeel(appController.getThemeProperties()));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Frame gui = new Frame(appController);
            appController.initModules(gui);
            GUI pgui = new GUI(gui, appController);
            appController.setGuiApi(pgui);
            gui.initializeUI(pgui);
            gui.setVisible(true);
        });
    }
    /**
     * Run a script file in batch mode without showing the GUI
     * @param scriptPath Path to the script file
     */
    private static void runScriptInBatchMode(String scriptPath) {
        try {
            File scriptFile = new File(scriptPath);
            if (!scriptFile.exists()) {
                System.err.println("Error: Script file not found: " + scriptPath);
                System.exit(1);
            }
            // Set up the necessary components without showing the GUI
            AppController dataManager = new AppController();
            Dialogs fileActions = new Dialogs(dataManager, null, 
                new java.io.File(System.getProperty("user.dir") + java.io.File.separator + "data"), ".ppl");
            // Fix: instantiate PersonTerminal, not PTerminal
            PTerminal terminalModule = new PersonTerminal(null, dataManager, fileActions);
            // Run the script
            System.out.println("Running script in batch mode: " + scriptPath);
            terminalModule.runScript(scriptFile);
            System.out.println("Script execution completed");
        } catch (Exception e) {
            System.err.println("Error executing script: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        // Exit after the script is done
        System.exit(0);
    }
}
