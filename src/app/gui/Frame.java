package src.app.gui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import src.app.modules.filter.PFilter;
import src.app.modules.list.PList;
import src.app.modules.terminal.PTerminal;
import src.app.modules.viewer.PViewer;
import src.app.AppController;

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
    private MainBar menuBarModule;
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

    // Remove all module instantiations from Frame constructor
    public Frame(AppController appController) {
        super("Person Manager");
        this.appController = appController;
        // Do not access modules or UI setup here!
    }

    public void initializeUI(GUI pgui) {
        // Now safe to access modules after initModules has been called
        // Assign module fields
        this.personModule = appController.getPersonModule();
        this.menuBarModule = appController.getMainBarModule();
        this.listModule = appController.getListModule();
        this.filterModule = appController.getFilterModule();
        this.terminalModule = appController.getTerminalModule();
        this.fileActions = appController.getDialogsModule();
        appController.reloadConfigAndTheme();
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
        applyThemeFromThemes();
        // --- Ensure robust theme propagation after all UI is attached ---
        if (pgui != null) pgui.forceThemeRefresh();
    }

    private void saveConfig() {
        appController.saveConfig(new java.io.File("data/.config/config"));
    }

    // Remove all theming logic from Frame and delegate to Themes
    public void applyThemeFromThemes() {
        // Only trigger UI updates, do not set UIManager properties
        if (personModule != null && personModule.getPanel() != null)
            recursivelyUpdateComponentTreeUI(personModule.getPanel());
        if (listModule != null && listModule instanceof JComponent)
            recursivelyUpdateComponentTreeUI((JComponent) listModule);
        if (filterModule != null && filterModule instanceof JComponent)
            recursivelyUpdateComponentTreeUI((JComponent) filterModule);
        if (terminalModule != null && terminalModule.getPanel() != null)
            recursivelyUpdateComponentTreeUI(terminalModule.getPanel());
        SwingUtilities.updateComponentTreeUI(this);
        if (getJMenuBar() != null)
            SwingUtilities.updateComponentTreeUI(getJMenuBar());
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    // Recursively update UI for all children
    private void recursivelyUpdateComponentTreeUI(Component c) {
        SwingUtilities.updateComponentTreeUI(c);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                recursivelyUpdateComponentTreeUI(child);
            }
        }
    }

    // Recursively update UI for all children (public for theme refresh)
    public void refreshAllUI() {
        recursivelyUpdateComponentTreeUI(this);
        if (getJMenuBar() != null) SwingUtilities.updateComponentTreeUI(getJMenuBar());
        this.revalidate();
        this.repaint();
    }

    public void reloadConfigAndTheme() {
        // Always reload config and theme from AppController, then re-apply UI
        appController.reloadConfigAndTheme();
        getContentPane().removeAll();
        setupMenuBar();
        setupSplitPane();
        removeBordersRecursively(this);
        getRootPane().setBorder(null);
        applyThemeFromThemes();
        getContentPane().revalidate();
        getContentPane().repaint();
        SwingUtilities.invokeLater(() -> {
            if (splitPane != null) splitPane.setDividerLocation(appController.getSidebarWidth());
            if (rightSplit != null) rightSplit.setDividerLocation(appController.getListTerminalDivider());
            if (filterSplit != null) filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
            setSize(appController.getWindowWidth(), appController.getWindowHeight());
        });
    }

    public void applyConfigAndTheme(Properties props) {
        // Update AppController config/theme, then re-apply UI
        appController.setSidebarWidth(Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(appController.getSidebarWidth()))));
        appController.setFilterWidth(Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(appController.getFilterWidth()))));
        appController.setListTerminalDivider(Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(appController.getListTerminalDivider()))));
        appController.setWindowWidth(Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(appController.getWindowWidth()))));
        appController.setWindowHeight(Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(appController.getWindowHeight()))));
        appController.setThemeName(props.getProperty("THEME", appController.getThemeName()));
        appController.loadTheme(appController.getThemeName());
        reloadConfigAndTheme();
    }

    public void reloadConfigAndApply() {
        appController.reloadConfigAndTheme();
        getContentPane().removeAll();
        setupMenuBar();
        setupSplitPane();
        removeBordersRecursively(this);
        getRootPane().setBorder(null);
        applyThemeFromThemes();
        getContentPane().revalidate();
        getContentPane().repaint();
        // --- Ensure all dimension settings are applied after config reload ---
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
        applyThemeFromThemes();
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
        applyThemeFromThemes();
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
        applyThemeFromThemes();
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
        applyThemeFromThemes();
        getContentPane().revalidate();
        getContentPane().repaint();
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

    public void setupMenuBar() {
        setJMenuBar(menuBarModule.newMainBar());
        menuBarModule.setOnNew(() -> fileActions.doNew(personModule::clearFields, listModule::clearSelection));
        menuBarModule.setOnOpen(() -> fileActions.doOpen(personModule::clearFields, listModule::clearSelection));
        menuBarModule.setOnSave(() -> fileActions.doSave());
        menuBarModule.setOnSaveAs(() -> fileActions.doSaveAs());
        menuBarModule.setOnExportAs(() -> fileActions.doExportAs());
        menuBarModule.setOnImport(() -> fileActions.doImport());
    }

    public void setupSplitPane() {
        int dividerGrabSize = 8; // Visually thin, but easy to grab
        // Create a vertical split for the right panel: list on top, terminal on bottom
        rightSplit = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(listModule),
            terminalModule.getPanel()
        );
        rightSplit.setResizeWeight(0.7);
        rightSplit.setContinuousLayout(true);
        rightSplit.setDividerSize(dividerGrabSize);
        rightSplit.setOneTouchExpandable(false);
        installThinDividerUI(rightSplit);
        // Add filter bar to the right of the rightSplit (list+terminal)
        filterSplit = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            rightSplit,
            filterModule
        );
        filterModule.setMinimumSize(new Dimension(SIDEBAR_MIN_WIDTH, 100));
        filterModule.setPreferredSize(new Dimension(appController.getFilterWidth(), 100));
        filterModule.setMaximumSize(new Dimension(SIDEBAR_MAX_WIDTH, Integer.MAX_VALUE));
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
        // Main split: personModule on left, filterSplit on right
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
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        listModule.refreshList();
        // Set initial divider locations
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(appController.getSidebarWidth());
            rightSplit.setDividerLocation(appController.getListTerminalDivider());
            filterSplit.setDividerLocation(filterSplit.getWidth() - appController.getFilterWidth());
        });
        // Listen for manual divider moves
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

    // Helper to install a custom divider UI that paints a thin accent line in the center
    private void installThinDividerUI(JSplitPane splitPane) {
        Color accent = appController.getThemeColor("ACCENT", Color.BLUE);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        int w = getWidth();
                        int h = getHeight();
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
    
    /**
     * Set up document listeners on text fields to update menu items when fields change
     */
    private void setupFieldChangeListeners() {
        // Add document listeners to all text fields in PersonManager
        personModule.addTextFieldChangeListener(() -> updateSaveMenuItemState());
    }
    
    /**
     * Updates the enabled state of save menu items
     */
    public void updateSaveMenuItemState() {
        if (menuBarModule != null) {
            menuBarModule.updateSaveMenuItems();
        }
    }
    
    /**
     * Check for unsaved changes and prompt the user before exiting if needed
     */
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
    
    /**
     * Configure components to properly handle resize operations
     */
    private void configureResizableComponents() {
        // Ensure modules can resize appropriately
        personModule.getPanel().setMinimumSize(new Dimension(250, 300));
        personModule.getPanel().setPreferredSize(new Dimension(450, 450));
        
        // Add mouse listener to PersonManager to detect when user clicks on it
        // This will reset the mode to creation when clicking away from DataList
        personModule.getPanel().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Clear selection in data list when clicking on PersonManager
                // Only if we're not specifically clicking inside a form component
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

    // Expose these for PGUI
    public JSplitPane getSplitPane() { return splitPane; }
    public JSplitPane getRightSplit() { return rightSplit; }
    public JSplitPane getFilterSplit() { return filterSplit; }
    public MainBar getMenuBarModule() { return menuBarModule; }
    public src.app.modules.list.PList getListModule() { return listModule; }
    public src.app.modules.filter.PFilter getFilterModule() { return filterModule; }
    public src.app.modules.terminal.PTerminal getTerminalModule() { return terminalModule; }
    public src.app.modules.viewer.PViewer getPersonModule() { return personModule; }
    public Dialogs getFileActions() { return fileActions; }
}