package src.app.gui;

import src.person.Person;
import src.person.People;
import src.person.RegisteredPerson;
import src.person.OCCCPerson;
import src.date.OCCCDate;
import javax.swing.*;
import java.awt.Color;

public class ConflictResolution {
    public enum ConflictChoice {
        USE_NEW,
        KEEP_EXISTING,
        SKIP,
        CANCEL,
        APPLY_TO_ALL
    }

    public static class ConflictInfo {
        public final Person existingPerson;
        public final Person newPerson;
        public final int existingIndex;
        public final String conflictType;
        public final String conflictValue;
        public ConflictInfo(Person existing, Person newP, int index, String type, String value) {
            this.existingPerson = existing;
            this.newPerson = newP;
            this.existingIndex = index;
            this.conflictType = type;
            this.conflictValue = value;
        }
    }

    public static ConflictInfo checkForConflict(Person person, People people) {
        if (person == null) return null;
        if (person instanceof RegisteredPerson) {
            RegisteredPerson regPerson = (RegisteredPerson) person;
            String govID = regPerson.getGovID();
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                if (existingPerson instanceof RegisteredPerson) {
                    RegisteredPerson existingReg = (RegisteredPerson) existingPerson;
                    if (govID.equals(existingReg.getGovID())) {
                        if (arePersonsIdentical(existingPerson, person)) {
                            return null;
                        }
                        return new ConflictInfo(existingPerson, person, i, "govID", govID);
                    }
                }
            }
        }
        if (person instanceof OCCCPerson) {
            OCCCPerson occPerson = (OCCCPerson) person;
            String studentID = occPerson.getStudentID();
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                if (existingPerson instanceof OCCCPerson) {
                    OCCCPerson existingOcc = (OCCCPerson) existingPerson;
                    if (studentID.equals(existingOcc.getStudentID())) {
                        if (arePersonsIdentical(existingPerson, person)) {
                            return null;
                        }
                        return new ConflictInfo(existingPerson, person, i, "studentID", studentID);
                    }
                }
            }
        }
        String firstName = person.getFirstName();
        String lastName = person.getLastName();
        if (firstName != null && !firstName.isEmpty() && lastName != null && !lastName.isEmpty()) {
            for (int i = 0; i < people.size(); i++) {
                Person existingPerson = people.get(i);
                if (firstName.equals(existingPerson.getFirstName()) &&
                    lastName.equals(existingPerson.getLastName()) &&
                    person.getDOB().equals(existingPerson.getDOB())) {
                    if (person instanceof RegisteredPerson || existingPerson instanceof RegisteredPerson) {
                        continue;
                    }
                    return new ConflictInfo(existingPerson, person, i, "basicPerson", firstName + " " + lastName);
                }
            }
        }
        return null;
    }

    public static boolean arePersonsIdentical(Person person1, Person person2) {
        if (person1 == null || person2 == null) {
            return false;
        }
        boolean basicMatch = person1.getFirstName().equals(person2.getFirstName()) && 
                            person1.getLastName().equals(person2.getLastName()) &&
                            person1.getDOB().equals(person2.getDOB());
        if (!basicMatch) {
            return false;
        }
        if (person1 instanceof RegisteredPerson && person2 instanceof RegisteredPerson) {
            RegisteredPerson reg1 = (RegisteredPerson) person1;
            RegisteredPerson reg2 = (RegisteredPerson) person2;
            if (!reg1.getGovID().equals(reg2.getGovID())) {
                return false;
            }
            if (person1 instanceof OCCCPerson && person2 instanceof OCCCPerson) {
                OCCCPerson occc1 = (OCCCPerson) person1;
                OCCCPerson occc2 = (OCCCPerson) person2;
                if (!occc1.getStudentID().equals(occc2.getStudentID())) {
                    return false;
                }
            } else if ((person1 instanceof OCCCPerson) != (person2 instanceof OCCCPerson)) {
                return false;
            }
        } else if ((person1 instanceof RegisteredPerson) != (person2 instanceof RegisteredPerson)) {
            return false;
        } else if (!(person1 instanceof RegisteredPerson) && !(person2 instanceof RegisteredPerson)) {
            return true;
        }
        return true;
    }

    public static boolean isExactDuplicate(Person person, People people) {
        if (person == null) return false;
        for (int i = 0; i < people.size(); i++) {
            Person existingPerson = people.get(i);
            if (arePersonsIdentical(existingPerson, person)) {
                return true;
            }
        }
        return false;
    }

    public static ConflictChoice showConflictResolutionDialogWithApplyToAll(ConflictInfo conflict, int remainingCount, JFrame parentFrame) {
        JPanel panel = new JPanel();
        panel.setLayout(new java.awt.BorderLayout(10, 10));
        String idType = conflict.conflictType.equals("govID") ? "Government ID" : "Student ID";
        String countInfo = (remainingCount > 1) ? " (" + remainingCount + " conflicts remaining)" : "";
        JLabel conflictLabel = new JLabel("Conflict detected: " + idType + " '" + conflict.conflictValue + "' already exists." + countInfo);
        panel.add(conflictLabel, java.awt.BorderLayout.NORTH);
        JPanel comparisonPanel = new JPanel(new java.awt.GridLayout(0, 3, 5, 5));
        comparisonPanel.add(new JLabel("Field"));
        comparisonPanel.add(new JLabel("Existing"));
        comparisonPanel.add(new JLabel("New (Import)"));
        comparisonPanel.add(new JLabel("Name:"));
        comparisonPanel.add(new JLabel(conflict.existingPerson.getFirstName() + " " + conflict.existingPerson.getLastName()));
        comparisonPanel.add(new JLabel(conflict.newPerson.getFirstName() + " " + conflict.newPerson.getLastName()));
        comparisonPanel.add(new JLabel("Birth Date:"));
        comparisonPanel.add(new JLabel(formatDateForDisplay(conflict.existingPerson.getDOB())));
        comparisonPanel.add(new JLabel(formatDateForDisplay(conflict.newPerson.getDOB())));
        if (conflict.existingPerson instanceof RegisteredPerson && conflict.newPerson instanceof RegisteredPerson) {
            String existingGovId = ((RegisteredPerson)conflict.existingPerson).getGovID();
            String newGovId = ((RegisteredPerson)conflict.newPerson).getGovID();
            comparisonPanel.add(new JLabel("Government ID:"));
            comparisonPanel.add(new JLabel(existingGovId));
            comparisonPanel.add(new JLabel(newGovId));
        }
        if (conflict.existingPerson instanceof OCCCPerson && conflict.newPerson instanceof OCCCPerson) {
            String existingStudentId = ((OCCCPerson)conflict.existingPerson).getStudentID();
            String newStudentId = ((OCCCPerson)conflict.newPerson).getStudentID();
            comparisonPanel.add(new JLabel("Student ID:"));
            comparisonPanel.add(new JLabel(existingStudentId));
            comparisonPanel.add(new JLabel(newStudentId));
        }
        panel.add(comparisonPanel, java.awt.BorderLayout.CENTER);
        highlightDifferences(comparisonPanel);
        Object[] options;
        if (remainingCount > 1) {
            options = new Object[]{"Keep Existing", "Use New", "Skip", "Apply to All...", "Cancel"};
        } else {
            options = new Object[]{"Keep Existing", "Use New", "Skip", "Cancel"};
        }
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            panel,
            "Resolve Import Conflict",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        if (remainingCount > 1) {
            switch (choice) {
                case 1: return ConflictChoice.USE_NEW;
                case 2: return ConflictChoice.SKIP;
                case 3: return ConflictChoice.APPLY_TO_ALL;
                case 4: return ConflictChoice.CANCEL;
                case 0:
                default: return ConflictChoice.KEEP_EXISTING;
            }
        } else {
            switch (choice) {
                case 1: return ConflictChoice.USE_NEW;
                case 2: return ConflictChoice.SKIP;
                case 3: return ConflictChoice.CANCEL;
                case 0:
                default: return ConflictChoice.KEEP_EXISTING;
            }
        }
    }

    public static ConflictChoice showGlobalResolutionDialog(JFrame parentFrame) {
        Object[] options = {"Keep All Existing", "Import All New", "Skip All", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            "How do you want to resolve all remaining conflicts?",
            "Apply to All Conflicts",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        switch (choice) {
            case 1: return ConflictChoice.USE_NEW;
            case 2: return ConflictChoice.SKIP;
            case 3: return ConflictChoice.CANCEL;
            case 0:
            default: return ConflictChoice.KEEP_EXISTING;
        }
    }

    public static void highlightDifferences(JPanel panel) {
        java.awt.Component[] components = panel.getComponents();
        Color errorColor = getThemeColor("ERROR", Color.RED);
        Color successColor = getThemeColor("SUCCESS", new Color(0, 100, 0));
        for (int i = 3; i < components.length; i += 3) {
            if (i + 2 < components.length && components[i] instanceof JLabel && 
                components[i+1] instanceof JLabel && components[i+2] instanceof JLabel) {
                JLabel fieldLabel = (JLabel)components[i];
                JLabel existingValue = (JLabel)components[i+1];
                JLabel newValue = (JLabel)components[i+2];
                if (!existingValue.getText().equals(newValue.getText())) {
                    existingValue.setForeground(errorColor);
                    newValue.setForeground(successColor);
                    java.awt.Font boldFont = new java.awt.Font(fieldLabel.getFont().getName(), 
                                           java.awt.Font.BOLD, 
                                           fieldLabel.getFont().getSize());
                    fieldLabel.setFont(boldFont);
                }
            }
        }
    }

    private static Color getThemeColor(String key, Color fallback) {
        Color c = UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    private static String formatDateForDisplay(OCCCDate date) {
        if (date == null) return "";
        return String.format("%02d/%02d/%04d", date.getMonthNumber(), date.getDayOfMonth(), date.getYear());
    }
}
