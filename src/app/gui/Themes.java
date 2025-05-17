package src.app.gui;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

/**
 * Theme utility for loading and parsing theme files.
 * No static state. All theme data is managed by AppController.
 */
public class Themes {
    public static Properties loadThemeFile(String themeName) {
        String THEME_DIR = "data/.config/themes/";
        String DEFAULT_THEME = "light";
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(THEME_DIR + themeName)) {
            props.load(fis);
        } catch (Exception e) {
            // fallback to default
            try (FileInputStream fis = new FileInputStream(THEME_DIR + DEFAULT_THEME)) {
                props.load(fis);
            } catch (Exception ignored) {}
        }
        return props;
    }

    public static Color getColor(Properties props, String key, Color fallback) {
        if (props == null) return fallback;
        String val = props.getProperty(key);
        if (val == null) return fallback;
        try {
            if (val.startsWith("#")) return Color.decode(val);
            if (val.startsWith("0x")) return Color.decode(val.replace("0x", "#"));
            return Color.decode("#" + val);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static Font getFont(Properties props, String key, Font fallback) {
        if (props == null) return fallback;
        String fontStr = props.getProperty(key);
        if (fontStr == null || fontStr.isEmpty()) return fallback;
        try {
            int lastDash = fontStr.lastIndexOf('-');
            if (lastDash < 0) return new Font(fontStr, Font.PLAIN, 14);
            String nameAndMaybeFamily = fontStr.substring(0, lastDash);
            String styleAndSize = fontStr.substring(lastDash + 1);
            int secondLastDash = nameAndMaybeFamily.lastIndexOf('-');
            String fontName, styleStr;
            if (secondLastDash >= 0) {
                fontName = nameAndMaybeFamily.substring(0, secondLastDash);
                styleStr = nameAndMaybeFamily.substring(secondLastDash + 1);
            } else {
                fontName = nameAndMaybeFamily;
                styleStr = "PLAIN";
            }
            String[] styleSize = styleAndSize.split("-");
            int style = switch (styleStr.toUpperCase()) {
                case "BOLD" -> Font.BOLD;
                case "ITALIC" -> Font.ITALIC;
                case "BOLDITALIC", "BOLD_ITALIC", "ITALICBOLD", "ITALIC_BOLD" -> Font.BOLD | Font.ITALIC;
                default -> Font.PLAIN;
            };
            int size = 14;
            if (styleSize.length > 0) {
                try { size = Integer.parseInt(styleSize[styleSize.length - 1]); } catch (Exception ignored) {}
            }
            return new Font(fontName.split(",")[0].trim(), style, size);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * Map theme property keys to UIManager keys for Swing/Nimbus.
     * Call this BEFORE setLookAndFeel for dynamic theme switching.
     */
    public static void applyThemePropertiesToUIManager(Properties props) {
        if (props == null) return;
        // General
        UIManager.put("control", getColor(props, "BG", Color.DARK_GRAY));
        UIManager.put("Panel.background", getColor(props, "BG", Color.DARK_GRAY));
        UIManager.put("Label.foreground", getColor(props, "FG", Color.LIGHT_GRAY));
        UIManager.put("text", getColor(props, "FG", Color.LIGHT_GRAY));
        UIManager.put("nimbusBase", getColor(props, "BG", Color.DARK_GRAY));
        UIManager.put("nimbusBlueGrey", getColor(props, "BG", Color.DARK_GRAY));
        UIManager.put("nimbusFocus", getColor(props, "ACCENT", Color.BLUE));
        UIManager.put("nimbusSelectionBackground", getColor(props, "SELECTION_BG", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("nimbusSelectedText", getColor(props, "SELECTION_FG", Color.WHITE));
        UIManager.put("nimbusBorder", getColor(props, "BORDER", Color.GRAY));
        UIManager.put("nimbusLightBackground", getColor(props, "BG", Color.DARK_GRAY));
        UIManager.put("nimbusBackground", getColor(props, "BG", Color.DARK_GRAY));
        // Buttons
        UIManager.put("Button.background", getColor(props, "BUTTON_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Button.foreground", getColor(props, "BUTTON_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Button.select", getColor(props, "BUTTON_ACCENT", getColor(props, "ACCENT", Color.BLUE)));
        // Text fields
        UIManager.put("TextField.background", getColor(props, "TEXTFIELD_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("TextField.foreground", getColor(props, "TEXTFIELD_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("TextField.inactiveBackground", getColor(props, "TEXTFIELD_INACTIVE_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("TextField.border", getColor(props, "TEXTFIELD_BORDER", getColor(props, "BORDER", Color.GRAY)));
        // Lists
        UIManager.put("List.background", getColor(props, "LIST_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("List.foreground", getColor(props, "LIST_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("List.selectionBackground", getColor(props, "LIST_SELECTION_BG", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("List.selectionForeground", getColor(props, "LIST_SELECTION_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("List.alternateRowColor", getColor(props, "LIST_ALT_BG", getColor(props, "BG", Color.DARK_GRAY)));
        // Tables
        UIManager.put("Table.background", getColor(props, "LIST_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Table.foreground", getColor(props, "LIST_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Table.selectionBackground", getColor(props, "LIST_SELECTION_BG", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("Table.selectionForeground", getColor(props, "LIST_SELECTION_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Table.alternateRowColor", getColor(props, "LIST_ALT_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("TableHeader.background", getColor(props, "LIST_HEADER_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("TableHeader.foreground", getColor(props, "LIST_HEADER_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        // Menus
        UIManager.put("Menu.background", getColor(props, "MAINBAR_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Menu.foreground", getColor(props, "MAINBAR_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Menu.selectionBackground", getColor(props, "MENU_SEL_BG", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("Menu.selectionForeground", getColor(props, "MENU_SEL_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("PopupMenu.selectionBackground", getColor(props, "POPUPMENU_SEL_BG", getColor(props, "MENU_SEL_BG", getColor(props, "ACCENT", Color.BLUE))));
        UIManager.put("PopupMenu.selectionForeground", getColor(props, "POPUPMENU_SEL_FG", getColor(props, "MENU_SEL_FG", getColor(props, "FG", Color.WHITE))));
        UIManager.put("MENU_BG", getColor(props, "MENU_BG", getColor(props, "MAINBAR_BG", getColor(props, "BG", Color.DARK_GRAY))));
        // Explicitly set for Nimbus compatibility
        UIManager.put("MenuItem[Selected].textForeground", getColor(props, "POPUPMENU_SEL_FG", getColor(props, "MENU_SEL_FG", getColor(props, "FG", Color.WHITE))));
        UIManager.put("Menu[Selected].textForeground", getColor(props, "POPUPMENU_SEL_FG", getColor(props, "MENU_SEL_FG", getColor(props, "FG", Color.WHITE))));
        UIManager.put("MenuItem.foreground", getColor(props, "MAINBAR_FG", getColor(props, "FG", Color.DARK_GRAY)));
        UIManager.put("MenuItem.selectionForeground", getColor(props, "POPUPMENU_SEL_FG", getColor(props, "MENU_SEL_FG", getColor(props, "FG", Color.DARK_GRAY))));
        UIManager.put("MenuItem.selectionBackground", getColor(props, "POPUPMENU_SEL_BG", getColor(props, "MENU_SEL_BG", getColor(props, "ACCENT", Color.BLUE))));
        // Scrollbars, etc. (add more as needed)
        // Custom keys for your modules (for convenience, not used by Swing):
        UIManager.put("Sidebar.background", getColor(props, "SIDEBAR_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Sidebar.foreground", getColor(props, "SIDEBAR_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Module.background", getColor(props, "MODULE_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Module.foreground", getColor(props, "MODULE_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Module.buttonBackground", getColor(props, "MODULE_BUTTON_BG", getColor(props, "BUTTON_BG", Color.DARK_GRAY)));
        UIManager.put("Module.buttonForeground", getColor(props, "MODULE_BUTTON_FG", getColor(props, "BUTTON_FG", Color.LIGHT_GRAY)));
        UIManager.put("Module.textBackground", getColor(props, "MODULE_TEXT_BG", getColor(props, "TEXTFIELD_BG", Color.DARK_GRAY)));
        UIManager.put("Module.textForeground", getColor(props, "MODULE_TEXT_FG", getColor(props, "TEXTFIELD_FG", Color.LIGHT_GRAY)));
        UIManager.put("Terminal.background", getColor(props, "TERMINAL_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Terminal.foreground", getColor(props, "TERMINAL_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Terminal.accent", getColor(props, "TERMINAL_ACCENT", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("Filter.background", getColor(props, "FILTER_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Filter.foreground", getColor(props, "FILTER_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Filter.buttonBackground", getColor(props, "FILTER_BUTTON_BG", getColor(props, "BUTTON_BG", Color.DARK_GRAY)));
        UIManager.put("Filter.buttonForeground", getColor(props, "FILTER_BUTTON_FG", getColor(props, "BUTTON_FG", Color.LIGHT_GRAY)));
        UIManager.put("Filter.textBackground", getColor(props, "FILTER_TEXT_BG", getColor(props, "TEXTFIELD_BG", Color.DARK_GRAY)));
        UIManager.put("Filter.textForeground", getColor(props, "FILTER_TEXT_FG", getColor(props, "TEXTFIELD_FG", Color.LIGHT_GRAY)));
        UIManager.put("Viewer.background", getColor(props, "VIEWER_BG", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("Viewer.foreground", getColor(props, "VIEWER_FG", getColor(props, "FG", Color.LIGHT_GRAY)));
        UIManager.put("Viewer.fieldBackground", getColor(props, "VIEWER_FIELD_BG", getColor(props, "TEXTFIELD_BG", Color.DARK_GRAY)));
        UIManager.put("Viewer.fieldForeground", getColor(props, "VIEWER_FIELD_FG", getColor(props, "TEXTFIELD_FG", Color.LIGHT_GRAY)));
        // Set custom ButtonUI for Viewer and Filter modules only
        UIManager.put("Viewer.buttonUI", "src.app.gui.FlatButtonUI");
        UIManager.put("Filter.buttonUI", "src.app.gui.FlatButtonUI");
        // Status/semantic colors
        UIManager.put("Error.foreground", getColor(props, "ERROR", Color.RED));
        UIManager.put("Success.foreground", getColor(props, "SUCCESS", Color.GREEN));
        UIManager.put("Highlight.background", getColor(props, "HIGHLIGHT_BG", Color.CYAN));
        UIManager.put("Highlight.border", getColor(props, "HIGHLIGHT_BORDER", Color.BLUE));
        UIManager.put("CloseButton.foreground", getColor(props, "CLOSE_BUTTON_FG", Color.RED));
        UIManager.put("Primary", getColor(props, "PRIMARY", getColor(props, "ACCENT", Color.BLUE)));
        UIManager.put("Secondary", getColor(props, "SECONDARY", getColor(props, "BUTTON_BG", Color.DARK_GRAY)));
        UIManager.put("Surface", getColor(props, "SURFACE", getColor(props, "BG", Color.DARK_GRAY)));
        UIManager.put("ACCENT", getColor(props, "ACCENT", Color.BLUE));
    }

    /**
     * Clear all Nimbus and common UIManager keys that may be set by previous themes.
     * This prevents theme mixing when switching themes live.
     */
    public static void clearNimbusUIManagerKeys() {
        String[] keysToClear = {
            // Standard Nimbus/Swing keys
            "control", "Panel.background", "Label.foreground", "text", "nimbusBase", "nimbusBlueGrey", "nimbusFocus",
            "nimbusSelectionBackground", "nimbusSelectedText", "nimbusBorder", "nimbusLightBackground", "nimbusBackground",
            "Button.background", "Button.foreground", "Button.select",
            "TextField.background", "TextField.foreground", "TextField.inactiveBackground", "TextField.border",
            "List.background", "List.foreground", "List.selectionBackground", "List.selectionForeground", "List.alternateRowColor",
            "Table.background", "Table.foreground", "Table.selectionBackground", "Table.selectionForeground", "Table.alternateRowColor",
            "TableHeader.background", "TableHeader.foreground",
            "Menu.background", "Menu.foreground", "Menu.selectionBackground", "Menu.selectionForeground",
            // Custom keys for modules
            "Sidebar.background", "Sidebar.foreground",
            "Module.background", "Module.foreground", "Module.buttonBackground", "Module.buttonForeground",
            "Module.textBackground", "Module.textForeground",
            "Terminal.background", "Terminal.foreground", "Terminal.accent",
            "Filter.background", "Filter.foreground", "Filter.buttonBackground", "Filter.buttonForeground",
            "Filter.textBackground", "Filter.textForeground",
            "Viewer.background", "Viewer.foreground", "Viewer.fieldBackground", "Viewer.fieldForeground",
            // Status/semantic colors
            "Error.foreground", "Success.foreground", "Highlight.background", "Highlight.border", "CloseButton.foreground",
            "Primary", "Secondary", "Surface"
        };
        for (String key : keysToClear) {
            UIManager.put(key, null);
        }
    }

    /**
     * Recursively update the UI for all components in the tree.
     */
    public static void recursivelyUpdateComponentTreeUI(Component c) {
        SwingUtilities.updateComponentTreeUI(c);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                recursivelyUpdateComponentTreeUI(child);
            }
        }
    }

    /**
     * Load and set Inter as the default font for the entire app.
     * Call this before applying theme properties.
     */
    public static void setGlobalAppFont() {
        try {
            // Load font from vendored location
            File fontFile = new File("data/.assets/Inter-Regular.ttf");
            if (fontFile.exists()) {
                Font inter = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.PLAIN, 13f); // Use 13f for compact UI
                // Register font with the graphics environment
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(inter);
                // Set as default for all UI
                UIManager.put("defaultFont", inter);
                // For Nimbus and other L&Fs, set for all major component types
                String[] keys = {"Button.font", "ToggleButton.font", "RadioButton.font", "CheckBox.font", "ColorChooser.font", "ComboBox.font", "Label.font", "List.font", "MenuBar.font", "MenuItem.font", "RadioButtonMenuItem.font", "CheckBoxMenuItem.font", "Menu.font", "PopupMenu.font", "OptionPane.font", "Panel.font", "ProgressBar.font", "ScrollPane.font", "Viewport.font", "TabbedPane.font", "Table.font", "TableHeader.font", "TextField.font", "PasswordField.font", "TextArea.font", "TextPane.font", "EditorPane.font", "TitledBorder.font", "ToolBar.font", "ToolTip.font", "Tree.font"};
                for (String key : keys) {
                    UIManager.put(key, inter);
                }
            }
        } catch (FontFormatException | IOException e) {
            System.err.println("Failed to load Inter font: " + e.getMessage());
        }
    }

    /**
     * Apply the theme and refresh all open windows/dialogs in one step.
     * This is the only method you need to call for live theme switching.
     */
    public static void applyThemeAndRefreshAllWindows(Properties themeProps) {
        setGlobalAppFont(); // Ensure font is set before theme
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {}
        clearNimbusUIManagerKeys();
        applyThemePropertiesToUIManager(themeProps);
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            recursivelyUpdateComponentTreeUI(w);
            // --- JTable retheming fix: force updateUI on all JTables in this window ---
            updateAllJTables(w);
            w.revalidate();
            w.repaint();
        }
    }

    // Recursively call updateUI on all JTables in the component tree
    private static void updateAllJTables(Component c) {
        if (c instanceof javax.swing.JTable) {
            ((javax.swing.JTable) c).setDefaultRenderer(Object.class, null);
            ((javax.swing.JTable) c).updateUI();
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                updateAllJTables(child);
            }
        }
    }
}
