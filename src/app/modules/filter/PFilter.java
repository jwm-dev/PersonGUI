package src.app.modules.filter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Predicate;
import javax.swing.filechooser.FileNameExtensionFilter;
import src.person.Person;
import src.app.gui.Dialogs;
import src.app.modules.list.PList;
import src.person.People;

/**
 * PFilter is a filtration widget for filtering the People list.
 * It allows users to set custom filtration routines and see sublists based on provided terms.
 * Designed to be user-friendly, powerful, and extensible.
 */
public class PFilter extends JPanel {
    private final JTextField searchField;
    private final JComboBox<String> filterTypeBox;
    private final JButton clearButton;
    private JButton exportButton;
    private Predicate<Person> customFilter;
    private FilterListener filterListener;
    private Dialogs operations;
    private PList listModule;

    public interface FilterListener {
        void onFilterChanged(Predicate<Person> filter);
    }

    public PFilter() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Filter List"));
        int fieldHeight = 22;
        int fieldWidth = 100;
        Dimension fieldDim = new Dimension(fieldWidth, fieldHeight);
        Dimension labelDim = new Dimension(fieldWidth, fieldHeight);

        add(Box.createVerticalStrut(4));
        JLabel filterTypeLabel = new JLabel("Field");
        filterTypeLabel.setAlignmentX(LEFT_ALIGNMENT);
        filterTypeLabel.setMaximumSize(labelDim);
        add(filterTypeLabel);
        filterTypeBox = new JComboBox<>(new String[] {
            "All Fields", "First Name", "Last Name", "DOB", "Government ID", "Student ID"
        });
        filterTypeBox.setToolTipText("Select field to filter by");
        filterTypeBox.setPreferredSize(fieldDim);
        filterTypeBox.setMaximumSize(fieldDim);
        filterTypeBox.setAlignmentX(LEFT_ALIGNMENT);
        add(filterTypeBox);
        add(Box.createVerticalStrut(4));

        JLabel searchLabel = new JLabel("Term");
        searchLabel.setAlignmentX(LEFT_ALIGNMENT);
        searchLabel.setMaximumSize(labelDim);
        add(searchLabel);
        // Make the searchField (term textbox) much larger vertically
        searchField = new JTextField();
        searchField.setToolTipText("Enter filter term(s)");
        searchField.setPreferredSize(new Dimension(fieldWidth, 60));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        searchField.setMinimumSize(new Dimension(fieldWidth, 40));
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        add(searchField);
        add(Box.createVerticalStrut(8));

        // Button panel: vertical, justified to bottom, no label shortening
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        clearButton = new JButton("Clear");
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.setMaximumSize(new Dimension(160, fieldHeight));
        exportButton = new JButton("Export Filtered");
        exportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportButton.setMaximumSize(new Dimension(160, fieldHeight));
        buttonPanel.add(clearButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(exportButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(buttonPanel);

        // Listeners
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { notifyFilterChanged(); }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { notifyFilterChanged(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { notifyFilterChanged(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { notifyFilterChanged(); }
        });
        filterTypeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { notifyFilterChanged(); }
        });
        clearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                notifyFilterChanged();
            }
        });
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { exportFilteredList(); }
        });
    }

    /**
     * Set the People list to filter (optional, for advanced use)
     */
    public void setPeople(People people) {
        // Removed unused field
    }

    /**
     * Set a custom filter predicate (for extensibility)
     */
    public void setCustomFilter(Predicate<Person> filter) {
        this.customFilter = filter;
        notifyFilterChanged();
    }

    /**
     * Set a listener to be notified when the filter changes
     */
    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    /**
     * Set the operations instance for exporting
     */
    public void setOperations(Dialogs operations) {
        this.operations = operations;
    }

    /**
     * Set the list module instance for exporting
     */
    public void setListModule(PList listModule) {
        this.listModule = listModule;
    }

    /**
     * Get the current filter predicate
     */
    public Predicate<Person> getCurrentFilter() {
        String term = searchField.getText().trim().toLowerCase();
        String type = (String) filterTypeBox.getSelectedItem();
        Predicate<Person> baseFilter;
        if (term.isEmpty()) {
            baseFilter = _ -> true;
        } else {
            switch (type) {
                case "First Name":
                    baseFilter = p -> p.getFirstName() != null && p.getFirstName().toLowerCase().contains(term);
                    break;
                case "Last Name":
                    baseFilter = p -> p.getLastName() != null && p.getLastName().toLowerCase().contains(term);
                    break;
                case "DOB":
                    baseFilter = p -> p.getDOB() != null && p.getDOB().toString().toLowerCase().contains(term);
                    break;
                case "Government ID":
                    baseFilter = p -> {
                        try {
                            java.lang.reflect.Method m = p.getClass().getMethod("getGovID");
                            Object v = m.invoke(p);
                            return v != null && v.toString().toLowerCase().contains(term);
                        } catch (Exception ex) { return false; }
                    };
                    break;
                case "Student ID":
                    baseFilter = p -> {
                        try {
                            java.lang.reflect.Method m = p.getClass().getMethod("getStudentID");
                            Object v = m.invoke(p);
                            return v != null && v.toString().toLowerCase().contains(term);
                        } catch (Exception ex) { return false; }
                    };
                    break;
                default:
                    baseFilter = p ->
                        (p.getFirstName() != null && p.getFirstName().toLowerCase().contains(term)) ||
                        (p.getLastName() != null && p.getLastName().toLowerCase().contains(term)) ||
                        (p.getDOB() != null && p.getDOB().toString().toLowerCase().contains(term));
            }
        }
        if (customFilter != null) {
            return baseFilter.and(customFilter);
        }
        return baseFilter;
    }

    private void notifyFilterChanged() {
        if (filterListener != null) {
            filterListener.onFilterChanged(getCurrentFilter());
        }
    }

    private void exportFilteredList() {
        if (operations == null || listModule == null) {
            JOptionPane.showMessageDialog(this, "Export not available.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        People filtered = listModule.getFilteredPeople();
        if (filtered == null || filtered.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No people to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Filtered List");
        FileNameExtensionFilter pplFilter = new FileNameExtensionFilter("People Files (*.ppl)", "ppl");
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON Files (*.json)", "json");
        FileNameExtensionFilter txtFilter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        fileChooser.addChoosableFileFilter(pplFilter);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(pplFilter);
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File selectedFile = fileChooser.getSelectedFile();
            String name = selectedFile.getName().toLowerCase();
            String format;
            if (name.endsWith(".json")) {
                format = "json";
            } else if (name.endsWith(".txt")) {
                format = "txt";
            } else {
                format = "ppl";
            }
            if (!name.endsWith("." + format)) {
                selectedFile = new java.io.File(selectedFile.getParentFile(), selectedFile.getName() + "." + format);
            }
            try {
                int count = exportPeople(filtered, selectedFile, format);
                JOptionPane.showMessageDialog(this, count + " people exported to " + selectedFile.getName(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Helper for exportFilteredList: call Operations exportPeople with correct signature
    private int exportPeople(People people, java.io.File file, String format) throws Exception {
        if (operations == null) throw new IllegalStateException("No Operations instance");
        java.util.function.Function<src.date.OCCCDate, String> dateFormatter = (date) -> {
            if (date == null) return "";
            return String.format("%02d/%02d/%04d", date.getMonthNumber(), date.getDayOfMonth(), date.getYear());
        };
        if ("json".equals(format)) {
            return Dialogs.exportToJson(people, file, dateFormatter);
        } else if ("txt".equals(format)) {
            return Dialogs.exportToText(people, file, dateFormatter);
        } else {
            // Default to .ppl (serialized)
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file))) {
                oos.writeObject(people);
            }
            return people.size();
        }
    }
}
