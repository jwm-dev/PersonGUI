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
 * Handles field state, listeners, and change logic for the Person Viewer.
 */
public class PViewerFields {
    private final PViewerPanel panel;
    private final AppController appController;
    private final JFrame parent;
    private boolean creationMode = true;
    private Person currentPerson = null;
    private boolean hasPendingEdits = false;
    private String originalFirstName, originalLastName, originalDob, originalGovID, originalStudentID;
    private PList dataList;
    private Color textFieldBg = Color.WHITE;
    private Color textFieldInactiveBg = new Color(240, 240, 240);
    private Color textFieldHighlightBg = new Color(240, 255, 240);
    private Color textFieldHighlightBorder = new Color(0, 100, 0);
    public interface FieldChangeListener {
        void onFieldsChanged();
    }
    private List<FieldChangeListener> fieldChangeListeners = new ArrayList<>();
    public void addTextFieldChangeListener(FieldChangeListener listener) {
        if (listener != null && !fieldChangeListeners.contains(listener)) {
            fieldChangeListeners.add(listener);
            DocumentListener docListener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { notifyFieldChangeListeners(); }
                @Override
                public void removeUpdate(DocumentEvent e) { notifyFieldChangeListeners(); }
                @Override
                public void changedUpdate(DocumentEvent e) { notifyFieldChangeListeners(); }
            };
            panel.firstNameField.getDocument().addDocumentListener(docListener);
            panel.lastNameField.getDocument().addDocumentListener(docListener);
            panel.dobField.getDocument().addDocumentListener(docListener);
            panel.govIDField.getDocument().addDocumentListener(docListener);
            panel.studentIDField.getDocument().addDocumentListener(docListener);
        }
    }
    private void notifyFieldChangeListeners() {
        for (FieldChangeListener listener : fieldChangeListeners) {
            listener.onFieldsChanged();
        }
    }
    public PViewerFields(PViewerPanel panel, AppController manager, JFrame parent) {
        this.panel = panel;
        this.appController = manager;
        this.parent = parent;
        attachButtonActions();
        attachFieldListeners();
        updateFieldStates();
        setCreationMode(true);
    }
    private void attachButtonActions() {
        panel.addButton.addActionListener(_ -> {
            String firstName = panel.firstNameField.getText().trim();
            String lastName = panel.lastNameField.getText().trim();
            String dob = panel.dobField.getText().trim();
            String govID = panel.govIDField.getText().trim();
            String studentID = panel.studentIDField.getText().trim();
            AppController.AddResult result = appController.addPersonFromFields(firstName, lastName, dob, govID, studentID);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person added successfully");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(parent, result.errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.updateButton.addActionListener(_ -> {
            if (currentPerson == null) return;
            int index = appController.getPeople().indexOf(currentPerson);
            if (index < 0) return;
            String firstName = panel.firstNameField.getText().trim();
            String lastName = panel.lastNameField.getText().trim();
            String dob = panel.dobField.getText().trim();
            String govID = panel.govIDField.getText().trim();
            String studentID = panel.studentIDField.getText().trim();
            AppController.AddResult result = appController.updatePersonFromFields(index, firstName, lastName, dob, govID, studentID);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person updated successfully");
                clearFields();
            } else {
                JOptionPane.showMessageDialog(parent, result.errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        panel.deleteButton.addActionListener(_ -> {
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
        panel.govIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateFieldStates(); }
            public void removeUpdate(DocumentEvent e) { updateFieldStates(); }
            public void insertUpdate(DocumentEvent e) { updateFieldStates(); }
        });
        panel.studentIDField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateFieldStates(); }
            public void removeUpdate(DocumentEvent e) { updateFieldStates(); }
            public void insertUpdate(DocumentEvent e) { updateFieldStates(); }
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
        boolean hasGovID = !panel.govIDField.getText().trim().isEmpty();
        boolean hasStudentID = !panel.studentIDField.getText().trim().isEmpty();
        panel.govIDField.setBackground(hasGovID ? textFieldBg : textFieldInactiveBg);
        panel.studentIDField.setBackground(hasStudentID ? textFieldBg : textFieldInactiveBg);
        panel.studentIDField.setEnabled(true);
        panel.govIDField.repaint();
        panel.studentIDField.repaint();
    }
    private void updateButtonVisibility() {
        panel.addButton.setVisible(creationMode);
        panel.updateButton.setVisible(!creationMode);
        panel.deleteButton.setVisible(!creationMode);
    }
    private void setCreationMode(boolean isCreating) {
        this.creationMode = isCreating;
        updateButtonVisibility();
    }
    public void clearFields() {
        panel.firstNameField.setText("");
        panel.lastNameField.setText("");
        panel.dobField.setText("");
        panel.govIDField.setText("");
        panel.studentIDField.setText("");
        currentPerson = null;
        highlightEditFields(false);
        setCreationMode(true);
    }
    public boolean displayPersonDetails(Person person) {
        if (person == null) return false;
        currentPerson = person;
        panel.firstNameField.setText(person.getFirstName());
        panel.lastNameField.setText(person.getLastName());
        OCCCDate dob = person.getDOB();
        String formattedDate = String.format("%02d/%02d/%04d", dob.getMonthNumber(), dob.getDayOfMonth(), dob.getYear());
        panel.dobField.setText(formattedDate);
        panel.govIDField.setText("");
        panel.studentIDField.setText("");
        if (person instanceof RegisteredPerson) {
            RegisteredPerson regPerson = (RegisteredPerson) person;
            panel.govIDField.setText(regPerson.getGovID());
            if (person instanceof OCCCPerson) {
                OCCCPerson occPerson = (OCCCPerson) person;
                panel.studentIDField.setText(occPerson.getStudentID());
            }
        }
        storeOriginalValues();
        hasPendingEdits = false;
        highlightEditFields(true);
        setCreationMode(false);
        return true;
    }
    private void storeOriginalValues() {
        originalFirstName = panel.firstNameField.getText().trim();
        originalLastName = panel.lastNameField.getText().trim();
        originalDob = panel.dobField.getText().trim();
        originalGovID = panel.govIDField.getText().trim();
        originalStudentID = panel.studentIDField.getText().trim();
    }
    private boolean checkForPendingEdits() {
        if (creationMode || currentPerson == null) {
            return false;
        }
        String currentFirstName = panel.firstNameField.getText().trim();
        String currentLastName = panel.lastNameField.getText().trim();
        String currentDob = panel.dobField.getText().trim();
        String currentGovID = panel.govIDField.getText().trim();
        String currentStudentID = panel.studentIDField.getText().trim();
        boolean hasChanges = !currentFirstName.equals(originalFirstName) ||
                            !currentLastName.equals(originalLastName) ||
                            !currentDob.equals(originalDob) ||
                            !currentGovID.equals(originalGovID) ||
                            !currentStudentID.equals(originalStudentID);
        hasPendingEdits = hasChanges;
        return hasPendingEdits;
    }
    private void highlightEditFields(boolean isEditing) {
        Color bgColor = isEditing ? textFieldHighlightBg : textFieldBg;
        Color borderColor = isEditing ? textFieldHighlightBorder : null;
        panel.firstNameField.setBackground(bgColor);
        panel.lastNameField.setBackground(bgColor);
        panel.dobField.setBackground(bgColor);
        if (!panel.govIDField.getText().trim().isEmpty()) {
            panel.govIDField.setBackground(bgColor);
        }
        if (!panel.studentIDField.getText().trim().isEmpty()) {
            panel.studentIDField.setBackground(bgColor);
        }
        if (isEditing) {
            panel.firstNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            panel.lastNameField.setBorder(BorderFactory.createLineBorder(borderColor));
            panel.dobField.setBorder(BorderFactory.createLineBorder(borderColor));
            panel.govIDField.setBorder(BorderFactory.createLineBorder(borderColor));
            panel.studentIDField.setBorder(BorderFactory.createLineBorder(borderColor));
        } else {
            panel.firstNameField.setBorder(UIManager.getBorder("TextField.border"));
            panel.lastNameField.setBorder(UIManager.getBorder("TextField.border"));
            panel.dobField.setBorder(UIManager.getBorder("TextField.border"));
            panel.govIDField.setBorder(UIManager.getBorder("TextField.border"));
            panel.studentIDField.setBorder(UIManager.getBorder("TextField.border"));
        }
    }
    public void setDataList(PList dataList) {
        this.dataList = dataList;
    }
    public Person getCurrentPerson() {
        return currentPerson;
    }
    public boolean hasPartialData() {
        String firstName = panel.firstNameField.getText().trim();
        String lastName = panel.lastNameField.getText().trim();
        String dob = panel.dobField.getText().trim();
        String govID = panel.govIDField.getText().trim();
        String studentID = panel.studentIDField.getText().trim();
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
    public void setThemeColors(Color bg, Color inactiveBg, Color highlightBg, Color highlightBorder) {
        this.textFieldBg = bg;
        this.textFieldInactiveBg = inactiveBg;
        this.textFieldHighlightBg = highlightBg;
        this.textFieldHighlightBorder = highlightBorder;
    }
}
