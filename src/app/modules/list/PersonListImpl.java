package src.app.modules.list;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import src.app.AppController;
import src.app.modules.viewer.PViewer;
import src.person.OCCCPerson;
import src.person.People;
import src.person.Person;
import src.person.RegisteredPerson;

import java.awt.*;
import java.util.function.Predicate;

/**
 * Implementation of the PList interface that displays all Person objects in a sortable table
 */
public class PersonListImpl extends JPanel implements PList, AppController.DataChangeListener {
    private JTable personTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private AppController dataManager;
    private PViewer personManager;
    private boolean ignoreSelectionEvents = false;
    private JLabel statusLabel;
    private JLabel titleLabel;
    private java.util.List<Person> filteredPeople = null;
    private Predicate<Person> currentFilter = null;
    private JScrollPane scrollPane; // Store the scroll pane for robust retheming

    /**
     * Creates a new List module
     * @param manager The data manager to use
     */
    public PersonListImpl(AppController manager) {
        this.dataManager = manager;
        manager.addDataChangeListener(this);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // --- THEME: Set background/foreground from UIManager ---
        setBackground(UIManager.getColor("Module.background"));
        setForeground(UIManager.getColor("Module.foreground"));
        titleLabel = new JLabel("Person List");
        titleLabel.setFont(UIManager.getFont("Label.font"));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        titleLabel.setForeground(UIManager.getColor("Module.foreground"));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(true);
        headerPanel.setBackground(UIManager.getColor("Module.background"));
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        add(headerPanel, BorderLayout.NORTH);
        String[] columnNames = {"Type", "First Name", "Last Name", "Date of Birth", "Age", "GID", "SID"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;
                    case 1: return String.class;
                    case 2: return String.class;
                    case 3: return src.date.OCCCDate.class;
                    case 4: return Integer.class;
                    case 5: return String.class;
                    case 6: return String.class;
                    default: return Object.class;
                }
            }
        };
        personTable = new JTable(tableModel);
        // Remove the custom renderer from the constructor. It will be set in updateUI().
        personTable.setFont(UIManager.getFont("Table.font"));
        personTable.setBackground(UIManager.getColor("Table.background"));
        personTable.setForeground(UIManager.getColor("Table.foreground"));
        personTable.setSelectionBackground(UIManager.getColor("Table.selectionBackground"));
        personTable.setSelectionForeground(UIManager.getColor("Table.selectionForeground"));
        sorter = new TableRowSorter<>(tableModel);
        personTable.setRowSorter(sorter);
        statusLabel = new JLabel("No person selected");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        statusLabel.setForeground(UIManager.getColor("Module.foreground"));
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(true);
        contentPanel.setBackground(UIManager.getColor("Module.background"));
        scrollPane = new JScrollPane(personTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
        personTable.getSelectionModel().addListSelectionListener(e -> {
            if (!ignoreSelectionEvents && !e.getValueIsAdjusting()) {
                int selectedRow = personTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = personTable.convertRowIndexToModel(selectedRow);
                    People people = dataManager.getPeople();
                    if (people != null && modelRow < people.size()) {
                        Person selectedPerson = people.get(modelRow);
                        updateStatusLabel(selectedPerson);
                        if (personManager != null) personManager.displayPersonDetails(selectedPerson);
                    }
                } else {
                    statusLabel.setText("No person selected");
                }
            }
        });
        updateTitleLabel();
        // Ensure the custom renderer is set at least once
        updateUI();
    }

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

    private void updateTitleLabel() {
        java.io.File currentFile = dataManager.getCurrentFile();
        boolean isModified = dataManager.isModified();
        boolean hasChanges = dataManager.hasChanges();
        if (currentFile != null) {
            String fileName = currentFile.getName();
            if (isModified) fileName += " *";
            titleLabel.setText(fileName);
            if (isModified) {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.ITALIC | Font.BOLD));
            } else {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
            }
        } else {
            String defaultTitle = "Person List";
            if (isModified && hasChanges) defaultTitle = "Unsaved List*";
            titleLabel.setText(defaultTitle);
            if (isModified && hasChanges) {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.ITALIC | Font.BOLD));
            } else {
                titleLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD));
            }
        }
    }

    /**
     * Updates the displayed data in the list
     */
    public void refreshList() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            People people = dataManager.getPeople();
            if (people == null || people.isEmpty()) {
                tableModel.addRow(new Object[]{"No data", "", "", "", "", "", ""});
                personTable.setEnabled(false);
            } else {
                personTable.setEnabled(true);
                for (Person person : people) {
                    if (person == null) continue;
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
        });
    }

    /**
     * Applies the given filter to the list of people
     * @param filter The filter to apply
     */
    public void applyFilter(Predicate<Person> filter) {
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

    @Override
    public People getFilteredPeople() {
        People result = new People();
        if (filteredPeople != null) {
            for (Person p : filteredPeople) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    public void setPersonManager(PViewer personManager) {
        this.personManager = personManager;
    }

    @Override
    public void selectPerson(Person person) {
        if (person == null) return;
        try {
            People people = dataManager.getPeople();
            int modelIndex = -1;
            for (int i = 0; i < people.size(); i++) {
                if (people.get(i).equals(person)) {
                    modelIndex = i;
                    break;
                }
            }
            if (modelIndex >= 0) {
                int viewIndex = personTable.convertRowIndexToView(modelIndex);
                if (viewIndex >= 0 && viewIndex < personTable.getRowCount()) {
                    personTable.setRowSelectionInterval(viewIndex, viewIndex);
                    Rectangle rect = personTable.getCellRect(viewIndex, 0, true);
                    personTable.scrollRectToVisible(rect);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error selecting person: " + ex.getMessage());
        }
    }

    @Override
    public void clearSelection() {
        ignoreSelectionEvents = true;
        try {
            personTable.clearSelection();
            statusLabel.setText("No person selected");
        } finally {
            ignoreSelectionEvents = false;
        }
    }

    @Override
    public JTable getJList() {
        return personTable;
    }

    @Override
    public JLabel getHeader() {
        return titleLabel;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void onDataChanged() {
        if (currentFilter != null) {
            applyFilter(currentFilter);
        } else {
            refreshList();
        }
        updateTitleLabel();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (personTable != null) {
            // Remove all custom renderers for all column classes
            for (int i = 0; i < personTable.getColumnCount(); i++) {
                Class<?> columnClass = personTable.getColumnClass(i);
                personTable.setDefaultRenderer(columnClass, null);
            }
            personTable.setDefaultRenderer(Object.class, null);

            // Set a new renderer for all column classes
            javax.swing.table.DefaultTableCellRenderer renderer = new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (isSelected) {
                        Color selBg = UIManager.getColor("Table.selectionBackground");
                        Color selFg = UIManager.getColor("Table.selectionForeground");
                        if (selBg == null) selBg = table.getSelectionBackground();
                        if (selFg == null) selFg = table.getSelectionForeground();
                        c.setBackground(selBg);
                        c.setForeground(selFg);
                    } else {
                        Color even = UIManager.getColor("Table.background");
                        Color odd = UIManager.getColor("Table.alternateRowColor");
                        if (even == null) even = Color.WHITE;
                        if (odd == null) odd = new Color(245,245,245);
                        c.setBackground((row % 2 == 0) ? even : odd);
                        c.setForeground(UIManager.getColor("Table.foreground"));
                    }
                    return c;
                }
            };
            for (int i = 0; i < personTable.getColumnCount(); i++) {
                Class<?> columnClass = personTable.getColumnClass(i);
                personTable.setDefaultRenderer(columnClass, renderer);
            }
            personTable.setDefaultRenderer(Object.class, renderer);

            personTable.revalidate();
            personTable.repaint();
        }
        if (scrollPane != null) {
            scrollPane.updateUI();
            scrollPane.revalidate();
            scrollPane.repaint();
        }
        applyThemeRecursively(this);
    }

    private void applyThemeRecursively(Component comp) {
        if (comp instanceof JLabel) {
            comp.setForeground(UIManager.getColor("Module.foreground"));
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(UIManager.getColor("Module.background"));
            comp.setForeground(UIManager.getColor("Module.foreground"));
        } else if (comp instanceof JPanel) {
            comp.setBackground(UIManager.getColor("Module.background"));
            comp.setForeground(UIManager.getColor("Module.foreground"));
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                applyThemeRecursively(child);
            }
        }
    }
}
