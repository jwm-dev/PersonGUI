package src.app.gui;

import javax.swing.*;

import src.app.AppController;

import java.awt.*;

/**
 * A module that provides menu bar functionality for the Person Management application.
 */
public class MainBar {
    private JFrame parentFrame;
    private Object reloadTarget;
    private AppController dataManager;
    private JMenuItem saveItem, saveAsItem, exportAsItem;
    // Menu action callbacks
    private Runnable onNew, onOpen, onSave, onSaveAs, onExportAs, onImport;
    private Color closeButtonColor = Color.RED; // Default, will be set by theme
    
    /**
     * Creates a menu bar module
     * @param parentFrame The JFrame instance
     * @param manager The data manager to check for save status
     */
    public MainBar(JFrame parentFrame, AppController manager) {
        this(parentFrame, manager, parentFrame);
    }

    /**
     * Creates a menu bar module
     * @param parentFrame The JFrame instance
     * @param manager The data manager to check for save status
     * @param reloadTarget The target object for hot reload
     */
    public MainBar(JFrame parentFrame, AppController manager, Object reloadTarget) {
        this.parentFrame = parentFrame;
        this.dataManager = manager;
        this.reloadTarget = reloadTarget;
    }
    
    // Setters for menu action callbacks
    public void setOnNew(Runnable onNew) { this.onNew = onNew; }
    public void setOnOpen(Runnable onOpen) { this.onOpen = onOpen; }
    public void setOnSave(Runnable onSave) { this.onSave = onSave; }
    public void setOnSaveAs(Runnable onSaveAs) { this.onSaveAs = onSaveAs; }
    public void setOnExportAs(Runnable onExportAs) { this.onExportAs = onExportAs; }
    public void setOnImport(Runnable onImport) { this.onImport = onImport; }
    
    public void setThemeColors(Color closeButtonColor) {
        this.closeButtonColor = closeButtonColor;
    }
    
    /**
     * Creates and returns the application menu bar
     * @return The configured JMenuBar
     */
    public JMenuBar newMainBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open...");
        saveItem = new JMenuItem("Save");
        saveAsItem = new JMenuItem("Save As...");
        exportAsItem = new JMenuItem("Export As...");
        JMenuItem importItem = new JMenuItem("Import...");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        newItem.addActionListener(_ -> { if (onNew != null) onNew.run(); });
        openItem.addActionListener(_ -> { if (onOpen != null) onOpen.run(); });
        saveItem.addActionListener(_ -> { if (onSave != null) onSave.run(); });
        saveAsItem.addActionListener(_ -> { if (onSaveAs != null) onSaveAs.run(); });
        exportAsItem.addActionListener(_ -> { if (onExportAs != null) onExportAs.run(); });
        importItem.addActionListener(_ -> { if (onImport != null) onImport.run(); });
        // Updated to trigger unsaved changes check
        exitItem.addActionListener(_ -> {
            // Fire window closing event to trigger the same check as when clicking X
            parentFrame.dispatchEvent(new java.awt.event.WindowEvent(
                parentFrame, java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
        
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.add(exportAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Settings Menu
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem resetDefaultsItem = new JMenuItem("Reset to Default Settings");
        JMenuItem editConfigItem = new JMenuItem("Edit Config...");
        resetDefaultsItem.addActionListener(_ -> {
            int confirm = JOptionPane.showConfirmDialog(parentFrame, "Are you sure you want to reset all settings to default?", "Reset Settings", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    java.nio.file.Files.copy(
                        java.nio.file.Paths.get("data/.config/default"),
                        java.nio.file.Paths.get("data/.config/config"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    // Load the new config and theme from the default file
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.FileInputStream fis = new java.io.FileInputStream("data/.config/config")) {
                        props.load(fis);
                    }
                    dataManager.reloadConfigAndTheme();
                    // Apply config and theme live
                    dataManager.applyConfigAndTheme(props, () -> {
                        if (reloadTarget instanceof GuiAPI guiApi) {
                            guiApi.reloadConfigAndTheme();
                        }
                    });
                    JOptionPane.showMessageDialog(parentFrame, "Settings reset to default and applied.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parentFrame, "Failed to reset settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        editConfigItem.addActionListener(_ -> {
            new src.app.gui.ConfigEditorDialog(reloadTarget, parentFrame, "data/.config/config", dataManager).setVisible(true);
        });
        settingsMenu.add(resetDefaultsItem);
        settingsMenu.add(editConfigItem);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(_ -> 
            JOptionPane.showMessageDialog(parentFrame, 
                "Person Management System\nCreated for OCCC Java Course", 
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        
        // Add simple window control buttons to the right
        JPanel controlsPanel = createSimpleWindowControlsPanel();
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(controlsPanel);
        
        // Initialize save button states
        updateSaveMenuItems();
        
        return menuBar;
    }
    
    /**
     * Updates the enabled state of save menu items based on current application state
     */
    public void updateSaveMenuItems() {
        if (saveItem == null || saveAsItem == null || exportAsItem == null) {
            return; // Menu items not yet created
        }
        
        boolean hasData = !dataManager.getPeople().isEmpty();
        // For save validation, we need a way to check for partial data. We'll let the callback handle this.
        boolean hasValidData = hasData; // The GUI will handle disabling if partial data
        boolean hasChanges = dataManager.hasChanges();
        boolean isFileOpen = dataManager.getCurrentFile() != null;
        
        // Disable save options if:
        // 1. The People collection is empty
        // 2. There is incomplete/partial data in PersonManager
        // 3. There are no changes since file was opened/created new
        saveItem.setEnabled(hasValidData && hasChanges);
        
        // For Save As and Export As:
        // 1. Enable when there's data
        // 2. Enable when a file is already open, even if empty (allows saving empty files)
        // 3. Disable when there's no data AND no file is open
        saveAsItem.setEnabled(hasData || isFileOpen);
        exportAsItem.setEnabled(hasData || isFileOpen);
    }
    
    /**
     * Creates a panel with simple text-based window control buttons
     * that should work across all platforms
     * @return Panel containing window control buttons
     */
    private JPanel createSimpleWindowControlsPanel() {
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        controlsPanel.setOpaque(false);
        
        // Simple text-based buttons that should work on all platforms
        JButton minimizeButton = new JButton("_");
        JButton maximizeButton = new JButton("□");
        JButton closeButton = new JButton("X");
        
        // Use standard fonts with fallbacks
        Font buttonFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        
        minimizeButton.setFont(buttonFont);
        maximizeButton.setFont(buttonFont);
        closeButton.setFont(buttonFont);
        
        minimizeButton.setToolTipText("Minimize");
        maximizeButton.setToolTipText("Maximize/Restore");
        closeButton.setToolTipText("Close");
        
        // Keep button handlers simple with good error handling
        minimizeButton.addActionListener(_ -> {
            try {
                // Alternative approach that works on more platforms
                parentFrame.setState(Frame.ICONIFIED);
            } catch (Exception ex) {
                System.err.println("Warning: Could not minimize window: " + ex.getMessage());
            }
        });
        
        maximizeButton.addActionListener(_ -> {
            try {
                // Simple toggle approach
                if (parentFrame.getState() == Frame.MAXIMIZED_BOTH) {
                    parentFrame.setState(Frame.NORMAL);
                } else {
                    parentFrame.setState(Frame.MAXIMIZED_BOTH);
                }
            } catch (Exception ex) {
                System.err.println("Warning: Could not maximize/restore window: " + ex.getMessage());
            }
        });
        
        closeButton.addActionListener(_ -> {
            // Trigger the same window closing event as clicking X
            parentFrame.dispatchEvent(new java.awt.event.WindowEvent(
                parentFrame, java.awt.event.WindowEvent.WINDOW_CLOSING));
        });
        
        // Style the buttons minimally to look good across platforms
        for (JButton button : new JButton[] { minimizeButton, maximizeButton, closeButton }) {
            button.setMargin(new Insets(1, 4, 1, 4));
            button.setFocusPainted(false);
        }
        
        // Use theme color for close button
        closeButton.setForeground(closeButtonColor);
        
        // Add buttons to panel
        controlsPanel.add(minimizeButton);
        controlsPanel.add(maximizeButton);
        controlsPanel.add(closeButton);
        
        return controlsPanel;
    }

    // Add this method so AppController can trigger a theme/config refresh
    public void refreshThemeAndConfig(AppController appController) {
        // Always pull theme/config from AppController
        java.util.Properties themeProps = appController.getThemeProperties();
        Color closeButtonColor = Color.RED;
        String closeColorStr = themeProps.getProperty("CLOSE_BUTTON_FG");
        if (closeColorStr != null) {
            try { closeButtonColor = Color.decode(closeColorStr); } catch (Exception ignored) {}
        }
        setThemeColors(closeButtonColor);
        // Force menu bar and children to update if needed
        // (Assume parentFrame is a Frame and will update the menu bar)
        if (parentFrame instanceof JFrame frame) {
            JMenuBar jmb = frame.getJMenuBar();
            if (jmb != null) {
                SwingUtilities.updateComponentTreeUI(jmb);
                jmb.repaint();
            }
        }
    }
}
