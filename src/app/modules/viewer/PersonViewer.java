package src.app.modules.viewer;

import javax.swing.*;
import src.app.AppController;
import src.app.gui.MainBar;
import src.app.modules.list.PList;
import src.person.Person;

/**
 * Implementation of the PViewer API interface.
 * Delegates to internal helper classes for UI, theming, and field logic.
 */
public class PersonViewer implements PViewer {
    private final PViewerPanel panel;
    private final PViewerFields fields;

    public PersonViewer(JFrame parent, AppController manager, src.app.gui.Dialogs dialogs, MainBar bar) {
        this.panel = new PViewerPanel();
        this.fields = new PViewerFields(panel, manager, parent);
    }

    @Override
    public boolean displayPersonDetails(Person person) { return fields.displayPersonDetails(person); }
    @Override
    public void clearFields() { fields.clearFields(); }
    @Override
    public void setDataList(PList dataList) { fields.setDataList(dataList); }
    @Override
    public Person getCurrentPerson() { return fields.getCurrentPerson(); }
    @Override
    public boolean hasPartialData() { return fields.hasPartialData(); }
    @Override
    public JPanel getPanel() { return panel; }
    @Override
    public void addTextFieldChangeListener(PViewerFields.FieldChangeListener listener) {
        fields.addTextFieldChangeListener(listener);
    }
}
