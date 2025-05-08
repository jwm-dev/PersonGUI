package src.app;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import java.awt.*;
import java.io.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import src.date.InvalidOCCCDateException;
import src.date.OCCCDate;
import src.person.OCCCPerson;
import src.person.People;
import src.person.Person;
import src.person.RegisteredPerson;

import java.util.ArrayList;
import java.util.List;

/**
 * Swing GUI module for managing Person objects.
 */
public class PersonManager extends JPanel {
    // Use DataManager instead of direct list
    private DataManager dataManager;
    
    private JTextField firstNameField, lastNameField, govIDField, studentIDField, dobField;
    private JFrame parentFrame;
    // Documentation labels for optional fields
    private JLabel govIDLabel, studentIDLabel;
    // Buttons for mode handling
    private JButton addButton, updateButton, deleteButton;
    // Mode tracking - true if creating a new person, false if editing existing
    private boolean creationMode = true;
    // Current person being edited (null when creating new)
    private Person currentPerson = null;
    // Track if we have pending changes to an existing person that haven't been committed
    private boolean hasPendingEdits = false;
    // Original field values for detecting changes
    private String originalFirstName, originalLastName, originalDob, originalGovID, originalStudentID;
    // File extension for People files
    private static final String FILE_EXTENSION = ".ppl";
    // Default directory for file operations
    private final File DATA_DIRECTORY = new File(System.getProperty("user.dir") + File.separator + "data");
    // Reference to DataList for synchronization
    private DataList dataList;

    // Field change listener interface and storage
    public interface FieldChangeListener {
        void onFieldsChanged();
    }
    
    private List<FieldChangeListener> fieldChangeListeners = new ArrayList<>();
    
    /**
     * Adds a listener to be notified when field values change
     * @param listener The listener to add
     */
    public void addTextFieldChangeListener(FieldChangeListener listener) {
        if (listener != null && !fieldChangeListeners.contains(listener)) {
            fieldChangeListeners.add(listener);
            
            // Add document listeners to all text fields
            DocumentListener docListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    notifyFieldChangeListeners();
                }
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    notifyFieldChangeListeners();
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                    notifyFieldChangeListeners();
                }
            };
            
            // Add the listener to all text fields
            firstNameField.getDocument().addDocumentListener(docListener);
            lastNameField.getDocument().addDocumentListener(docListener);
            dobField.getDocument().addDocumentListener(docListener);
            govIDField.getDocument().addDocumentListener(docListener);
            studentIDField.getDocument().addDocumentListener(docListener);
        }
    }
    
    /**
     * Notifies all field change listeners
     */
    private void notifyFieldChangeListeners() {
        for (FieldChangeListener listener : fieldChangeListeners) {
            listener.onFieldsChanged();
        }
        
        // Update menu bar save items whenever fields change
        notifyMenuBarUpdate();
    }

    public PersonManager(JFrame parent, DataManager manager) {
        this.parentFrame = parent;
        this.dataManager = manager;
        buildMainPanel();
    }

    private void buildMainPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Fields Panel - switch from GridLayout to a more flexible layout
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        
        // First Name field
        JPanel firstNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel firstNameLabel = new JLabel("First Name:");
        firstNameLabel.setPreferredSize(new Dimension(100, 25));
        firstNameField = new JTextField();
        firstNameField.setPreferredSize(new Dimension(150, 25));
        firstNamePanel.add(firstNameLabel);
        firstNamePanel.add(firstNameField);
        fieldsPanel.add(firstNamePanel);
        
        // Last Name field
        JPanel lastNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel lastNameLabel = new JLabel("Last Name:");
        lastNameLabel.setPreferredSize(new Dimension(100, 25));
        lastNameField = new JTextField();
        lastNameField.setPreferredSize(new Dimension(150, 25));
        lastNamePanel.add(lastNameLabel);
        lastNamePanel.add(lastNameField);
        fieldsPanel.add(lastNamePanel);
        
        // DOB field
        JPanel dobPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel dobLabel = new JLabel("Date of Birth:");
        dobLabel.setPreferredSize(new Dimension(100, 25));
        dobField = new JTextField();
        dobField.setPreferredSize(new Dimension(100, 25));
        dobField.setText("");
        dobPanel.add(dobLabel);
        dobPanel.add(dobField);
        fieldsPanel.add(dobPanel);

        // Government ID field
        JPanel govIDPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        govIDLabel = new JLabel("Gov. ID (optional):");
        govIDLabel.setPreferredSize(new Dimension(150, 25));
        govIDField = new JTextField();
        govIDField.setPreferredSize(new Dimension(150, 25));
        govIDPanel.add(govIDLabel);
        govIDPanel.add(govIDField);
        fieldsPanel.add(govIDPanel);

        // Student ID field
        JPanel studentIDPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentIDLabel = new JLabel("Student ID (optional):");
        studentIDLabel.setPreferredSize(new Dimension(150, 25));
        studentIDField = new JTextField();
        studentIDField.setPreferredSize(new Dimension(150, 25));
        studentIDPanel.add(studentIDLabel);
        studentIDPanel.add(studentIDField);
        fieldsPanel.add(studentIDPanel);
        
        add(fieldsPanel, BorderLayout.CENTER);
    
        // Button Panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add Person");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        
        addButton.addActionListener(_ -> addNewPerson());
        updateButton.addActionListener(_ -> updateSelectedPerson());
        deleteButton.addActionListener(_ -> deleteSelectedPerson());
        
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        
        add(buttonPanel, BorderLayout.SOUTH);

        // Add document listeners to dynamically update field states
        govIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateFieldStates();
            }
            public void removeUpdate(DocumentEvent e) {
                updateFieldStates();
            }
            public void insertUpdate(DocumentEvent e) {
                updateFieldStates();
            }
        });

        studentIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateFieldStates();
            }
            public void removeUpdate(DocumentEvent e) {
                updateFieldStates();
            }
            public void insertUpdate(DocumentEvent e) {
                updateFieldStates();
            }
        });
        
        // Add mouse listener to this panel to handle clicks in empty areas
        // When clicking in an empty area, clear the DataList selection and reset to creation mode
        this.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getSource() == PersonManager.this) {
                    // Only clear if user is clicking directly on the panel (not on child components)
                    if (dataList != null && currentPerson != null) {
                        dataList.clearSelection();
                        clearFields();
                    }
                }
            }
        });
        
        // Set initial field states
        updateFieldStates();
        
        // Set initial button visibility based on creation mode
        setCreationMode(true);
    }

    private void updateFieldStates() {
        boolean hasGovID = !govIDField.getText().trim().isEmpty();
        boolean hasStudentID = !studentIDField.getText().trim().isEmpty();

        // Visual feedback about fields - gray out empty fields
        if (hasGovID) {
            govIDField.setBackground(Color.WHITE);
        } else {
            govIDField.setBackground(new Color(240, 240, 240));  // Light gray
        }

        if (hasStudentID) {
            studentIDField.setBackground(Color.WHITE);
        } else {
            studentIDField.setBackground(new Color(240, 240, 240));  // Light gray
        }
        
        // Student ID can only be used if Gov ID is also present (since OCCCPerson extends RegisteredPerson)
        // This provides a visual cue but doesn't prevent entry
        studentIDField.setEnabled(true);  // Always allow entry but provide visual feedback
        
        // Force the fields to repaint with the new visual styles
        govIDField.repaint();
        studentIDField.repaint();
        
        // Update menu items whenever field states change
        notifyMenuBarUpdate();
    }

    private OCCCDate parseDate(String dateStr) throws Exception {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new Exception("Date cannot be empty");
        }

        // Parse the date string in MM/dd/yyyy format
        String[] parts = dateStr.split("/");
        if (parts.length != 3) {
            throw new Exception("Invalid date format. Please use MM/dd/yyyy");
        }

        try {
            // Parse the date components
            int month = Integer.parseInt(parts[0].trim());
            int day = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            
            // Let OCCCDate constructor handle the validation
            return new OCCCDate(day, month, year);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid date format. Please use MM/dd/yyyy with numeric values");
        } catch (InvalidOCCCDateException e) {
            // Pass through the InvalidOCCCDateException without wrapping it
            throw e;
        }
    }

    private boolean isValidID(String id) {
        // Check if the ID contains only alphanumeric characters
        return id.matches("^[a-zA-Z0-9]*$");
    }
    
    private String normalizeID(String id) {
        // Convert all alphabetical characters to uppercase
        return id.toUpperCase();
    }

    private void addNewPerson() {
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            
            // Validate that names are not empty
            if (firstName.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "First Name cannot be empty.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (lastName.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Last Name cannot be empty.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse the date and handle OCCCDate validation
            OCCCDate dob;
            try {
                dob = parseDate(dobField.getText());
            } catch (InvalidOCCCDateException ex) {
                // This is the specific behavior you requested
                JOptionPane.showMessageDialog(parentFrame, 
                    "Invalid Date!", 
                    "Date Error", JOptionPane.ERROR_MESSAGE);
                dobField.setText("");
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get ID fields
            String govID = govIDField.getText().trim();
            String studentID = studentIDField.getText().trim();
            
            // Validate IDs contain only alphanumeric characters
            if (!govID.isEmpty() && !isValidID(govID)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Government ID must contain only letters and numbers.",
                    "Invalid ID Format", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!studentID.isEmpty() && !isValidID(studentID)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Student ID must contain only letters and numbers.",
                    "Invalid ID Format", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Normalize IDs (capitalize all letters)
            if (!govID.isEmpty()) {
                govID = normalizeID(govID);
            }
            
            if (!studentID.isEmpty()) {
                studentID = normalizeID(studentID);
            }
            
            // Prevent adding a Person with studentID but no govID
            if (studentID.length() > 0 && govID.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Cannot add a Person with a Student ID but no Government ID.",
                    "Invalid Configuration", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check for duplicate IDs
            if (!govID.isEmpty() && dataManager.isDuplicateGovID(govID, -1)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "A person with this Government ID already exists.",
                    "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!studentID.isEmpty() && dataManager.isDuplicateStudentID(studentID, -1)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "A person with this Student ID already exists.",
                    "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Person newPerson;
            
            // Create the appropriate Person type based on entered data
            if (!govID.isEmpty() && !studentID.isEmpty()) {
                // OCCC Student - requires both government ID and student ID
                RegisteredPerson regPerson = new RegisteredPerson(firstName, lastName, dob, govID);
                newPerson = new OCCCPerson(regPerson, studentID);
            } else if (!govID.isEmpty()) {
                // Registered Person - requires government ID
                newPerson = new RegisteredPerson(firstName, lastName, dob, govID);
            } else {
                // Regular Person
                newPerson = new Person(firstName, lastName, dob);
            }
            
            // Use DataManager to add the person
            if (dataManager.addPerson(newPerson)) {
                // Clear fields instead of displaying the added person
                clearFields();
                
                // Notify any listeners that data has changed
                dataManager.notifyDataChanged();
                
                JOptionPane.showMessageDialog(parentFrame, "Person added successfully");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentFrame, 
                "Error creating person: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedPerson() {
        if (currentPerson == null) {
            JOptionPane.showMessageDialog(parentFrame, "No person selected", 
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            
            // Validate that names are not empty
            if (firstName.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "First Name cannot be empty.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (lastName.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Last Name cannot be empty.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Parse the date and handle OCCCDate validation
            OCCCDate dob;
            try {
                dob = parseDate(dobField.getText());
            } catch (InvalidOCCCDateException ex) {
                // This is the specific behavior you requested
                JOptionPane.showMessageDialog(parentFrame, 
                    "Invalid Date!", 
                    "Date Error", JOptionPane.ERROR_MESSAGE);
                dobField.setText("");
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Get ID fields
            String govID = govIDField.getText().trim();
            String studentID = studentIDField.getText().trim();
            
            // Validate IDs contain only alphanumeric characters
            if (!govID.isEmpty() && !isValidID(govID)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Government ID must contain only letters and numbers.",
                    "Invalid ID Format", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!studentID.isEmpty() && !isValidID(studentID)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Student ID must contain only letters and numbers.",
                    "Invalid ID Format", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Normalize IDs (capitalize all letters)
            if (!govID.isEmpty()) {
                govID = normalizeID(govID);
            }
            
            if (!studentID.isEmpty()) {
                studentID = normalizeID(studentID);
            }
            
            // Prevent updating a Person with studentID but no govID
            if (studentID.length() > 0 && govID.isEmpty()) {
                JOptionPane.showMessageDialog(parentFrame,
                    "Cannot update a Person with a Student ID but no Government ID.",
                    "Invalid Configuration", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Find the index of currentPerson in the People collection
            People people = dataManager.getPeople();
            int index = -1;
            for (int i = 0; i < people.size(); i++) {
                if (people.get(i).equals(currentPerson)) {
                    index = i;
                    break;
                }
            }
            
            if (index < 0) {
                JOptionPane.showMessageDialog(parentFrame, 
                                             "Error: Could not locate original person in data model",
                                             "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Check for duplicate IDs
            if (!govID.isEmpty() && dataManager.isDuplicateGovID(govID, index)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "A person with this Government ID already exists.",
                    "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!studentID.isEmpty() && dataManager.isDuplicateStudentID(studentID, index)) {
                JOptionPane.showMessageDialog(parentFrame,
                    "A person with this Student ID already exists.",
                    "Duplicate ID", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Person updatedPerson;
            
            // Create the appropriate Person type based on entered data
            if (!govID.isEmpty() && !studentID.isEmpty()) {
                // OCCC Student - requires both government ID and student ID
                RegisteredPerson regPerson = new RegisteredPerson(firstName, lastName, dob, govID);
                updatedPerson = new OCCCPerson(regPerson, studentID);
            } else if (!govID.isEmpty()) {
                // Registered Person - requires government ID
                updatedPerson = new RegisteredPerson(firstName, lastName, dob, govID);
            } else {
                // Regular Person
                updatedPerson = new Person(firstName, lastName, dob);
            }
            
            // Use DataManager to update
            if (dataManager.updatePerson(index, updatedPerson)) {
                // Update our reference to the current person
                currentPerson = updatedPerson;
                
                // Update original values to reflect the committed changes
                storeOriginalValues();
                
                // Reset the pending edits flag as changes have been committed
                hasPendingEdits = false;
                
                // Notify any listeners that data has changed
                dataManager.notifyDataChanged();
                
                // Update menu items state now that pending edits are committed
                notifyMenuBarUpdate();
                
                // Tell DataList to highlight the updated person
                if (dataList != null) {
                    dataList.selectPerson(updatedPerson);
                }
                
                JOptionPane.showMessageDialog(parentFrame, "Person updated successfully");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentFrame, 
                "Error updating person: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedPerson() {
        if (currentPerson == null) {
            JOptionPane.showMessageDialog(parentFrame, "No person selected", 
                                         "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(parentFrame, 
                                                  "Are you sure you want to delete this person?",
                                                  "Confirm Delete", 
                                                  JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (dataManager.deletePerson(currentPerson)) {
                // Clear the form fields and reset our state
                clearFields();
                
                // Notify any listeners that data has changed
                dataManager.notifyDataChanged();
            }
        }
    }

    /**
     * Updates the button visibility based on the current mode
     * Creation mode shows only Add button, editing mode shows Update and Delete
     */
    private void updateButtonVisibility() {
        addButton.setVisible(creationMode);
        updateButton.setVisible(!creationMode);
        deleteButton.setVisible(!creationMode);
    }

    private void setCreationMode(boolean isCreating) {
        this.creationMode = isCreating;
        updateButtonVisibility();
    }

    /**
     * Clears all input fields and resets to creation mode
     */
    public void clearFields() {
        // Reset all field values
        firstNameField.setText("");
        lastNameField.setText("");
        dobField.setText("");
        govIDField.setText("");
        studentIDField.setText("");
        
        // Reset currentPerson to null to indicate we're not editing anyone
        currentPerson = null;
        
        // Reset visual field styling
        highlightEditFields(false);
        
        // When clearing fields, switch to creation mode
        setCreationMode(true);
    }

    /**
     * Sets up a file chooser with the standard configuration
     * @param forSave true if setting up for save dialog, false for open dialog
     * @return Configured JFileChooser
     */
    private JFileChooser setupFileChooser(boolean forSave) {
        JFileChooser fileChooser = new JFileChooser();
        
        // Set the initial directory to the data folder
        if (DATA_DIRECTORY.exists() && DATA_DIRECTORY.isDirectory()) {
            fileChooser.setCurrentDirectory(DATA_DIRECTORY);
        } else {
            // Create the directory if it doesn't exist
            DATA_DIRECTORY.mkdirs();
            fileChooser.setCurrentDirectory(DATA_DIRECTORY);
        }
        
        // Add file filter for .ppl files
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(FILE_EXTENSION);
            }

            @Override
            public String getDescription() {
                return "People Files (*" + FILE_EXTENSION + ")";
            }
        });
        
        // Only accept .ppl files for open dialog
        if (!forSave) {
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        
        return fileChooser;
    }
    
    /**
     * Ensures the file has the correct extension
     * @param file The file to check
     * @return File with the proper extension
     */
    private File ensureFileExtension(File file) {
        String path = file.getPath();
        if (!path.toLowerCase().endsWith(FILE_EXTENSION)) {
            return new File(path + FILE_EXTENSION);
        }
        return file;
    }

    // Update file operations to use DataManager
    public void doNew() {
        dataManager.clear();
        clearFields();
        
        // Ensure no person is selected in DataList when creating new file
        if (dataList != null) {
            dataList.clearSelection();
        }
        
        // Notify any listeners that data has changed
        dataManager.notifyDataChanged();
        
        // Explicitly update menu bar to ensure menu items are properly disabled
        notifyMenuBarUpdate();
    }

    public void doOpen() {
        JFileChooser fileChooser = setupFileChooser(false);
        if (fileChooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                int count = dataManager.loadFromFile(selectedFile);
                
                // Clear selection in PersonManager and ensure we're in creation mode
                clearFields();
                
                // Clear selection in DataList
                if (dataList != null) {
                    dataList.clearSelection();
                }
                
                // Notify any listeners that data has changed
                dataManager.notifyDataChanged();
                
                JOptionPane.showMessageDialog(parentFrame, 
                    count + " people loaded successfully", 
                    "Load Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error loading file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public void doSave() {
        File currentFile = dataManager.getCurrentFile();
        if (currentFile == null) {
            // If we don't have a file yet, use Save As...
            doSaveAs();
        } else {
            // We have a file, so save to it
            try {
                int count = dataManager.saveToFile(currentFile);
                
                // Immediately update menu bar state after saving
                notifyMenuBarUpdate();
                
                JOptionPane.showMessageDialog(parentFrame, 
                    count + " people saved successfully",
                    "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error saving file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    public void doSaveAs() {
        JFileChooser fileChooser = setupFileChooser(true);
        
        // If there's a current file, start the file chooser with that file selected
        File currentFile = dataManager.getCurrentFile();
        if (currentFile != null) {
            fileChooser.setSelectedFile(currentFile);
        }
        
        if (fileChooser.showSaveDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = ensureFileExtension(fileChooser.getSelectedFile());
            
            // Check if file already exists and confirm overwrite
            if (selectedFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "File already exists. Do you want to overwrite it?",
                    "Confirm Overwrite", 
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                int count = dataManager.saveToFile(selectedFile);
                
                // Immediately update menu bar state after saving
                notifyMenuBarUpdate();
                
                JOptionPane.showMessageDialog(parentFrame, 
                    count + " people saved successfully",
                    "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error saving file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    /**
     * Imports Person objects from a selected file and adds them to the current working list
     */
    public void doImport() {
        JFileChooser fileChooser = setupFileChooser(false); // Reuse the existing file chooser setup
        fileChooser.setDialogTitle("Import People File");
        
        if (fileChooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Load the people from the selected file
                People importedPeople = loadPeopleFromFile(selectedFile);
                
                if (importedPeople != null && !importedPeople.isEmpty()) {
                    // Prepare counts for statistics
                    int importedCount = 0;
                    int duplicateCount = 0;
                    int conflictResolved = 0;
                    
                    // Create a list to hold conflicts for batch processing
                    List<ConflictInfo> conflicts = new ArrayList<>();
                    
                    // Keep track of persons we've already processed (either as conflicts or silent skips)
                    List<Person> processedPersons = new ArrayList<>();
                    
                    // Initial scan for conflicts and silent duplicates
                    for (Person person : importedPeople) {
                        // Skip null entries that might be in the file
                        if (person == null) continue;
                        
                        // Check for exact duplicates of basic Person objects
                        if (isExactDuplicate(person)) {
                            // Add to processed list to skip later
                            processedPersons.add(person);
                            duplicateCount++;
                            continue;
                        }
                        
                        // Check for other types of conflicts
                        ConflictInfo conflict = checkForConflict(person);
                        if (conflict != null) {
                            conflicts.add(conflict);
                            // Add to processed list to skip later
                            processedPersons.add(person);
                        }
                    }
                    
                    // Process conflicts if any are found - Windows Explorer style
                    if (!conflicts.isEmpty()) {
                        // Start with case-by-case resolution
                        boolean resolveAllRemaining = false;
                        ConflictChoice globalChoice = null;
                        
                        for (int i = 0; i < conflicts.size(); i++) {
                            ConflictInfo conflict = conflicts.get(i);
                            ConflictChoice choice;
                            
                            if (!resolveAllRemaining) {
                                // Show conflict resolution dialog with remaining count
                                choice = showConflictResolutionDialogWithApplyToAll(conflict, conflicts.size() - i);
                                
                                // Check if user selected "Apply to All"
                                if (choice == ConflictChoice.APPLY_TO_ALL) {
                                    // Show dialog to choose how to resolve all remaining conflicts
                                    globalChoice = showGlobalResolutionDialog();
                                    
                                    // If user canceled the global resolution, continue with case-by-case
                                    if (globalChoice == ConflictChoice.CANCEL) {
                                        i--; // Retry this conflict
                                        continue;
                                    }
                                    
                                    // Otherwise apply global choice to all remaining conflicts
                                    resolveAllRemaining = true;
                                    choice = globalChoice;
                                }
                            } else {
                                // Apply the global choice
                                choice = globalChoice;
                            }
                            
                            // If user canceled and wasn't using "Apply to All", skip this conflict
                            if (choice == ConflictChoice.CANCEL && !resolveAllRemaining) {
                                continue;
                            }
                            
                            // Apply the chosen resolution
                            switch (choice) {
                                case USE_NEW:
                                    // Replace the existing person with the new one
                                    if (dataManager.updatePerson(conflict.existingIndex, conflict.newPerson)) {
                                        conflictResolved++;
                                    }
                                    break;
                                    
                                case KEEP_EXISTING:
                                    // Keep existing, count as handled
                                    conflictResolved++;
                                    break;
                                    
                                case SKIP:
                                    // Skip this conflict, count as duplicate
                                    duplicateCount++;
                                    break;
                                    
                                case CANCEL:
                                    // This case is handled before the switch, but included for completeness
                                    break;
                                    
                                case APPLY_TO_ALL:
                                    // This case is handled before the switch, but included for completeness
                                    break;
                            }
                        }
                    }
                    
                    // Now add all non-conflicting people
                    for (Person person : importedPeople) {
                        // Skip null entries
                        if (person == null) continue;
                        
                        // Skip entries we've already processed
                        if (processedPersons.contains(person)) {
                            continue;
                        }
                        
                        // Double-check for conflicts to ensure we don't miss any
                        ConflictInfo conflict = checkForConflict(person);
                        if (conflict != null) {
                            // Skip - this shouldn't normally happen but is a safety check
                            continue;
                        }
                        
                        // One more check for exact duplicates in case anything was missed
                        if (isExactDuplicate(person)) {
                            duplicateCount++;
                            continue;
                        }
                        
                        // Add the person
                        if (dataManager.addPerson(person)) {
                            importedCount++;
                        }
                    }
                    
                    // Refresh the list
                    dataManager.notifyDataChanged();
                    
                    // Update menu items if changes were made during import
                    if (importedCount > 0 || conflictResolved > 0) {
                        notifyMenuBarUpdate();
                    }
                    
                    // Improved message formatting for import results
                    if (importedCount == 0 && conflictResolved == 0 && duplicateCount == 0) {
                        // Nothing was imported or changed
                        JOptionPane.showMessageDialog(parentFrame,
                            "No changes were made during import. All entries already exist in the system.",
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else if (importedCount == 0 && conflictResolved > 0) {
                        // Only conflicts were resolved, nothing new added
                        JOptionPane.showMessageDialog(parentFrame,
                            String.format("%d %s resolved. No new entries were added.",
                                conflictResolved, 
                                conflictResolved == 1 ? "conflict was" : "conflicts were"),
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // Format a detailed message about what happened
                        StringBuilder message = new StringBuilder();
                        
                        // Add imported count with better grammar
                        if (importedCount > 0) {
                            message.append(String.format("%d %s imported successfully", 
                                importedCount, 
                                importedCount == 1 ? "person was" : "people were"));
                        }
                        
                        // Add details about conflicts and duplicates if any
                        if (conflictResolved > 0 || duplicateCount > 0) {
                            if (importedCount > 0) {
                                message.append(" (");
                            }
                            
                            List<String> details = new ArrayList<>();
                            if (conflictResolved > 0) {
                                details.add(String.format("%d %s resolved", 
                                    conflictResolved,
                                    conflictResolved == 1 ? "conflict" : "conflicts"));
                            }
                            
                            if (duplicateCount > 0) {
                                details.add(String.format("%d %s skipped", 
                                    duplicateCount,
                                    duplicateCount == 1 ? "duplicate" : "duplicates"));
                            }
                            
                            if (importedCount > 0) {
                                message.append(String.join(", ", details));
                                message.append(")");
                            } else {
                                message.append(String.join(", ", details));
                            }
                        }
                        
                        JOptionPane.showMessageDialog(parentFrame, message.toString(),
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(parentFrame, 
                        "No people to import in the selected file.",
                        "Import Result", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error importing file: " + ex.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Enum for conflict resolution choices
     */
    private enum ConflictChoice {
        USE_NEW,       // Use the new (imported) person
        KEEP_EXISTING, // Keep the existing person
        SKIP,          // Skip this conflict
        CANCEL,        // Cancel operation
        APPLY_TO_ALL   // Apply a choice to all remaining conflicts
    }
    
    /**
     * Class to store information about a conflict
     */
    private class ConflictInfo {
        Person existingPerson;
        Person newPerson;
        int existingIndex;
        String conflictType; // "govID" or "studentID"
        String conflictValue; // The actual ID that caused the conflict
        
        public ConflictInfo(Person existing, Person newP, int index, String type, String value) {
            this.existingPerson = existing;
            this.newPerson = newP;
            this.existingIndex = index;
            this.conflictType = type;
            this.conflictValue = value;
        }
    }
    
    /**
     * Checks if a person would conflict with existing data
     * @param person The person to check
     * @return ConflictInfo if there's a conflict, null otherwise
     */
    private ConflictInfo checkForConflict(Person person) {
        if (person == null) return null;
        
        // Check for government ID conflicts in RegisteredPerson
        if (person instanceof RegisteredPerson) {
            RegisteredPerson regPerson = (RegisteredPerson) person;
            String govID = regPerson.getGovID();
            
            // Find any existing person with this gov ID
            People people = dataManager.getPeople();
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                if (existingPerson instanceof RegisteredPerson) {
                    RegisteredPerson existingReg = (RegisteredPerson) existingPerson;
                    if (govID.equals(existingReg.getGovID())) {
                        // Check if the persons are essentially identical
                        if (arePersonsIdentical(existingPerson, person)) {
                            // Not a conflict if they're identical - we'll just skip it silently
                            return null;
                        }
                        return new ConflictInfo(existingPerson, person, i, "govID", govID);
                    }
                }
            }
        }
        
        // Check for student ID conflicts in OCCCPerson
        if (person instanceof OCCCPerson) {
            OCCCPerson occPerson = (OCCCPerson) person;
            String studentID = occPerson.getStudentID();
            
            // Find any existing person with this student ID
            People people = dataManager.getPeople();
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                if (existingPerson instanceof OCCCPerson) {
                    OCCCPerson existingOcc = (OCCCPerson) existingPerson;
                    if (studentID.equals(existingOcc.getStudentID())) {
                        // Check if the persons are essentially identical
                        if (arePersonsIdentical(existingPerson, person)) {
                            // Not a conflict if they're identical - we'll just skip it silently
                            return null;
                        }
                        return new ConflictInfo(existingPerson, person, i, "studentID", studentID);
                    }
                }
            }
        }
        
        // Check for duplicate basic Person based on name and date of birth
        String firstName = person.getFirstName();
        String lastName = person.getLastName();
        
        // Only proceed if we have valid name data
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            People people = dataManager.getPeople();
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                
                // Check for matching name and DOB
                if (firstName.equals(existingPerson.getFirstName()) &&
                    lastName.equals(existingPerson.getLastName()) &&
                    person.getDOB().equals(existingPerson.getDOB())) {
                    
                    // Skip if this is a RegisteredPerson or if the existing person is a RegisteredPerson
                    // as we've already handled those cases above
                    if (person instanceof RegisteredPerson || existingPerson instanceof RegisteredPerson) {
                        continue;
                    }
                    
                    // For basic Person objects with same name and DOB, treat it as a conflict
                    // rather than silently skipping so user has control
                    return new ConflictInfo(existingPerson, person, i, "basicPerson", 
                                          firstName + " " + lastName);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Checks if two Person objects are essentially identical in their data
     * @param person1 First person to compare
     * @param person2 Second person to compare
     * @return true if the persons have identical data, false otherwise
     */
    private boolean arePersonsIdentical(Person person1, Person person2) {
        if (person1 == null || person2 == null) {
            return false;
        }
        
        // Compare the basic Person attributes
        boolean basicMatch = person1.getFirstName().equals(person2.getFirstName()) && 
                            person1.getLastName().equals(person2.getLastName()) &&
                            person1.getDOB().equals(person2.getDOB());
        
        if (!basicMatch) {
            return false;
        }
        
        // If they're both RegisteredPerson, compare Gov IDs
        if (person1 instanceof RegisteredPerson && person2 instanceof RegisteredPerson) {
            RegisteredPerson reg1 = (RegisteredPerson) person1;
            RegisteredPerson reg2 = (RegisteredPerson) person2;
            
            if (!reg1.getGovID().equals(reg2.getGovID())) {
                return false;
            }
            
            // If they're both OCCCPerson, compare Student IDs
            if (person1 instanceof OCCCPerson && person2 instanceof OCCCPerson) {
                OCCCPerson occc1 = (OCCCPerson) person1;
                OCCCPerson occc2 = (OCCCPerson) person2;
                
                if (!occc1.getStudentID().equals(occc2.getStudentID())) {
                    return false;
                }
            } else if ((person1 instanceof OCCCPerson) != (person2 instanceof OCCCPerson)) {
                // One is OCCCPerson but the other isn't
                return false;
            }
        } else if ((person1 instanceof RegisteredPerson) != (person2 instanceof RegisteredPerson)) {
            // One is RegisteredPerson but the other isn't
            return false;
        } else if (!(person1 instanceof RegisteredPerson) && !(person2 instanceof RegisteredPerson)) {
            return true;
        }
        
        // All relevant checks passed
        return true;
    }

    /**
     * Checks if a person already exists in the list with exactly the same data
     * @param person The person to check for duplicates
     * @return true if an identical person already exists
     */
    private boolean isExactDuplicate(Person person) {
        if (person == null) return false;
        
        People people = dataManager.getPeople();
        for (int i = 0; i < people.size(); i++) {
            Person existingPerson = people.get(i);
            
            // Use the arePersonsIdentical method to check if they're the same
            if (arePersonsIdentical(existingPerson, person)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shows a dialog to resolve a single conflict with an "Apply to All" option
     * @param conflict The conflict to resolve
     * @param remainingCount Number of remaining conflicts (including this one)
     * @return The user's choice
     */
    private ConflictChoice showConflictResolutionDialogWithApplyToAll(ConflictInfo conflict, int remainingCount) {
        // Create a panel to display the conflict details
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        
        // Add conflict description with counter
        String idType = conflict.conflictType.equals("govID") ? "Government ID" : "Student ID";
        String countInfo = (remainingCount > 1) ? " (" + remainingCount + " conflicts remaining)" : "";
        JLabel conflictLabel = new JLabel("Conflict detected: " + idType + " '" + conflict.conflictValue + "' already exists." + countInfo);
        panel.add(conflictLabel, BorderLayout.NORTH);
        
        // Create a panel to show existing vs new data
        JPanel comparisonPanel = new JPanel(new GridLayout(0, 3, 5, 5));
        comparisonPanel.add(new JLabel("Field"));
        comparisonPanel.add(new JLabel("Existing"));
        comparisonPanel.add(new JLabel("New (Import)"));
        
        // Add name comparison
        comparisonPanel.add(new JLabel("Name:"));
        comparisonPanel.add(new JLabel(conflict.existingPerson.getFirstName() + " " + conflict.existingPerson.getLastName()));
        comparisonPanel.add(new JLabel(conflict.newPerson.getFirstName() + " " + conflict.newPerson.getLastName()));
        
        // Add DOB comparison
        comparisonPanel.add(new JLabel("Birth Date:"));
        comparisonPanel.add(new JLabel(formatDateForDisplay(conflict.existingPerson.getDOB())));
        comparisonPanel.add(new JLabel(formatDateForDisplay(conflict.newPerson.getDOB())));
        
        // Add Government ID comparison if applicable
        if (conflict.existingPerson instanceof RegisteredPerson && conflict.newPerson instanceof RegisteredPerson) {
            String existingGovId = ((RegisteredPerson)conflict.existingPerson).getGovID();
            String newGovId = ((RegisteredPerson)conflict.newPerson).getGovID();
            
            comparisonPanel.add(new JLabel("Government ID:"));
            comparisonPanel.add(new JLabel(existingGovId));
            comparisonPanel.add(new JLabel(newGovId));
        }
        
        // Add Student ID comparison if applicable
        if (conflict.existingPerson instanceof OCCCPerson && conflict.newPerson instanceof OCCCPerson) {
            String existingStudentId = ((OCCCPerson)conflict.existingPerson).getStudentID();
            String newStudentId = ((OCCCPerson)conflict.newPerson).getStudentID();
            
            comparisonPanel.add(new JLabel("Student ID:"));
            comparisonPanel.add(new JLabel(existingStudentId));
            comparisonPanel.add(new JLabel(newStudentId));
        }
        
        panel.add(comparisonPanel, BorderLayout.CENTER);
        
        // Highlight the differences for better visibility
        highlightDifferences(comparisonPanel);
        
        // Options for user choice - only show "Apply to All" if there are multiple conflicts
        Object[] options;
        if (remainingCount > 1) {
            options = new Object[]{"Keep Existing", "Use New", "Skip", "Apply to All...", "Cancel"};
        } else {
            options = new Object[]{"Keep Existing", "Use New", "Skip", "Cancel"};
        }
        
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            panel,
            "Resolve Import Conflict",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        // Convert choice to enum based on available options
        if (remainingCount > 1) {
            switch (choice) {
                case 1: return ConflictChoice.USE_NEW;
                case 2: return ConflictChoice.SKIP;
                case 3: return ConflictChoice.APPLY_TO_ALL;
                case 4: return ConflictChoice.CANCEL;
                case 0:
                default: return ConflictChoice.KEEP_EXISTING;
            }
        } else {
            switch (choice) {
                case 1: return ConflictChoice.USE_NEW;
                case 2: return ConflictChoice.SKIP;
                case 3: return ConflictChoice.CANCEL;
                case 0:
                default: return ConflictChoice.KEEP_EXISTING;
            }
        }
    }
    
    /**
     * Shows a dialog to choose how to resolve all remaining conflicts
     * @return The user's choice for all remaining conflicts
     */
    private ConflictChoice showGlobalResolutionDialog() {
        // Options for user choice
        Object[] options = {"Keep All Existing", "Import All New", "Skip All", "Cancel"};
        
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            "How do you want to resolve all remaining conflicts?",
            "Apply to All Conflicts",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        // Convert choice to enum
        switch (choice) {
            case 1: return ConflictChoice.USE_NEW;
            case 2: return ConflictChoice.SKIP;
            case 3: return ConflictChoice.CANCEL;
            case 0:
            default: return ConflictChoice.KEEP_EXISTING;
        }
    }
    
    /**
     * Highlights differences between existing and new data in the comparison panel
     * @param panel The comparison panel to update
     */
    private void highlightDifferences(JPanel panel) {
        // Get all components in the panel
        Component[] components = panel.getComponents();
        
        // Skip the header row (first 3 components)
        for (int i = 3; i < components.length; i += 3) {
            // Every row has 3 components: label, existing value, new value
            if (i + 2 < components.length && components[i] instanceof JLabel && 
                components[i+1] instanceof JLabel && components[i+2] instanceof JLabel) {
                
                JLabel fieldLabel = (JLabel)components[i];
                JLabel existingValue = (JLabel)components[i+1];
                JLabel newValue = (JLabel)components[i+2];
                
                // Check if values are different
                if (!existingValue.getText().equals(newValue.getText())) {
                    // Highlight the differences
                    existingValue.setForeground(Color.RED);
                    newValue.setForeground(new Color(0, 100, 0)); // Dark green
                    
                    // Make field label bold
                    Font boldFont = new Font(fieldLabel.getFont().getName(), 
                                           Font.BOLD, 
                                           fieldLabel.getFont().getSize());
                    fieldLabel.setFont(boldFont);
                }
            }
        }
    }

    /**
     * Public method for other modules to trigger displaying a specific person
     * @param person The person to display
     * @return true if the person was successfully displayed
     */
    public boolean displayPersonDetails(Person person) {
        if (person == null) return false;
        
        // Store the current person being edited
        currentPerson = person;
        
        // Set the field values
        firstNameField.setText(person.getFirstName());
        lastNameField.setText(person.getLastName());
        
        // Format the date in MM/dd/yyyy format for the input field
        OCCCDate dob = person.getDOB();
        String formattedDate = String.format("%02d/%02d/%04d", 
                                           dob.getMonthNumber(), 
                                           dob.getDayOfMonth(), 
                                           dob.getYear());
        dobField.setText(formattedDate);
        
        // Reset ID fields
        govIDField.setText("");
        studentIDField.setText("");
        
        // Check if selected person is a RegisteredPerson
        if (person instanceof RegisteredPerson) {
            RegisteredPerson regPerson = (RegisteredPerson) person;
            govIDField.setText(regPerson.getGovID());
            
            // Check if selected person is also an OCCCPerson
            if (person instanceof OCCCPerson) {
                OCCCPerson occPerson = (OCCCPerson) person;
                studentIDField.setText(occPerson.getStudentID());
            }
        }
        
        // Store original values to detect changes
        storeOriginalValues();
        
        // Reset the pending edits flag
        hasPendingEdits = false;
        
        // Highlight the form fields to indicate editing mode
        highlightEditFields(true);
        
        // Switch to editing mode
        setCreationMode(false);
        
        return true;
    }
    
    /**
     * Stores the original values of all fields to detect changes
     */
    private void storeOriginalValues() {
        originalFirstName = firstNameField.getText().trim();
        originalLastName = lastNameField.getText().trim();
        originalDob = dobField.getText().trim();
        originalGovID = govIDField.getText().trim();
        originalStudentID = studentIDField.getText().trim();
    }
    
    /**
     * Checks if there are any pending edits to an existing Person
     * @return true if there are uncommitted changes to a Person being edited
     */
    private boolean checkForPendingEdits() {
        // Only apply to edit mode, not creation mode
        if (creationMode || currentPerson == null) {
            return false;
        }
        
        // Compare current field values with original values
        String currentFirstName = firstNameField.getText().trim();
        String currentLastName = lastNameField.getText().trim();
        String currentDob = dobField.getText().trim();
        String currentGovID = govIDField.getText().trim();
        String currentStudentID = studentIDField.getText().trim();
        
        // Check if any field has changed
        boolean hasChanges = !currentFirstName.equals(originalFirstName) ||
                            !currentLastName.equals(originalLastName) ||
                            !currentDob.equals(originalDob) ||
                            !currentGovID.equals(originalGovID) ||
                            !currentStudentID.equals(originalStudentID);
        
        // Update the pending edits flag
        hasPendingEdits = hasChanges;
        
        return hasPendingEdits;
    }

    /**
     * Highlights form fields to indicate edit mode vs. create mode
     * @param isEditing true if in edit mode, false if in creation mode
     */
    private void highlightEditFields(boolean isEditing) {
        Color bgColor = isEditing ? new Color(240, 255, 240) : Color.WHITE; // Light green for editing
        Color borderColor = isEditing ? new Color(0, 100, 0) : null; // Dark green border for editing
        
        firstNameField.setBackground(bgColor);
        lastNameField.setBackground(bgColor);
        dobField.setBackground(bgColor);
        
        // Only change the background of ID fields if they are not already styled
        if (!govIDField.getText().trim().isEmpty()) {
            govIDField.setBackground(bgColor);
        }
        if (!studentIDField.getText().trim().isEmpty()) {
            studentIDField.setBackground(bgColor);
        }
        
        // Set border color for edit mode - this makes it very clear which fields are being edited
        if (isEditing) {
            firstNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            lastNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            dobField.setBorder(BorderFactory.createLineBorder(borderColor));
            govIDField.setBorder(BorderFactory.createLineBorder(borderColor));
            studentIDField.setBorder(BorderFactory.createLineBorder(borderColor));
        } else {
            // Reset to default borders
            firstNameField.setBorder(UIManager.getBorder("TextField.border"));
            lastNameField.setBorder(UIManager.getBorder("TextField.border"));
            dobField.setBorder(UIManager.getBorder("TextField.border"));
            govIDField.setBorder(UIManager.getBorder("TextField.border"));
            studentIDField.setBorder(UIManager.getBorder("TextField.border"));
        }
    }
    
    /**
     * Sets a reference to the DataList module for synchronization
     * @param dataList The DataList module
     */
    public void setDataList(DataList dataList) {
        // Store reference to DataList for highlighting selected person
        this.dataList = dataList;
    }
    
    /**
     * Gets the currently active person in the manager
     * @return The current person being edited, or null if in creation mode
     */
    public Person getCurrentPerson() {
        return currentPerson;
    }

    /**
     * Checks if there's partial/incomplete data in the form fields
     * or if there are uncommitted edits to an existing Person
     * @return true if there is partial data or uncommitted edits
     */
    public boolean hasPartialData() {
        // Get current field values
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String dob = dobField.getText().trim();
        String govID = govIDField.getText().trim();
        String studentID = studentIDField.getText().trim();
        
        // If all fields are empty, form is not in a partial state
        boolean allEmpty = firstName.isEmpty() && lastName.isEmpty() && 
                           dob.isEmpty() && govID.isEmpty() && studentID.isEmpty();
        
        if (allEmpty) {
            return false;
        }
        
        // In edit mode, check for uncommitted changes to an existing person
        if (!creationMode && currentPerson != null) {
            // Check if fields have been modified but not committed yet
            checkForPendingEdits();
            if (hasPendingEdits) {
                return true; // Uncommitted edits count as "partial data" for save purposes
            }
        }
        
        // Form has partial data if any required field for a basic Person is missing
        boolean missingRequiredFields = firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty();
        
        // Form has invalid configuration if there's a student ID but no gov ID
        boolean invalidConfig = !studentID.isEmpty() && govID.isEmpty();
        
        return missingRequiredFields || invalidConfig;
    }
    
    /**
     * Notifies the menu bar to update the enabled state of save menu items
     */
    private void notifyMenuBarUpdate() {
        // Use a more generic approach rather than relying on specific class type
        if (parentFrame != null) {
            // Look for updateSaveMenuItemState method using reflection
            try {
                java.lang.reflect.Method updateMethod = 
                    parentFrame.getClass().getMethod("updateSaveMenuItemState");
                updateMethod.invoke(parentFrame);
            } catch (Exception e) {
                // Method not found or couldn't be invoked, silently ignore
                // This is fine as the method is optional
            }
        }
    }

    /**
     * Loads people from a file without replacing the current working list
     * @param file The file to load from
     * @return People object containing the loaded data
     * @throws Exception If there's an error during loading
     */
    private People loadPeopleFromFile(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            Object obj = ois.readObject();
            if (obj instanceof People) {
                return (People) obj;
            } else {
                throw new ClassCastException("File does not contain a valid People object");
            }
        }
    }

    /**
     * Format a date for display in conflict resolution dialog
     * @param date The date to format
     * @return Formatted date string in MM/dd/yyyy format
     */
    private String formatDateForDisplay(OCCCDate date) {
        if (date == null) return "";
        return String.format("%02d/%02d/%04d", 
                           date.getMonthNumber(), 
                           date.getDayOfMonth(), 
                           date.getYear());
    }

    /**
     * Export the current people data to a file in a specified format
     * Supported formats: .txt and .json
     */
    public void doExportAs() {
        // Setup file chooser for export
        JFileChooser fileChooser = new JFileChooser();
        
        // Set the initial directory to the data folder
        if (DATA_DIRECTORY.exists() && DATA_DIRECTORY.isDirectory()) {
            fileChooser.setCurrentDirectory(DATA_DIRECTORY);
        } else {
            // Create the directory if it doesn't exist
            DATA_DIRECTORY.mkdirs();
            fileChooser.setCurrentDirectory(DATA_DIRECTORY);
        }
        
        fileChooser.setDialogTitle("Export As");
        
        // Add file filters for supported formats
        FileFilter txtFilter = new javax.swing.filechooser.FileNameExtensionFilter(
                "Text Files (*.txt)", "txt");
        FileFilter jsonFilter = new javax.swing.filechooser.FileNameExtensionFilter(
                "JSON Files (*.json)", "json");
        
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(txtFilter); // Default to TXT
        
        if (fileChooser.showSaveDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            FileFilter selectedFilter = fileChooser.getFileFilter();
            
            // Determine export format based on selected filter
            String exportFormat;
            if (selectedFilter.equals(jsonFilter)) {
                exportFormat = "json";
            } else {
                // Default to txt format
                exportFormat = "txt";
            }
            
            // Ensure the file has the correct extension
            String filePath = selectedFile.getPath();
            if (!filePath.toLowerCase().endsWith("." + exportFormat)) {
                selectedFile = new File(filePath + "." + exportFormat);
            }
            
            // Check if file already exists and confirm overwrite
            if (selectedFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "File already exists. Do you want to overwrite it?",
                    "Confirm Overwrite", 
                    JOptionPane.YES_NO_OPTION
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                // Export the data in the selected format
                People people = dataManager.getPeople();
                int count = 0;
                
                if (exportFormat.equals("json")) {
                    count = exportToJson(people, selectedFile);
                } else {
                    count = exportToText(people, selectedFile);
                }
                
                JOptionPane.showMessageDialog(parentFrame, 
                    count + " people exported successfully to " + exportFormat.toUpperCase() + " format.",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error exporting file: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Export people data to a text file format
     * @param people The people collection to export
     * @param file The file to export to
     * @return Number of people exported
     * @throws IOException If there's an error writing the file
     */
    private int exportToText(People people, File file) throws IOException {
        if (people == null || people.isEmpty()) {
            return 0;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Person Manager Export - " + new java.util.Date());
            writer.println("------------------------------------");
            writer.println();
            
            int count = 0;
            for (int i = 0; i < people.size(); i++) {
                Person person = people.get(i);
                if (person == null) continue;
                
                writer.println("Person #" + (i + 1));
                writer.println("First Name: " + person.getFirstName());
                writer.println("Last Name: " + person.getLastName());
                writer.println("DOB: " + formatDateForDisplay(person.getDOB()));
                
                if (person instanceof RegisteredPerson) {
                    RegisteredPerson rp = (RegisteredPerson) person;
                    writer.println("Government ID: " + rp.getGovID());
                    
                    if (person instanceof OCCCPerson) {
                        OCCCPerson op = (OCCCPerson) person;
                        writer.println("Student ID: " + op.getStudentID());
                        writer.println("Type: OCCC Person");
                    } else {
                        writer.println("Type: Registered Person");
                    }
                } else {
                    writer.println("Type: Basic Person");
                }
                
                writer.println();
                count++;
            }
            
            writer.println("Total: " + count + " people");
            return count;
        }
    }
    
    /**
     * Export people data to a JSON file format
     * @param people The people collection to export
     * @param file The file to export to
     * @return Number of people exported
     * @throws IOException If there's an error writing the file
     */
    private int exportToJson(People people, File file) throws IOException {
        if (people == null || people.isEmpty()) {
            return 0;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Start JSON array
            writer.println("{");
            writer.println("  \"exportDate\": \"" + new java.util.Date() + "\",");
            writer.println("  \"people\": [");
            
            int count = 0;
            for (int i = 0; i < people.size(); i++) {
                Person person = people.get(i);
                if (person == null) continue;
                
                writer.println("    {");
                writer.println("      \"firstName\": \"" + escapeJsonString(person.getFirstName()) + "\",");
                writer.println("      \"lastName\": \"" + escapeJsonString(person.getLastName()) + "\",");
                writer.println("      \"dob\": \"" + formatDateForDisplay(person.getDOB()) + "\",");
                
                if (person instanceof RegisteredPerson) {
                    RegisteredPerson rp = (RegisteredPerson) person;
                    writer.println("      \"governmentID\": \"" + escapeJsonString(rp.getGovID()) + "\",");
                    
                    if (person instanceof OCCCPerson) {
                        OCCCPerson op = (OCCCPerson) person;
                        writer.println("      \"studentID\": \"" + escapeJsonString(op.getStudentID()) + "\",");
                        writer.println("      \"type\": \"OCCCPerson\"");
                    } else {
                        writer.println("      \"type\": \"RegisteredPerson\"");
                    }
                } else {
                    writer.println("      \"type\": \"Person\"");
                }
                
                // Add comma if not the last person
                if (i < people.size() - 1) {
                    writer.println("    },");
                } else {
                    writer.println("    }");
                }
                
                count++;
            }
            
            // Close JSON array and object
            writer.println("  ],");
            writer.println("  \"total\": " + count);
            writer.println("}");
            
            return count;
        }
    }
    
    /**
     * Escape special characters for JSON output
     * @param input String to escape
     * @return Escaped string safe for JSON
     */
    private String escapeJsonString(String input) {
        if (input == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
