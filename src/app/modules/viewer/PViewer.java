package src.app.modules.viewer;

import javax.swing.*;
import src.app.modules.list.PList;
import src.person.Person;

/**
 * API interface for the Person Viewer module.
 */
public interface PViewer {
    boolean displayPersonDetails(Person person);
    void clearFields();
    void setDataList(PList dataList);
    Person getCurrentPerson();
    boolean hasPartialData();
    JPanel getPanel();
    void addTextFieldChangeListener(PViewerFields.FieldChangeListener listener);
}
