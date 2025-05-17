package src.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A collection class for Person objects that provides convenient group operations
 * and implements serialization for the entire collection.
 */
public class People implements Serializable, Iterable<Person> {
    private static final long serialVersionUID = 1L;

    /**
     * Wrapper for Person with metadata.
     */
    public static class PersonMeta implements Serializable {
        private static final long serialVersionUID = 1L;
        private Person person;
        private String description;
        private String tags;

        public PersonMeta(Person person, String description, String tags) {
            this.person = person;
            this.description = description;
            this.tags = tags;
        }
        public PersonMeta(Person person) {
            this(person, "", "");
        }
        public Person getPerson() { return person; }
        public void setPerson(Person person) { this.person = person; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    private List<PersonMeta> people = new ArrayList<>();

    /**
     * Creates an empty People collection
     */
    public People() {
        // Creates an empty collection
    }

    /**
     * Creates a People collection with initial person entries
     * @param initialPeople Initial array of Person objects
     */
    public People(Person... initialPeople) {
        if (initialPeople != null) {
            for (Person p : initialPeople) {
                add(p);
            }
        }
    }

    /**
     * Adds a person to the collection
     * @param person Person to add
     * @return true if successfully added
     */
    public boolean add(Person person) {
        if (person != null) {
            return people.add(new PersonMeta(person));
        }
        return false;
    }

    /**
     * Adds a person with metadata
     */
    public boolean add(Person person, String description, String tags) {
        if (person != null) {
            return people.add(new PersonMeta(person, description, tags));
        }
        return false;
    }

    /**
     * Removes a person from the collection
     * @param person Person to remove
     * @return true if successfully removed
     */
    public boolean remove(Person person) {
        return people.removeIf(meta -> meta.getPerson().equals(person));
    }

    /**
     * Removes a person at a specific index
     * @param index Index of the person to remove
     * @return true if successfully removed
     */
    public boolean remove(int index) {
        if (index >= 0 && index < people.size()) {
            people.remove(index);
            return true;
        }
        return false;
    }

    /**
     * Gets a person at specific index
     * @param index Index of the person to retrieve
     * @return Person at the specified index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Person get(int index) {
        return people.get(index).getPerson();
    }

    /**
     * Gets the PersonMeta at a specific index
     */
    public PersonMeta getMeta(int index) {
        return people.get(index);
    }

    /**
     * Sets a person at a specific index
     * @param index Index where to set the person
     * @param person Person to set
     * @return Previous person at that index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Person set(int index, Person person) {
        PersonMeta old = people.get(index);
        people.set(index, new PersonMeta(person, old.getDescription(), old.getTags()));
        return old.getPerson();
    }

    /**
     * Gets the size of the collection
     * @return Number of people in the collection
     */
    public int size() {
        return people.size();
    }

    /**
     * Checks if collection is empty
     * @return true if the collection contains no people
     */
    public boolean isEmpty() {
        return people.isEmpty();
    }

    /**
     * Clears all people from the collection
     */
    public void clear() {
        people.clear();
    }

    /**
     * Gets all people as a list
     * @return ArrayList containing all people
     */
    public List<Person> getAllPeople() {
        List<Person> result = new ArrayList<>();
        for (PersonMeta meta : people) result.add(meta.getPerson());
        return result;
    }

    /**
     * Gets all PersonMeta as a list
     */
    public List<PersonMeta> getAllMeta() {
        return new ArrayList<>(people);
    }

    /**
     * Provides an iterator for the People collection
     * @return Iterator over Person objects
     */
    @Override
    public Iterator<Person> iterator() {
        return new Iterator<Person>() {
            private final Iterator<PersonMeta> metaIt = people.iterator();
            public boolean hasNext() { return metaIt.hasNext(); }
            public Person next() { return metaIt.next().getPerson(); }
        };
    }

    /**
     * String representation of the People collection
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("People collection [size=" + size() + "]:\n");
        for (PersonMeta meta : people) {
            sb.append("  ").append(meta.getPerson().toString());
            if (!meta.getDescription().isEmpty()) sb.append(" | Desc: ").append(meta.getDescription());
            if (!meta.getTags().isEmpty()) sb.append(" | Tags: ").append(meta.getTags());
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Updates a person at a specific index
     * @param index Index to update
     * @param person New person data
     * @return true if updated
     */
    public boolean update(int index, Person person) {
        if (index >= 0 && index < people.size() && person != null) {
            PersonMeta old = people.get(index);
            people.set(index, new PersonMeta(person, old.getDescription(), old.getTags()));
            return true;
        }
        return false;
    }

    /**
     * Updates metadata for a person at a specific index
     */
    public boolean updateMeta(int index, String description, String tags) {
        if (index >= 0 && index < people.size()) {
            PersonMeta meta = people.get(index);
            meta.setDescription(description);
            meta.setTags(tags);
            return true;
        }
        return false;
    }

    /**
     * Returns the index of a person in the collection, or -1 if not found
     */
    public int indexOf(Person person) {
        for (int i = 0; i < people.size(); i++) {
            if (people.get(i).getPerson().equals(person)) return i;
        }
        return -1;
    }

    /**
     * Checks if a government ID already exists in the collection (optionally excluding an index)
     */
    public boolean isDuplicateGovID(String govID, int excludeIndex) {
        if (govID == null || govID.isEmpty()) return false;
        for (int i = 0; i < people.size(); i++) {
            if (i == excludeIndex) continue;
            Person person = people.get(i).getPerson();
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
     * Checks if a student ID already exists in the collection (optionally excluding an index)
     */
    public boolean isDuplicateStudentID(String studentID, int excludeIndex) {
        if (studentID == null || studentID.isEmpty()) return false;
        for (int i = 0; i < people.size(); i++) {
            if (i == excludeIndex) continue;
            Person person = people.get(i).getPerson();
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
     * Gets a PersonMeta by Person reference
     * @param person The Person to look up
     * @return The PersonMeta, or null if not found
     */
    public PersonMeta getMetaByPerson(Person person) {
        for (PersonMeta meta : people) {
            if (meta.getPerson().equals(person)) return meta;
        }
        return null;
    }
}
