package src.app.modules.filter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Predicate;
import javax.swing.filechooser.FileNameExtensionFilter;
import src.person.Person;
import src.app.dialogs.Dialogs;
import src.app.modules.list.PList;
import src.person.People;

public class PersonFilterImpl extends JPanel implements PFilter {
    private final JTextField searchField;
    private final JComboBox<String> filterTypeBox;
    private final JButton clearButton;
    private JButton exportButton;
    private Predicate<Person> customFilter;
    private FilterListener filterListener;
    private Dialogs operations;
    private PList listModule;

    public PersonFilterImpl() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Filter List"));
        setBackground(UIManager.getColor("Filter.background"));
        setForeground(UIManager.getColor("Filter.foreground"));
        int fieldHeight = 22;
        int fieldWidth = 100;
        Dimension fieldDim = new Dimension(fieldWidth, fieldHeight);
        Dimension labelDim = new Dimension(fieldWidth, fieldHeight);

        add(Box.createVerticalStrut(4));
        JLabel filterTypeLabel = new JLabel("Field");
        filterTypeLabel.setAlignmentX(CENTER_ALIGNMENT);
        filterTypeLabel.setMaximumSize(labelDim);
        filterTypeLabel.setForeground(UIManager.getColor("Filter.foreground"));
        add(filterTypeLabel);
        filterTypeBox = new JComboBox<>(new String[] {
            "All Fields", "First Name", "Last Name", "DOB", "Government ID", "Student ID"
        });
        filterTypeBox.setToolTipText("Select field to filter by");
        filterTypeBox.setPreferredSize(fieldDim);
        filterTypeBox.setMaximumSize(fieldDim);
        filterTypeBox.setAlignmentX(CENTER_ALIGNMENT);
        filterTypeBox.setBackground(UIManager.getColor("Filter.textBackground"));
        filterTypeBox.setForeground(UIManager.getColor("Filter.textForeground"));
        add(filterTypeBox);
        add(Box.createVerticalStrut(4));

        JLabel searchLabel = new JLabel("Term");
        searchLabel.setAlignmentX(CENTER_ALIGNMENT);
        searchLabel.setMaximumSize(labelDim);
        searchLabel.setForeground(UIManager.getColor("Filter.foreground"));
        add(searchLabel);
        searchField = new JTextField();
        searchField.setToolTipText("Enter filter term(s)");
        searchField.setPreferredSize(new Dimension(fieldWidth, 60));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        searchField.setMinimumSize(new Dimension(fieldWidth, 40));
        searchField.setAlignmentX(CENTER_ALIGNMENT);
        searchField.setBackground(UIManager.getColor("Filter.textBackground"));
        searchField.setForeground(UIManager.getColor("Filter.textForeground"));
        add(searchField);
        add(Box.createVerticalStrut(8));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(UIManager.getColor("Filter.background"));
        buttonPanel.setForeground(UIManager.getColor("Filter.foreground"));
        clearButton = new JButton("Clear");
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        clearButton.setMaximumSize(new Dimension(160, fieldHeight));
        clearButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
        clearButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        exportButton = new JButton("Export Filtered");
        exportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportButton.setMaximumSize(new Dimension(160, fieldHeight));
        exportButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
        exportButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        buttonPanel.add(clearButton);
        buttonPanel.add(Box.createVerticalStrut(8));
        buttonPanel.add(exportButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(Box.createVerticalGlue());
        add(buttonPanel);

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

    @Override
    public void setPeople(People people) {
        // Removed unused field
    }

    @Override
    public void setCustomFilter(Predicate<Person> filter) {
        this.customFilter = filter;
        notifyFilterChanged();
    }

    @Override
    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    @Override
    public void setOperations(Dialogs operations) {
        this.operations = operations;
    }

    @Override
    public void setListModule(PList listModule) {
        this.listModule = listModule;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }
    @Override
    public JTextField getSearchField() {
        return searchField;
    }
    @Override
    public JComboBox<String> getFilterTypeBox() {
        return filterTypeBox;
    }
    @Override
    public JButton getClearButton() {
        return clearButton;
    }
    @Override
    public JButton getExportButton() {
        return exportButton;
    }

    @Override
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
            try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(file))) {
                oos.writeObject(people);
            }
            return people.size();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyThemeRecursively(this);
    }
    private void applyThemeRecursively(Component comp) {
        if (comp instanceof JTextField) {
            comp.setBackground(UIManager.getColor("Filter.textBackground"));
            comp.setForeground(UIManager.getColor("Filter.textForeground"));
        } else if (comp instanceof JComboBox) {
            comp.setBackground(UIManager.getColor("Filter.textBackground"));
            comp.setForeground(UIManager.getColor("Filter.textForeground"));
        } else if (comp instanceof JButton) {
            comp.setBackground(UIManager.getColor("Filter.buttonBackground"));
            comp.setForeground(UIManager.getColor("Filter.buttonForeground"));
        } else if (comp instanceof JLabel) {
            comp.setForeground(UIManager.getColor("Filter.foreground"));
        } else if (comp instanceof JPanel) {
            comp.setBackground(UIManager.getColor("Filter.background"));
            comp.setForeground(UIManager.getColor("Filter.foreground"));
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }
}
