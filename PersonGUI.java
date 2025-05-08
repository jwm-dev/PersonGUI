import javax.swing.*;

import src.app.Bar;
import src.app.DataList;
import src.app.DataManager;
import src.app.PersonManager;

import java.awt.*;
import java.awt.event.*;

/**
 * Modular container for Person Manager application.
 * This class serves as the main application window which loads and manages modules.
 */
public class PersonGUI extends JFrame {
    private PersonManager personModule;
    private Bar menuBarModule;
    private DataList listModule;
    private DataManager dataManager;
    private JSplitPane splitPane;

    public PersonGUI() {
        super("Person Manager");
        // Use DO_NOTHING_ON_CLOSE so we can handle closing manually
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 500); // Increased window size to accommodate split panes
        
        // Create the data manager
        dataManager = new DataManager();
        
        // Create the person management module with data manager
        personModule = new PersonManager(this, dataManager);
        
        // Create the list module with the same data manager
        listModule = new DataList(dataManager);
        
        // Connect the modules bidirectionally for synchronization
        listModule.setPersonManager(personModule);
        personModule.setDataList(listModule);
        
        // Create the menu bar module and set it as the frame's menu bar
        menuBarModule = new Bar(this, personModule, dataManager);
        setJMenuBar(menuBarModule.createMenuBar());
        
        // Configure panels to handle resize
        configureResizableComponents();
        
        // Create a split pane to hold both modules
        splitPane = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            new JScrollPane(personModule),
            new JScrollPane(listModule)
        );
        splitPane.setResizeWeight(0.5); // Equal initial sizing
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(8); // Slightly larger divider for easier handling
        splitPane.setOneTouchExpandable(true); // Add one-touch expand buttons
        
        // Add the split pane to the frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        
        // Initialize the list with any existing data
        listModule.refreshList();
        
        // Set minimum size for better resize behavior
        setMinimumSize(new Dimension(600, 400));
        
        // Add component listener to handle window resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (splitPane != null) {
                    // Maintain the proportional sizing when window is resized
                    splitPane.setDividerLocation(splitPane.getResizeWeight());
                }
            }
        });
        
        // Add window listener to handle closing with unsaved changes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExitIfUnsaved();
            }
        });
        
        // Center the window on the screen
        setLocationRelativeTo(null);
        
        // Set divider location after components are visible
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.5);
        });
        
        // Set up document listeners to track field changes
        setupFieldChangeListeners();
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
        if (dataManager.isModified()) {
            // There are unsaved changes, prompt the user
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
                    // Save then exit
                    try {
                        // If no current file, show Save As dialog
                        if (dataManager.getCurrentFile() == null) {
                            personModule.doSaveAs();
                            
                            // Check if saving was successful (user might have cancelled)
                            if (!dataManager.isModified()) {
                                System.exit(0);
                            }
                        } else {
                            // Save to current file then exit
                            personModule.doSave();
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
                    // Exit without saving
                    System.exit(0);
                    break;
                    
                case JOptionPane.CANCEL_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    // Do nothing, user cancelled exit
                    break;
            }
        } else {
            // No unsaved changes, just exit
            System.exit(0);
        }
    }
    
    /**
     * Configure components to properly handle resize operations
     */
    private void configureResizableComponents() {
        // Ensure modules can resize appropriately
        if (personModule instanceof JComponent) {
            ((JComponent) personModule).setMinimumSize(new Dimension(250, 300));
            ((JComponent) personModule).setPreferredSize(new Dimension(450, 450));
            
            // Add mouse listener to PersonManager to detect when user clicks on it
            // This will reset the mode to creation when clicking away from DataList
            personModule.addMouseListener(new java.awt.event.MouseAdapter() {
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
        }
        
        if (listModule instanceof JComponent) {
            ((JComponent) listModule).setMinimumSize(new Dimension(250, 300));
            ((JComponent) listModule).setPreferredSize(new Dimension(450, 450));
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            PersonGUI gui = new PersonGUI();
            gui.setVisible(true);
        });
    }
}