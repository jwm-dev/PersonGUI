package src.app;

import java.io.*;
import java.util.*;

import src.person.Person;
import src.person.People;
import src.person.OCCCPerson;
import src.person.RegisteredPerson;

/**
 * Manages all data operations for the Person Management application.
 * This class handles create, read, update, delete operations for Person objects
 * as well as file operations (save and load).
 */
public class DataManager {
    private People people = new People();
    private File currentFile;
    private boolean modified = false; // Track whether data has been modified since last save
    private boolean hasChanges = false; // Track whether data has changed at all since new/open
    private List<DataChangeListener> listeners = new ArrayList<>();
    
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
     * Gets all people stored in the manager
     * @return People collection
     */
    public People getPeople() {
        return people;
    }
    
    /**
     * Adds a new person to the collection
     * @param person The person to add
     * @return true if successfully added
     */
    public boolean addPerson(Person person) {
        if (person != null) {
            // Check for duplicate government ID or student ID
            if (person instanceof RegisteredPerson) {
                RegisteredPerson regPerson = (RegisteredPerson) person;
                String govID = regPerson.getGovID();
                
                // Check for duplicate government ID
                if (isDuplicateGovID(govID, -1)) {
                    return false;
                }
                
                // If it's an OCCC person, check for duplicate student ID
                if (person instanceof OCCCPerson) {
                    OCCCPerson occPerson = (OCCCPerson) person;
                    String studentID = occPerson.getStudentID();
                    
                    if (isDuplicateStudentID(studentID, -1)) {
                        return false;
                    }
                }
            }
            
            boolean result = people.add(person);
            modified = true; // Mark data as modified
            hasChanges = true; // Mark that we have changes since new/open
            notifyDataChanged();
            return result;
        }
        return false;
    }
    
    /**
     * Updates a person in the collection
     * @param index Index of person to update
     * @param updatedPerson New person data
     * @return true if successfully updated
     */
    public boolean updatePerson(int index, Person updatedPerson) {
        if (index >= 0 && index < people.size() && updatedPerson != null) {
            // Check for duplicate government ID or student ID, excluding the current index
            if (updatedPerson instanceof RegisteredPerson) {
                RegisteredPerson regPerson = (RegisteredPerson) updatedPerson;
                String govID = regPerson.getGovID();
                
                // Check for duplicate government ID
                if (isDuplicateGovID(govID, index)) {
                    return false;
                }
                
                // If it's an OCCC person, check for duplicate student ID
                if (updatedPerson instanceof OCCCPerson) {
                    OCCCPerson occPerson = (OCCCPerson) updatedPerson;
                    String studentID = occPerson.getStudentID();
                    
                    if (isDuplicateStudentID(studentID, index)) {
                        return false;
                    }
                }
            }
            
            people.set(index, updatedPerson);
            modified = true;
            hasChanges = true; // Mark that we have changes since new/open
            notifyDataChanged();
            return true;
        }
        return false;
    }
    
    /**
     * Checks if a government ID already exists in the collection
     * @param govID The government ID to check
     * @param excludeIndex Index to exclude from the check (-1 for new person)
     * @return true if duplicate exists, false otherwise
     */
    public boolean isDuplicateGovID(String govID, int excludeIndex) {
        if (govID == null || govID.isEmpty()) return false;
        
        for (int i = 0; i < people.size(); i++) {
            if (i == excludeIndex) continue; // Skip the current person when updating
            
            Person person = people.get(i);
            if (person instanceof RegisteredPerson) {
                RegisteredPerson regPerson = (RegisteredPerson) person;
                if (govID.equals(regPerson.getGovID())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a student ID already exists in the collection
     * @param studentID The student ID to check
     * @param excludeIndex Index to exclude from the check (-1 for new person)
     * @return true if duplicate exists, false otherwise
     */
    public boolean isDuplicateStudentID(String studentID, int excludeIndex) {
        if (studentID == null || studentID.isEmpty()) return false;
        
        for (int i = 0; i < people.size(); i++) {
            if (i == excludeIndex) continue; // Skip the current person when updating
            
            Person person = people.get(i);
            if (person instanceof OCCCPerson) {
                OCCCPerson occPerson = (OCCCPerson) person;
                if (studentID.equals(occPerson.getStudentID())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Deletes a person from the collection
     * @param person The person to delete
     * @return true if successfully deleted
     */
    public boolean deletePerson(Person person) {
        if (person != null) {
            boolean result = people.remove(person);
            modified = true;
            hasChanges = true; // Mark that we have changes since new/open
            notifyDataChanged();
            return result;
        }
        return false;
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
    
    /**
     * Loads people data from a file
     * @param file The file to load from
     * @return Number of people loaded
     * @throws Exception If there's an error during loading
     */
    public int loadFromFile(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            Object obj = ois.readObject();
            if (obj instanceof People) {
                people = (People) obj;
                currentFile = file;
                modified = false;
                hasChanges = false; // Reset changes tracking on successful load
                notifyDataChanged();
                return people.size();
            } else {
                throw new ClassCastException("File does not contain a valid People object");
            }
        }
    }
    
    /**
     * Saves people data to a file
     * @param file The file to save to
     * @return Number of people saved
     * @throws IOException If there's an error during saving
     */
    public int saveToFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            oos.writeObject(people);
            
            currentFile = file;
            modified = false; // Reset modified flag to indicate data has been saved
            hasChanges = false; // Reset changes flag since we've saved all changes
            notifyDataChanged(); // Notify listeners that modification state has changed
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
}
