package src.app.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;

import src.app.AppController;
import src.app.gui.GuiAPI;

import java.util.List;
import java.util.ArrayList;

public class ThemeChange extends JDialog {
    private final String configPath;
    private Properties properties;
    private Properties originalProperties;
    private Object personGui;
    private JTable themeTable;
    private DefaultTableModel themeTableModel;
    private JScrollPane themeScrollPane;
    private final AppController appController;

    // Only keep theme-related config
    private static final String[] THEME_LABELS = {"light", "dark", "developer"};
    static {
    }

    public ThemeChange(Object personGui, JFrame parent, String configPath, AppController appController) {
        super(parent, "Theme Changer", true);
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
        properties = new Properties();
        File configFile = new File(configPath);
        if (configFile.exists()) {
            appController.loadConfig(configFile);
        }
        properties.setProperty("THEME", appController.getThemeName());
        // Save a copy for cancel/revert
        originalProperties = new Properties();
        originalProperties.putAll(properties);
    }

    private void buildUI() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) bg = Color.WHITE;
        if (fg == null) fg = Color.BLACK;
        formPanel.setBackground(bg);
        // --- THEME TABLE AT TOP ---
        // Remove the extra "Theme" label above the table
        // Table setup
        String[] columnNames = {"Themes"};
        themeTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        // Always add "light" and "dark" first
        themeTableModel.addRow(new Object[]{"light"});
        themeTableModel.addRow(new Object[]{"dark"});
        Set<String> added = new HashSet<>();
        added.add("light");
        added.add("dark");
        String[] themes = getAvailableThemes();
        for (String theme : themes) {
            if (!added.contains(theme)) {
                themeTableModel.addRow(new Object[]{theme});
                added.add(theme);
            }
        }
        themeTable = new JTable(themeTableModel);
        // Defensive: Remove all ListSelectionListeners before updating selection programmatically
        ListSelectionModel selectionModel = themeTable.getSelectionModel();
        List<ListSelectionListener> listeners = new ArrayList<>();
        for (ListSelectionListener l : ((javax.swing.DefaultListSelectionModel) selectionModel).getListSelectionListeners()) {
            listeners.add(l);
            selectionModel.removeListSelectionListener(l);
        }
        // Select current theme (always sync to config)
        String currentTheme = appController.getThemeName();
        for (int i = 0; i < themeTableModel.getRowCount(); i++) {
            if (themeTableModel.getValueAt(i, 0).equals(currentTheme)) {
                themeTable.setRowSelectionInterval(i, i);
                break;
            }
        }
        // Restore listeners after programmatic selection
        for (ListSelectionListener l : listeners) selectionModel.addListSelectionListener(l);
        if (themeTable != null) {
            themeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            themeTable.setFont(UIManager.getFont("Table.font"));
            themeTable.setBackground(UIManager.getColor("Table.background"));
            themeTable.setForeground(UIManager.getColor("Table.foreground"));
            themeTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
            themeTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
            themeTable.setRowHeight(32);
            themeTable.setShowGrid(false);
            themeTable.setIntercellSpacing(new Dimension(0, 0));
            themeTable.setFillsViewportHeight(true);
            // Center header text
            JTableHeader header = themeTable.getTableHeader();
            if (header != null && header.getDefaultRenderer() instanceof DefaultTableCellRenderer) {
                ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
                header.setFont(header.getFont().deriveFont(Font.BOLD, 15f));
                header.setReorderingAllowed(false);
            }
            // Center cell text
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            if (themeTable.getColumnModel().getColumnCount() > 0) {
                themeTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
            }
            // Select current theme (always sync to config)
            String currentTheme2 = appController.getThemeName();
            for (int i = 0; i < themeTableModel.getRowCount(); i++) {
                if (themeTableModel.getValueAt(i, 0).equals(currentTheme2)) {
                    themeTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
            themeTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && themeTable != null && themeTable.getColumnModel() != null) {
                    int row = themeTable.getSelectedRow();
                    if (row >= 0 && row < themeTableModel.getRowCount()) {
                        String selectedTheme = (String) themeTableModel.getValueAt(row, 0);
                        if (!selectedTheme.equals(appController.getThemeName())) {
                            SwingUtilities.invokeLater(() -> {
                                properties.setProperty("THEME", selectedTheme);
                                appController.setThemeName(selectedTheme);
                                appController.saveConfig(new File(configPath));
                                if (personGui instanceof GuiAPI guiApi) {
                                    guiApi.reloadConfigAndTheme();
                                }
                                src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
                                // Only updateDialogTheme if table is still valid
                                if (themeTable != null && themeTable.getColumnModel() != null) {
                                    updateDialogTheme();
                                }
                            });
                        }
                    }
                }
            });
        }
        themeScrollPane = new JScrollPane(themeTable);
        themeScrollPane.setBorder(BorderFactory.createEmptyBorder());
        themeScrollPane.getViewport().setBackground(bg);
        themeScrollPane.setPreferredSize(new Dimension(600, 240));
        themeScrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setLayout(new BorderLayout());
        formPanel.add(themeScrollPane, BorderLayout.CENTER);
        formPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        add(formPanel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(bg);
        JButton doneBtn = new JButton("Done");
        Color accent = UIManager.getColor("nimbusFocus");
        if (accent == null) accent = new Color(60, 120, 220);
        doneBtn.setBackground(accent);
        doneBtn.setForeground(Color.WHITE);
        Font btnFont = doneBtn.getFont();
        if (btnFont == null) btnFont = UIManager.getFont("Button.font");
        if (btnFont == null) btnFont = new Font("SansSerif", Font.BOLD, 13);
        doneBtn.setFont(btnFont.deriveFont(Font.BOLD, 13f));
        doneBtn.setFocusPainted(false);
        doneBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        doneBtn.addActionListener(_ -> {
            int row = themeTable.getSelectedRow();
            if (row >= 0 && row < themeTableModel.getRowCount()) {
                String selectedTheme = (String) themeTableModel.getValueAt(row, 0);
                appController.setThemeName(selectedTheme);
                appController.loadTheme(selectedTheme);
                appController.saveConfig(new File(configPath));
                if (personGui instanceof GuiAPI guiApi) {
                    guiApi.reloadConfigAndTheme();
                }
                src.app.gui.Themes.applyThemeAndRefreshAllWindows(appController.getThemeProperties());
            }
            dispose();
        });
        buttonPanel.add(doneBtn);
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
        // Update table colors and renderer for live theme switching
        if (themeTable != null) {
            themeTable.setFont(UIManager.getFont("Table.font"));
            themeTable.setBackground(UIManager.getColor("Table.background"));
            themeTable.setForeground(UIManager.getColor("Table.foreground"));
            themeTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
            themeTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
            javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (isSelected) {
                        Color selBg = UIManager.getColor("Table.selectionBackground");
                        Color selFg = UIManager.getColor("Table.selectionForeground");
                        if (selBg == null) selBg = table.getSelectionBackground();
                        if (selFg == null) selFg = table.getSelectionForeground();
                        c.setBackground(selBg);
                        c.setForeground(selFg);
                    } else {
                        Color even = UIManager.getColor("Table.background");
                        Color odd = UIManager.getColor("Table.alternateRowColor");
                        if (even == null) even = Color.WHITE;
                        if (odd == null) odd = new Color(245,245,245);
                        c.setBackground((row % 2 == 0) ? even : odd);
                        c.setForeground(UIManager.getColor("Table.foreground"));
                    }
                    return c;
                }
            };
            themeTable.setDefaultRenderer(Object.class, renderer);
            themeTable.revalidate();
            themeTable.repaint();
        }
        // Always sync selected row to current config theme
        if (themeTable != null && themeTableModel != null && themeTable.getColumnModel() != null) {
            String currentTheme = appController.getThemeName();
            // Remove listeners before programmatic selection to avoid recursion/NPE
            ListSelectionModel selectionModel = themeTable.getSelectionModel();
            List<ListSelectionListener> listeners = new ArrayList<>();
            for (ListSelectionListener l : ((javax.swing.DefaultListSelectionModel) selectionModel).getListSelectionListeners()) {
                listeners.add(l);
                selectionModel.removeListSelectionListener(l);
            }
            for (int i = 0; i < themeTableModel.getRowCount(); i++) {
                if (themeTableModel.getValueAt(i, 0).equals(currentTheme)) {
                    themeTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
            for (ListSelectionListener l : listeners) selectionModel.addListSelectionListener(l);
        }
        if (themeScrollPane != null) {
            themeScrollPane.updateUI();
            themeScrollPane.revalidate();
            themeScrollPane.repaint();
        }
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