package src.app.modules.viewer;

import javax.swing.*;
import java.awt.*;

/**
 * Handles UI construction and layout for the Person Viewer.
 */
public class PViewerPanel extends JPanel {
    public JTextField firstNameField, lastNameField, govIDField, studentIDField, dobField;
    public JLabel govIDLabel, studentIDLabel;
    public JButton addButton, updateButton, deleteButton;

    public PViewerPanel() {
        setLayout(new BorderLayout());
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 6, 0, 6));
        int fieldHeight = 22;
        int fieldWidth = 150;
        Dimension fieldDim = new Dimension(fieldWidth, fieldHeight);
        Dimension labelDim = new Dimension(fieldWidth, fieldHeight);

        fieldsPanel.add(Box.createVerticalStrut(4));
        JLabel firstNameLabel = new JLabel("First Name");
        firstNameLabel.setAlignmentX(LEFT_ALIGNMENT);
        firstNameLabel.setMaximumSize(labelDim);
        fieldsPanel.add(firstNameLabel);
        firstNameField = new JTextField();
        firstNameField.setMaximumSize(fieldDim);
        firstNameField.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.add(firstNameField);
        fieldsPanel.add(Box.createVerticalStrut(4));

        JLabel lastNameLabel = new JLabel("Last Name");
        lastNameLabel.setAlignmentX(LEFT_ALIGNMENT);
        lastNameLabel.setMaximumSize(labelDim);
        fieldsPanel.add(lastNameLabel);
        lastNameField = new JTextField();
        lastNameField.setMaximumSize(fieldDim);
        lastNameField.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.add(lastNameField);
        fieldsPanel.add(Box.createVerticalStrut(4));

        JLabel dobLabel = new JLabel("DOB");
        dobLabel.setAlignmentX(LEFT_ALIGNMENT);
        dobLabel.setMaximumSize(labelDim);
        fieldsPanel.add(dobLabel);
        dobField = new JTextField();
        dobField.setMaximumSize(fieldDim);
        dobField.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.add(dobField);
        fieldsPanel.add(Box.createVerticalStrut(4));

        govIDLabel = new JLabel("Gov. ID");
        govIDLabel.setAlignmentX(LEFT_ALIGNMENT);
        govIDLabel.setMaximumSize(labelDim);
        fieldsPanel.add(govIDLabel);
        govIDField = new JTextField();
        govIDField.setMaximumSize(fieldDim);
        govIDField.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.add(govIDField);
        fieldsPanel.add(Box.createVerticalStrut(4));

        studentIDLabel = new JLabel("Student ID");
        studentIDLabel.setAlignmentX(LEFT_ALIGNMENT);
        studentIDLabel.setMaximumSize(labelDim);
        fieldsPanel.add(studentIDLabel);
        studentIDField = new JTextField();
        studentIDField.setMaximumSize(fieldDim);
        studentIDField.setAlignmentX(LEFT_ALIGNMENT);
        fieldsPanel.add(studentIDField);
        fieldsPanel.add(Box.createVerticalStrut(8));

        add(fieldsPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        Dimension btnDim = new Dimension(120, fieldHeight);
        addButton.setMaximumSize(btnDim);
        updateButton.setMaximumSize(btnDim);
        deleteButton.setMaximumSize(btnDim);
        addButton.setAlignmentX(LEFT_ALIGNMENT);
        updateButton.setAlignmentX(LEFT_ALIGNMENT);
        deleteButton.setAlignmentX(LEFT_ALIGNMENT);
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createVerticalStrut(4));
        buttonPanel.add(updateButton);
        buttonPanel.add(Box.createVerticalStrut(4));
        buttonPanel.add(deleteButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 10, 6));
        add(buttonPanel, BorderLayout.SOUTH);
    }
}
