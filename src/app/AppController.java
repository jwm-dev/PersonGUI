package src.app;

import java.io.*;
import java.util.Properties;
import javax.swing.*;
import java.awt.*;
import src.person.*;
import src.app.dialogs.Dialogs;
import src.app.gui.*;
import src.date.OCCCDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

public class AppController {
    // --- Constants: UI and Config Defaults ---
    public static final int DEFAULT_SIDEBAR_WIDTH = 190;
    public static final int DEFAULT_FILTER_WIDTH = 190;
    public static final double DEFAULT_LIST_TERMINAL_DIVIDER = 0.7;
    public static final int DEFAULT_WINDOW_WIDTH = 900;
    public static final int DEFAULT_WINDOW_HEIGHT = 500;
    public static final String DEFAULT_THEME = "light";
    public static final String CONFIG_PATH = "data/.config/config";

    // --- State: Data, Config, Theme ---
    private People people = new People();
    private File currentFile;
    private boolean modified = false, hasChanges = false;
    private final List<DataChangeListener> listeners = new ArrayList<>();
    private Properties themeProps = new Properties();

    // --- UI/Config State ---
    private int sidebarWidth = DEFAULT_SIDEBAR_WIDTH, filterWidth = DEFAULT_FILTER_WIDTH;
    private double listTerminalDivider = DEFAULT_LIST_TERMINAL_DIVIDER;
    private int windowWidth = DEFAULT_WINDOW_WIDTH, windowHeight = DEFAULT_WINDOW_HEIGHT;
    private String themeName = DEFAULT_THEME;

    // --- Modules ---
    private GuiAPI guiApi;
    private src.app.modules.viewer.PViewer personModule;
    private src.app.modules.list.PList listModule;
    private src.app.modules.filter.PFilter filterModule;
    private src.app.modules.terminal.PTerminal terminalModule;
    private src.app.dialogs.Dialogs dialogsModule;

    // --- Coordinator: Module Initialization ---
    public void initModules(JFrame frame) {
        dialogsModule = new Dialogs(this, frame, new File(System.getProperty("user.dir") + File.separator + "data"), ".ppl");
        personModule = new src.app.modules.viewer.PersonViewerImpl(frame, this);
        listModule = new src.app.modules.list.PersonListImpl(this);
        listModule.setPersonManager(personModule);
        personModule.setDataList(listModule);
        terminalModule = new src.app.modules.terminal.PersonTerminalImpl(frame, this, dialogsModule);
        filterModule = new src.app.modules.filter.PersonFilterImpl();
        filterModule.setFilterListener(listModule::applyFilter);
        filterModule.setOperations(dialogsModule);
        filterModule.setListModule(listModule);
    }

    // --- Config/Theme Management ---
    public void reloadConfigAndTheme() {
        loadConfig(new File(CONFIG_PATH));
        loadTheme(themeName);
    }
    public void loadConfig(File configFile) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) { props.load(fis); } catch (Exception ignored) {}
        sidebarWidth = Integer.parseInt(props.getProperty("SIDEBAR_WIDTH", String.valueOf(DEFAULT_SIDEBAR_WIDTH)));
        filterWidth = Integer.parseInt(props.getProperty("FILTER_WIDTH", String.valueOf(DEFAULT_FILTER_WIDTH)));
        listTerminalDivider = Double.parseDouble(props.getProperty("LIST_TERMINAL_DIVIDER", String.valueOf(DEFAULT_LIST_TERMINAL_DIVIDER)));
        windowWidth = Integer.parseInt(props.getProperty("WINDOW_WIDTH", String.valueOf(DEFAULT_WINDOW_WIDTH)));
        windowHeight = Integer.parseInt(props.getProperty("WINDOW_HEIGHT", String.valueOf(DEFAULT_WINDOW_HEIGHT)));
        themeName = props.getProperty("THEME", DEFAULT_THEME);
        loadTheme(themeName);
    }
    public void saveConfig(File configFile) {
        Properties props = new Properties();
        props.setProperty("SIDEBAR_WIDTH", String.valueOf(sidebarWidth));
        props.setProperty("FILTER_WIDTH", String.valueOf(filterWidth));
        props.setProperty("LIST_TERMINAL_DIVIDER", String.valueOf(listTerminalDivider));
        props.setProperty("WINDOW_WIDTH", String.valueOf(windowWidth));
        props.setProperty("WINDOW_HEIGHT", String.valueOf(windowHeight));
        props.setProperty("THEME", themeName);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "User config updated on " + new Date());
        } catch (Exception ignored) {}
    }
    public void loadTheme(String theme) {
        themeProps = Themes.loadThemeFile(theme);
        if (guiApi != null) guiApi.reloadConfigAndTheme();
    }
    public void setThemeName(String t) {
        themeName = t;
        loadTheme(themeName);
        if (guiApi != null) guiApi.reloadConfigAndTheme();
    }
    public Properties getThemeProperties() { return themeProps; }
    public Color getThemeColor(String key, Color fallback) { return Themes.getColor(themeProps, key, fallback); }
    public Font getThemeFont(String key, Font fallback) { return Themes.getFont(themeProps, key, fallback); }

    // --- UI Config Getters/Setters ---
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

    // --- Data Change Notification ---
    public interface DataChangeListener { void onDataChanged(); }
    public void addDataChangeListener(DataChangeListener l) { if (l != null && !listeners.contains(l)) listeners.add(l); }
    public void removeDataChangeListener(DataChangeListener l) { listeners.remove(l); }
    public void notifyDataChanged() { listeners.forEach(DataChangeListener::onDataChanged); }

    // --- Data Access/Modification ---
    public People getPeople() { return people; }
    public int size() { return people.size(); }
    public Person getPersonAt(int idx) { return (idx >= 0 && idx < people.size()) ? people.get(idx) : null; }
    public File getCurrentFile() { return currentFile; }
    public void setCurrentFile(File file) { currentFile = file; }
    public boolean isModified() { return modified; }
    public boolean hasChanges() { return hasChanges; }
    public void clearAll() {
        people.clear();
        currentFile = null;
        modified = hasChanges = false;
        notifyDataChanged();
    }

    // --- File Operations ---
    public int loadFromFile(File file) throws Exception {
        People loaded = Dialogs.loadPeopleFromFile(file);
        if (loaded == null) throw new ClassCastException("File does not contain a valid People object");
        people = loaded;
        currentFile = file;
        modified = hasChanges = false;
        notifyDataChanged();
        return people.size();
    }
    public int saveToFile(File file) throws IOException {
        if (file == null) throw new IllegalArgumentException("File cannot be null");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(people);
        }
        currentFile = file;
        modified = hasChanges = false;
        notifyDataChanged();
        return people.size();
    }
    public int loadPeople(File file) throws Exception { return loadFromFile(file); }
    public int savePeople() throws IOException { return saveToFile(currentFile); }
    public int savePeopleAs(File file) throws IOException { return saveToFile(file); }
    public People importPeople(File file) throws Exception { return Dialogs.loadPeopleFromFile(file); }
    public int exportPeople(People p, File file, String format, java.util.function.Function<OCCCDate, String> dateFormatter) throws IOException {
        return "json".equalsIgnoreCase(format) ? Dialogs.exportToJson(p, file, dateFormatter) : Dialogs.exportToText(p, file, dateFormatter);
    }

    // --- Person Add/Update/Delete ---
    public static class AddResult {
        public final boolean success; public final String errorMessage;
        public AddResult(boolean s, String e) { success = s; errorMessage = e; }
    }
    public AddResult addPersonFromFields(String first, String last, String dobStr, String govID, String studentID) {
        String err = validatePersonFields(first, last, dobStr, govID, studentID, -1);
        if (err != null) return new AddResult(false, err);
        Person p = buildPerson(first, last, dobStr, govID, studentID);
        if (people.add(p)) { modified = hasChanges = true; notifyDataChanged(); return new AddResult(true, null); }
        return new AddResult(false, "Failed to add person (unknown error).");
    }
    public AddResult updatePersonFromFields(int idx, String first, String last, String dobStr, String govID, String studentID) {
        String err = validatePersonFields(first, last, dobStr, govID, studentID, idx);
        if (err != null) return new AddResult(false, err);
        Person p = buildPerson(first, last, dobStr, govID, studentID);
        if (people.update(idx, p)) { modified = hasChanges = true; notifyDataChanged(); return new AddResult(true, null); }
        return new AddResult(false, "Failed to update person (unknown error).");
    }
    public boolean deletePersonByIndex(int idx) {
        boolean removed = people.remove(idx);
        if (removed) { modified = hasChanges = true; notifyDataChanged(); }
        return removed;
    }
    public boolean deletePerson(Person p) {
        boolean removed = people.remove(p);
        if (removed) { modified = hasChanges = true; notifyDataChanged(); }
        return removed;
    }

    // --- Validation/Construction Helpers ---
    private String validatePersonFields(String first, String last, String dobStr, String govID, String studentID, int idx) {
        if (first == null || first.trim().isEmpty()) return "First Name cannot be empty.";
        if (last == null || last.trim().isEmpty()) return "Last Name cannot be empty.";
        try { parseDate(dobStr); } catch (Exception ex) { return ex.getMessage(); }
        if (govID != null && !govID.isEmpty() && !isValidID(govID)) return "Government ID must contain only letters and numbers.";
        if (studentID != null && !studentID.isEmpty() && !isValidID(studentID)) return "Student ID must contain only letters and numbers.";
        if (studentID != null && !studentID.isEmpty() && (govID == null || govID.isEmpty())) return "Cannot use Student ID without Government ID.";
        if (govID != null && !govID.isEmpty() && people.isDuplicateGovID(normalizeID(govID), idx)) return "A person with this Government ID already exists.";
        if (studentID != null && !studentID.isEmpty() && people.isDuplicateStudentID(normalizeID(studentID), idx)) return "A person with this Student ID already exists.";
        return null;
    }
    private Person buildPerson(String first, String last, String dobStr, String govID, String studentID) {
        OCCCDate dob = parseDateUnchecked(dobStr);
        govID = (govID != null && !govID.isEmpty()) ? normalizeID(govID) : null;
        studentID = (studentID != null && !studentID.isEmpty()) ? normalizeID(studentID) : null;
        if (govID != null && studentID != null) return new OCCCPerson(new RegisteredPerson(first, last, dob, govID), studentID);
        if (govID != null) return new RegisteredPerson(first, last, dob, govID);
        return new Person(first, last, dob);
    }
    public static boolean isValidID(String id) { return id != null && id.matches("^[a-zA-Z0-9]*$"); }
    public static String normalizeID(String id) { return id == null ? null : id.toUpperCase(); }
    public static OCCCDate parseDate(String dateStr) throws Exception {
        if (dateStr == null || dateStr.trim().isEmpty()) throw new Exception("Date cannot be empty");
        String[] parts = dateStr.split("/");
        if (parts.length != 3) throw new Exception("Invalid date format. Please use MM/dd/yyyy");
        try {
            int month = Integer.parseInt(parts[0].trim()), day = Integer.parseInt(parts[1].trim()), year = Integer.parseInt(parts[2].trim());
            return new OCCCDate(day, month, year);
        } catch (NumberFormatException e) { throw new Exception("Invalid date format. Please use MM/dd/yyyy with numeric values"); }
    }
    private static OCCCDate parseDateUnchecked(String dateStr) {
        try { return parseDate(dateStr); } catch (Exception e) { return null; }
    }

    // --- Module Getters ---
    public src.app.modules.viewer.PViewer getPersonModule() { return personModule; }
    public src.app.modules.list.PList getListModule() { return listModule; }
    public src.app.modules.filter.PFilter getFilterModule() { return filterModule; }
    public src.app.modules.terminal.PTerminal getTerminalModule() { return terminalModule; }
    public src.app.dialogs.Dialogs getDialogsModule() { return dialogsModule; }
    public void setGuiApi(GuiAPI api) { this.guiApi = api; }

    /**
     * Applies the given config/theme properties and then runs the callback.
     */
    public void applyConfigAndTheme(Properties props, Runnable callback) {
        if (props != null) {
            // Optionally update config fields from props here
            themeProps = props;
            Themes.applyThemeAndRefreshAllWindows(themeProps);
        }
        if (callback != null) callback.run();
    }
}