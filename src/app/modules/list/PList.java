package src.app.modules.list;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import src.app.AppController;
import src.app.modules.viewer.PViewer;
import src.date.OCCCDate;
import src.person.OCCCPerson;
import src.person.People;
import src.person.Person;
import src.person.RegisteredPerson;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.function.Predicate;

/**
 * A module that displays all Person objects in a sortable table
 */
public class PList extends JPanel implements AppController.DataChangeListener {
    private JTable personTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private AppController dataManager;
    private PViewer personManager; // Reference to the person manager module
    private boolean ignoreSelectionEvents = false;
    private JLabel statusLabel; // Status label to show currently selected person
    private JLabel titleLabel; // Title label that will show the current file name
    private java.util.List<Person> filteredPeople = null;
    private Predicate<Person> currentFilter = null;
    
    /**
     * Creates a new List module
     * @param manager The data manager to use
     */
    public PList(AppController manager) {
        this.dataManager = manager;
        
        // Register as a listener for data changes
        manager.addDataChangeListener(this);
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create section title
        titleLabel = new JLabel("Person List");
        titleLabel.setFont(UIManager.getFont("Label.font"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        // Add title to the top of the panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        add(headerPanel, BorderLayout.NORTH);
        
        // Create column headers - Add GID and SID columns
        String[] columnNames = {"Type", "First Name", "Last Name", "Date of Birth", "Age", "GID", "SID"};
        
        // Create table model
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
            
            // Define column classes for proper sorting
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;  // Type
                    case 1: return String.class;  // First Name
                    case 2: return String.class;  // Last Name
                    case 3: return OCCCDate.class; // DOB
                    case 4: return Integer.class; // Age
                    case 5: return String.class;  // GID
                    case 6: return String.class;  // SID
                    default: return Object.class;
                }
            }
        };
        
        // Create the table with the model
        personTable = new JTable(tableModel);
        personTable.setFont(UIManager.getFont("Table.font"));
        
        // Custom date renderer for proper date display
        personTable.setDefaultRenderer(OCCCDate.class, new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof OCCCDate) {
                    // Use the OCCCDate's toString() method which formats the date
                    value = value.toString();
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        
        // Custom cell renderer to handle ID fields and highlighting
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                // Handle special ID values
                if ((column == 5 || column == 6) && value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.equals("-1")) {
                        value = "";  // Display as empty for -1 ID values
                    }
                }
                
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
                
                // Use theme colors only
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        };
        
        // Apply the custom renderer to all columns
        for (int i = 0; i < personTable.getColumnCount(); i++) {
            personTable.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }
        
        // Add sorting capability
        sorter = new TableRowSorter<>(tableModel);
        personTable.setRowSorter(sorter);
        
        // Set column widths
        personTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Type
        personTable.getColumnModel().getColumn(1).setPreferredWidth(100); // First Name
        personTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Last Name
        personTable.getColumnModel().getColumn(3).setPreferredWidth(100); // DOB
        personTable.getColumnModel().getColumn(4).setPreferredWidth(50);  // Age
        personTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // GID
        personTable.getColumnModel().getColumn(6).setPreferredWidth(80);  // SID
        
        // Enable auto resize mode for better table resizing
        personTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        
        // Create status label to show selected person
        statusLabel = new JLabel("No person selected");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        // Create a panel for the table and status label
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(new JScrollPane(personTable), BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Add selection listener to the table
        personTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!ignoreSelectionEvents && !e.getValueIsAdjusting()) {
                    int selectedRow = personTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        int modelRow = personTable.convertRowIndexToModel(selectedRow);
                        Person selectedPerson = dataManager.getPeople().get(modelRow);
                        
                        // Update status label with selected person information
                        updateStatusLabel(selectedPerson);
                        
                        // Notify the PersonManager to display this person
                        personManager.displayPersonDetails(selectedPerson);
                    } else {
                        // No selection
                        statusLabel.setText("No person selected");
                    }
                }
            }
        });
        
        // Initialize the title based on current file (if any)
        updateTitleLabel();
    }
    
    /**
     * Updates the status label with information about the selected person
     */
    private void updateStatusLabel(Person person) {
        if (person != null) {
            String personType = "Person";
            if (person instanceof OCCCPerson) {
                personType = "OCCC Person";
            } else if (person instanceof RegisteredPerson) {
                personType = "Registered Person";
            }
            statusLabel.setText("Selected: " + personType + " - " + person.getFirstName() + " " + person.getLastName());
        } else {
            statusLabel.setText("No person selected");
        }
    }
    
    /**
     * Refresh the list with current data
     */
    public void refreshList() {
        try {
            // Clear existing rows safely
            SwingUtilities.invokeLater(() -> tableModel.setRowCount(0));
            
            People people = dataManager.getPeople();
            if (people == null || people.isEmpty()) {
                // Show empty state
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{"No data", "", "", "", "", "", ""});
                    personTable.setEnabled(false);
                });
            } else {
                SwingUtilities.invokeLater(() -> {
                    personTable.setEnabled(true);
                    
                    for (Person person : people) {
                        if (person == null) continue;
                        
                        String type = "Person";
                        String govID = "-1";  // Default for non-registered persons
                        String studentID = "-1";  // Default for non-OCCC persons
                        
                        if (person instanceof OCCCPerson) {
                            type = "OCCC Person";
                            govID = ((OCCCPerson) person).getGovID();
                            studentID = ((OCCCPerson) person).getStudentID();
                        } else if (person instanceof RegisteredPerson) {
                            type = "Registered Person";
                            govID = ((RegisteredPerson) person).getGovID();
                        }
                        
                        try {
                            // Add row with proper objects
                            tableModel.addRow(new Object[]{
                                type,
                                person.getFirstName(),
                                person.getLastName(),
                                person.getDOB(),
                                person.getAge(),
                                govID,
                                studentID
                            });
                        } catch (Exception ex) {
                            System.err.println("Error processing person: " + ex.getMessage());
                        }
                    }
                });
            }
        } catch (Exception ex) {
            System.err.println("Error refreshing list: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    /**
     * Apply a filter to the list and update the table
     */
    public void applyFilter(java.util.function.Predicate<Person> filter) {
        this.currentFilter = filter;
        People people = dataManager.getPeople();
        filteredPeople = new java.util.ArrayList<>();
        tableModel.setRowCount(0);
        if (people != null) {
            for (Person person : people) {
                if (filter == null || filter.test(person)) {
                    filteredPeople.add(person);
                    String type = "Person";
                    String govID = "-1";
                    String studentID = "-1";
                    if (person instanceof OCCCPerson) {
                        type = "OCCC Person";
                        govID = ((OCCCPerson) person).getGovID();
                        studentID = ((OCCCPerson) person).getStudentID();
                    } else if (person instanceof RegisteredPerson) {
                        type = "Registered Person";
                        govID = ((RegisteredPerson) person).getGovID();
                    }
                    tableModel.addRow(new Object[]{
                        type,
                        person.getFirstName(),
                        person.getLastName(),
                        person.getDOB(),
                        person.getAge(),
                        govID,
                        studentID
                    });
                }
            }
        }
        personTable.setEnabled(filteredPeople != null && !filteredPeople.isEmpty());
    }

    /**
     * Get the currently filtered people as a People object
     */
    public People getFilteredPeople() {
        People result = new People();
        if (filteredPeople != null) {
            for (Person p : filteredPeople) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Implements DataChangeListener interface
     * Called when data in the DataManager changes
     */
    @Override
    public void onDataChanged() {
        // Update the list when data changes
        if (currentFilter != null) {
            applyFilter(currentFilter);
        } else {
            refreshList();
        }
        // Update the title to reflect the current file
        updateTitleLabel();
    }
    
    /**
     * Sets the person manager reference
     * @param personManager The person manager to set
     */
    public void setPersonManager(PViewer personManager) {
        this.personManager = personManager;
    }
    
    /**
     * Selects a person in the table
     * @param person The person to select
     */
    public void selectPerson(Person person) {
        if (person == null) return;
        
        try {
            People people = dataManager.getPeople();
            int modelIndex = -1;
            
            // Find the model index of the person
            for (int i = 0; i < people.size(); i++) {
                if (people.get(i).equals(person)) {
                    modelIndex = i;
                    break;
                }
            }
            
            if (modelIndex >= 0) {
                // Convert model index to view index
                int viewIndex = personTable.convertRowIndexToView(modelIndex);
                
                // Only set selection if the view index is valid
                if (viewIndex >= 0 && viewIndex < personTable.getRowCount()) {
                    personTable.setRowSelectionInterval(viewIndex, viewIndex);
                    
                    // Ensure the selected row is visible
                    Rectangle rect = personTable.getCellRect(viewIndex, 0, true);
                    personTable.scrollRectToVisible(rect);
                }
            }
        } catch (Exception ex) {
            // Log the error but don't crash
            System.err.println("Error selecting person: " + ex.getMessage());
        }
    }

    /**
     * Clears the current selection in the table
     */
    public void clearSelection() {
        ignoreSelectionEvents = true;
        try {
            personTable.clearSelection();
            statusLabel.setText("No person selected");
        } finally {
            ignoreSelectionEvents = false;
        }
    }

    /**
     * Updates the title label based on the current file in DataManager
     */
    private void updateTitleLabel() {
        java.io.File currentFile = dataManager.getCurrentFile();
        boolean isModified = dataManager.isModified();
        boolean hasChanges = dataManager.hasChanges();
        
        if (currentFile != null) {
            String fileName = currentFile.getName();
            if (isModified) {
                fileName += " *";
            }
            titleLabel.setText(fileName);
            if (isModified) {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.ITALIC | Font.BOLD));
            } else {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
            }
        } else {
            String defaultTitle = "Person List";
            if (isModified && hasChanges) {
                defaultTitle = "Unsaved List*";
            }
            titleLabel.setText(defaultTitle);
            if (isModified && hasChanges) {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.ITALIC | Font.BOLD));
            } else {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
            }
        }
    }

    /**
     * Returns the JTable used in this list (for theming)
     */
    public JTable getJList() {
        return personTable;
    }

    /**
     * Returns the header label (for theming)
     */
    public JLabel getHeader() {
        return titleLabel;
    }
}
