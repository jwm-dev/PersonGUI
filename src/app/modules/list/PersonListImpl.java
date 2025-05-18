package src.app.modules.list;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.JTableHeader;

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
public class PersonListImpl extends JPanel implements PList, AppController.DataChangeListener, AppController.DateFormatChangeListener {
    private JTable personTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private AppController dataManager;
    private PViewer personManager;
    private boolean ignoreSelectionEvents = false;
    private JLabel statusLabel;
    private JLabel titleLabel;
    private java.util.List<People.PersonMeta> filteredPeople = null;
    private Predicate<Person> currentFilter = null;
    private JScrollPane scrollPane; // Store the scroll pane for robust retheming

    /**
     * Creates a new List module
     * @param manager The data manager to use
     */
    public PersonListImpl(AppController manager) {
        this.dataManager = manager;
        manager.addDataChangeListener(this);
        manager.addDateFormatChangeListener(this);
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
        String[] columnNames = {"Type", "First", "Last", "DOB", "Age", "GID", "SID"};
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
        // Remove FlatTableHeaderUI and use a custom header renderer for theming
        JTableHeader header = personTable.getTableHeader();
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Color fg = UIManager.getColor("LIST_HEADER_FG");
                Color bg = UIManager.getColor("LIST_HEADER_BG");
                if (fg == null) fg = UIManager.getColor("TableHeader.foreground");
                if (bg == null) bg = UIManager.getColor("TableHeader.background");
                if (fg == null) fg = Color.DARK_GRAY;
                if (bg == null) bg = Color.WHITE;
                label.setForeground(fg);
                label.setBackground(bg);
                label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
                label.setBorder(BorderFactory.createEmptyBorder());
                label.setHorizontalAlignment(CENTER);
                return label;
            }
        });
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
        // --- Modern, flat, auto-hiding scrollbars (React/Vue inspired) ---
        scrollPane = new JScrollPane(personTable);
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        JScrollBar hBar = scrollPane.getHorizontalScrollBar();
        Color accent = UIManager.getColor("ACCENT") != null ? UIManager.getColor("ACCENT") : new Color(0x4f8cff);
        Color faint = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48);
        int arc = 6;
        vBar.setUI(makeFlatUI(accent, faint, arc));
        hBar.setUI(makeFlatUI(accent, faint, arc));
        vBar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
        hBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
        vBar.setOpaque(false);
        hBar.setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(statusLabel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
        personTable.getSelectionModel().addListSelectionListener(e -> {
            if (!ignoreSelectionEvents && !e.getValueIsAdjusting()) {
                int selectedRow = personTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int modelRow = personTable.convertRowIndexToModel(selectedRow);
                    java.util.List<People.PersonMeta> displayList;
                    if (currentFilter != null && filteredPeople != null) {
                        displayList = filteredPeople;
                    } else {
                        People people = dataManager.getPeople();
                        displayList = (people != null) ? people.getAllMeta() : null;
                    }
                    if (displayList != null && modelRow < displayList.size()) {
                        Person selectedPerson = displayList.get(modelRow).getPerson();
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
            java.util.List<People.PersonMeta> displayList = (people != null) ? people.getAllMeta() : null;
            if (displayList == null || displayList.isEmpty()) {
                tableModel.addRow(new Object[]{"No data", "", "", "", "", "", ""});
                personTable.setEnabled(false);
            } else {
                personTable.setEnabled(true);
                for (People.PersonMeta meta : displayList) {
                    Person person = meta.getPerson();
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
                    // Format DOB using AppController's current date format
                    String dobStr = dataManager.formatDate(person.getDOB());
                    tableModel.addRow(new Object[]{
                        type,
                        person.getFirstName(),
                        person.getLastName(),
                        dobStr,
                        person.getAge(),
                        govID,
                        studentID
                    });
                }
            }
            personTable.revalidate();
            personTable.repaint();
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
        java.util.List<People.PersonMeta> displayList = (people != null) ? people.getAllMeta() : null;
        if (displayList != null) {
            for (People.PersonMeta meta : displayList) {
                Person person = meta.getPerson();
                if (filter == null || filter.test(person)) {
                    filteredPeople.add(meta);
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
                    // Format DOB using AppController's current date format
                    String dobStr = dataManager.formatDate(person.getDOB());
                    tableModel.addRow(new Object[]{
                        type,
                        person.getFirstName(),
                        person.getLastName(),
                        dobStr,
                        person.getAge(),
                        govID,
                        studentID
                    });
                }
            }
        }
        personTable.setEnabled(filteredPeople != null && !filteredPeople.isEmpty());
        personTable.revalidate();
        personTable.repaint();
    }

    @Override
    public People getFilteredPeople() {
        People result = new People();
        if (filteredPeople != null) {
            for (People.PersonMeta meta : filteredPeople) {
                result.add(meta.getPerson());
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
            java.util.List<People.PersonMeta> displayList;
            if (currentFilter != null && filteredPeople != null) {
                displayList = filteredPeople;
            } else {
                People people = dataManager.getPeople();
                displayList = (people != null) ? people.getAllMeta() : null;
            }
            if (displayList == null) return;
            int modelIndex = -1;
            for (int i = 0; i < displayList.size(); i++) {
                if (displayList.get(i).getPerson().equals(person)) {
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

    // Called when the date format changes
    @Override
    public void onDateFormatChanged() {
        if (currentFilter != null) {
            applyFilter(currentFilter);
        } else {
            refreshList();
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (personTable != null) {
            // Remove reference to FlatTableHeaderUI, not needed anymore
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
            JScrollBar vBar = scrollPane.getVerticalScrollBar();
            JScrollBar hBar = scrollPane.getHorizontalScrollBar();
            Color accent = UIManager.getColor("ACCENT") != null ? UIManager.getColor("ACCENT") : new Color(0x4f8cff);
            Color faint = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48);
            int arc = 6;
            SwingUtilities.invokeLater(() -> {
                javax.swing.plaf.basic.BasicScrollBarUI vUI = makeFlatUI(accent, faint, arc);
                javax.swing.plaf.basic.BasicScrollBarUI hUI = makeFlatUI(accent, faint, arc);
                vBar.setUI(vUI);
                hBar.setUI(hUI);
                vBar.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
                hBar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
                vBar.setOpaque(false);
                hBar.setOpaque(false);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                vBar.repaint();
                hBar.repaint();
                // Immediately trigger fade-out after retheming so animation is visible
                try {
                    java.lang.reflect.Method m = vUI.getClass().getDeclaredMethod("onInactive");
                    m.setAccessible(true);
                    m.invoke(vUI);
                } catch (Exception ignored) {}
                try {
                    java.lang.reflect.Method m = hUI.getClass().getDeclaredMethod("onInactive");
                    m.setAccessible(true);
                    m.invoke(hUI);
                } catch (Exception ignored) {}
            });
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

    private javax.swing.plaf.basic.BasicScrollBarUI makeFlatUI(final Color accent, final Color faint, final int arc) {
        return new javax.swing.plaf.basic.BasicScrollBarUI() {
            private javax.swing.Timer fadeTimer;
            private javax.swing.Timer delayTimer;
            private float fade = 0.0f; // Start faded out
            private final int fadeSteps = 10;
            private final int fadeInterval = 25; // ms per step
            private final int delayBeforeFade = 700; // ms before starting fade out after scroll/mouse
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = faint;
                this.trackColor = new Color(0,0,0,0);
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                // No track
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (!c.isEnabled() || thumbBounds.width > thumbBounds.height && thumbBounds.height < 8) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                Color color = blendColors(faint, accent, fade);
                g2.setColor(color);
                g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, arc, arc);
                g2.dispose();
            }
            private Color blendColors(Color c1, Color c2, float t) {
                float u = 1 - t;
                int r = (int)(c1.getRed() * u + c2.getRed() * t);
                int g = (int)(c1.getGreen() * u + c2.getGreen() * t);
                int b = (int)(c1.getBlue() * u + c2.getBlue() * t);
                int a = (int)(c1.getAlpha() * u + c2.getAlpha() * t);
                return new Color(r, g, b, a);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0,0));
                btn.setMinimumSize(new Dimension(0,0));
                btn.setMaximumSize(new Dimension(0,0));
                btn.setVisible(false);
                return btn;
            }
            @Override
            protected void installListeners() {
                super.installListeners();
                scrollbar.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { onActive(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { restartFadeOutDebounce(); }
                });
                scrollbar.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                    @Override public void mouseMoved(java.awt.event.MouseEvent e) { onActive(); }
                    @Override public void mouseDragged(java.awt.event.MouseEvent e) { onActive(); }
                });
                scrollbar.addAdjustmentListener(_ -> {
                    onActive();
                    restartFadeOutDebounce();
                });
                scrollbar.addHierarchyListener(e -> {
                    if (scrollbar != null && (e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && scrollbar.isShowing()) {
                        fade = 0.0f;
                        scrollbar.repaint();
                    }
                });
            }
            // Debounce fade-out so it only starts after scrolling has stopped for delayBeforeFade ms
            private void restartFadeOutDebounce() {
                if (delayTimer != null && delayTimer.isRunning()) delayTimer.stop();
                delayTimer = new javax.swing.Timer(delayBeforeFade, _ -> fadeTo(0.0f));
                delayTimer.setRepeats(false);
                delayTimer.start();
            }
            private void onActive() {
                cancelFadeTimers();
                fadeTo(1.0f);
            }
            private void cancelFadeTimers() {
                if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
                if (delayTimer != null && delayTimer.isRunning()) delayTimer.stop();
            }
            private void fadeTo(float target) {
                if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
                final float start = fade;
                final float end = target;
                final int steps = fadeSteps;
                final float delta = (end - start) / steps;
                fadeTimer = new javax.swing.Timer(fadeInterval, null);
                fadeTimer.addActionListener(_ -> {
                    fade += delta;
                    if ((delta > 0 && fade >= end) || (delta < 0 && fade <= end)) {
                        fade = end;
                        fadeTimer.stop();
                    }
                    if (scrollbar != null) scrollbar.repaint();
                });
                fadeTimer.start();
            }
        };
    }

    @Override
    public int getListSize() {
        People people = dataManager.getPeople();
        return (people != null) ? people.size() : 0;
    }
}
