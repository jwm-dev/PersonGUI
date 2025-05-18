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
import src.app.gui.FlatButton;
import src.app.AppController;

public class PersonFilterImpl extends JPanel implements PFilter {
    // --- Field and Term controls: must be initialized before use ---
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> filterTypeBox = new JComboBox<>(new String[]{
        "First Name", "Last Name", "DOB", "Government ID", "Student ID", "All Fields"
    });
    private final FlatButton clearButton = new FlatButton("Clear");
    private FlatButton exportButton = new FlatButton("Export Filtered");
    private Predicate<Person> customFilter;
    private FilterListener filterListener;
    private Dialogs operations;
    private PList listModule;

    // --- Saved Filters ---
    private DefaultListModel<String> savedFiltersModel = new DefaultListModel<>();
    private JList<String> savedFiltersList = new JList<>(savedFiltersModel);
    private FlatButton saveFilterButton;
    private FlatButton deleteFilterButton;
    private static final String FILTERS_FILE = "data/.assets/filters.ser";
    private static final int MAX_SAVED_FILTERS = 16;

    // Use a reference to AppController directly for date formatting
    private AppController appController;

    public PersonFilterImpl() {
        // Change main layout to BorderLayout
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Filter List"));
        setBackground(UIManager.getColor("Filter.background"));
        setForeground(UIManager.getColor("Filter.foreground"));
        int fieldHeight = 22;
        int fieldWidth = 100;

        // --- Top-aligned Field/Term controls and Clear/Export buttons ---
        JPanel topFieldsPanel = new JPanel();
        topFieldsPanel.setLayout(new BoxLayout(topFieldsPanel, BoxLayout.Y_AXIS));
        topFieldsPanel.setOpaque(false);
        topFieldsPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 0));
        topFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel filterTypeLabel = new JLabel("Field");
        filterTypeLabel.setForeground(UIManager.getColor("Filter.foreground"));
        filterTypeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topFieldsPanel.add(filterTypeLabel);
        filterTypeBox.setBackground(UIManager.getColor("Filter.textBackground"));
        filterTypeBox.setForeground(UIManager.getColor("Filter.textForeground"));
        filterTypeBox.setPreferredSize(new Dimension(120, fieldHeight));
        filterTypeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, fieldHeight));
        filterTypeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        topFieldsPanel.add(filterTypeBox);
        JLabel searchLabel = new JLabel("Term");
        searchLabel.setForeground(UIManager.getColor("Filter.foreground"));
        searchLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topFieldsPanel.add(Box.createVerticalStrut(6));
        topFieldsPanel.add(searchLabel);
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN, 16f));
        searchField.setMargin(new Insets(6, 8, 6, 8));
        searchField.setPreferredSize(new Dimension(240, 36));
        searchField.setMinimumSize(new Dimension(120, 36));
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        searchField.setBackground(UIManager.getColor("Filter.textBackground"));
        searchField.setForeground(UIManager.getColor("Filter.textForeground"));
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);
        topFieldsPanel.add(searchField);
        topFieldsPanel.add(Box.createVerticalStrut(8));
        // --- Clear/Export buttons panel (directly below Term field) ---
        JPanel belowTermButtonPanel = new JPanel();
        belowTermButtonPanel.setLayout(new BoxLayout(belowTermButtonPanel, BoxLayout.Y_AXIS));
        belowTermButtonPanel.setOpaque(false);
        clearButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        belowTermButtonPanel.add(clearButton);
        belowTermButtonPanel.add(Box.createVerticalStrut(8));
        belowTermButtonPanel.add(exportButton);
        belowTermButtonPanel.add(Box.createVerticalStrut(12));
        belowTermButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topFieldsPanel.add(belowTermButtonPanel);
        topFieldsPanel.add(Box.createVerticalStrut(8));
        // --- Save/Delete filter buttons panel (above filter list) ---
        JPanel aboveListButtonPanel = new JPanel();
        aboveListButtonPanel.setLayout(new BoxLayout(aboveListButtonPanel, BoxLayout.Y_AXIS));
        aboveListButtonPanel.setOpaque(false);
        saveFilterButton = new FlatButton("Save Filter");
        deleteFilterButton = new FlatButton("Delete Filter");
        saveFilterButton.setMaximumSize(new Dimension(120, fieldHeight));
        deleteFilterButton.setMaximumSize(new Dimension(120, fieldHeight));
        saveFilterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteFilterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveFilterButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
        saveFilterButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        deleteFilterButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
        deleteFilterButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        aboveListButtonPanel.add(saveFilterButton);
        aboveListButtonPanel.add(Box.createVerticalStrut(4));
        aboveListButtonPanel.add(deleteFilterButton);
        // --- Delete All button (below filter list) ---
        FlatButton deleteAllButton = new FlatButton("Delete All");
        deleteAllButton.setMaximumSize(new Dimension(120, fieldHeight));
        deleteAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteAllButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
        deleteAllButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        // --- Saved Filters UI ---
        JPanel savedFiltersPanel = new JPanel();
        savedFiltersPanel.setLayout(new BorderLayout(6, 0));
        savedFiltersPanel.setOpaque(false);
        savedFiltersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savedFiltersList.setPrototypeCellValue("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
        int cellHeight = 22;
        savedFiltersList.setFixedCellHeight(cellHeight);
        savedFiltersList.setVisibleRowCount(MAX_SAVED_FILTERS);
        int listHeight = cellHeight * MAX_SAVED_FILTERS + 2;
        JScrollPane savedFiltersScroll = new JScrollPane(savedFiltersList);
        savedFiltersScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        savedFiltersScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        savedFiltersScroll.setPreferredSize(new Dimension(fieldWidth, listHeight));
        savedFiltersScroll.setMaximumSize(new Dimension(fieldWidth, listHeight));
        savedFiltersScroll.setMinimumSize(new Dimension(fieldWidth, listHeight));
        savedFiltersScroll.setBackground(UIManager.getColor("Filter.textBackground"));
        savedFiltersScroll.setForeground(UIManager.getColor("Filter.textForeground"));
        savedFiltersPanel.add(savedFiltersScroll, BorderLayout.CENTER);
        // --- Wrapper to prevent stretching ---
        JPanel savedFiltersWrapper = new JPanel();
        savedFiltersWrapper.setLayout(new BoxLayout(savedFiltersWrapper, BoxLayout.Y_AXIS));
        savedFiltersWrapper.setOpaque(false);
        savedFiltersWrapper.add(aboveListButtonPanel);
        savedFiltersWrapper.add(savedFiltersPanel);
        savedFiltersWrapper.add(Box.createVerticalStrut(8));
        savedFiltersWrapper.add(deleteAllButton);
        savedFiltersWrapper.setPreferredSize(new Dimension(fieldWidth + 130, listHeight + 80));
        savedFiltersWrapper.setMaximumSize(new Dimension(fieldWidth + 130, listHeight + 80));
        savedFiltersWrapper.setMinimumSize(new Dimension(fieldWidth + 130, listHeight + 80));
        // --- Layout main panel ---
        setLayout(new BorderLayout());
        add(topFieldsPanel, BorderLayout.NORTH);
        // Center panel for vertical glue and bottom alignment
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.add(Box.createVerticalGlue());
        // Bottom panel for filter list and its buttons, aligned to bottom
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.add(savedFiltersWrapper);
        bottomPanel.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        centerPanel.add(bottomPanel);
        add(centerPanel, BorderLayout.CENTER);
        // --- Left-justify combo box and term field ---
        filterTypeBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchField.setAlignmentX(Component.LEFT_ALIGNMENT);

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
        loadSavedFilters();
        saveFilterButton.addActionListener(_ -> saveCurrentFilter());
        deleteFilterButton.addActionListener(_ -> deleteSelectedFilter());
        deleteAllButton.addActionListener(_ -> {
            if (savedFiltersModel.size() > 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Delete all saved filters?", "Confirm Delete All", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    savedFiltersModel.clear();
                    saveFiltersToDisk();
                }
            }
        });
        savedFiltersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && savedFiltersList.getSelectedValue() != null) {
                searchField.setText(savedFiltersList.getSelectedValue());
            }
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

    public void setAppController(AppController controller) {
        this.appController = controller;
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
                    baseFilter = p -> p.getDOB() != null &&
                        appController != null && appController.formatDate(p.getDOB()).toLowerCase().contains(term);
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
                        (p.getDOB() != null && appController != null && appController.formatDate(p.getDOB()).toLowerCase().contains(term));
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
        if (appController == null) throw new IllegalStateException("No AppController instance");
        java.util.function.Function<src.date.OCCCDate, String> dateFormatter = (date) -> {
            if (date == null) return "";
            return appController.formatDate(date);
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

    // --- Persistence for saved filters ---
    private void loadSavedFilters() {
        savedFiltersModel.clear();
        java.io.File file = new java.io.File(FILTERS_FILE);
        if (file.exists()) {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(file))) {
                Object obj = ois.readObject();
                if (obj instanceof java.util.List) {
                    int count = 0;
                    for (Object s : (java.util.List<?>) obj) {
                        if (s instanceof String str) {
                            if (count < MAX_SAVED_FILTERS) {
                                savedFiltersModel.addElement(str);
                                count++;
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore, show empty list
            }
        }
    }
    private void saveFiltersToDisk() {
        java.util.List<String> filters = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(savedFiltersModel.size(), MAX_SAVED_FILTERS); i++) filters.add(savedFiltersModel.get(i));
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(FILTERS_FILE))) {
            oos.writeObject(filters);
        } catch (Exception ex) {
            // Ignore
        }
    }
    private void saveCurrentFilter() {
        String filter = searchField.getText().trim();
        if (!filter.isEmpty() && !savedFiltersModel.contains(filter)) {
            if (savedFiltersModel.size() >= MAX_SAVED_FILTERS) {
                JOptionPane.showMessageDialog(this, "You can only save up to " + MAX_SAVED_FILTERS + " filters.", "Limit Reached", JOptionPane.WARNING_MESSAGE);
                return;
            }
            savedFiltersModel.addElement(filter);
            saveFiltersToDisk();
        }
    }
    private void deleteSelectedFilter() {
        int idx = savedFiltersList.getSelectedIndex();
        if (idx >= 0) {
            savedFiltersModel.remove(idx);
            saveFiltersToDisk();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        applyThemeRecursively(this);
        if (savedFiltersList != null) {
            savedFiltersList.setBackground(UIManager.getColor("Filter.textBackground"));
            savedFiltersList.setForeground(UIManager.getColor("Filter.textForeground"));
            savedFiltersList.setFixedCellHeight(22);
            savedFiltersList.setVisibleRowCount(MAX_SAVED_FILTERS);
            JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, savedFiltersList);
            if (scroll != null) {
                int listHeight = 22 * MAX_SAVED_FILTERS + 2;
                int fieldWidth = scroll.getPreferredSize().width;
                scroll.setPreferredSize(new Dimension(fieldWidth, listHeight));
                scroll.setMaximumSize(new Dimension(fieldWidth, listHeight));
                scroll.setMinimumSize(new Dimension(fieldWidth, listHeight));
                scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scroll.revalidate();
                // Also update wrapper panel if present
                Container wrapper = scroll.getParent();
                if (wrapper instanceof JPanel) {
                    wrapper.setPreferredSize(new Dimension(fieldWidth + 130, listHeight));
                    wrapper.setMaximumSize(new Dimension(fieldWidth + 130, listHeight));
                    wrapper.setMinimumSize(new Dimension(fieldWidth + 130, listHeight));
                    wrapper.revalidate();
                }
            }
        }
        if (saveFilterButton != null) {
            saveFilterButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
            saveFilterButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        }
        if (deleteFilterButton != null) {
            deleteFilterButton.setBackground(UIManager.getColor("Filter.buttonBackground"));
            deleteFilterButton.setForeground(UIManager.getColor("Filter.buttonForeground"));
        }
        // Theme Delete All button if present
        // (find it by traversing the parent filterButtonsPanel)
        if (saveFilterButton != null) {
            Container parent = saveFilterButton.getParent();
            if (parent != null) {
                for (Component c : parent.getComponents()) {
                    if (c instanceof FlatButton btn && "Delete All".equals(btn.getText())) {
                        btn.setBackground(UIManager.getColor("Filter.buttonBackground"));
                        btn.setForeground(UIManager.getColor("Filter.buttonForeground"));
                    }
                }
            }
        }
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
