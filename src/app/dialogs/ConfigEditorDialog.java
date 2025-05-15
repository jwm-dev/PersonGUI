package src.app.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import src.app.AppController;
import src.app.gui.GuiAPI;

public class ConfigEditorDialog extends JDialog {
    private final String configPath;
    private final Map<String, JTextField> fields = new LinkedHashMap<>();
    private Properties properties;
    private Properties originalProperties;
    private Object personGui;
    private JComboBox<String> themeCombo;
    private String originalTheme;
    private final AppController appController;
    private boolean configSaved = false;

    private static final Map<String, String> FRIENDLY_LABELS = new LinkedHashMap<>();
    private static final String[] THEME_LABELS = {"light", "dark", "developer"};
    static {
        FRIENDLY_LABELS.put("SIDEBAR_WIDTH", "Left Sidebar Width (px)");
        FRIENDLY_LABELS.put("FILTER_WIDTH", "Right Sidebar Width (px)");
        FRIENDLY_LABELS.put("LIST_TERMINAL_DIVIDER", "List/Terminal Divider (0.0-1.0, percent from top)");
        FRIENDLY_LABELS.put("WINDOW_WIDTH", "Window Width (px)");
        FRIENDLY_LABELS.put("WINDOW_HEIGHT", "Window Height (px)");
    }

    // Group settings by category for sorting
    private static final Map<String, String> CATEGORY_LABELS = new LinkedHashMap<>();
    static {
        CATEGORY_LABELS.put("SIDEBAR_WIDTH", "Layout");
        CATEGORY_LABELS.put("FILTER_WIDTH", "Layout");
        CATEGORY_LABELS.put("LIST_TERMINAL_DIVIDER", "Layout");
        CATEGORY_LABELS.put("WINDOW_WIDTH", "Window");
        CATEGORY_LABELS.put("WINDOW_HEIGHT", "Window");
    }

    private static final Map<String, int[]> SLIDER_SETTINGS = new HashMap<>();
    static {
        SLIDER_SETTINGS.put("SIDEBAR_WIDTH", new int[]{100, 400});
        SLIDER_SETTINGS.put("FILTER_WIDTH", new int[]{100, 400});
        SLIDER_SETTINGS.put("WINDOW_WIDTH", new int[]{400, 2000});
        SLIDER_SETTINGS.put("WINDOW_HEIGHT", new int[]{300, 1500});
        SLIDER_SETTINGS.put("LIST_TERMINAL_DIVIDER", new int[]{0, 100}); // Represent as percent
    }

    public ConfigEditorDialog(Object personGui, JFrame parent, String configPath, AppController appController) {
        super(parent, "Edit Config", true);
        this.personGui = personGui;
        this.configPath = configPath;
        this.appController = appController;
        setLayout(new BorderLayout(10, 10));
        // Make dialog always float above parent and not block the main window
        setModalityType(Dialog.ModalityType.MODELESS);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(650, 500);
        setPreferredSize(new Dimension(650, 500));
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(parent);
        // Always reload config from disk and AppController before showing
        reloadConfigState();
        buildUI();
    }

    private void reloadConfigState() {
        // Load from disk
        properties = new Properties();
        File configFile = new File(configPath);
        if (configFile.exists()) {
            appController.loadConfig(configFile);
        }
        // Sync properties from AppController (source of truth)
        properties.setProperty("SIDEBAR_WIDTH", String.valueOf(appController.getSidebarWidth()));
        properties.setProperty("FILTER_WIDTH", String.valueOf(appController.getFilterWidth()));
        properties.setProperty("LIST_TERMINAL_DIVIDER", String.valueOf(appController.getListTerminalDivider()));
        properties.setProperty("WINDOW_WIDTH", String.valueOf(appController.getWindowWidth()));
        properties.setProperty("WINDOW_HEIGHT", String.valueOf(appController.getWindowHeight()));
        properties.setProperty("THEME", appController.getThemeName());
        // Save a copy for cancel/revert
        originalProperties = new Properties();
        originalProperties.putAll(properties);
        originalTheme = appController.getThemeName();
    }

    // Dynamically set slider ranges based on current AppController values
    private Map<String, int[]> getDynamicSliderSettings() {
        Map<String, int[]> dynamic = new HashMap<>(SLIDER_SETTINGS);
        // Expand slider max if current value is outside default range
        int sidebar = appController.getSidebarWidth();
        int filter = appController.getFilterWidth();
        int winW = appController.getWindowWidth();
        int winH = appController.getWindowHeight();
        if (sidebar > dynamic.get("SIDEBAR_WIDTH")[1]) dynamic.put("SIDEBAR_WIDTH", new int[]{dynamic.get("SIDEBAR_WIDTH")[0], Math.max(sidebar, 600)});
        if (filter > dynamic.get("FILTER_WIDTH")[1]) dynamic.put("FILTER_WIDTH", new int[]{dynamic.get("FILTER_WIDTH")[0], Math.max(filter, 600)});
        if (winW > dynamic.get("WINDOW_WIDTH")[1]) dynamic.put("WINDOW_WIDTH", new int[]{dynamic.get("WINDOW_WIDTH")[0], Math.max(winW, 3000)});
        if (winH > dynamic.get("WINDOW_HEIGHT")[1]) dynamic.put("WINDOW_HEIGHT", new int[]{dynamic.get("WINDOW_HEIGHT")[0], Math.max(winH, 2000)});
        return dynamic;
    }

    private void buildUI() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) bg = Color.WHITE;
        if (fg == null) fg = Color.BLACK;
        formPanel.setBackground(bg);
        // --- THEME COMBO AT TOP ---
        JPanel themeRow = new JPanel(new BorderLayout(5, 0));
        themeRow.setBackground(bg);
        JLabel themeLabel = new JLabel("Theme");
        themeLabel.setForeground(fg.darker());
        themeCombo = new JComboBox<>(getAvailableThemes());
        themeCombo.setSelectedItem(properties.getProperty("THEME", "light"));
        // Removed call to undefined applyModernNimbusLookAndFeel(). Theming is now handled in GUI.applyThemeFromConfig()
        themeCombo.setBackground(Color.WHITE);
        themeCombo.setForeground(Color.BLACK);
        themeCombo.setFocusable(false);
        themeCombo.setOpaque(true);
        themeCombo.addActionListener(_ -> {
            if (themeCombo.getSelectedItem() != null) {
                String selectedTheme = themeCombo.getSelectedItem().toString();
                if (!selectedTheme.equals(appController.getThemeName())) {
                    properties.setProperty("THEME", selectedTheme);
                    appController.setThemeName(selectedTheme); // This loads the new theme
                    appController.saveConfig(new File(configPath));
                    if (personGui instanceof GuiAPI guiApi) {
                        guiApi.reloadConfigAndTheme();
                    }
                    src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
                    updateDialogTheme(); // <-- update dialog theme live
                }
            }
        });
        themeRow.add(themeLabel, BorderLayout.WEST);
        themeRow.add(themeCombo, BorderLayout.CENTER);
        formPanel.add(themeRow);
        formPanel.add(Box.createVerticalStrut(8));
        // ...existing code for settings fields...
        List<String> keys = new ArrayList<>(FRIENDLY_LABELS.keySet());
        keys.sort(Comparator.comparing((String k) -> CATEGORY_LABELS.getOrDefault(k, "Other"))
                            .thenComparing(FRIENDLY_LABELS::get));
        String lastCategory = null;
        Map<String, int[]> dynamicSliderSettings = getDynamicSliderSettings();
        for (String key : keys) {
            String category = CATEGORY_LABELS.getOrDefault(key, "Other");
            if (!category.equals(lastCategory)) {
                JLabel catLabel = new JLabel(category + " Settings");
                catLabel.setFont(catLabel.getFont().deriveFont(Font.BOLD, 15f));
                catLabel.setForeground(fg.darker());
                catLabel.setBackground(bg);
                formPanel.add(catLabel);
                formPanel.add(Box.createVerticalStrut(4));
                lastCategory = category;
            }
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(bg);
            JLabel label = new JLabel(FRIENDLY_LABELS.get(key));
            label.setForeground(fg);
            label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
            if (dynamicSliderSettings.containsKey(key)) {
                int[] range = dynamicSliderSettings.get(key);
                int min = range[0];
                int max = range[1];
                int value;
                if (key.equals("LIST_TERMINAL_DIVIDER")) {
                    try {
                        value = (int) (Double.parseDouble(properties.getProperty(key, "0.7")) * 100);
                    } catch (Exception e) {
                        value = 70;
                    }
                } else {
                    try {
                        value = Integer.parseInt(properties.getProperty(key, String.valueOf(min)));
                    } catch (Exception e) {
                        value = min;
                    }
                }
                if (value < min) value = min;
                if (value > max) value = max;
                JSlider slider = new JSlider(JSlider.HORIZONTAL, min, max, value) {
                    @Override
                    public void updateUI() {
                        super.updateUI();
                        setUI(new javax.swing.plaf.basic.BasicSliderUI(this) {
                            @Override
                            public void paintThumb(Graphics g) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(new Color(60, 120, 220));
                                int w = 16, h = 16;
                                int x = thumbRect.x + (thumbRect.width - w) / 2;
                                int y = thumbRect.y + (thumbRect.height - h) / 2;
                                g2.fillRoundRect(x, y, w, h, 8, 8);
                                g2.dispose();
                            }
                            @Override
                            public void paintTrack(Graphics g) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                int cy = trackRect.y + trackRect.height / 2 - 2;
                                g2.setColor(new Color(200, 200, 200));
                                g2.fillRoundRect(trackRect.x, cy, trackRect.width, 4, 4, 4);
                                g2.setColor(new Color(60, 120, 220));
                                int fillW = (int) ((slider.getValue() - slider.getMinimum()) * (trackRect.width * 1.0 / (slider.getMaximum() - slider.getMinimum())));
                                g2.fillRoundRect(trackRect.x, cy, fillW, 4, 4, 4);
                                g2.dispose();
                            }
                            @Override
                            public void paintTicks(Graphics g) {
                                // No ticks for a flat look
                            }
                        });
                    }
                };
                slider.setBackground(bg);
                slider.setForeground(fg);
                slider.setFocusable(false);
                slider.setPaintLabels(false);
                slider.setPaintTicks(false);
                slider.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                slider.setSnapToTicks(false);
                slider.setPaintLabels(false);
                slider.setPaintTicks(false);
                slider.setFocusable(false);
                slider.setOpaque(false);
                slider.setDoubleBuffered(true);
                JTextField valueField = new JTextField(5);
                valueField.setEditable(false);
                valueField.setHorizontalAlignment(JTextField.CENTER);
                valueField.setFont(valueField.getFont().deriveFont(Font.BOLD, 13f));
                valueField.setBackground(bg);
                valueField.setForeground(new Color(30, 30, 30));
                if (key.equals("LIST_TERMINAL_DIVIDER")) {
                    valueField.setText((slider.getValue()) + "%");
                } else {
                    valueField.setText(String.valueOf(slider.getValue()));
                }
                slider.addChangeListener(_ -> {
                    int v = slider.getValue();
                    if (key.equals("LIST_TERMINAL_DIVIDER")) {
                        valueField.setText(v + "%");
                        properties.setProperty(key, String.valueOf(v / 100.0));
                    } else {
                        valueField.setText(String.valueOf(v));
                        properties.setProperty(key, String.valueOf(v));
                    }
                });
                row.add(label, BorderLayout.WEST);
                row.add(slider, BorderLayout.CENTER);
                row.add(valueField, BorderLayout.EAST);
                fields.put(key, valueField);
                valueField.putClientProperty("slider", slider);
            } else {
                JTextField field = new JTextField(properties.getProperty(key, ""));
                field.setFont(field.getFont().deriveFont(Font.PLAIN, 13f));
                field.setBackground(bg);
                field.setForeground(fg);
                row.add(label, BorderLayout.WEST);
                row.add(field, BorderLayout.CENTER);
                fields.put(key, field);
            }
            formPanel.add(row);
            formPanel.add(Box.createVerticalStrut(8));
        }
        JScrollPane scroll = new JScrollPane(formPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(bg);
        scroll.setPreferredSize(new Dimension(600, 350));
        add(scroll, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(bg);
        JButton doneBtn = new JButton("Done");
        JButton exitBtn = new JButton("Exit");
        // Remove old saveBtn, cancelBtn, finishBtn
        // Color the save button with the accent color
        Color accent = UIManager.getColor("nimbusFocus");
        if (accent == null) accent = new Color(60, 120, 220);
        doneBtn.setBackground(accent);
        doneBtn.setForeground(Color.WHITE);
        doneBtn.setFont(doneBtn.getFont().deriveFont(Font.BOLD, 13f));
        doneBtn.setFocusPainted(false);
        doneBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        exitBtn.setBackground(null);
        exitBtn.setForeground(null);
        exitBtn.setFont(exitBtn.getFont().deriveFont(Font.BOLD, 13f));
        exitBtn.setFocusPainted(false);
        exitBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        // Done button acts as Save
        doneBtn.addActionListener(_ -> {
            for (Map.Entry<String, JTextField> entry : fields.entrySet()) {
                String key = entry.getKey();
                JTextField field = entry.getValue();
                Object sliderObj = field.getClientProperty("slider");
                if (sliderObj instanceof JSlider) {
                    int v = ((JSlider) sliderObj).getValue();
                    if (key.equals("LIST_TERMINAL_DIVIDER")) {
                        appController.setListTerminalDivider(v / 100.0);
                    } else if (key.equals("SIDEBAR_WIDTH")) {
                        appController.setSidebarWidth(v);
                    } else if (key.equals("FILTER_WIDTH")) {
                        appController.setFilterWidth(v);
                    } else if (key.equals("WINDOW_WIDTH")) {
                        appController.setWindowWidth(v);
                    } else if (key.equals("WINDOW_HEIGHT")) {
                        appController.setWindowHeight(v);
                    }
                } else {
                    if (key.equals("THEME")) {
                        appController.setThemeName(field.getText().trim());
                        appController.loadTheme(field.getText().trim());
                    }
                }
            }
            if (themeCombo != null && themeCombo.getSelectedItem() != null) {
                appController.setThemeName(themeCombo.getSelectedItem().toString());
                appController.loadTheme(themeCombo.getSelectedItem().toString());
            }
            appController.saveConfig(new File(configPath));
            if (personGui instanceof GuiAPI guiApi) {
                guiApi.reloadConfigAndTheme();
            }
            src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
            configSaved = true;
            // Close dialog after saving
            dispose();
        });
        // Exit button acts as Cancel/Finish
        exitBtn.addActionListener(_ -> {
            if (configSaved) {
                dispose();
                return;
            }
            // Revert to original config state from AppController
            reloadConfigState();
            // Restore the original theme for the whole app
            appController.setThemeName(originalTheme);
            appController.loadTheme(originalTheme);
            src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
            javax.swing.SwingUtilities.updateComponentTreeUI(this);
            repaint();
            dispose();
        });
        buttonPanel.add(doneBtn);
        buttonPanel.add(exitBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        updateDialogTheme();
    }

    private void applyThemeRecursively(Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(UIManager.getColor("Panel.background"));
            comp.setForeground(UIManager.getColor("Label.foreground"));
        } else if (comp instanceof JLabel) {
            comp.setForeground(UIManager.getColor("Label.foreground"));
        } else if (comp instanceof JButton) {
            JButton btn = (JButton) comp;
            if ("Done".equals(btn.getText())) {
                Color accent = UIManager.getColor("nimbusFocus");
                if (accent == null && appController != null) {
                    accent = appController.getThemeColor("ACCENT", new Color(60, 120, 220));
                }
                if (accent == null) accent = new Color(60, 120, 220);
                btn.setBackground(accent);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(UIManager.getColor("Button.foreground"));
            }
        } else if (comp instanceof JTextField) {
            comp.setBackground(UIManager.getColor("TextField.background"));
            comp.setForeground(UIManager.getColor("TextField.foreground"));
        } else if (comp instanceof JComboBox) {
            comp.setBackground(UIManager.getColor("ComboBox.background"));
            comp.setForeground(UIManager.getColor("ComboBox.foreground"));
        } else if (comp instanceof JSlider) {
            comp.setBackground(UIManager.getColor("Panel.background"));
            comp.setForeground(UIManager.getColor("Label.foreground"));
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }

    public void updateDialogTheme() {
        applyThemeRecursively(this);
        this.revalidate();
        this.repaint();
    }

    private String[] getAvailableThemes() {
        java.io.File themeDir = new java.io.File("data/.config/themes");
        String[] themes = themeDir.list((_, name) -> !name.startsWith(".") && !name.endsWith("~"));
        return themes != null ? themes : THEME_LABELS;
    }
}