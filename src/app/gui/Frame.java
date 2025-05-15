package src.app.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import src.app.modules.filter.PFilter;
import src.app.modules.list.PList;
import src.app.modules.terminal.PTerminal;
import src.app.modules.viewer.PViewer;
import src.app.AppController;
import src.app.dialogs.Dialogs;

import java.awt.*;
import java.awt.event.*;
import java.util.Properties;

/**
 * Modular container for Person Manager application.
 * This class serves as the main application window which loads and manages modules.
 */
public class Frame extends JFrame {
    private AppController appController;
    private PViewer personModule;
    private PList listModule;
    private PFilter filterModule;
    private PTerminal terminalModule;
    private Dialogs fileActions;
    private JSplitPane splitPane;

    private int SIDEBAR_MIN_WIDTH = 170;
    private int SIDEBAR_MAX_WIDTH = 210;

    // Make rightSplit and filterSplit fields for access in methods
    private JSplitPane rightSplit;
    private JSplitPane filterSplit;

    // Define mainPanel as a private JPanel field
    private JPanel mainPanel;

    // --- Menu Bar Creation ---
    private JMenuBar createMainMenuBar() {
        FlatMenuBar menuBar = new FlatMenuBar();
        menuBar.setLayout(new BoxLayout(menuBar, BoxLayout.X_AXIS));
        int barHeight = 26;
        menuBar.setPreferredSize(new Dimension(1, barHeight));
        menuBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, barHeight));
        menuBar.setMinimumSize(new Dimension(1, barHeight));
        // --- File Menu ---
        JMenu fileMenu = new FlatMenu("File");
        JMenuItem newItem = new FlatMenuItem("New");
        JMenuItem openItem = new FlatMenuItem("Open...");
        JMenuItem saveItem = new FlatMenuItem("Save");
        JMenuItem saveAsItem = new FlatMenuItem("Save As...");
        JMenuItem exportAsItem = new FlatMenuItem("Export As...");
        JMenuItem importItem = new FlatMenuItem("Import...");
        JMenuItem exitItem = new FlatMenuItem("Exit");
        newItem.addActionListener(_ -> fileActions.doNew(personModule::clearFields, listModule::clearSelection));
        openItem.addActionListener(_ -> fileActions.doOpen(personModule::clearFields, listModule::clearSelection));
        saveItem.addActionListener(_ -> fileActions.doSave());
        saveAsItem.addActionListener(_ -> fileActions.doSaveAs());
        exportAsItem.addActionListener(_ -> fileActions.doExportAs());
        importItem.addActionListener(_ -> fileActions.doImport());
        exitItem.addActionListener(_ -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(exportAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        // --- Tools Menu ---
        JMenu settingsMenu = new FlatMenu("Tools");
        JMenuItem resetDefaultsItem = new FlatMenuItem("Reset to Default Settings");
        JMenuItem editConfigItem = new FlatMenuItem("Edit Config...");
        resetDefaultsItem.addActionListener(_ -> resetToDefaults());
        editConfigItem.addActionListener(_ -> new src.app.dialogs.ConfigEditorDialog(this, this, "data/.config/config", appController).setVisible(true));
        settingsMenu.add(resetDefaultsItem);
        settingsMenu.add(editConfigItem);
        // --- Help Menu ---
        JMenu helpMenu = new FlatMenu("Help");
        JMenuItem aboutItem = new FlatMenuItem("About");
        aboutItem.addActionListener(_ -> JOptionPane.showMessageDialog(this, "Person Management System\nCreated for OCCC Java Course", "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        // --- Add Menus to Bar with extra spacing ---
        menuBar.add(Box.createHorizontalStrut(12));
        menuBar.add(fileMenu);
        menuBar.add(Box.createHorizontalStrut(18));
        menuBar.add(settingsMenu);
        menuBar.add(Box.createHorizontalStrut(18));
        menuBar.add(helpMenu);
        menuBar.add(Box.createHorizontalGlue());
        // --- Window Controls (Linux-friendly) ---
        JPanel controlsPanel = createSimpleWindowControlsPanel(barHeight);
        menuBar.add(Box.createHorizontalGlue()); // Push controls to the right
        menuBar.add(controlsPanel);
        return menuBar;
    }

    private void resetToDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to reset all settings to default?", "Reset Settings", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                java.nio.file.Files.copy(
                    java.nio.file.Paths.get("data/.config/default"),
                    java.nio.file.Paths.get("data/.config/config"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                Properties props = new Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream("data/.config/config")) {
                    props.load(fis);
                }
                appController.reloadConfigAndTheme();
                applyConfigAndTheme(props);
                JOptionPane.showMessageDialog(this, "Settings reset to default and applied.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to reset settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel createSimpleWindowControlsPanel(int barHeight) {
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        controlsPanel.setOpaque(false);
        controlsPanel.setPreferredSize(new Dimension(90, barHeight));
        controlsPanel.setMaximumSize(new Dimension(90, barHeight));
        controlsPanel.setMinimumSize(new Dimension(90, barHeight));
        WindowControlButton minimizeButton = new WindowControlButton(WindowControlButton.Type.MINIMIZE, barHeight);
        WindowControlButton maximizeButton = new WindowControlButton(WindowControlButton.Type.MAXIMIZE, barHeight);
        WindowControlButton closeButton = new WindowControlButton(WindowControlButton.Type.CLOSE, barHeight);
        minimizeButton.setToolTipText("Minimize");
        maximizeButton.setToolTipText("Maximize/Restore");
        closeButton.setToolTipText("Close");
        minimizeButton.addActionListener(_ -> setState(Frame.ICONIFIED));
        maximizeButton.addActionListener(_ -> setState(getState() == Frame.MAXIMIZED_BOTH ? Frame.NORMAL : Frame.MAXIMIZED_BOTH));
        closeButton.addActionListener(_ -> dispatchEvent(new java.awt.event.WindowEvent(this, java.awt.event.WindowEvent.WINDOW_CLOSING)));
        controlsPanel.add(Box.createHorizontalStrut(6));
        controlsPanel.add(minimizeButton);
        controlsPanel.add(Box.createHorizontalStrut(8));
        controlsPanel.add(maximizeButton);
        controlsPanel.add(Box.createHorizontalStrut(8));
        controlsPanel.add(closeButton);
        controlsPanel.add(Box.createHorizontalStrut(6));
        return controlsPanel;
    }

    // Remove all module instantiations from Frame constructor
    public Frame(AppController appController) {
        super("Person Manager");
        this.appController = appController;
        this.mainPanel = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                applyThemeRecursively(this);
                JMenuBar mb = getJMenuBar();
                if (mb != null) {
                    applyThemeRecursively(mb);
                    themeMenuBarAndControls(mb);
                    updateAllMenus(mb); // Ensure all menu popups update with theme
                }
                // Ensure custom divider UI is re-installed after all L&F updates
                SwingUtilities.invokeLater(() -> {
                    if (splitPane != null) installThinDividerUI(splitPane);
                    if (rightSplit != null) installThinDividerUI(rightSplit);
                    if (filterSplit != null) installThinDividerUI(filterSplit);
                });
            }
        };
        setContentPane(mainPanel);
    }

    public void initializeUI(GUI pgui) {
        // Now safe to access modules after initModules has been called
        // Assign module fields
        this.personModule = appController.getPersonModule();
        this.listModule = appController.getListModule();
        this.filterModule = appController.getFilterModule();
        this.terminalModule = appController.getTerminalModule();
        this.fileActions = appController.getDialogsModule();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(appController.getWindowWidth(), appController.getWindowHeight());
        setupMenuBar();
        setupSplitPane();
        configureResizableComponents();
        setupListeners();
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(appController.getSidebarWidth());
        });
        setupFieldChangeListeners();
        // Remove pgui.refreshAllUI();
        mainPanel.updateUI(); // Ensure theming is applied at startup
    }

    private void saveConfig() {
        appController.saveConfig(new java.io.File("data/.config/config"));
    }

    // Recursively update UI for all children (public for theme refresh)
    public void refreshAllUI() {
        mainPanel.updateUI();
        this.revalidate();
        this.repaint();
        JMenuBar menuBar = getJMenuBar();
        if (menuBar != null) {
            updateAllMenus(menuBar);
        }
    }

    // Recursively update all menus and menu items to force UI refresh after theme change
    public static void updateAllMenus(JMenuBar menuBar) {
        SwingUtilities.updateComponentTreeUI(menuBar);
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                updateMenuRecursively(menu);
            }
        }
    }
    private static void updateMenuRecursively(JMenu menu) {
        SwingUtilities.updateComponentTreeUI(menu);
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item instanceof JMenu) {
                updateMenuRecursively((JMenu) item);
            } else if (item != null) {
                SwingUtilities.updateComponentTreeUI(item);
            }
        }
    }

    // Recursively theme menu bar and its children, including custom window controls
    private void themeMenuBarAndControls(JMenuBar menuBar) {
        Color mainbarBg = UIManager.getColor("MAINBAR_BG");
        Color mainbarFg = UIManager.getColor("MAINBAR_FG");
        if (mainbarBg == null) mainbarBg = UIManager.getColor("Menu.background");
        if (mainbarFg == null) mainbarFg = UIManager.getColor("Menu.foreground");
        for (int i = 0; i < menuBar.getComponentCount(); i++) {
            Component c = menuBar.getComponent(i);
            if (c instanceof JMenu || c instanceof JMenuItem) {
                c.setBackground(mainbarBg);
                c.setForeground(mainbarFg);
                if (c instanceof JComponent) ((JComponent) c).setOpaque(true);
            } else if (c instanceof JPanel) {
                JPanel panel = (JPanel) c;
                panel.setOpaque(false);
                for (Component btn : panel.getComponents()) {
                    if (btn instanceof JButton) {
                        btn.setBackground(mainbarBg);
                        btn.setForeground(mainbarFg);
                        ((JButton) btn).setBorderPainted(false);
                        ((JButton) btn).setContentAreaFilled(false);
                        ((JButton) btn).setOpaque(false);
                    }
                }
            }
        }
        menuBar.setBackground(mainbarBg);
        menuBar.setForeground(mainbarFg);
        menuBar.setOpaque(true);
    }

    private void applyThemeRecursively(Component comp) {
        Color mainbarBg = UIManager.getColor("MAINBAR_BG");
        Color mainbarFg = UIManager.getColor("MAINBAR_FG");
        if (mainbarBg == null) mainbarBg = UIManager.getColor("Menu.background");
        if (mainbarFg == null) mainbarFg = UIManager.getColor("Menu.foreground");
        if (comp instanceof JMenuBar) {
            comp.setBackground(mainbarBg);
            comp.setForeground(mainbarFg);
        } else if (comp instanceof JMenu || comp instanceof JMenuItem) {
            comp.setBackground(mainbarBg);
            comp.setForeground(mainbarFg);
            if (comp instanceof JComponent) ((JComponent) comp).setOpaque(true);
        } else if (comp instanceof JPanel && comp.getParent() instanceof JMenuBar) {
            for (Component btn : ((JPanel) comp).getComponents()) {
                if (btn instanceof JButton) {
                    btn.setBackground(mainbarBg);
                    btn.setForeground(mainbarFg);
                    ((JButton) btn).setBorderPainted(false);
                    ((JButton) btn).setContentAreaFilled(false);
                    ((JButton) btn).setOpaque(false);
                }
            }
        } else if (comp instanceof JSplitPane) {
            installThinDividerUI((JSplitPane) comp);
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }

    // Remove any theme refresh logic (refreshAllUI, reloadConfigAndTheme, etc.) related to theme switching

    public void applyConfigAndTheme(Properties props) {
        // Update AppController config/theme, then re-apply UI
        appController.setSidebarWidth(Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(appController.getSidebarWidth()))));
        appController.setFilterWidth(Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(appController.getFilterWidth()))));
        appController.setListTerminalDivider(Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(appController.getListTerminalDivider()))));
        appController.setWindowWidth(Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(appController.getWindowWidth()))));
        appController.setWindowHeight(Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(appController.getWindowHeight()))));
        appController.setThemeName(props.getProperty("THEME", appController.getThemeName()));
        appController.loadTheme(appController.getThemeName());
        // Removed call to reloadConfigAndTheme();
    }

    public void reloadConfigAndApply() {
        appController.reloadConfigAndTheme();
        getContentPane().removeAll();
        setupMenuBar();
        setupSplitPane();
        removeBordersRecursively(this);
        getRootPane().setBorder(null);
        refreshAllUI();
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) splitPane.setDividerLocation(appController.getSidebarWidth());
            if (rightSplit != null) rightSplit.setDividerLocation(appController.getListTerminalDivider());
            if (filterSplit != null) filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    // Add this method for config live preview
    public void loadConfigFromFile(String configFilePath) {
        appController.loadConfig(new java.io.File(configFilePath));
        appController.loadTheme(appController.getThemeName());
        // Optionally, update split pane and window size for live preview
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(appController.getSidebarWidth());
            rightSplit.setDividerLocation(appController.getListTerminalDivider());
            filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    // New: Apply config and theme live from a Properties object (no file I/O, instant)
    public void applyLiveConfig(Properties props) {
        // Manually update AppController fields from Properties
        appController.setSidebarWidth(Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(appController.getSidebarWidth()))));
        appController.setFilterWidth(Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(appController.getFilterWidth()))));
        appController.setListTerminalDivider(Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(appController.getListTerminalDivider()))));
        appController.setWindowWidth(Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(appController.getWindowWidth()))));
        appController.setWindowHeight(Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(appController.getWindowHeight()))));
        appController.setThemeName(props.getProperty("THEME", appController.getThemeName()));
        appController.loadTheme(appController.getThemeName());
        // Only update UI tree, no full reload
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) splitPane.setDividerLocation(appController.getSidebarWidth());
            if (rightSplit != null) rightSplit.setDividerLocation(appController.getListTerminalDivider());
            if (filterSplit != null) filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    // Minimal: Only update UI from AppController state, no redundant config/theme/data logic
    public void applyLiveConfig() {
        // Only update UI components from AppController's current state
        // Only update UI tree, no full reload
        SwingUtilities.updateComponentTreeUI(this);
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) splitPane.setDividerLocation(appController.getSidebarWidth());
            if (rightSplit != null) rightSplit.setDividerLocation(appController.getListTerminalDivider());
            if (filterSplit != null) filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    // Add this method so AppController can trigger a theme/config refresh
    public void refreshThemeAndConfig(AppController appController) {
        // Only update UI from AppController's current state
        // Optionally update split pane and window size
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) splitPane.setDividerLocation(appController.getSidebarWidth());
            if (rightSplit != null) rightSplit.setDividerLocation(appController.getListTerminalDivider());
            if (filterSplit != null) filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    // Recursively remove borders from all JSplitPane, JScrollPane, and JComponent children
    public void removeBordersRecursively(Component comp) {
        if (comp instanceof javax.swing.JViewport) return; // Prevent setBorder() on JViewport
        if (comp instanceof JSplitPane) {
            ((JSplitPane) comp).setBorder(BorderFactory.createEmptyBorder());
        }
        if (comp instanceof JScrollPane) {
            ((JScrollPane) comp).setBorder(BorderFactory.createEmptyBorder());
        }
        if (comp instanceof JComponent) {
            ((JComponent) comp).setBorder(BorderFactory.createEmptyBorder());
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                removeBordersRecursively(child);
            }
        }
    }

    // Make setupMenuBar and setupSplitPane public for GUI.java
    public void setupMenuBar() { setJMenuBar(createMainMenuBar()); }
    public void setupSplitPane() { setupSplitPaneImpl(); }
    // Move the real implementation to a private method to avoid recursion
    private void setupSplitPaneImpl() {
        int dividerGrabSize = 8;
        rightSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            new JScrollPane((JComponent) listModule),
            terminalModule.getPanel()
        );
        rightSplit.setResizeWeight(0.7);
        rightSplit.setContinuousLayout(true);
        rightSplit.setDividerSize(dividerGrabSize);
        rightSplit.setOneTouchExpandable(false);
        installThinDividerUI(rightSplit);
        filterSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            rightSplit,
            (JComponent) filterModule
        );
        ((JComponent) filterModule).setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 100));
        ((JComponent) filterModule).setPreferredSize(new Dimension(appController.getFilterWidth(), 100));
        ((JComponent) filterModule).setMaximumSize(new Dimension(SIDEBAR_MAX_WIDTH, Integer.MAX_VALUE));
        filterSplit.setResizeWeight(1.0);
        filterSplit.setContinuousLayout(true);
        filterSplit.setDividerSize(dividerGrabSize);
        filterSplit.setOneTouchExpandable(false);
        installThinDividerUI(filterSplit);
        JScrollPane personScroll = new JScrollPane(personModule.getPanel(),
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        personScroll.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 100));
        personScroll.setPreferredSize(new Dimension(appController.getSidebarWidth(), 100));
        personScroll.setMaximumSize(new Dimension(SIDEBAR_MAX_WIDTH, Integer.MAX_VALUE));
        splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            personScroll,
            filterSplit
        );
        splitPane.setResizeWeight(0.0);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(dividerGrabSize);
        splitPane.setOneTouchExpandable(false);
        installThinDividerUI(splitPane);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        listModule.refreshList();
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(appController.getSidebarWidth());
            rightSplit.setDividerLocation(appController.getListTerminalDivider());
            filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
        });
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, _ -> {
            appController.setSidebarWidth(splitPane.getDividerLocation());
            saveConfig();
        });
        rightSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, _ -> {
            appController.setListTerminalDivider((double) rightSplit.getDividerLocation() / (double) rightSplit.getHeight());
            saveConfig();
        });
        filterSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, _ -> {
            appController.setFilterWidth(filterSplit.getWidth() - filterSplit.getDividerLocation());
            saveConfig();
        });
    }

    private void configureResizableComponents() {
        personModule.getPanel().setMinimumSize(new Dimension(250, 300));
        personModule.getPanel().setPreferredSize(new Dimension(450, 450));
        personModule.getPanel().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!(e.getSource() instanceof JTextField || e.getSource() instanceof JFormattedTextField)) {
                    listModule.clearSelection();
                    personModule.clearFields();
                }
            }
        });
        if (listModule instanceof JComponent) {
            ((JComponent) listModule).setMinimumSize(new Dimension(250, 300));
            ((JComponent) listModule).setPreferredSize(new Dimension(450, 450));
        }
    }

    private void setupListeners() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                appController.setWindowWidth(getWidth());
                appController.setWindowHeight(getHeight());
                saveConfig();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExitIfUnsaved();
            }
        });
    }

    private void setupFieldChangeListeners() {
        // Remove broken call to addTextFieldChangeListener(this::updateSaveMenuItems)
        // If you want to update save menu items on field change, implement a valid listener here.
    }

    // --- Public Getters for GUI and other classes ---
    public JSplitPane getSplitPane() { return splitPane; }
    public JSplitPane getRightSplit() { return rightSplit; }
    public JSplitPane getFilterSplit() { return filterSplit; }
    public PList getListModule() { return listModule; }
    public PFilter getFilterModule() { return filterModule; }
    public PTerminal getTerminalModule() { return terminalModule; }
    public PViewer getPersonModule() { return personModule; }
    public Dialogs getFileActions() { return fileActions; }
    
    // Helper to install a custom divider UI that paints a thin accent line in the center
    private void installThinDividerUI(JSplitPane splitPane) {
        AppController controller = this.appController;
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        int w = getWidth();
                        int h = getHeight();
                        // Always fetch the accent color from AppController for live theme support
                        Color accent = controller.getThemeColor("ACCENT", Color.BLUE);
                        g.setColor(accent);
                        if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                            int x = w / 2;
                            g.fillRect(x, 0, 1, h);
                        } else {
                            int y = h / 2;
                            g.fillRect(0, y, w, 1);
                        }
                    }
                };
            }
        });
    }

    // Confirm exit helper for window closing
    private void confirmExitIfUnsaved() {
        if (appController.isModified()) {
            Object[] options = {"Save", "Don't Save", "Cancel"};
            int response = JOptionPane.showOptionDialog(
                this,
                "There are unsaved changes. What would you like to do?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]
            );
            switch (response) {
                case JOptionPane.YES_OPTION:
                    try {
                        fileActions.doSaveAs();
                        if (!appController.isModified()) {
                            System.exit(0);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            this,
                            "Error saving file: " + ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    System.exit(0);
                    break;
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    break;
            }
        } else {
            System.exit(0);
        }
    }

    // --- Custom FlatMenu and FlatMenuItem for seamless menu bar theming ---
    public static class FlatMenu extends JMenu {
        public FlatMenu(String text) { super(text); setOpaque(true); setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16)); }
        @Override
        public void updateUI() {
            super.updateUI();
            Color fg = UIManager.getColor("MENU_FG");
            Color bg = UIManager.getColor("MENU_BG");
            if (fg == null) fg = UIManager.getColor("Menu.foreground");
            if (bg == null) bg = UIManager.getColor("Menu.background");
            setForeground(fg);
            setBackground(bg);
            setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        }
    }
    public static class FlatMenuItem extends JMenuItem {
        public FlatMenuItem(String text) { super(text); setOpaque(true); setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16)); }
        @Override
        public void updateUI() {
            super.updateUI();
            Color fg = UIManager.getColor("MENU_FG");
            Color bg = UIManager.getColor("MENU_BG");
            Color selFg = UIManager.getColor("MENU_SEL_FG");
            Color selBg = UIManager.getColor("MENU_SEL_BG");
            if (fg == null) fg = UIManager.getColor("MenuItem.foreground");
            if (bg == null) bg = UIManager.getColor("MenuItem.background");
            if (selFg == null) selFg = UIManager.getColor("MenuItem.selectionForeground");
            if (selBg == null) selBg = UIManager.getColor("MenuItem.selectionBackground");
            setForeground(fg);
            setBackground(bg);
            setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
            // Set selection colors for L&F that respect them
            putClientProperty("MenuItem.selectionForeground", selFg);
            putClientProperty("MenuItem.selectionBackground", selBg);
        }
    }
    // --- Custom FlatMenuBar for seamless menu bar theming ---
    public static class FlatMenuBar extends JMenuBar {
        @Override
        protected void paintComponent(Graphics g) {
            Color mainbarBg = UIManager.getColor("MAINBAR_BG");
            if (mainbarBg == null) mainbarBg = UIManager.getColor("Menu.background");
            g.setColor(mainbarBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    // --- Custom WindowControlButton for robust label rendering ---
    public static class WindowControlButton extends JButton {
        public enum Type { MINIMIZE, MAXIMIZE, CLOSE }
        private final Type type;
        private boolean hovered = false;
        private boolean pressed = false;
        private final int barHeight;
        public WindowControlButton(Type type, int barHeight) {
            super("");
            this.type = type;
            this.barHeight = barHeight;
            setOpaque(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            int btnSize = barHeight - 12;
            setPreferredSize(new Dimension(btnSize, btnSize));
            setMaximumSize(new Dimension(btnSize, btnSize));
            setMinimumSize(new Dimension(btnSize, btnSize));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; pressed = false; repaint(); }
                @Override public void mousePressed(java.awt.event.MouseEvent e) { pressed = true; repaint(); }
                @Override public void mouseReleased(java.awt.event.MouseEvent e) { pressed = false; repaint(); }
            });
        }
        @Override
        public void updateUI() {
            super.updateUI();
            setUI(new javax.swing.plaf.basic.BasicButtonUI());
            setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(9, barHeight/4)));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fg = UIManager.getColor("MAINBAR_FG");
            Color closeFg = UIManager.getColor("CLOSE_BUTTON_FG");
            Color mainbarBg = UIManager.getColor("MAINBAR_BG");
            if (fg == null) fg = Color.DARK_GRAY;
            if (closeFg == null) closeFg = Color.RED;
            if (mainbarBg == null) mainbarBg = Color.LIGHT_GRAY;
            // Only paint a subtle hover/pressed effect
            if (pressed) {
                g2.setColor(new Color(0,0,0,36));
                g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
            } else if (hovered) {
                g2.setColor(new Color(0,0,0,18));
                g2.fillOval(2, 2, getWidth()-4, getHeight()-4);
            }
            // Icon
            int w = getWidth(), h = getHeight();
            g2.setStroke(new BasicStroke(Math.max(1.1f, barHeight/24f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Color iconColor = (type == Type.CLOSE) ? closeFg : fg;
            g2.setColor(iconColor);
            int cx = w / 2, cy = h / 2;
            int iconSize = Math.max(5, barHeight/4);
            if (type == Type.MINIMIZE) {
                g2.drawLine(cx - iconSize, cy + iconSize/2, cx + iconSize, cy + iconSize/2);
            } else if (type == Type.MAXIMIZE) {
                g2.drawRoundRect(cx - iconSize, cy - iconSize, iconSize*2, iconSize*2, 3, 3);
            } else if (type == Type.CLOSE) {
                g2.drawLine(cx - iconSize, cy - iconSize, cx + iconSize, cy + iconSize);
                g2.drawLine(cx + iconSize, cy - iconSize, cx - iconSize, cy + iconSize);
            }
            g2.dispose();
        }
    }
}