package src.app.dialogs;

import javax.swing.*;
import java.awt.*;
import src.app.AppController;

public class DateFormatDialog extends JDialog {
    private final JComboBox<String> formatCombo;
    public DateFormatDialog(JFrame parent, AppController controller) {
        super(parent, "Select Date Format", true);
        setLayout(new BorderLayout(10, 10));
        String[] formats = {"US (MM/dd/yyyy)", "EURO (dd/MM/yyyy)", "ISO (yyyy-MM-dd)"};
        formatCombo = new JComboBox<>(formats);
        formatCombo.setSelectedIndex(controller.getDateFormat().ordinal());
        JPanel center = new JPanel();
        center.add(new JLabel("Date Format:"));
        center.add(formatCombo);
        add(center, BorderLayout.CENTER);
        JButton ok = new JButton("OK");
        ok.addActionListener(_ -> {
            controller.setDateFormat(AppController.DateFormatType.values()[formatCombo.getSelectedIndex()]);
            dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(_ -> dispose());
        JPanel south = new JPanel();
        south.add(ok);
        south.add(cancel);
        add(south, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }
    public static void showDialog(JFrame parent, AppController controller) {
        new DateFormatDialog(parent, controller).setVisible(true);
    }
}
