package src.app.gui;

import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.UIDefaults;
import java.util.Properties;
import java.awt.Color;
import java.awt.Font;

/**
 * CloudyLookAndFeel: Custom LookAndFeel, no dependency on Nimbus.
 * Accepts a theme Properties object and parses palette/font using Themes.
 */
public class CloudyLookAndFeel extends BasicLookAndFeel {
    private final Properties theme;

    public CloudyLookAndFeel(Properties theme) {
        super();
        this.theme = theme;
    }

    @Override
    public String getName() {
        return "Cloudy";
    }

    @Override
    public String getID() {
        return "Cloudy";
    }

    @Override
    public String getDescription() {
        return "Cloudy Look and Feel (custom, no Nimbus dependency)";
    }

    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    @Override
    public void initialize() {
        // No Nimbus initialization
    }

    @Override
    public void uninitialize() {
        // No Nimbus uninitialization
    }

    @Override
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();
        // --- Cloudy theme palette from config ---
        Color primary = Themes.getColor(theme, "PRIMARY", new Color(0x4f8cff));
        Color secondary = Themes.getColor(theme, "SECONDARY", new Color(0xe0e7ef));
        Color surface = Themes.getColor(theme, "SURFACE", new Color(0xffffff));
        Color background = Themes.getColor(theme, "BG", new Color(0xffffff));
        Color foreground = Themes.getColor(theme, "FG", new Color(0x23272e));
        Color error = Themes.getColor(theme, "ERROR", new Color(0xe11d48));
        Color border = Themes.getColor(theme, "BORDER", new Color(0xd1d5db));
        Color buttonBg = Themes.getColor(theme, "BUTTON_BG", new Color(0xe0e7ef));
        Color buttonFg = Themes.getColor(theme, "BUTTON_FG", new Color(0x23272e));
        Color selectionBg = Themes.getColor(theme, "SELECTION_BG", new Color(0xdbeafe));
        Color selectionFg = Themes.getColor(theme, "SELECTION_FG", new Color(0x23272e));
        Color mainbarBg = Themes.getColor(theme, "MAINBAR_BG", new Color(0xf8fafc));
        Color mainbarFg = Themes.getColor(theme, "MAINBAR_FG", new Color(0x23272e));
        Color menuSelBg = Themes.getColor(theme, "MENU_SEL_BG", new Color(0xdbeafe));
        Color menuSelFg = Themes.getColor(theme, "MENU_SEL_FG", new Color(0x23272e));
        // --- Module-specific colors ---
        Color listBg = Themes.getColor(theme, "LIST_BG", surface);
        Color listFg = Themes.getColor(theme, "LIST_FG", foreground);
        Color listSelBg = Themes.getColor(theme, "LIST_SELECTION_BG", selectionBg);
        Color listSelFg = Themes.getColor(theme, "LIST_SELECTION_FG", selectionFg);
        Color filterBg = Themes.getColor(theme, "FILTER_BG", mainbarBg);
        Color filterFg = Themes.getColor(theme, "FILTER_FG", foreground);
        Color terminalBg = Themes.getColor(theme, "TERMINAL_BG", new Color(0xe5e7eb));
        Color terminalFg = Themes.getColor(theme, "TERMINAL_FG", foreground);
        Color viewerBg = Themes.getColor(theme, "VIEWER_BG", surface);
        Color viewerFg = Themes.getColor(theme, "VIEWER_FG", foreground);
        Color viewerFieldBg = Themes.getColor(theme, "VIEWER_FIELD_BG", mainbarBg);
        Color viewerFieldFg = Themes.getColor(theme, "VIEWER_FIELD_FG", foreground);
        // --- Register all module color keys ---
        defaults.put("list.background", listBg);
        defaults.put("list.foreground", listFg);
        defaults.put("list.selectionBackground", listSelBg);
        defaults.put("list.selectionForeground", listSelFg);
        defaults.put("filter.background", filterBg);
        defaults.put("filter.foreground", filterFg);
        defaults.put("terminal.background", terminalBg);
        defaults.put("terminal.foreground", terminalFg);
        defaults.put("viewer.background", viewerBg);
        defaults.put("viewer.foreground", viewerFg);
        defaults.put("viewer.fieldBackground", viewerFieldBg);
        defaults.put("viewer.fieldForeground", viewerFieldFg);
        // --- Core palette mapping ---
        defaults.put("control", surface);
        defaults.put("info", surface);
        defaults.put("primary", primary);
        defaults.put("secondary", secondary);
        defaults.put("lightBackground", background);
        defaults.put("focus", border);
        defaults.put("border", border);
        defaults.put("selectionBackground", selectionBg);
        defaults.put("selection", selectionBg);
        defaults.put("text", foreground);
        defaults.put("disabledText", foreground.darker());
        defaults.put("selectedText", selectionFg);
        defaults.put("alert", error);
        defaults.put("success", primary);
        defaults.put("error", error);
        defaults.put("warning", secondary);
        // --- General backgrounds/foregrounds ---
        defaults.put("background", background);
        defaults.put("foreground", foreground);
        defaults.put("OptionPane.background", surface);
        defaults.put("Panel.background", surface);
        defaults.put("Label.background", surface);
        defaults.put("Label.foreground", foreground);
        defaults.put("Button.background", buttonBg);
        defaults.put("Button.foreground", buttonFg);
        defaults.put("TextField.background", surface);
        defaults.put("TextField.foreground", foreground);
        defaults.put("TextArea.background", surface);
        defaults.put("TextArea.foreground", foreground);
        defaults.put("PasswordField.background", surface);
        defaults.put("PasswordField.foreground", foreground);
        defaults.put("ComboBox.background", surface);
        defaults.put("ComboBox.foreground", foreground);
        defaults.put("ScrollBar.thumb", secondary);
        defaults.put("ScrollBar.track", surface);
        defaults.put("Table.background", surface);
        defaults.put("Table.foreground", foreground);
        defaults.put("Table.selectionBackground", selectionBg);
        defaults.put("Table.selectionForeground", selectionFg);
        defaults.put("List.background", surface);
        defaults.put("List.foreground", foreground);
        defaults.put("List.selectionBackground", selectionBg);
        defaults.put("List.selectionForeground", selectionFg);
        defaults.put("TabbedPane.background", surface);
        defaults.put("TabbedPane.foreground", foreground);
        defaults.put("TabbedPane.selected", selectionBg);
        defaults.put("Separator.foreground", border);
        defaults.put("Separator.background", surface);
        // --- Menu bar and menu items ---
        defaults.put("MenuBar.background", mainbarBg);
        defaults.put("MenuBar.foreground", mainbarFg);
        defaults.put("Menu.background", mainbarBg);
        defaults.put("Menu.foreground", mainbarFg);
        defaults.put("MenuItem.background", mainbarBg);
        defaults.put("MenuItem.foreground", mainbarFg);
        defaults.put("MenuItem.selectionBackground", menuSelBg);
        defaults.put("MenuItem.selectionForeground", menuSelFg);
        defaults.put("Menu.selectionBackground", menuSelBg);
        defaults.put("Menu.selectionForeground", menuSelFg);
        defaults.put("PopupMenu.background", surface);
        defaults.put("PopupMenu.foreground", foreground);
        // --- SplitPane and borders ---
        defaults.put("SplitPane.dividerFocusColor", primary);
        defaults.put("SplitPane.border", javax.swing.BorderFactory.createLineBorder(primary, 1));
        defaults.put("SplitPaneDivider.border", javax.swing.BorderFactory.createLineBorder(primary, 1));
        defaults.put("SplitPaneDivider.background", primary);
        defaults.put("TitledBorder.borderColor", border);
        // --- Fonts ---
        Font uiFont = Themes.getFont(theme, "FONT_UI", new Font("Fira Code, JetBrains Mono, Cascadia Code, monospaced, sans-serif", Font.PLAIN, 15));
        Font monoFont = Themes.getFont(theme, "FONT_MONO", new Font("Fira Code, JetBrains Mono, Cascadia Code, monospaced, sans-serif", Font.PLAIN, 15));
        Font boldFont = Themes.getFont(theme, "FONT_BOLD", new Font("Fira Code, JetBrains Mono, Cascadia Code, monospaced, sans-serif", Font.BOLD, 15));
        defaults.put("defaultFont", uiFont);
        defaults.put("Label.font", uiFont);
        defaults.put("Button.font", uiFont);
        defaults.put("TextField.font", uiFont);
        defaults.put("TextArea.font", monoFont);
        defaults.put("PasswordField.font", uiFont);
        defaults.put("ComboBox.font", uiFont);
        defaults.put("Panel.font", uiFont);
        defaults.put("Table.font", monoFont);
        defaults.put("List.font", uiFont);
        defaults.put("Menu.font", uiFont);
        defaults.put("MenuItem.font", uiFont);
        defaults.put("TabbedPane.font", uiFont);
        defaults.put("Terminal.font", monoFont);
        defaults.put("Viewer.font", uiFont);
        defaults.put("Viewer.boldFont", boldFont);
        return defaults;
    }
}
