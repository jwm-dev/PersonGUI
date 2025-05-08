package src.person;

public class OCCCPerson extends RegisteredPerson { 
    private String studentID;

    // constructors
    public OCCCPerson(RegisteredPerson p, String studentID) {
        super(p);
        this.studentID = studentID;
    }
    public OCCCPerson(OCCCPerson p) { // copy constructor
        super(p);
        this.studentID = p.studentID;
    }

    // getters
    public String getStudentID() {
        return studentID;
    }

    // equals
    public boolean equals(OCCCPerson p) {
        return super.equals(p) && studentID.equals(p.studentID);
    }
    public boolean equals(RegisteredPerson p) {
        return super.equals(p);
    }
    public boolean equals(Person p) {
        return super.equals(p);
    }

    // toString
    public String toString() {
        return super.toString() + " {" + studentID + "}";
    }
}
