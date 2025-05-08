package src.app;

import javax.swing.*;
import java.awt.*;

/**
 * A module that provides menu bar functionality for the Person Management application.
 */
public class Bar {
    private JFrame parentFrame;
    private PersonManager guiModule;
    private DataManager dataManager;
    private JMenuItem saveItem, saveAsItem, exportAsItem;
    
    /**
     * Creates a menu bar module
     * @param parent The parent JFrame
     * @param module The GUI module that will handle menu actions
     * @param manager The data manager to check for save status
     */
    public Bar(JFrame parent, PersonManager module, DataManager manager) {
        this.parentFrame = parent;
        this.guiModule = module;
        this.dataManager = manager;
    }
    
    /**
     * Creates and returns the application menu bar
     * @return The configured JMenuBar
     */
    public JMenuBar createMenuBar() {
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
        
        newItem.addActionListener(_ -> guiModule.doNew());
        openItem.addActionListener(_ -> guiModule.doOpen());
        saveItem.addActionListener(_ -> guiModule.doSave());
        saveAsItem.addActionListener(_ -> guiModule.doSaveAs());
        exportAsItem.addActionListener(_ -> guiModule.doExportAs());
        importItem.addActionListener(_ -> guiModule.doImport());
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

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(_ -> 
            JOptionPane.showMessageDialog(parentFrame, 
                "Person Management System\nCreated for OCCC Java Course", 
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
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
        boolean hasValidData = hasData && !guiModule.hasPartialData();
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
        
        // Give the close button a distinctive color
        closeButton.setForeground(Color.RED);
        
        // Add buttons to panel
        controlsPanel.add(minimizeButton);
        controlsPanel.add(maximizeButton);
        controlsPanel.add(closeButton);
        
        return controlsPanel;
    }
}
