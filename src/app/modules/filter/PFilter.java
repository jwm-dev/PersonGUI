package src.app.modules.filter;

import java.util.function.Predicate;
import src.person.Person;
import src.person.People;
import src.app.dialogs.Dialogs;
import src.app.modules.list.PList;

/**
 * PFilter is a filtration widget for filtering the People list.
 * It allows users to set custom filtration routines and see sublists based on provided terms.
 * Designed to be user-friendly, powerful, and extensible.
 */
public interface PFilter {
    interface FilterListener {
        void onFilterChanged(Predicate<Person> filter);
    }

    /**
     * Set the People list to filter (optional, for advanced use)
     */
    void setPeople(People people);

    /**
     * Set a custom filter predicate (for extensibility)
     */
    void setCustomFilter(Predicate<Person> filter);

    /**
     * Set a listener to be notified when the filter changes
     */
    void setFilterListener(FilterListener listener);

    /**
     * Set the operations instance for exporting
     */
    void setOperations(Dialogs operations);

    /**
     * Set the list module instance for exporting
     */
    void setListModule(PList listModule);

    /**
     * Get the current filter predicate
     */
    Predicate<Person> getCurrentFilter();

    /**
     * Get the main filter panel (for theming)
     */
    javax.swing.JPanel getPanel();
    /**
     * Get the search field (for theming)
     */
    javax.swing.JTextField getSearchField();
    /**
     * Get the filter type combo box (for theming)
     */
    javax.swing.JComboBox<String> getFilterTypeBox();
    /**
     * Get the clear button (for theming)
     */
    javax.swing.JButton getClearButton();
    /**
     * Get the export button (for theming)
     */
    javax.swing.JButton getExportButton();
}
