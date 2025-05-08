package src.date;

/**
 * Custom exception class for invalid dates in OCCCDate
 * Extends IllegalArgumentException so it is unchecked
 */
public class InvalidOCCCDateException extends IllegalArgumentException {
    public InvalidOCCCDateException() {
        super("Illegal date format!");
    }
    
    public InvalidOCCCDateException(String message) {
        super(message);
    }
    
    public InvalidOCCCDateException(int day, int month, int year) {
        super("Illegal expression for $OCCCDATE: " + month + "/" + day + "/" + year);
    }
}