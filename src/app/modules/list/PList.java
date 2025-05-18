package src.app.modules.list;

import src.app.modules.viewer.PViewer;
import src.person.People;
import src.person.Person;

import javax.swing.*;
import java.util.function.Predicate;

/**
 * API for the Person List module
 */
public interface PList {
    void refreshList();
    void applyFilter(Predicate<Person> filter);
    People getFilteredPeople();
    void setPersonManager(PViewer personManager);
    void selectPerson(Person person);
    void clearSelection();
    JTable getJList();
    JLabel getHeader();
    /**
     * Get the main list panel (for theming)
     */
    javax.swing.JPanel getPanel();
    // Add method to get the number of people in the list
    int getListSize();
}
