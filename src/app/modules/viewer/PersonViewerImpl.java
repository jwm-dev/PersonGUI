package src.app.modules.viewer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import src.app.AppController;
import src.app.modules.list.PList;
import src.date.OCCCDate;
import src.person.OCCCPerson;
import src.person.Person;
import src.person.RegisteredPerson;

/**
 * Implementation of the PViewer API interface, merging all viewer logic and UI.
 */
public class PersonViewerImpl implements PViewer {
    private final JPanel panel;
    private final JTextField firstNameField, lastNameField, govIDField, studentIDField, dobField;
    private final JLabel govIDLabel, studentIDLabel;
    private final JButton addButton, updateButton, deleteButton;
    private final AppController appController;
    private final JFrame parent;
    private boolean creationMode = true;
    private Person currentPerson = null;
    private boolean hasPendingEdits = false;
    private String originalFirstName, originalLastName, originalDob, originalGovID, originalStudentID;
    private PList dataList;

    private final List<FieldChangeListener> fieldChangeListeners = new ArrayList<>();

    public interface FieldChangeListener {
        void onFieldsChanged();
    }

    public PersonViewerImpl(JFrame parent, AppController manager) {
        this.parent = parent;
        this.appController = manager;
        this.panel = new JPanel(new BorderLayout()) {
            @Override
            public void updateUI() {
                super.updateUI();
                setBackground(UIManager.getColor("Viewer.background"));
                for (Component c : getComponents()) {
                    updateComponentThemeRecursively(c);
                }
                // Ensure textbox coloring updates with retheming
                if (firstNameField != null && lastNameField != null && dobField != null && govIDField != null && studentIDField != null) {
                    updateFieldStates();
                    highlightEditFields(!creationMode && currentPerson != null);
                }
            }
            private void updateComponentThemeRecursively(Component comp) {
                if (comp instanceof JPanel) {
                    comp.setBackground(UIManager.getColor("Viewer.background"));
                } else if (comp instanceof JLabel) {
                    comp.setForeground(UIManager.getColor("Viewer.foreground"));
                } else if (comp instanceof JTextField) {
                    comp.setBackground(UIManager.getColor("Viewer.fieldBackground"));
                    comp.setForeground(UIManager.getColor("Viewer.fieldForeground"));
                } else if (comp instanceof JButton) {
                    comp.setBackground(UIManager.getColor("Viewer.background"));
                    comp.setForeground(UIManager.getColor("Viewer.foreground"));
                }
                if (comp instanceof Container) {
                    for (Component child : ((Container) comp).getComponents()) {
                        updateComponentThemeRecursively(child);
                    }
                }
            }
        };
        panel.setBackground(UIManager.getColor("Viewer.background"));
        // UI construction (from PViewerPanel)
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 6));
        fieldsPanel.setBackground(UIManager.getColor("Viewer.background"));
        int fieldHeight = 22;
        int fieldWidth = 150;
        Dimension fieldDim = new Dimension(fieldWidth, fieldHeight);
        Dimension labelDim = new Dimension(fieldWidth, fieldHeight);
        fieldsPanel.add(Box.createVerticalStrut(4));
        JLabel firstNameLabel = new JLabel("First Name");
        firstNameLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        firstNameLabel.setMaximumSize(labelDim);
        firstNameLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(firstNameLabel);
        firstNameField = new JTextField();
        firstNameField.setMaximumSize(fieldDim);
        firstNameField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        firstNameField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        firstNameField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        fieldsPanel.add(firstNameField);
        fieldsPanel.add(Box.createVerticalStrut(4));
        JLabel lastNameLabel = new JLabel("Last Name");
        lastNameLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        lastNameLabel.setMaximumSize(labelDim);
        lastNameLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(lastNameLabel);
        lastNameField = new JTextField();
        lastNameField.setMaximumSize(fieldDim);
        lastNameField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        lastNameField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        lastNameField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        fieldsPanel.add(lastNameField);
        fieldsPanel.add(Box.createVerticalStrut(4));
        JLabel dobLabel = new JLabel("DOB");
        dobLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        dobLabel.setMaximumSize(labelDim);
        dobLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(dobLabel);
        dobField = new JTextField();
        dobField.setMaximumSize(fieldDim);
        dobField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        dobField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        dobField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        fieldsPanel.add(dobField);
        fieldsPanel.add(Box.createVerticalStrut(4));
        govIDLabel = new JLabel("Gov. ID");
        govIDLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        govIDLabel.setMaximumSize(labelDim);
        govIDLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(govIDLabel);
        govIDField = new JTextField();
        govIDField.setMaximumSize(fieldDim);
        govIDField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        govIDField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        govIDField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        fieldsPanel.add(govIDField);
        fieldsPanel.add(Box.createVerticalStrut(4));
        studentIDLabel = new JLabel("Student ID");
        studentIDLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        studentIDLabel.setMaximumSize(labelDim);
        studentIDLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(studentIDLabel);
        studentIDField = new JTextField();
        studentIDField.setMaximumSize(fieldDim);
        studentIDField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        studentIDField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        studentIDField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        fieldsPanel.add(studentIDField);
        fieldsPanel.add(Box.createVerticalStrut(8));
        panel.add(fieldsPanel, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(UIManager.getColor("Viewer.background"));
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        Dimension btnDim = new Dimension(120, fieldHeight);
        addButton.setMaximumSize(btnDim);
        updateButton.setMaximumSize(btnDim);
        deleteButton.setMaximumSize(btnDim);
        addButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
        updateButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
        deleteButton.setAlignmentX(JButton.LEFT_ALIGNMENT);
        addButton.setBackground(UIManager.getColor("Viewer.background"));
        updateButton.setBackground(UIManager.getColor("Viewer.background"));
        deleteButton.setBackground(UIManager.getColor("Viewer.background"));
        addButton.setForeground(UIManager.getColor("Viewer.foreground"));
        updateButton.setForeground(UIManager.getColor("Viewer.foreground"));
        deleteButton.setForeground(UIManager.getColor("Viewer.foreground"));
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(4));
        buttonPanel.add(updateButton);
        buttonPanel.add(Box.createVerticalStrut(4));
        buttonPanel.add(deleteButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 10, 6));
        panel.add(buttonPanel, BorderLayout.SOUTH);
        // Remove hardcoded textFieldBg, textFieldInactiveBg, textFieldHighlightBg, textFieldHighlightBorder
        attachButtonActions();
        attachFieldListeners();
        updateFieldStates();
        setCreationMode(true);
        // After all fields are initialized:
        applyThemeToTextFields();
        updateFieldStates();
        highlightEditFields(!creationMode && currentPerson != null);
    }

    // Helper to re-apply theme colors to all text fields
    private void applyThemeToTextFields() {
        Color bg = UIManager.getColor("Viewer.fieldBackground");
        Color fg = UIManager.getColor("Viewer.fieldForeground");
        firstNameField.setBackground(bg);
        firstNameField.setForeground(fg);
        lastNameField.setBackground(bg);
        lastNameField.setForeground(fg);
        dobField.setBackground(bg);
        dobField.setForeground(fg);
        govIDField.setBackground(bg);
        govIDField.setForeground(fg);
        studentIDField.setBackground(bg);
        studentIDField.setForeground(fg);
    }

    @Override
    public boolean displayPersonDetails(Person person) {
        // ...existing code from PViewerFields.displayPersonDetails...
        if (person == null) return false;
        currentPerson = person;
        firstNameField.setText(person.getFirstName());
        lastNameField.setText(person.getLastName());
        OCCCDate dob = person.getDOB();
        String formattedDate = String.format("%02d/%02d/%04d", dob.getMonthNumber(), dob.getDayOfMonth(), dob.getYear());
        dobField.setText(formattedDate);
        govIDField.setText("");
        studentIDField.setText("");
        if (person instanceof RegisteredPerson) {
            RegisteredPerson regPerson = (RegisteredPerson) person;
            govIDField.setText(regPerson.getGovID());
            if (person instanceof OCCCPerson) {
                OCCCPerson occPerson = (OCCCPerson) person;
                studentIDField.setText(occPerson.getStudentID());
            }
        }
        storeOriginalValues();
        hasPendingEdits = false;
        highlightEditFields(true);
        setCreationMode(false);
        return true;
    }
    @Override
    public void clearFields() {
        firstNameField.setText("");
        lastNameField.setText("");
        dobField.setText("");
        govIDField.setText("");
        studentIDField.setText("");
        currentPerson = null;
        highlightEditFields(false);
        setCreationMode(true);
    }
    @Override
    public void setDataList(PList dataList) {
        this.dataList = dataList;
    }
    @Override
    public Person getCurrentPerson() {
        return currentPerson;
    }
    @Override
    public boolean hasPartialData() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String dob = dobField.getText().trim();
        String govID = govIDField.getText().trim();
        String studentID = studentIDField.getText().trim();
        boolean allEmpty = firstName.isEmpty() && lastName.isEmpty() && dob.isEmpty() && govID.isEmpty() && studentID.isEmpty();
        if (allEmpty) {
            return false;
        }
        if (!creationMode && currentPerson != null) {
            checkForPendingEdits();
            if (hasPendingEdits) {
                return true;
            }
        }
        boolean missingRequiredFields = firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty();
        boolean invalidConfig = !studentID.isEmpty() && govID.isEmpty();
        return missingRequiredFields || invalidConfig;
    }
    @Override
    public JPanel getPanel() {
        return panel;
    }
    @Override
    public void addTextFieldChangeListener(PViewer.FieldChangeListener listener) {
        if (listener != null) {
            // Wrap the PViewer.FieldChangeListener in a local FieldChangeListener
            FieldChangeListener localListener = listener::onFieldsChanged;
            fieldChangeListeners.add(localListener);
            DocumentListener docListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { localListener.onFieldsChanged(); }
                @Override
                public void removeUpdate(DocumentEvent e) { localListener.onFieldsChanged(); }
                @Override
                public void changedUpdate(DocumentEvent e) { localListener.onFieldsChanged(); }
            };
            firstNameField.getDocument().addDocumentListener(docListener);
            lastNameField.getDocument().addDocumentListener(docListener);
            dobField.getDocument().addDocumentListener(docListener);
            govIDField.getDocument().addDocumentListener(docListener);
            studentIDField.getDocument().addDocumentListener(docListener);
        }
    }
    private void notifyFieldChangeListeners() {
        for (FieldChangeListener listener : fieldChangeListeners) {
            listener.onFieldsChanged();
        }
    }
    private void attachButtonActions() {
        addButton.addActionListener(_ -> {
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String dob = dobField.getText().trim();
            String govID = govIDField.getText().trim();
            String studentID = studentIDField.getText().trim();
            AppController.AddResult result = appController.addPersonFromFields(firstName, lastName, dob, govID, studentID);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person added successfully");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(parent, result.errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        updateButton.addActionListener(_ -> {
            if (currentPerson == null) return;
            int index = appController.getPeople().indexOf(currentPerson);
            if (index < 0) return;
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String dob = dobField.getText().trim();
            String govID = govIDField.getText().trim();
            String studentID = studentIDField.getText().trim();
            AppController.AddResult result = appController.updatePersonFromFields(index, firstName, lastName, dob, govID, studentID);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person updated successfully");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(parent, result.errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        deleteButton.addActionListener(_ -> {
            if (currentPerson == null) return;
            int confirm = JOptionPane.showConfirmDialog(parent, "Are you sure you want to delete this person?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean result = appController.deletePerson(currentPerson);
                if (result) {
                    JOptionPane.showMessageDialog(parent, "Person deleted successfully");
                    clearFields();
                } else {
                    JOptionPane.showMessageDialog(parent, "Failed to delete person", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    private void attachFieldListeners() {
        govIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
            public void removeUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
            public void insertUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
        });
        studentIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
            public void removeUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
            public void insertUpdate(DocumentEvent e) { 
                updateFieldStates(); 
                notifyFieldChangeListeners();
            }
        });
        panel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getSource() == panel) {
                    if (dataList != null && currentPerson != null) {
                        dataList.clearSelection();
                        clearFields();
                    }
                }
            }
        });
    }
    private void updateFieldStates() {
        Color textFieldBg = UIManager.getColor("Viewer.fieldBackground");
        Color textFieldInactiveBg = UIManager.getColor("TextField.inactiveBackground");
        boolean hasGovID = !govIDField.getText().trim().isEmpty();
        boolean hasStudentID = !studentIDField.getText().trim().isEmpty();
        govIDField.setBackground(hasGovID ? textFieldBg : textFieldInactiveBg);
        studentIDField.setBackground(hasStudentID ? textFieldBg : textFieldInactiveBg);
        studentIDField.setEnabled(true);
        govIDField.repaint();
        studentIDField.repaint();
    }
    private void updateButtonVisibility() {
        addButton.setVisible(creationMode);
        updateButton.setVisible(!creationMode);
        deleteButton.setVisible(!creationMode);
    }
    private void setCreationMode(boolean isCreating) {
        this.creationMode = isCreating;
        updateButtonVisibility();
    }
    private void storeOriginalValues() {
        originalFirstName = firstNameField.getText().trim();
        originalLastName = lastNameField.getText().trim();
        originalDob = dobField.getText().trim();
        originalGovID = govIDField.getText().trim();
        originalStudentID = studentIDField.getText().trim();
    }
    private boolean checkForPendingEdits() {
        if (creationMode || currentPerson == null) {
            return false;
        }
        String currentFirstName = firstNameField.getText().trim();
        String currentLastName = lastNameField.getText().trim();
        String currentDob = dobField.getText().trim();
        String currentGovID = govIDField.getText().trim();
        String currentStudentID = studentIDField.getText().trim();
        boolean hasChanges = !currentFirstName.equals(originalFirstName) ||
                            !currentLastName.equals(originalLastName) ||
                            !currentDob.equals(originalDob) ||
                            !currentGovID.equals(originalGovID) ||
                            !currentStudentID.equals(originalStudentID);
        hasPendingEdits = hasChanges;
        return hasPendingEdits;
    }
    private void highlightEditFields(boolean isEditing) {
        Color textFieldBg = UIManager.getColor("Viewer.fieldBackground");
        Color textFieldHighlightBg = UIManager.getColor("Highlight.background");
        Color textFieldHighlightBorder = UIManager.getColor("Highlight.border");
        Color bgColor = isEditing ? textFieldHighlightBg : textFieldBg;
        Color borderColor = isEditing ? textFieldHighlightBorder : null;
        firstNameField.setBackground(bgColor);
        lastNameField.setBackground(bgColor);
        dobField.setBackground(bgColor);
        if (!govIDField.getText().trim().isEmpty()) {
            govIDField.setBackground(bgColor);
        }
        if (!studentIDField.getText().trim().isEmpty()) {
            studentIDField.setBackground(bgColor);
        }
        if (isEditing) {
            firstNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            lastNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            dobField.setBorder(BorderFactory.createLineBorder(borderColor));
            govIDField.setBorder(BorderFactory.createLineBorder(borderColor));
            studentIDField.setBorder(BorderFactory.createLineBorder(borderColor));
        } else {
            firstNameField.setBorder(UIManager.getBorder("TextField.border"));
            lastNameField.setBorder(UIManager.getBorder("TextField.border"));
            dobField.setBorder(UIManager.getBorder("TextField.border"));
            govIDField.setBorder(UIManager.getBorder("TextField.border"));
            studentIDField.setBorder(UIManager.getBorder("TextField.border"));
        }
    }
}