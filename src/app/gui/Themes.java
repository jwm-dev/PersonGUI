package src.app.gui;

import java.awt.*;
import java.io.FileInputStream;
import java.util.Properties;

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
}
