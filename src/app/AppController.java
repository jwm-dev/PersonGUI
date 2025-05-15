package src.app;

import java.io.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;

import src.person.Person;
import src.person.People;
import src.person.OCCCPerson;
import src.person.RegisteredPerson;
import src.app.gui.Dialogs;
import src.date.OCCCDate;
import src.app.gui.Themes;
import src.app.gui.GuiAPI;

/**
 * Manages all data operations for the Person Management application.
 * This class handles create, read, update, delete operations for Person objects
 * as well as file operations (save and load).
 */
public class AppController {
    private People people = new People();
    private File currentFile;
    private boolean modified = false; // Track whether data has been modified since last save
    private boolean hasChanges = false; // Track whether data has changed at all since new/open
    private List<DataChangeListener> listeners = new ArrayList<>();
    
    // UI dimension/config settings
    private int sidebarWidth = 190;
    private int filterWidth = 190;
    private double listTerminalDivider = 0.7;
    private int windowWidth = 900;
    private int windowHeight = 500;
    private String themeName = "light";

    // Move config/theme logic here from Frame
    private Properties themeProps = new Properties();

    private GuiAPI guiApi;
    public void setGuiApi(GuiAPI guiApi) { this.guiApi = guiApi; }

    public Properties getThemeProperties() {
        return themeProps;
    }

    public void loadTheme(String theme) {
        themeProps = Themes.loadThemeFile(theme);
        applyThemeEverywhere();
    }

    public Color getThemeColor(String key, Color fallback) {
        return Themes.getColor(themeProps, key, fallback);
    }

    public Font getThemeFont(String key, Font fallback) {
        return Themes.getFont(themeProps, key, fallback);
    }

    public void reloadConfigAndTheme() {
        loadConfig(new File("data/.config/config"));
        loadTheme(getThemeName());
        applyThemeEverywhere();
    }

    // Getters and setters for UI config
    public int getSidebarWidth() { return sidebarWidth; }
    public void setSidebarWidth(int w) { sidebarWidth = w; }
    public int getFilterWidth() { return filterWidth; }
    public void setFilterWidth(int w) { filterWidth = w; }
    public double getListTerminalDivider() { return listTerminalDivider; }
    public void setListTerminalDivider(double d) { listTerminalDivider = d; }
    public int getWindowWidth() { return windowWidth; }
    public void setWindowWidth(int w) { windowWidth = w; }
    public int getWindowHeight() { return windowHeight; }
    public void setWindowHeight(int h) { windowHeight = h; }
    public String getThemeName() { return themeName; }
    public void setThemeName(String t) {
        themeName = t;
        themeProps = Themes.loadThemeFile(themeName);
        applyThemeEverywhere();
        if (guiApi != null) {
            guiApi.reloadConfigAndTheme();
        }
    }

    // Load config from file
    public void loadConfig(File configFile) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        } catch (Exception e) { return; }
        sidebarWidth = Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(sidebarWidth)));
        filterWidth = Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(filterWidth)));
        listTerminalDivider = Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(listTerminalDivider)));
        windowWidth = Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(windowWidth)));
        windowHeight = Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(windowHeight)));
        themeName = props.getProperty("THEME", themeName);
        // --- FIX: Always load theme after config ---
        loadTheme(themeName);
    }
    // Save config to file
    public void saveConfig(File configFile) {
        Properties props = new Properties();
        props.setProperty("SIDEBAR_WIDTH", String.valueOf(sidebarWidth));
        props.setProperty("FILTER_WIDTH", String.valueOf(filterWidth));
        props.setProperty("LIST_TERMINAL_DIVIDER", String.valueOf(listTerminalDivider));
        props.setProperty("WINDOW_WIDTH", String.valueOf(windowWidth));
        props.setProperty("WINDOW_HEIGHT", String.valueOf(windowHeight));
        props.setProperty("THEME", themeName);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "User config updated on " + new java.util.Date());
        } catch (Exception ignored) {}
    }

    /**
     * Interface for data change listeners
     */
    public interface DataChangeListener {
        void onDataChanged();
    }
    
    /**
     * Add a listener to be notified of data changes
     * @param listener The listener to add
     */
    public void addDataChangeListener(DataChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a data change listener
     * @param listener The listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify all listeners that data has changed
     */
    public void notifyDataChanged() {
        for (DataChangeListener listener : listeners) {
            listener.onDataChanged();
        }
    }
  
    /**
     * Gets people stored in the manager
     * @return People collection
     */
    public People getPeople() {
        return people;
    }
    
    /**
     * Clears all data and resets the manager
     */
    public void clear() {
        people.clear();
        currentFile = null;
        modified = false; // Not modified since there's nothing to modify
        hasChanges = false; // No changes since clear
        notifyDataChanged();
    }
    
    /**
     * Gets the current file being operated on
     * @return The current file
     */
    public File getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Sets the current file
     * @param file The file to set as current
     */
    public void setCurrentFile(File file) {
        this.currentFile = file;
    }
    
    public int loadFromFile(File file) throws Exception {
        People loaded = Dialogs.loadPeopleFromFile(file);
        if (loaded != null) {
            people = loaded;
            currentFile = file;
            modified = false;
            hasChanges = false;
            notifyDataChanged();
            return people.size();
        } else {
            throw new ClassCastException("File does not contain a valid People object");
        }
    }

    public int saveToFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(people);
            currentFile = file;
            modified = false;
            hasChanges = false;
            notifyDataChanged();
            return people.size();
        }
    }

    /**
     * Gets the size of the people collection
     * @return Number of people in the collection
     */
    public int size() {
        return people.size();
    }
    
    /**
     * Gets a person at a specific index
     * @param index The index
     * @return The person at that index
     */
    public Person getPersonAt(int index) {
        if (index >= 0 && index < people.size()) {
            return people.get(index);
        }
        return null;
    }
    
    /**
     * Checks if data has been modified since last save
     * @return true if data has been modified, false otherwise
     */
    public boolean isModified() {
        return modified;
    }
    
    /**
     * Checks if there have been changes to the data since it was loaded or created new
     * @return true if there have been any changes
     */
    public boolean hasChanges() {
        return hasChanges;
    }

    /**
     * Parses a date string in MM/dd/yyyy format and returns an OCCCDate.
     * @throws Exception if the format is invalid or the date is not valid
     */
    public static src.date.OCCCDate parseDate(String dateStr) throws Exception {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new Exception("Date cannot be empty");
        }
        String[] parts = dateStr.split("/");
        if (parts.length != 3) {
            throw new Exception("Invalid date format. Please use MM/dd/yyyy");
        }
        try {
            int month = Integer.parseInt(parts[0].trim());
            int day = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            return new src.date.OCCCDate(day, month, year);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid date format. Please use MM/dd/yyyy with numeric values");
        } catch (src.date.InvalidOCCCDateException e) {
            throw e;
        }
    }

    /**
     * Checks if the ID contains only alphanumeric characters.
     */
    public static boolean isValidID(String id) {
        return id != null && id.matches("^[a-zA-Z0-9]*$");
    }

    /**
     * Converts all alphabetical characters in the ID to uppercase.
     */
    public static String normalizeID(String id) {
        return id == null ? null : id.toUpperCase();
    }

    /**
     * Clears all people and resets the current file and modification state.
     */
    public void clearAll() {
        people.clear();
        currentFile = null;
        modified = false;
        hasChanges = false;
        notifyDataChanged();
    }

    /**
     * Loads people from a file and sets as current file.
     * @param file The file to load from
     * @return number of people loaded
     * @throws Exception if loading fails
     */
    public int loadPeople(File file) throws Exception {
        int count = loadFromFile(file);
        notifyDataChanged();
        return count;
    }

    /**
     * Saves people to the current file.
     * @return number of people saved
     * @throws IOException if saving fails
     */
    public int savePeople() throws IOException {
        if (currentFile == null) throw new IOException("No file selected");
        int count = saveToFile(currentFile);
        notifyDataChanged();
        return count;
    }

    /**
     * Saves people to a specified file and sets it as current.
     * @param file The file to save to
     * @return number of people saved
     * @throws IOException if saving fails
     */
    public int savePeopleAs(File file) throws IOException {
        int count = saveToFile(file);
        notifyDataChanged();
        return count;
    }

    /**
     * Imports people from a file (does not replace current list).
     * @param file The file to import from
     * @return People object loaded
     * @throws Exception if loading fails
     */
    public People importPeople(File file) throws Exception {
        return Dialogs.loadPeopleFromFile(file);
    }

    /**
     * Exports people to a file in the specified format.
     * @param people The people to export
     * @param file The file to export to
     * @param format "txt" or "json"
     * @param dateFormatter Formatter for dates
     * @return number of people exported
     * @throws IOException if export fails
     */
    public int exportPeople(People people, File file, String format, java.util.function.Function<src.date.OCCCDate, String> dateFormatter) throws IOException {
        if ("json".equalsIgnoreCase(format)) {
            return Dialogs.exportToJson(people, file, dateFormatter);
        } else {
            return Dialogs.exportToText(people, file, dateFormatter);
        }
    }

    // Result object for add/update operations
    public static class AddResult {
        public final boolean success;
        public final String errorMessage;
        public AddResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * Adds a person from raw field values, handling all validation and business logic.
     */
    public AddResult addPersonFromFields(String firstName, String lastName, String dobStr, String govID, String studentID) {
        if (firstName == null || firstName.trim().isEmpty()) {
            return new AddResult(false, "First Name cannot be empty.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            return new AddResult(false, "Last Name cannot be empty.");
        }
        OCCCDate dob;
        try {
            dob = parseDate(dobStr);
        } catch (Exception ex) {
            return new AddResult(false, ex.getMessage());
        }
        if (govID != null && !govID.isEmpty() && !isValidID(govID)) {
            return new AddResult(false, "Government ID must contain only letters and numbers.");
        }
        if (studentID != null && !studentID.isEmpty() && !isValidID(studentID)) {
            return new AddResult(false, "Student ID must contain only letters and numbers.");
        }
        if (govID != null && !govID.isEmpty()) govID = normalizeID(govID);
        if (studentID != null && !studentID.isEmpty()) studentID = normalizeID(studentID);
        if (studentID != null && !studentID.isEmpty() && (govID == null || govID.isEmpty())) {
            return new AddResult(false, "Cannot add a Person with a Student ID but no Government ID.");
        }
        if (govID != null && !govID.isEmpty() && people.isDuplicateGovID(govID, -1)) {
            return new AddResult(false, "A person with this Government ID already exists.");
        }
        if (studentID != null && !studentID.isEmpty() && people.isDuplicateStudentID(studentID, -1)) {
            return new AddResult(false, "A person with this Student ID already exists.");
        }
        Person newPerson;
        if (govID != null && !govID.isEmpty() && studentID != null && !studentID.isEmpty()) {
            RegisteredPerson regPerson = new RegisteredPerson(firstName, lastName, dob, govID);
            newPerson = new OCCCPerson(regPerson, studentID);
        } else if (govID != null && !govID.isEmpty()) {
            newPerson = new RegisteredPerson(firstName, lastName, dob, govID);
        } else {
            newPerson = new Person(firstName, lastName, dob);
        }
        boolean added = people.add(newPerson);
        if (added) {
            modified = true;
            hasChanges = true;
            notifyDataChanged();
            return new AddResult(true, null);
        } else {
            return new AddResult(false, "Failed to add person (unknown error).");
        }
    }

    /**
     * Updates a person at a given index from raw field values, handling all validation and business logic.
     */
    public AddResult updatePersonFromFields(int index, String firstName, String lastName, String dobStr, String govID, String studentID) {
        if (index < 0 || index >= people.size()) {
            return new AddResult(false, "Invalid person index.");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            return new AddResult(false, "First Name cannot be empty.");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            return new AddResult(false, "Last Name cannot be empty.");
        }
        OCCCDate dob;
        try {
            dob = parseDate(dobStr);
        } catch (Exception ex) {
            return new AddResult(false, ex.getMessage());
        }
        if (govID != null && !govID.isEmpty() && !isValidID(govID)) {
            return new AddResult(false, "Government ID must contain only letters and numbers.");
        }
        if (studentID != null && !studentID.isEmpty() && !isValidID(studentID)) {
            return new AddResult(false, "Student ID must contain only letters and numbers.");
        }
        if (govID != null && !govID.isEmpty()) govID = normalizeID(govID);
        if (studentID != null && !studentID.isEmpty()) studentID = normalizeID(studentID);
        if (studentID != null && !studentID.isEmpty() && (govID == null || govID.isEmpty())) {
            return new AddResult(false, "Cannot update a Person with a Student ID but no Government ID.");
        }
        if (govID != null && !govID.isEmpty() && people.isDuplicateGovID(govID, index)) {
            return new AddResult(false, "A person with this Government ID already exists.");
        }
        if (studentID != null && !studentID.isEmpty() && people.isDuplicateStudentID(studentID, index)) {
            return new AddResult(false, "A person with this Student ID already exists.");
        }
        Person updatedPerson;
        if (govID != null && !govID.isEmpty() && studentID != null && !studentID.isEmpty()) {
            RegisteredPerson regPerson = new RegisteredPerson(firstName, lastName, dob, govID);
            updatedPerson = new OCCCPerson(regPerson, studentID);
        } else if (govID != null && !govID.isEmpty()) {
            updatedPerson = new RegisteredPerson(firstName, lastName, dob, govID);
        } else {
            updatedPerson = new Person(firstName, lastName, dob);
        }
        boolean updated = people.update(index, updatedPerson);
        if (updated) {
            modified = true;
            hasChanges = true;
            notifyDataChanged();
            return new AddResult(true, null);
        } else {
            return new AddResult(false, "Failed to update person (unknown error).");
        }
    }

    /**
     * Deletes a person at the specified index.
     * @param index The index of the person to delete
     * @return true if the person was deleted, false otherwise
     */
    public boolean deletePersonByIndex(int index) {
        if (index < 0 || index >= people.size()) {
            return false;
        }
        boolean removed = people.remove(index);
        if (removed) {
            modified = true;
            hasChanges = true;
            notifyDataChanged();
        }
        return removed;
    }

    /**
     * Deletes a person by reference.
     * @param person The person to delete
     * @return true if the person was deleted, false otherwise
     */
    public boolean deletePerson(Person person) {
        if (person == null) return false;
        boolean removed = people.remove(person);
        if (removed) {
            modified = true;
            hasChanges = true;
            notifyDataChanged();
        }
        return removed;
    }

    // --- MODULES ---
    private src.app.modules.viewer.PViewer personModule;
    private src.app.modules.list.PList listModule;
    private src.app.modules.filter.PFilter filterModule;
    private src.app.modules.terminal.PTerminal terminalModule;
    private src.app.gui.MainBar mainBarModule;
    private src.app.gui.Dialogs dialogsModule;

    // --- MODULE GETTERS ---
    public src.app.modules.viewer.PViewer getPersonModule() { return personModule; }
    public src.app.modules.list.PList getListModule() { return listModule; }
    public src.app.modules.filter.PFilter getFilterModule() { return filterModule; }
    public src.app.modules.terminal.PTerminal getTerminalModule() { return terminalModule; }
    public src.app.gui.MainBar getMainBarModule() { return mainBarModule; }
    public src.app.gui.Dialogs getDialogsModule() { return dialogsModule; }

    // --- MODULE INITIALIZATION ---
    public void initModules(JFrame frame) {
        dialogsModule = new src.app.gui.Dialogs(this, frame, new java.io.File(System.getProperty("user.dir") + java.io.File.separator + "data"), ".ppl");
        personModule = new src.app.modules.viewer.PersonViewer(frame, this, dialogsModule, null);
        mainBarModule = new src.app.gui.MainBar(frame, this);
        listModule = new src.app.modules.list.PList(this);
        listModule.setPersonManager(personModule);
        personModule.setDataList(listModule);
        terminalModule = new src.app.modules.terminal.PersonTerminal(frame, this, dialogsModule);
        filterModule = new src.app.modules.filter.PFilter();
        filterModule.setFilterListener(predicate -> listModule.applyFilter(predicate));
        filterModule.setOperations(dialogsModule);
        filterModule.setListModule(listModule);
    }

    // --- ROBUST CONFIG/THEME UPDATE ---
    public void applyConfigAndTheme(Properties props, Runnable... uiNotifiers) {
        // Update config fields from props
        sidebarWidth = Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(sidebarWidth)));
        filterWidth = Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(filterWidth)));
        listTerminalDivider = Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(listTerminalDivider)));
        windowWidth = Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(windowWidth)));
        windowHeight = Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(windowHeight)));
        themeName = props.getProperty("THEME", themeName);
        loadTheme(themeName);
        // Notify all modules and UI on the Swing thread
        SwingUtilities.invokeLater(() -> {
            for (Runnable r : uiNotifiers) r.run();
        });
    }

    /**
     * Applies the current theme to all owned modules (for robust theme propagation).
     */
    public void applyThemeToAllModules() {
        // Update UI for all modules if they exist
        javax.swing.SwingUtilities.invokeLater(() -> {
            if (personModule != null && personModule.getPanel() != null)
                javax.swing.SwingUtilities.updateComponentTreeUI(personModule.getPanel());
            if (listModule != null && listModule instanceof javax.swing.JComponent)
                javax.swing.SwingUtilities.updateComponentTreeUI((javax.swing.JComponent) listModule);
            if (filterModule != null && filterModule instanceof javax.swing.JComponent)
                javax.swing.SwingUtilities.updateComponentTreeUI((javax.swing.JComponent) filterModule);
            if (terminalModule != null && terminalModule.getPanel() != null)
                javax.swing.SwingUtilities.updateComponentTreeUI(terminalModule.getPanel());
        });
    }

    /**
     * Propagate the current theme to all windows and modules.
     */
    public void propagateTheme() {
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            javax.swing.SwingUtilities.updateComponentTreeUI(w);
        }
    }

    /**
     * Apply the current theme everywhere: all windows, all modules, all dialogs.
     * This is the only place that should update LookAndFeel and refresh UI trees.
     */
    public void applyThemeEverywhere() {
        try {
            javax.swing.UIManager.setLookAndFeel(new src.app.gui.CloudyLookAndFeel(themeProps));
        } catch (Exception ignored) {}
        // Update all open windows recursively
        for (java.awt.Window w : java.awt.Window.getWindows()) {
            recursivelyUpdateComponentTreeUI(w);
            w.revalidate();
            w.repaint();
        }
        // Update all modules if they exist
        if (personModule != null && personModule.getPanel() != null)
            recursivelyUpdateComponentTreeUI(personModule.getPanel());
        if (listModule != null && listModule instanceof javax.swing.JComponent)
            recursivelyUpdateComponentTreeUI((javax.swing.JComponent) listModule);
        if (filterModule != null && filterModule instanceof javax.swing.JComponent)
            recursivelyUpdateComponentTreeUI((javax.swing.JComponent) filterModule);
        if (terminalModule != null && terminalModule.getPanel() != null)
            recursivelyUpdateComponentTreeUI(terminalModule.getPanel());
    }

    /**
     * Recursively update the UI for a component and all its children.
     */
    private void recursivelyUpdateComponentTreeUI(java.awt.Component c) {
        javax.swing.SwingUtilities.updateComponentTreeUI(c);
        if (c instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) c).getComponents()) {
                recursivelyUpdateComponentTreeUI(child);
            }
        }
    }
}
