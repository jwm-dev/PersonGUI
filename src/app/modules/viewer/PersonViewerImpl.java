package src.app.modules.viewer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import src.app.AppController;
import src.app.gui.FlatButton;
import src.app.modules.list.PList;
import src.date.OCCCDate;
import src.person.OCCCPerson;
import src.person.Person;
import src.person.RegisteredPerson;

/**
 * Implementation of the PViewer API interface, merging all viewer logic and UI.
 */
public class PersonViewerImpl implements PViewer, AppController.DateFormatChangeListener {
    private final JPanel panel;
    private final JTextField firstNameField;
    private final JTextField lastNameField;
    private final JTextField govIDField;
    private final JTextField studentIDField;
    private final JTextField dobField;
    private final JTextField tagsField;
    private final JTextArea descArea;
    private final JLabel govIDLabel;
    private final JLabel studentIDLabel;
    private final JButton addButton, updateButton, deleteButton;
    private final AppController appController;
    private final JFrame parent;
    private boolean creationMode = true;
    private Person currentPerson = null;
    private boolean hasPendingEdits = false;
    private String originalFirstName, originalLastName, originalDob, originalGovID, originalStudentID;
    private PList dataList;

    private JScrollPane descScroll;

    private final List<FieldChangeListener> fieldChangeListeners = new ArrayList<>();

    public PersonViewerImpl(JFrame parent, AppController manager) {
        this.parent = parent;
        this.appController = manager;
        manager.addDateFormatChangeListener(this);

        // Initialize all final fields before use
        this.firstNameField = new JTextField();
        this.lastNameField = new JTextField();
        this.dobField = new JTextField();
        this.govIDField = new JTextField();
        this.studentIDField = new JTextField();
        this.tagsField = new JTextField();
        this.descArea = new JTextArea();
        this.govIDLabel = new JLabel("Government ID");
        this.studentIDLabel = new JLabel("Student ID");

        this.panel = new PersonViewerPanel(new BorderLayout()) {
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
                // Explicitly re-theme the description area and its scroll pane
                if (descArea != null) {
                    descArea.setBackground(UIManager.getColor("Viewer.fieldBackground"));
                    descArea.setForeground(UIManager.getColor("Viewer.fieldForeground"));
                    if (descArea.getParent() instanceof JScrollPane descScroll) {
                        descScroll.setBackground(UIManager.getColor("Viewer.fieldBackground"));
                        descScroll.setForeground(UIManager.getColor("Viewer.fieldForeground"));
                    }
                }
                // Ensure calendar dialog is closed and recreated with the new theme
                refreshCalendarTheme();
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
        // --- Main fields panel (top-aligned) ---
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 6));
        fieldsPanel.setBackground(UIManager.getColor("Viewer.background"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 2, 0);
        JLabel firstNameLabel = new JLabel("First Name");
        firstNameLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(firstNameLabel, gbc);
        gbc.gridy++;
        fieldsPanel.add(firstNameField, gbc);
        gbc.gridy++;
        JLabel lastNameLabel = new JLabel("Last Name");
        lastNameLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(lastNameLabel, gbc);
        gbc.gridy++;
        fieldsPanel.add(lastNameField, gbc);
        gbc.gridy++;
        JLabel dobLabel = new JLabel("Date of Birth");
        dobLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(dobLabel, gbc);
        gbc.gridy++;
        fieldsPanel.add(createDOBFieldWithCalendar(), gbc);
        gbc.gridy++;
        govIDLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(govIDLabel, gbc);
        gbc.gridy++;
        fieldsPanel.add(govIDField, gbc);
        gbc.gridy++;
        studentIDLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        fieldsPanel.add(studentIDLabel, gbc);
        gbc.gridy++;
        fieldsPanel.add(studentIDField, gbc);
        gbc.gridy++;
        // --- Description area (center, expands) ---
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setOpaque(false);
        JLabel descLabel = new JLabel("Description");
        descLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        descPanel.add(descLabel, BorderLayout.NORTH);
        descArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        descScroll = new JScrollPane(descArea);
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        descScroll.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        descScroll.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        descScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        descPanel.add(descScroll, BorderLayout.CENTER);
        // --- Tag field and buttons (bottom) ---
        JPanel tagAndButtonsPanel = new JPanel(new BorderLayout());
        tagAndButtonsPanel.setOpaque(false);
        // Tag label and field at the top
        JPanel tagFieldPanel = new JPanel();
        tagFieldPanel.setLayout(new BoxLayout(tagFieldPanel, BoxLayout.Y_AXIS));
        tagFieldPanel.setOpaque(false);
        JLabel tagsLabel = new JLabel("Tags");
        tagsLabel.setForeground(UIManager.getColor("Viewer.foreground"));
        tagFieldPanel.add(tagsLabel);
        tagFieldPanel.add(Box.createVerticalStrut(4)); // Add space between label and field
        tagsField.setMaximumSize(new Dimension(Integer.MAX_VALUE, tagsField.getPreferredSize().height));
        tagsField.setPreferredSize(new Dimension(0, 22));
        tagsField.setMinimumSize(new Dimension(0, 22));
        tagsField.setBackground(UIManager.getColor("Viewer.fieldBackground"));
        tagsField.setForeground(UIManager.getColor("Viewer.fieldForeground"));
        tagsField.setBorder(UIManager.getBorder("TextField.border"));
        tagFieldPanel.add(tagsField);
        tagFieldPanel.add(Box.createVerticalStrut(16)); // Add space between tags field and buttons
        tagAndButtonsPanel.add(tagFieldPanel, BorderLayout.NORTH);
        // Button panel at the bottom, always centered horizontally
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setOpaque(false);
        addButton = new FlatButton("Add");
        updateButton = new FlatButton("Update");
        deleteButton = new FlatButton("Delete");
        Dimension btnSize = new Dimension(120, 28);
        addButton.setPreferredSize(btnSize);
        updateButton.setPreferredSize(btnSize);
        deleteButton.setPreferredSize(btnSize);
        GridBagConstraints bgbc = new GridBagConstraints();
        bgbc.gridy = 0;
        bgbc.insets = new Insets(0, 8, 0, 8);
        bgbc.anchor = GridBagConstraints.CENTER;
        bgbc.fill = GridBagConstraints.NONE;
        bgbc.gridx = 0;
        buttonPanel.add(addButton, bgbc);
        bgbc.gridx = 1;
        buttonPanel.add(updateButton, bgbc);
        bgbc.gridx = 2;
        buttonPanel.add(deleteButton, bgbc);
        // Add space above the button panel
        JPanel buttonPanelWrapper = new JPanel(new BorderLayout());
        buttonPanelWrapper.setOpaque(false);
        buttonPanelWrapper.add(Box.createVerticalStrut(16), BorderLayout.NORTH);
        buttonPanelWrapper.add(buttonPanel, BorderLayout.CENTER);
        buttonPanelWrapper.add(Box.createVerticalStrut(12), BorderLayout.SOUTH); // Space below buttons
        tagAndButtonsPanel.add(buttonPanelWrapper, BorderLayout.SOUTH);
        // --- Compose main panel ---
        panel.setLayout(new BorderLayout());
        panel.add(fieldsPanel, BorderLayout.NORTH);
        panel.add(descPanel, BorderLayout.CENTER);
        panel.add(tagAndButtonsPanel, BorderLayout.SOUTH);
        // Remove hardcoded textFieldBg, textFieldInactiveBg, textFieldHighlightBg, textFieldHighlightBorder
        attachButtonActions();
        attachFieldListeners();
        updateFieldStates();
        setCreationMode(true);
        // After all fields are initialized:
        applyThemeToTextFields();
        installFieldFocusListeners();
        updateFieldStates();
        highlightEditFields(!creationMode && currentPerson != null);
        // Remove all setPreferredSize, setMaximumSize, and setMinimumSize calls for all fields, labels, and scrolls
        // Only use GridBagLayout with weightx=1.0 and fill=HORIZONTAL for every field and label
        // This ensures fields always match the module width and never overflow or get covered
    }

    // Helper to re-apply theme colors to all text fields
    private void applyThemeToTextFields() {
        // Always fetch the latest color from UIManager for every border
        Color bg = UIManager.getColor("Viewer.fieldBackground");
        Color fg = UIManager.getColor("Viewer.fieldForeground");
        JTextField[] fields = {firstNameField, lastNameField, dobField, govIDField, studentIDField, tagsField};
        for (JTextField field : fields) {
            field.setBackground(bg);
            field.setForeground(fg);
            field.setBorder(new AnimatedAccentBorder(field, 12, 1.5f));
        }
        if (descScroll != null) {
            descArea.setBackground(bg);
            descArea.setForeground(fg);
            descScroll.setBorder(new AnimatedAccentBorder(descScroll, 12, 1.5f));
        }
    }

    private static Color getLiveAccent() {
        Color accent = UIManager.getColor("ACCENT");
        if (accent == null) accent = Color.BLUE; // fallback to blue if theme is missing ACCENT
        return accent;
    }

    private static class AnimatedAccentBorder extends javax.swing.border.AbstractBorder {
        private int arc;
        private float thickness;
        private float animPhase = 0f;
        private boolean focused = false;
        private javax.swing.Timer timer;
        private final JComponent owner;
        public AnimatedAccentBorder(JComponent owner, int arc, float thickness) {
            this.owner = owner;
            this.arc = arc;
            this.thickness = thickness;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Color accent = getLiveAccent();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float anim = (float) (0.5 + 0.5 * Math.sin(animPhase));
            Color borderColor = focused ? accent.brighter() : accent.darker();
            if (focused) {
                borderColor = blend(accent, Color.WHITE, 0.18f + 0.12f * anim);
            }
            g2.setStroke(new BasicStroke(thickness + (focused ? 0.7f * anim : 0f)));
            g2.setColor(borderColor);
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, arc, arc);
            g2.dispose();
        }
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 8, 6, 8);
        }
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(6, 8, 6, 8);
            return insets;
        }
        private static Color blend(Color c1, Color c2, float ratio) {
            float ir = 1.0f - ratio;
            int r = (int) (c1.getRed() * ir + c2.getRed() * ratio);
            int g = (int) (c1.getGreen() * ir + c2.getGreen() * ratio);
            int b = (int) (c1.getBlue() * ir + c2.getBlue() * ratio);
            return new Color(r, g, b, 255);
        }
        private void startAnimation() {
            if (timer == null) {
                timer = new javax.swing.Timer(30, _ -> {
                    animPhase += 0.09f;
                    owner.repaint();
                });
            }
            if (!timer.isRunning()) timer.start();
        }
        private void stopAnimation() {
            if (timer != null && timer.isRunning()) timer.stop();
        }
        public void setFocused(boolean focused) {
            if (this.focused != focused) {
                this.focused = focused;
                if (focused) startAnimation(); else stopAnimation();
                owner.repaint();
            }
        }
    }

    private void installFieldFocusListeners() {
        JTextField[] fields = {firstNameField, lastNameField, dobField, govIDField, studentIDField, tagsField};
        for (JTextField field : fields) {
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (field.getBorder() instanceof AnimatedAccentBorder b) b.setFocused(true);
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (field.getBorder() instanceof AnimatedAccentBorder b) b.setFocused(false);
                }
            });
        }
        if (descScroll != null) {
            descArea.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (descScroll.getBorder() instanceof AnimatedAccentBorder b) b.setFocused(true);
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (descScroll.getBorder() instanceof AnimatedAccentBorder b) b.setFocused(false);
                }
            });
        }
    }

    @Override
    public boolean displayPersonDetails(Person person) {
        // ...existing code from PViewerFields.displayPersonDetails...
        if (person == null) return false;
        currentPerson = person;
        firstNameField.setText(person.getFirstName());
        lastNameField.setText(person.getLastName());
        OCCCDate dob = person.getDOB();
        // Format DOB using AppController's current date format
        String formattedDate = appController.formatDate(dob);
        dobField.setText(formattedDate);
        govIDField.setText("");
        studentIDField.setText("");
        // Load description and tags from People metadata if available
        descArea.setText("");
        tagsField.setText("");
        if (appController != null) {
            int idx = appController.getPeople().indexOf(person);
            if (idx >= 0) {
                var meta = appController.getPeople().getMeta(idx);
                descArea.setText(meta.getDescription());
                tagsField.setText(meta.getTags());
            }
        }
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
        descArea.setText("");
        tagsField.setText("");
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
            String desc = descArea.getText().trim();
            String tags = tagsField.getText().trim();
            try {
                OCCCDate parsedDOB = parseDOBField();
                dob = appController.formatDate(parsedDOB); // Normalize to formatted string
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null || !msg.toLowerCase().contains("date format")) {
                    msg = "Invalid date format.";
                }
                msg += "\nExpected format: " + getCurrentDateFormatExample();
                JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            AppController.AddResult result = appController.addPersonFromFields(firstName, lastName, dob, govID, studentID, desc, tags);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person added successfully");
                clearFields();
            } else {
                String msg = result.errorMessage;
                if (msg != null && msg.toLowerCase().contains("date format")) {
                    msg += "\nExpected format: " + getCurrentDateFormatExample();
                }
                JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
            String desc = descArea.getText().trim();
            String tags = tagsField.getText().trim();
            try {
                OCCCDate parsedDOB = parseDOBField();
                dob = appController.formatDate(parsedDOB);
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if (msg == null || !msg.toLowerCase().contains("date format")) {
                    msg = "Invalid date format.";
                }
                msg += "\nExpected format: " + getCurrentDateFormatExample();
                JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            AppController.AddResult result = appController.updatePersonFromFields(index, firstName, lastName, dob, govID, studentID, desc, tags);
            if (result.success) {
                JOptionPane.showMessageDialog(parent, "Person updated successfully");
                clearFields();
            } else {
                String msg = result.errorMessage;
                if (msg != null && msg.toLowerCase().contains("date format")) {
                    msg += "\nExpected format: " + getCurrentDateFormatExample();
                }
                JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
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
    private String getCurrentDateFormatExample() {
        AppController.DateFormatType fmt = appController.getDateFormat();
        switch (fmt) {
            case US: return "MM/dd/yyyy (e.g. 05/17/2025)";
            case EURO: return "dd/MM/yyyy (e.g. 17/05/2025)";
            case ISO: return "yyyy-MM-dd (e.g. 2025-05-17)";
            default: return "MM/dd/yyyy (e.g. 05/17/2025)";
        }
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
        Color bgColor = isEditing ? textFieldHighlightBg : textFieldBg;
        firstNameField.setBackground(bgColor);
        lastNameField.setBackground(bgColor);
        dobField.setBackground(bgColor);
        if (!govIDField.getText().trim().isEmpty()) {
            govIDField.setBackground(bgColor);
        }
        if (!studentIDField.getText().trim().isEmpty()) {
            studentIDField.setBackground(bgColor);
        }
        // Do not touch borders here!
    }

    // Helper to parse DOB field using the current date format
    private OCCCDate parseDOBField() throws Exception {
        String dob = dobField.getText().trim();
        // For ISO, allow both yyyy-MM-dd and yyyy/MM/dd for user convenience
        if (appController.getDateFormat() == AppController.DateFormatType.ISO) {
            dob = dob.replace('/', '-');
        }
        return appController.parseDateWithCurrentFormat(dob);
    }

    /**
     * Ensures all fields and borders update with the current theme. Call after theme changes.
     */
    public void updateUI() {
        // Recreate borders with the latest theme color
        applyThemeToTextFields();
        // Force repaint for all fields and scroll
        JTextField[] fields = {firstNameField, lastNameField, dobField, govIDField, studentIDField, tagsField};
        for (JTextField field : fields) {
            field.setBorder(new AnimatedAccentBorder(field, 12, 1.5f));
            field.updateUI();
        }
        if (descScroll != null) {
            descScroll.setBorder(new AnimatedAccentBorder(descScroll, 12, 1.5f));
            descScroll.updateUI();
        }
        if (descArea != null) descArea.updateUI();
        if (panel != null) panel.updateUI();
        // No need to update button theme colors, handled by global UI
        refreshCalendarTheme(); // Ensure calendar dialog is rethemed if open
    }

    /**
     * Ensures the calendar dialog (if open) is rethemed to match the current UI theme.
     */
    private void refreshCalendarTheme() {
        if (calendarDialog != null && calendarDialog.isVisible()) {
            calendarDialog.setVisible(false);
            calendarDialog.dispose();
            calendarDialog = null;
        }
    }

    /**
     * Call this after a theme change to ensure all field borders and colors update to the new theme.
     */
    public void refreshFieldBordersForTheme() {
        applyThemeToTextFields();
        // Force repaint for all fields and scroll
        JTextField[] fields = {firstNameField, lastNameField, dobField, govIDField, studentIDField, tagsField};
        for (JTextField field : fields) field.repaint();
        if (descScroll != null) descScroll.repaint();
        if (descArea != null) descArea.repaint();
        refreshCalendarTheme(); // Also retheme calendar dialog if open
    }

    /**
     * Call this after a theme change to ensure all viewer UI and popups are updated.
     */
    public void onThemeChanged() {
        refreshFieldBordersForTheme();
    }

    public class PersonViewerPanel extends JPanel implements Scrollable {
        public PersonViewerPanel(LayoutManager layout) {
            super(layout);
        }
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getParent() != null ? getParent().getSize() : getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.width, visibleRect.height) - 16;
        }
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }

    // Replace SimpleCalendarPopup with a JDialog-based popup
    private class CalendarDialog extends JDialog {
        private JPanel calendarPanel;
        private JComboBox<Integer> yearBox;
        private JComboBox<String> monthBox;
        private JPanel daysPanel;
        private java.util.Calendar selectedCal;
        private final java.util.function.Consumer<java.util.Calendar> onDateSelected;
        private final String[] monthNames = new java.text.DateFormatSymbols().getMonths();
        public CalendarDialog(JFrame parent, java.util.Calendar initial, java.util.function.Consumer<java.util.Calendar> onDateSelected) {
            super(parent, false);
            this.onDateSelected = onDateSelected;
            setUndecorated(true);
            setFocusableWindowState(false);
            selectedCal = (java.util.Calendar) initial.clone();
            calendarPanel = new JPanel(new BorderLayout());
            daysPanel = new JPanel(new GridLayout(0, 7));
            // Year range: 1900 to current year + 10
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            java.util.List<Integer> years = new java.util.ArrayList<>();
            for (int y = 1900; y <= currentYear + 10; y++) years.add(y);
            yearBox = new JComboBox<>(years.toArray(new Integer[0]));
            yearBox.setSelectedItem(initial.get(java.util.Calendar.YEAR));
            yearBox.addActionListener(_ -> updateCalendar());
            monthBox = new JComboBox<>();
            for (int i = 0; i < 12; i++) monthBox.addItem(monthNames[i]);
            monthBox.setSelectedIndex(initial.get(java.util.Calendar.MONTH));
            monthBox.addActionListener(_ -> updateCalendar());
            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
            topPanel.setOpaque(false);
            topPanel.add(monthBox);
            topPanel.add(yearBox);
            calendarPanel.add(topPanel, BorderLayout.NORTH);
            daysPanel.setOpaque(false);
            calendarPanel.add(daysPanel, BorderLayout.CENTER);
            calendarPanel.setOpaque(false);
            add(calendarPanel);
            applyTheme();
            setPreferredSize(new Dimension(220, 220));
            updateCalendar();
            pack();
        }
        private void updateCalendar() {
            int year = (Integer) yearBox.getSelectedItem();
            int month = monthBox.getSelectedIndex();
            selectedCal.set(java.util.Calendar.YEAR, year);
            selectedCal.set(java.util.Calendar.MONTH, month);
            selectedCal.set(java.util.Calendar.DAY_OF_MONTH, 1);
            daysPanel.removeAll();
            String[] weekDays = {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
            for (String wd : weekDays) {
                JLabel lbl = new JLabel(wd, SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                daysPanel.add(lbl);
            }
            int firstDay = selectedCal.get(java.util.Calendar.DAY_OF_WEEK);
            int daysInMonth = selectedCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
            for (int i = 1; i < firstDay; i++) daysPanel.add(new JLabel(""));
            for (int d = 1; d <= daysInMonth; d++) {
                FlatButton btn = new FlatButton(String.valueOf(d));
                btn.setFont(btn.getFont().deriveFont(Font.PLAIN));
                btn.setBackground(UIManager.getColor("Viewer.fieldBackground"));
                btn.setForeground(UIManager.getColor("Viewer.fieldForeground"));
                btn.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
                btn.addActionListener(_ -> {
                    selectedCal.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(btn.getText()));
                    onDateSelected.accept((java.util.Calendar) selectedCal.clone());
                    setVisible(false);
                });
                daysPanel.add(btn);
            }
            daysPanel.revalidate();
            daysPanel.repaint();
        }
        public void applyTheme() {
            Color fieldBg = UIManager.getColor("Viewer.fieldBackground");
            Color fieldFg = UIManager.getColor("Viewer.fieldForeground");
            setBackground(fieldBg);
            calendarPanel.setBackground(fieldBg);
            daysPanel.setBackground(fieldBg);
            yearBox.setBackground(fieldBg);
            yearBox.setForeground(fieldFg);
            monthBox.setBackground(fieldBg);
            monthBox.setForeground(fieldFg);
            // Ensure the content pane background is set and opaque
            getContentPane().setBackground(fieldBg);
            if (getContentPane() instanceof JComponent) {
                ((JComponent) getContentPane()).setOpaque(true);
            }
            // --- Fix: force popup list background/foreground for combo boxes ---
            updateComboPopupColors(yearBox, fieldBg, fieldFg);
            updateComboPopupColors(monthBox, fieldBg, fieldFg);
            // Force UI update for dialog and combo boxes to apply new theme
            yearBox.updateUI();
            monthBox.updateUI();
            calendarPanel.updateUI();
            daysPanel.updateUI();
            // Refresh the dialog itself
            this.repaint();
            this.revalidate();
        }
        // Helper to update popup list colors for JComboBox
        private void updateComboPopupColors(JComboBox<?> box, Color bg, Color fg) {
            Object comp = box.getUI().getAccessibleChild(box, 0);
            if (comp instanceof JPopupMenu popup) {
                Component scroller = popup.getComponent(0);
                if (scroller instanceof JScrollPane scrollPane) {
                    JViewport viewport = scrollPane.getViewport();
                    Component view = viewport.getView();
                    if (view instanceof JList<?> list) {
                        list.setBackground(bg);
                        list.setForeground(fg);
                        list.updateUI();
                    }
                }
            }
        }
    }

    private CalendarDialog calendarDialog;

    private JPanel createDOBFieldWithCalendar() {
        JPanel panel = new JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth();
                int h = getHeight();
                int btnW = 28;
                dobField.setBounds(0, 0, w - btnW, h);
                calendarButton.setBounds(w - btnW, 0, btnW, h);
            }
            @Override
            public Dimension getPreferredSize() {
                Dimension d = dobField.getPreferredSize();
                return new Dimension(d.width + 28, d.height);
            }
        };
        panel.setOpaque(false);
        dobField.setBorder(UIManager.getBorder("TextField.border"));
        panel.add(dobField);
        panel.add(calendarButton);
        calendarButton.setFocusable(false);
        calendarButton.setMargin(new Insets(0, 0, 0, 0));
        calendarButton.setBorderPainted(false);
        calendarButton.setContentAreaFilled(false);
        calendarButton.setToolTipText("Pick a date");
        for (var l : calendarButton.getActionListeners()) calendarButton.removeActionListener(l);
        for (var ml : calendarButton.getMouseListeners()) calendarButton.removeMouseListener(ml);
        calendarButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                showCalendarDialog();
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                showCalendarDialog();
            }
        });
        return panel;
    }
    private void showCalendarDialog() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.util.Date date = parseDOBFieldToDate();
        if (date != null) cal.setTime(date);
        if (calendarDialog != null && calendarDialog.isVisible()) {
            calendarDialog.setVisible(false);
        }
        calendarDialog = new CalendarDialog(parent, cal, picked -> {
            dobField.setText(formatCalendarDate(picked));
            calendarDialog.setVisible(false);
        });
        calendarDialog.applyTheme(); // Ensure theme is applied immediately
        // Only add mouse listeners to the main panel, not to combo boxes or their popups
        final boolean[] mouseOverDialog = {false};
        final javax.swing.Timer[] closeTimer = {null};
        MouseAdapter dialogHoverListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseOverDialog[0] = true;
                if (closeTimer[0] != null && closeTimer[0].isRunning()) closeTimer[0].stop();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                Point mouse = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mouse, calendarDialog.getContentPane());
                if (!calendarDialog.getContentPane().contains(mouse)) {
                    mouseOverDialog[0] = false;
                    if (closeTimer[0] == null) {
                        closeTimer[0] = new javax.swing.Timer(150, _ -> {
                            if (!mouseOverDialog[0] && calendarDialog != null && calendarDialog.isVisible()) {
                                calendarDialog.setVisible(false);
                            }
                        });
                        closeTimer[0].setRepeats(false);
                    }
                    closeTimer[0].restart();
                }
            }
        };
        // Only add to the main calendarPanel, not recursively
        calendarDialog.calendarPanel.addMouseListener(dialogHoverListener);
        // --- Improved positioning ---
        Point btnLoc = new Point(0, calendarButton.getHeight());
        SwingUtilities.convertPointToScreen(btnLoc, calendarButton);
        Dimension dialogSize = calendarDialog.getPreferredSize();
        Rectangle parentBounds = parent.getBounds();
        Point parentLocOnScreen = parent.getLocationOnScreen();
        Rectangle parentScreenBounds = new Rectangle(parentLocOnScreen.x, parentLocOnScreen.y, parentBounds.width, parentBounds.height);
        int x = btnLoc.x;
        int y = btnLoc.y;
        if (x + dialogSize.width > parentScreenBounds.x + parentScreenBounds.width) {
            x = parentScreenBounds.x + parentScreenBounds.width - dialogSize.width;
        }
        if (x < parentScreenBounds.x) {
            x = parentScreenBounds.x;
        }
        if (y + dialogSize.height > parentScreenBounds.y + parentScreenBounds.height) {
            y = btnLoc.y - dialogSize.height - calendarButton.getHeight(); // show above if not enough space below
        }
        if (y < parentScreenBounds.y) {
            y = parentScreenBounds.y;
        }
        calendarDialog.setLocation(x, y);
        calendarDialog.setVisible(true);
    }

    private java.util.Date parseDOBFieldToDate() {
        String dobText = dobField.getText().trim();
        if (dobText.isEmpty()) return null;
        try {
            OCCCDate occcDate = appController.parseDateWithCurrentFormat(dobText);
            // Convert OCCCDate to java.util.Date using GregorianCalendar
            java.util.GregorianCalendar gc = new java.util.GregorianCalendar(
                occcDate.getYear(), occcDate.getMonthNumber() - 1, occcDate.getDayOfMonth()
            );
            return gc.getTime();
        } catch (Exception e) {
            return null;
        }
    }

    private String formatCalendarDate(java.util.Calendar cal) {
        // Create OCCCDate from Calendar fields
        OCCCDate occcDate = new OCCCDate(
            cal.get(java.util.Calendar.DAY_OF_MONTH),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.YEAR)
        );
        return appController.formatDate(occcDate);
    }

    private final FlatButton calendarButton = new FlatButton("Cal"); // Changed from emoji to text for compatibility

    @Override
    public void onDateFormatChanged() {
        updateUI(); // Re-apply theme and update calendar if open
    }
}