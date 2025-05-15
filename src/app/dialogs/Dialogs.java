package src.app.dialogs;

import src.app.AppController;
import src.person.People;
import src.person.Person;
import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Dialogs {
    private final AppController appController;
    private final JFrame parentFrame;
    private final File DATA_DIRECTORY;
    private final String FILE_EXTENSION;
    private final java.util.function.Function<src.date.OCCCDate, String> dateFormatter = date -> {
        if (date == null) return "";
        return String.format("%02d/%02d/%04d", date.getMonthNumber(), date.getDayOfMonth(), date.getYear());
    };

    public Dialogs(AppController appController, JFrame parentFrame, File dataDirectory, String fileExtension) {
        this.appController = appController;
        this.parentFrame = parentFrame;
        this.DATA_DIRECTORY = dataDirectory;
        this.FILE_EXTENSION = fileExtension;
    }

    public void doNew(Runnable clearFields, Runnable clearSelection) {
        appController.clearAll();
        if (clearFields != null) clearFields.run();
        if (clearSelection != null) clearSelection.run();
        appController.notifyDataChanged();
    }

    public void doOpen(Runnable clearFields, Runnable clearSelection) {
        JFileChooser fileChooser = setupFileChooser(DATA_DIRECTORY, FILE_EXTENSION, false);
        fileChooser.setPreferredSize(new java.awt.Dimension(700, 500));
        int result = fileChooser.showOpenDialog(parentFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                int count = appController.loadPeople(selectedFile);
                if (clearFields != null) clearFields.run();
                if (clearSelection != null) clearSelection.run();
                appController.notifyDataChanged();
                JOptionPane.showMessageDialog(parentFrame, count + " people loaded successfully", "Load Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, "Error loading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public void doSave() {
        try {
            int count = appController.savePeople();
            JOptionPane.showMessageDialog(parentFrame, count + " people saved successfully", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parentFrame, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public void doSaveAs() {
        JFileChooser fileChooser = setupFileChooser(DATA_DIRECTORY, FILE_EXTENSION, true);
        fileChooser.setPreferredSize(new java.awt.Dimension(700, 500));
        File currentFile = appController.getCurrentFile();
        if (currentFile != null) {
            fileChooser.setSelectedFile(currentFile);
        }
        int result = fileChooser.showSaveDialog(parentFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = ensureFileExtension(fileChooser.getSelectedFile(), FILE_EXTENSION);
            if (selectedFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "File already exists. Do you want to overwrite it?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                int count = appController.savePeopleAs(selectedFile);
                JOptionPane.showMessageDialog(parentFrame, count + " people saved successfully", "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public void doImport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setPreferredSize(new java.awt.Dimension(700, 500));
        int result = fileChooser.showOpenDialog(parentFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String name = selectedFile.getName().toLowerCase();
            People importedPeople = null;
            try {
                if (name.endsWith(".json")) {
                    importedPeople = importFromJson(selectedFile);
                } else if (name.endsWith(".txt")) {
                    importedPeople = importFromText(selectedFile);
                } else {
                    importedPeople = appController.importPeople(selectedFile);
                }
                if (importedPeople != null && !importedPeople.isEmpty()) {
                    int importedCount = 0;
                    List<ConflictResolution.ConflictInfo> conflicts = new ArrayList<>();
                    List<Person> handledPersons = new ArrayList<>();
                    // First, collect all conflicts and duplicates
                    for (Person person : importedPeople) {
                        if (person == null) continue;
                        if (ConflictResolution.isExactDuplicate(person, appController.getPeople())) {
                            handledPersons.add(person);
                            continue;
                        }
                        ConflictResolution.ConflictInfo conflict = ConflictResolution.checkForConflict(person, appController.getPeople());
                        if (conflict != null) {
                            conflicts.add(conflict);
                            handledPersons.add(person);
                        }
                    }
                    // Resolve conflicts
                    if (!conflicts.isEmpty()) {
                        boolean resolveAllRemaining = false;
                        ConflictResolution.ConflictChoice globalChoice = null;
                        for (int i = 0; i < conflicts.size(); i++) {
                            ConflictResolution.ConflictInfo conflict = conflicts.get(i);
                            ConflictResolution.ConflictChoice choice;
                            if (!resolveAllRemaining) {
                                choice = ConflictResolution.showConflictResolutionDialogWithApplyToAll(conflict, conflicts.size() - i, parentFrame);
                                if (choice == ConflictResolution.ConflictChoice.APPLY_TO_ALL) {
                                    globalChoice = ConflictResolution.showGlobalResolutionDialog(parentFrame);
                                    if (globalChoice == ConflictResolution.ConflictChoice.CANCEL) {
                                        i--; continue;
                                    }
                                    resolveAllRemaining = true;
                                    choice = globalChoice;
                                }
                            } else {
                                choice = globalChoice;
                            }
                            if (choice == ConflictResolution.ConflictChoice.CANCEL && !resolveAllRemaining) {
                                continue;
                            }
                            switch (choice) {
                                case USE_NEW:
                                    if (appController.getPeople().update(conflict.existingIndex, conflict.newPerson)) {
                                    }
                                    break;
                                case KEEP_EXISTING:
                                    break;
                                case SKIP:
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    // Now import only those not handled as duplicate or conflict
                    for (Person person : importedPeople) {
                        if (person == null) continue;
                        if (handledPersons.contains(person)) continue;
                        if (ConflictResolution.isExactDuplicate(person, appController.getPeople())) {
                            continue;
                        }
                        ConflictResolution.ConflictInfo conflict = ConflictResolution.checkForConflict(person, appController.getPeople());
                        if (conflict != null) {
                            continue;
                        }
                        if (appController.getPeople().add(person)) {
                            importedCount++;
                        }
                    }
                    appController.notifyDataChanged();
                    if (importedCount == 0) {
                        JOptionPane.showMessageDialog(parentFrame,
                            "No changes were made during import. All entries already exist in the system.",
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        StringBuilder message = new StringBuilder();
                        if (importedCount > 0) {
                            message.append(String.format("%d %s imported successfully", 
                                importedCount, 
                                importedCount == 1 ? "person was" : "people were"));
                        }
                        JOptionPane.showMessageDialog(parentFrame, message.toString(),
                            "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(parentFrame, 
                        "No people to import in the selected file.",
                        "Import Result", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentFrame, 
                    "Error importing file: " + ex.getMessage(),
                    "Import Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public void doExportAs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setPreferredSize(new java.awt.Dimension(700, 500));
        // Add file filters for export
        javax.swing.filechooser.FileFilter jsonFilter = new javax.swing.filechooser.FileNameExtensionFilter("JSON Files (*.json)", "json");
        javax.swing.filechooser.FileFilter txtFilter = new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt");
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(txtFilter);
        fileChooser.setFileFilter(jsonFilter);
        int result = fileChooser.showSaveDialog(parentFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            javax.swing.filechooser.FileFilter selectedFilter = fileChooser.getFileFilter();
            String exportFormat = selectedFilter.equals(jsonFilter) ? "json" : "txt";
            String filePath = selectedFile.getName();
            File exportDir;
            if (exportFormat.equals("json")) {
                exportDir = new File(DATA_DIRECTORY, "json");
            } else {
                exportDir = new File(DATA_DIRECTORY, "txt");
            }
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }
            File exportFile = new File(exportDir, filePath.endsWith("." + exportFormat) ? filePath : filePath + "." + exportFormat);
            if (exportFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(parentFrame, "File already exists. Do you want to overwrite it?", "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                People people = appController.getPeople();
                int count = appController.exportPeople(people, exportFile, exportFormat, dateFormatter);
                JOptionPane.showMessageDialog(parentFrame, count + " people exported successfully to " + exportFormat.toUpperCase() + " format.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parentFrame, "Error exporting file: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    public static JFileChooser setupFileChooser(File dataDirectory, String fileExtension, boolean forSave) {
        JFileChooser fileChooser = new JFileChooser();
        if (dataDirectory.exists() && dataDirectory.isDirectory()) {
            fileChooser.setCurrentDirectory(dataDirectory);
        } else {
            dataDirectory.mkdirs();
            fileChooser.setCurrentDirectory(dataDirectory);
        }
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(fileExtension);
            }
            @Override
            public String getDescription() {
                return "People Files (*" + fileExtension + ")";
            }
        });
        if (!forSave) {
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        return fileChooser;
    }

    public static File ensureFileExtension(File file, String fileExtension) {
        String path = file.getPath();
        if (!path.toLowerCase().endsWith(fileExtension)) {
            return new File(path + fileExtension);
        }
        return file;
    }

    public static src.person.People loadPeopleFromFile(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".txt")) {
            return loadPeopleFromTextFile(file);
        } else if (fileName.endsWith(".json")) {
            return loadPeopleFromJsonFile(file);
        } else {
            // Default .ser format
            try (FileInputStream fis = new FileInputStream(file);
                 ObjectInputStream ois = new ObjectInputStream(fis)) {
                Object obj = ois.readObject();
                if (obj instanceof src.person.People) {
                    return (src.person.People) obj;
                } else {
                    throw new ClassCastException("File does not contain a valid People object");
                }
            }
        }
    }

    public static src.person.People loadPeopleFromTextFile(File file) throws IOException {
        src.person.People people = new src.person.People();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            src.person.Person currentPerson = null;
            String firstName = null, lastName = null, dateStr = null, govID = null, studentID = null;
            String personType = null;
            
            // Skip header lines
            while ((line = reader.readLine()) != null && !line.startsWith("Person #")) {
                continue;
            }
            
            while (line != null) {
                if (line.startsWith("Person #")) {
                    // Save previous person if exists
                    if (firstName != null && lastName != null) {
                        try {
                            src.date.OCCCDate dob = null;
                            if (dateStr != null && !dateStr.isEmpty()) {
                                String[] parts = dateStr.split("/");
                                if (parts.length == 3) {
                                    int month = Integer.parseInt(parts[0].trim());
                                    int day = Integer.parseInt(parts[1].trim());
                                    int year = Integer.parseInt(parts[2].trim());
                                    dob = new src.date.OCCCDate(day, month, year);
                                }
                            }
                            
                            if ("OCCC Person".equals(personType)) {
                                src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                                currentPerson = new src.person.OCCCPerson(regPerson, studentID);
                            } else if ("Registered Person".equals(personType)) {
                                currentPerson = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                            } else {
                                currentPerson = new src.person.Person(firstName, lastName, dob);
                            }
                            
                            people.add(currentPerson);
                        } catch (Exception e) {
                            // Skip invalid person entries
                        }
                    }
                    
                    // Reset for next person
                    firstName = lastName = dateStr = govID = studentID = personType = null;
                    currentPerson = null;
                } else if (line.startsWith("First Name: ")) {
                    firstName = line.substring("First Name: ".length()).trim();
                } else if (line.startsWith("Last Name: ")) {
                    lastName = line.substring("Last Name: ".length()).trim();
                } else if (line.startsWith("DOB: ")) {
                    dateStr = line.substring("DOB: ".length()).trim();
                } else if (line.startsWith("Government ID: ")) {
                    govID = line.substring("Government ID: ".length()).trim();
                } else if (line.startsWith("Student ID: ")) {
                    studentID = line.substring("Student ID: ".length()).trim();
                } else if (line.startsWith("Type: ")) {
                    personType = line.substring("Type: ".length()).trim();
                }
                
                line = reader.readLine();
            }
            
            // Handle the last person
            if (firstName != null && lastName != null) {
                try {
                    src.date.OCCCDate dob = null;
                    if (dateStr != null && !dateStr.isEmpty()) {
                        String[] parts = dateStr.split("/");
                        if (parts.length == 3) {
                            int month = Integer.parseInt(parts[0].trim());
                            int day = Integer.parseInt(parts[1].trim());
                            int year = Integer.parseInt(parts[2].trim());
                            dob = new src.date.OCCCDate(day, month, year);
                        }
                    }
                    
                    if ("OCCC Person".equals(personType)) {
                        src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                        currentPerson = new src.person.OCCCPerson(regPerson, studentID);
                    } else if ("Registered Person".equals(personType)) {
                        currentPerson = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                    } else {
                        currentPerson = new src.person.Person(firstName, lastName, dob);
                    }
                    
                    people.add(currentPerson);
                } catch (Exception e) {
                    // Skip invalid person entries
                }
            }
        }
        
        return people;
    }

    public static src.person.People loadPeopleFromJsonFile(File file) throws IOException {
        src.person.People people = new src.person.People();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder json = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            String jsonStr = json.toString();
            
            // Simple JSON parsing without external libraries
            int peopleStart = jsonStr.indexOf("\"people\":");
            if (peopleStart >= 0) {
                int arrayStart = jsonStr.indexOf('[', peopleStart);
                int arrayEnd = findMatchingBracket(jsonStr, arrayStart);
                
                if (arrayStart >= 0 && arrayEnd > arrayStart) {
                    String peopleArray = jsonStr.substring(arrayStart + 1, arrayEnd);
                    
                    // Split into individual person objects
                    List<String> personObjects = splitJsonObjects(peopleArray);
                    
                    for (String personJson : personObjects) {
                        try {
                            String firstName = extractJsonValue(personJson, "firstName");
                            String lastName = extractJsonValue(personJson, "lastName");
                            String dobStr = extractJsonValue(personJson, "dob");
                            String govID = extractJsonValue(personJson, "governmentId");
                            String studentID = extractJsonValue(personJson, "studentId");
                            String type = extractJsonValue(personJson, "type");
                            
                            src.date.OCCCDate dob = null;
                            if (dobStr != null && !dobStr.isEmpty()) {
                                String[] parts = dobStr.split("/");
                                if (parts.length == 3) {
                                    int month = Integer.parseInt(parts[0].trim());
                                    int day = Integer.parseInt(parts[1].trim());
                                    int year = Integer.parseInt(parts[2].trim());
                                    dob = new src.date.OCCCDate(day, month, year);
                                }
                            }
                            
                            src.person.Person person;
                            if ("OCCCPerson".equals(type)) {
                                src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                                person = new src.person.OCCCPerson(regPerson, studentID);
                            } else if ("RegisteredPerson".equals(type)) {
                                person = new src.person.RegisteredPerson(firstName, lastName, dob, govID);
                            } else {
                                person = new src.person.Person(firstName, lastName, dob);
                            }
                            
                            people.add(person);
                        } catch (Exception e) {
                            // Skip invalid entries
                        }
                    }
                }
            }
        }
        
        return people;
    }

    private static String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\":");
        if (keyIndex < 0) return null;
        
        int valueStart = json.indexOf('\"', keyIndex + key.length() + 3);
        if (valueStart < 0) return null;
        
        int valueEnd = json.indexOf('\"', valueStart + 1);
        if (valueEnd < 0) return null;
        
        return json.substring(valueStart + 1, valueEnd);
    }

    private static List<String> splitJsonObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int i = 0;
        
        while (i < jsonArray.length()) {
            // Skip whitespace
            while (i < jsonArray.length() && Character.isWhitespace(jsonArray.charAt(i))) {
                i++;
            }
            
            if (i < jsonArray.length() && jsonArray.charAt(i) == '{') {
                int objectEnd = findMatchingBracket(jsonArray, i);
                if (objectEnd > i) {
                    objects.add(jsonArray.substring(i, objectEnd + 1));
                    i = objectEnd + 1;
                } else {
                    break;
                }
            } else {
                i++;
            }
        }
        
        return objects;
    }

    private static int findMatchingBracket(String str, int openIndex) {
        if (openIndex < 0 || openIndex >= str.length()) {
            return -1;
        }
        
        char open = str.charAt(openIndex);
        char close;
        
        if (open == '{') {
            close = '}';
        } else if (open == '[') {
            close = ']';
        } else if (open == '(') {
            close = ')';
        } else {
            return -1;
        }
        
        int count = 1;
        for (int i = openIndex + 1; i < str.length(); i++) {
            if (str.charAt(i) == open) {
                count++;
            } else if (str.charAt(i) == close) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        
        return -1;
    }

    public static int exportToText(src.person.People people, File file, java.util.function.Function<src.date.OCCCDate, String> formatter) throws IOException {
        if (people == null || people.isEmpty()) {
            return 0;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Person Manager Export - " + new java.util.Date());
            writer.println("------------------------------------");
            writer.println();
            int count = 0;
            for (int i = 0; i < people.size(); i++) {
                src.person.Person person = people.get(i);
                if (person == null) continue;
                writer.println("Person #" + (i + 1));
                writer.println("First Name: " + person.getFirstName());
                writer.println("Last Name: " + person.getLastName());
                writer.println("DOB: " + formatter.apply(person.getDOB()));
                if (person instanceof src.person.RegisteredPerson) {
                    src.person.RegisteredPerson rp = (src.person.RegisteredPerson) person;
                    writer.println("Government ID: " + rp.getGovID());
                    if (person instanceof src.person.OCCCPerson) {
                        src.person.OCCCPerson op = (src.person.OCCCPerson) person;
                        writer.println("Student ID: " + op.getStudentID());
                        writer.println("Type: OCCC Person");
                    } else {
                        writer.println("Type: Registered Person");
                    }
                } else {
                    writer.println("Type: Basic Person");
                }
                writer.println();
                count++;
            }
            writer.println("Total: " + count + " people");
            return count;
        }
    }

    public static int exportToJson(src.person.People people, File file, java.util.function.Function<src.date.OCCCDate, String> formatter) throws IOException {
        if (people == null || people.isEmpty()) {
            return 0;
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("{");
            writer.println("  \"exportDate\": \"" + new java.util.Date() + "\",");
            writer.println("  \"people\": [");
            int count = 0;
            for (int i = 0; i < people.size(); i++) {
                src.person.Person person = people.get(i);
                if (person == null) continue;
                writer.println("    {");
                writer.println("      \"firstName\": \"" + escapeJsonString(person.getFirstName()) + "\",");
                writer.println("      \"lastName\": \"" + escapeJsonString(person.getLastName()) + "\",");
                writer.println("      \"dob\": \"" + formatter.apply(person.getDOB()) + "\",");
                if (person instanceof src.person.RegisteredPerson) {
                    src.person.RegisteredPerson rp = (src.person.RegisteredPerson) person;
                    writer.println("      \"governmentID\": \"" + escapeJsonString(rp.getGovID()) + "\",");
                    if (person instanceof src.person.OCCCPerson) {
                        src.person.OCCCPerson op = (src.person.OCCCPerson) person;
                        writer.println("      \"studentID\": \"" + escapeJsonString(op.getStudentID()) + "\",");
                        writer.println("      \"type\": \"OCCCPerson\"");
                    } else {
                        writer.println("      \"type\": \"RegisteredPerson\"");
                    }
                } else {
                    writer.println("      \"type\": \"Person\"");
                }
                if (i < people.size() - 1) {
                    writer.println("    },");
                } else {
                    writer.println("    }");
                }
                count++;
            }
            writer.println("  ],");
            writer.println("  \"total\": " + count);
            writer.println("}");
            return count;
        }
    }

    public static String escapeJsonString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '/': sb.append("\\/"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
    
    public static src.person.People importFromText(File file) throws IOException {
        src.person.People people = new src.person.People();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            src.person.Person currentPerson = null;
            String firstName = null, lastName = null, dob = null;
            String govID = null, studentID = null;
            String type = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("First Name: ")) {
                    firstName = line.substring("First Name: ".length());
                } else if (line.startsWith("Last Name: ")) {
                    lastName = line.substring("Last Name: ".length());
                } else if (line.startsWith("DOB: ")) {
                    dob = line.substring("DOB: ".length());
                } else if (line.startsWith("Government ID: ")) {
                    govID = line.substring("Government ID: ".length());
                } else if (line.startsWith("Student ID: ")) {
                    studentID = line.substring("Student ID: ".length());
                } else if (line.startsWith("Type: ")) {
                    type = line.substring("Type: ".length());
                } else if (line.trim().isEmpty() && firstName != null && lastName != null) {
                    // End of person entry, create person object
                    try {
                        src.date.OCCCDate occcDate = null;
                        if (dob != null && !dob.isEmpty()) {
                            String[] parts = dob.split("/");
                            if (parts.length == 3) {
                                int month = Integer.parseInt(parts[0]);
                                int day = Integer.parseInt(parts[1]);
                                int year = Integer.parseInt(parts[2]);
                                occcDate = new src.date.OCCCDate(month, day, year);
                            }
                        }
                        
                        if ("OCCC Person".equals(type) && studentID != null && govID != null) {
                            src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
                            currentPerson = new src.person.OCCCPerson(regPerson, studentID);
                        } else if ("Registered Person".equals(type) && govID != null) {
                            currentPerson = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
                        } else {
                            currentPerson = new src.person.Person(firstName, lastName, occcDate);
                        }
                        if (currentPerson != null) {
                            people.add(currentPerson);
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing person entry: " + e.getMessage());
                    }
                    
                    // Reset for next person
                    firstName = lastName = dob = govID = studentID = type = null;
                    currentPerson = null;
                }
            }
            
            // Handle the last person if data exists but no empty line followed
            if (firstName != null && lastName != null && currentPerson == null) {
                try {
                    src.date.OCCCDate occcDate = null;
                    if (dob != null && !dob.isEmpty()) {
                        String[] parts = dob.split("/");
                        if (parts.length == 3) {
                            int month = Integer.parseInt(parts[0]);
                            int day = Integer.parseInt(parts[1]);
                            int year = Integer.parseInt(parts[2]);
                            occcDate = new src.date.OCCCDate(month, day, year);
                        }
                    }
                    
                    if ("OCCC Person".equals(type) && studentID != null && govID != null) {
                        src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
                        currentPerson = new src.person.OCCCPerson(regPerson, studentID);
                    } else if ("Registered Person".equals(type) && govID != null) {
                        currentPerson = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
                    } else {
                        currentPerson = new src.person.Person(firstName, lastName, occcDate);
                    }
                    
                    if (currentPerson != null) {
                        people.add(currentPerson);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing person entry: " + e.getMessage());
                }
            }
        }
        return people;
    }
    
    public static src.person.People importFromJson(File file) throws IOException {
        src.person.People people = new src.person.People();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            
            String content = jsonContent.toString();
            int peopleStart = content.indexOf("\"people\":");
            if (peopleStart == -1) {
                return people;
            }
            
            int arrayStart = content.indexOf('[', peopleStart);
            int arrayEnd = content.lastIndexOf(']');
            if (arrayStart == -1 || arrayEnd == -1 || arrayEnd < arrayStart) {
                return people;
            }
            
            String peopleArray = content.substring(arrayStart + 1, arrayEnd).trim();
            if (peopleArray.isEmpty()) {
                return people;
            }
            
            // Split the array by objects
            int depth = 0;
            int startIndex = 0;
            for (int i = 0; i <= peopleArray.length(); i++) {
                if (i == peopleArray.length() || (peopleArray.charAt(i) == ',' && depth == 0)) {
                    String personJson = peopleArray.substring(startIndex, i).trim();
                    if (!personJson.isEmpty()) {
                        parseJsonPerson(personJson, people);
                    }
                    startIndex = i + 1;
                } else if (peopleArray.charAt(i) == '{') {
                    depth++;
                } else if (peopleArray.charAt(i) == '}') {
                    depth--;
                }
            }
        }
        return people;
    }
    
    private static void parseJsonPerson(String json, src.person.People people) {
        try {
            String firstName = extractJsonValue(json, "firstName");
            String lastName = extractJsonValue(json, "lastName");
            String dob = extractJsonValue(json, "dob");
            String govID = extractJsonValue(json, "governmentID");
            String studentID = extractJsonValue(json, "studentID");
            String type = extractJsonValue(json, "type");
            
            src.date.OCCCDate occcDate = null;
            if (dob != null && !dob.isEmpty()) {
                String[] parts = dob.split("/");
                if (parts.length == 3) {
                    try {
                        int month = Integer.parseInt(parts[0]);
                        int day = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        occcDate = new src.date.OCCCDate(month, day, year);
                    } catch (Exception e) {
                        System.err.println("Error parsing date: " + e.getMessage());
                    }
                }
            }
            
            src.person.Person person = null;
            if ("OCCCPerson".equals(type) && studentID != null && govID != null) {
                src.person.RegisteredPerson regPerson = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
                person = new src.person.OCCCPerson(regPerson, studentID);
            } else if ("RegisteredPerson".equals(type) && govID != null) {
                person = new src.person.RegisteredPerson(firstName, lastName, occcDate, govID);
            } else {
                person = new src.person.Person(firstName, lastName, occcDate);
            }
            
            if (person != null) {
                people.add(person);
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON person entry: " + e.getMessage());
        }
    }

    // Added methods for enhanced terminal operations
    
    /**
     * Get the current data directory
     * @return The data directory
     */
    public File getDataDirectory() {
        return DATA_DIRECTORY;
    }
    
    /**
     * List files in a directory
     * @param directory The directory to list files from
     * @return Array of files in the directory
     */
    public File[] listFiles(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return new File[0];
        }
        return directory.listFiles();
    }
    
    /**
     * Create a new directory
     * @param parent The parent directory
     * @param name The name of the new directory
     * @return True if directory was created, false otherwise
     */
    public boolean createDirectory(File parent, String name) {
        if (parent == null || !parent.exists() || !parent.isDirectory() || name == null || name.isEmpty()) {
            return false;
        }
        File newDir = new File(parent, name);
        return newDir.mkdir();
    }
    
    /**
     * Open a specific file
     * @param file The file to open
     * @param clearFields Runnable to clear fields
     * @param clearSelection Runnable to clear selection
     * @return Number of people loaded or -1 if error
     */
    public int openFile(File file, Runnable clearFields, Runnable clearSelection) {
        if (file == null || !file.exists() || !file.isFile()) {
            return -1;
        }
        try {
            int count = appController.loadPeople(file);
            if (clearFields != null) clearFields.run();
            if (clearSelection != null) clearSelection.run();
            appController.notifyDataChanged();
            return count;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Save to a specific file
     * @param file The file to save to
     * @return Number of people saved or -1 if error
     */
    public int saveToFile(File file) {
        if (file == null) {
            return -1;
        }
        
        // Ensure proper extension
        String extension = FILE_EXTENSION;
        if (file.getName().toLowerCase().endsWith(".json") || file.getName().toLowerCase().endsWith(".txt")) {
            extension = file.getName().substring(file.getName().lastIndexOf('.'));
        }
        
        File targetFile = ensureFileExtension(file, extension);
        try {
            int count;
            if (extension.equals(".json")) {
                count = appController.exportPeople(appController.getPeople(), targetFile, "json", dateFormatter);
            } else if (extension.equals(".txt")) {
                count = appController.exportPeople(appController.getPeople(), targetFile, "txt", dateFormatter);
            } else {
                count = appController.savePeopleAs(targetFile);
            }
            return count;
        } catch (IOException ex) {
            ex.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Import from a specific file
     * @param file The file to import from
     * @param silent Whether to suppress UI dialogs
     * @return Number of people imported or -1 if error
     */
    public int importFromFile(File file, boolean silent) {
        if (file == null || !file.exists() || !file.isFile()) {
            return -1;
        }
        
        try {
            People importedPeople = appController.importPeople(file);
            if (importedPeople == null || importedPeople.isEmpty()) {
                return 0;
            }
            
            int importedCount = 0;
            List<ConflictResolution.ConflictInfo> conflicts = new ArrayList<>();
            List<Person> handledPersons = new ArrayList<>();
            
            // First, collect all conflicts and duplicates
            for (Person person : importedPeople) {
                if (person == null) continue;
                if (ConflictResolution.isExactDuplicate(person, appController.getPeople())) {
                    handledPersons.add(person);
                    continue;
                }
                ConflictResolution.ConflictInfo conflict = ConflictResolution.checkForConflict(person, appController.getPeople());
                if (conflict != null) {
                    conflicts.add(conflict);
                    handledPersons.add(person);
                }
            }
            
            // Resolve conflicts
            if (!conflicts.isEmpty()) {
                boolean resolveAllRemaining = false;
                ConflictResolution.ConflictChoice globalChoice = null;
                for (int i = 0; i < conflicts.size(); i++) {
                    ConflictResolution.ConflictInfo conflict = conflicts.get(i);
                    ConflictResolution.ConflictChoice choice;
                    if (!silent) {
                        if (!resolveAllRemaining) {
                            choice = ConflictResolution.showConflictResolutionDialogWithApplyToAll(conflict, conflicts.size() - i, parentFrame);
                            if (choice == ConflictResolution.ConflictChoice.APPLY_TO_ALL) {
                                globalChoice = ConflictResolution.showGlobalResolutionDialog(parentFrame);
                                if (globalChoice == ConflictResolution.ConflictChoice.CANCEL) {
                                    i--; continue;
                                }
                                resolveAllRemaining = true;
                                choice = globalChoice;
                            }
                        } else {
                            choice = globalChoice;
                        }
                        if (choice == ConflictResolution.ConflictChoice.CANCEL && !resolveAllRemaining) {
                            continue;
                        }
                    } else {
                        // In silent mode, keep existing by default
                        choice = ConflictResolution.ConflictChoice.KEEP_EXISTING;
                    }
                    
                    switch (choice) {
                        case USE_NEW:
                            if (appController.getPeople().update(conflict.existingIndex, conflict.newPerson)) {
                            }
                            break;
                        case KEEP_EXISTING:
                            break;
                        case SKIP:
                            break;
                        default:
                            break;
                    }
                }
            }
            
            // Now import only those not handled as duplicate or conflict
            for (Person person : importedPeople) {
                if (person == null) continue;
                if (handledPersons.contains(person)) continue;
                if (ConflictResolution.isExactDuplicate(person, appController.getPeople())) {
                    continue;
                }
                ConflictResolution.ConflictInfo conflict = ConflictResolution.checkForConflict(person, appController.getPeople());
                if (conflict != null) {
                    continue;
                }
                if (appController.getPeople().add(person)) {
                    importedCount++;
                }
            }
            appController.notifyDataChanged();
            
            return importedCount;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Get stats about file sizes in data directory
     * @return Stats string
     */
    public String getDirectoryStats(File directory) {
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return "Invalid directory";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("Directory: ").append(directory.getAbsolutePath()).append("\n");
        stats.append("Last Modified: ").append(new java.util.Date(directory.lastModified())).append("\n");
        
        int fileCount = 0;
        int dirCount = 0;
        long totalSize = 0;
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    dirCount++;
                } else {
                    fileCount++;
                    totalSize += file.length();
                }
            }
        }
        
        stats.append("Subdirectories: ").append(dirCount).append("\n");
        stats.append("Files: ").append(fileCount).append("\n");
        stats.append("Total Size: ").append(formatFileSize(totalSize)).append("\n");
        stats.append("Readable: ").append(directory.canRead()).append("\n");
        stats.append("Writable: ").append(directory.canWrite()).append("\n");
        
        return stats.toString();
    }
    
    /**
     * Format file size in human-readable format
     * @param size The size in bytes
     * @return Formatted size string
     */
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
        }
    }
}
