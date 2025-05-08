package src.person;
import src.date.OCCCDate;

public class RegisteredPerson extends Person {
    private String govID;

    // constructors
    public RegisteredPerson(String firstName, String lastName, OCCCDate dob, String govID) {
        super(firstName, lastName, dob);
        this.govID = govID;
    }
    public RegisteredPerson(Person p, String govID) {
        super(p);
        this.govID = govID;
    }
    public RegisteredPerson(RegisteredPerson p) { // copy constructor
        super(p);
        this.govID = p.govID;
    }

    public RegisteredPerson(String firstName, String lastName, String govID) { // this constructor is not mentioned in the specifications but is tested for in your TestPerson.class file so I included it here
        super(firstName, lastName);
        this.govID = govID;
    }
    // getters
    public String getGovID() {
        return govID;
    }

    // equals
    public boolean equals(RegisteredPerson p) {
        return super.equals(p) && govID.equals(p.govID);
    }
    public boolean equals(Person p) {
        return super.equals(p);
    }

    // toString
    public String toString() {
        return super.toString() + " [" + govID + "]";
    }
}
