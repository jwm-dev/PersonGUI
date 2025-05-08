package src.date;

import java.util.*;
import java.io.Serializable;

public class OCCCDate implements Comparable<OCCCDate>, Serializable {
    private int dayOfMonth, monthOfYear, year;
    private GregorianCalendar gc;
    private boolean dateFormat, dateStyle, dateDayName;

    static final boolean FORMAT_US = true,
                         FORMAT_EURO = false, 
                         STYLE_NUMBERS = true, 
                         STYLE_NAMES = false, 
                         SHOW_DAY_NAME = true, 
                         HIDE_DAY_NAME = false;
    // constructors
    public OCCCDate() {
        gc = new GregorianCalendar();
        dayOfMonth = gc.get(Calendar.DAY_OF_MONTH);
        monthOfYear = gc.get(Calendar.MONTH) + 1;
        year = gc.get(Calendar.YEAR);
        dateFormat = FORMAT_US;
        dateStyle = STYLE_NUMBERS;
        dateDayName = SHOW_DAY_NAME;
    }
    
    public OCCCDate(int day, int month, int year) {
        // Create the GregorianCalendar object with the given day, month, and year
        gc = new GregorianCalendar(year, month - 1, day);
        
        // Extract the day, month, and year from the GregorianCalendar object
        int gcDay = gc.get(Calendar.DAY_OF_MONTH);
        int gcMonth = gc.get(Calendar.MONTH) + 1;
        int gcYear = gc.get(Calendar.YEAR);
        
        // Compare what was given with what the GregorianCalendar object provides
        if (day != gcDay || month != gcMonth || year != gcYear) {
            throw new InvalidOCCCDateException(day, month, year);
        }
        
        // Set the instance variables
        dayOfMonth = day;
        monthOfYear = month;
        this.year = year;
        dateFormat = FORMAT_US;
        dateStyle = STYLE_NUMBERS;
        dateDayName = SHOW_DAY_NAME;
    }
    
    public OCCCDate(GregorianCalendar gc) {
        this.gc = gc;
        dayOfMonth = gc.get(Calendar.DAY_OF_MONTH);
        monthOfYear = gc.get(Calendar.MONTH) + 1;
        year = gc.get(Calendar.YEAR);
        dateFormat = FORMAT_US;
        dateStyle = STYLE_NUMBERS;
        dateDayName = SHOW_DAY_NAME;
    }
    
    public OCCCDate(OCCCDate d) { // copy constructor
        // Create a new calendar instead of sharing reference
        gc = new GregorianCalendar();
        gc.set(d.year, d.monthOfYear - 1, d.dayOfMonth);
        dayOfMonth = d.dayOfMonth;
        monthOfYear = d.monthOfYear;
        year = d.year;
        dateFormat = d.dateFormat;
        dateStyle = d.dateStyle;
        dateDayName = d.dateDayName;
    }
    
    /**
     * Helper method to check if a date is valid using GregorianCalendar
     * @param day Day of month
     * @param month Month of year (1-12)
     * @param year Year
     * @return true if date is valid, false otherwise
     */
    public static boolean isValidDate(int day, int month, int year) {
        if (month < 1 || month > 12 || day < 1) {
            return false;
        }
        
        // Create the GregorianCalendar object with the given day, month, and year
        GregorianCalendar tempGc = new GregorianCalendar(year, month - 1, day);
        
        // Extract the day, month, and year from the GregorianCalendar object
        int gcDay = tempGc.get(Calendar.DAY_OF_MONTH);
        int gcMonth = tempGc.get(Calendar.MONTH) + 1;
        int gcYear = tempGc.get(Calendar.YEAR);
        
        // Compare what was given with what the GregorianCalendar object provides
        return (day == gcDay && month == gcMonth && year == gcYear);
    }
    
    // getters
    public int getDayOfMonth() {
        return dayOfMonth;
    }
    
    public String getDayName() {
        return gc.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US);
    }
    
    public int getMonthNumber() {
        return monthOfYear;
    }
    
    public String getMonthName() {
        return gc.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
    }
    
    public int getYear() {
        return year;
    }
    
    // setters
    public void setDateFormat(boolean df) {
        dateFormat = df;
    }
    
    public void setStyleFormat(boolean sf) {
        dateStyle = sf;
    }
    
    public void setDayName(boolean nf) {
        dateDayName = nf;
    }
    
    // difference functions
    /**
     * Calculate the actual difference in years between this date and today's date,
     * taking into account the precise months and days
     * @return The precise difference in years as a double value
     */
    public double getDifferenceInYears() {
        GregorianCalendar current = new GregorianCalendar();
        return calculateYearDifference(this.gc, current);
    }
    
    /**
     * Calculate the actual difference in years between this date and another date,
     * taking into account the precise months and days
     * @param d Another OCCCDate object
     * @return The precise difference in years as a double value
     */
    public double getDifferenceInYears(OCCCDate d) {
        return calculateYearDifference(this.gc, d.gc);
    }
    
    /**
     * Helper method to calculate precise year difference between two calendar objects
     * @param cal1 First calendar
     * @param cal2 Second calendar
     * @return Precise year difference as a double
     */
    private double calculateYearDifference(Calendar cal1, Calendar cal2) {
        // Ensure cal1 is the earlier date
        Calendar earlier, later;
        if (cal1.before(cal2)) {
            earlier = (Calendar) cal1.clone();
            later = (Calendar) cal2.clone();
        } else {
            earlier = (Calendar) cal2.clone();
            later = (Calendar) cal1.clone();
        }
        
        // Get the base year difference
        int yearDiff = later.get(Calendar.YEAR) - earlier.get(Calendar.YEAR);
        
        // Create a calendar for the same day/month but in the later year
        Calendar anniversary = (Calendar) earlier.clone();
        anniversary.set(Calendar.YEAR, later.get(Calendar.YEAR));
        
        // If the anniversary hasn't occurred yet in the later year, adjust the year difference
        if (anniversary.after(later)) {
            yearDiff--;
            // Set anniversary to previous year
            anniversary.set(Calendar.YEAR, later.get(Calendar.YEAR) - 1);
        }
        
        // Calculate the fractional part
        double fraction = 0.0;
        long millisBetween = later.getTimeInMillis() - anniversary.getTimeInMillis();
        
        // For the next anniversary (to calculate the fraction of a year)
        Calendar nextAnniversary = (Calendar) anniversary.clone();
        nextAnniversary.add(Calendar.YEAR, 1);
        long millisInYear = nextAnniversary.getTimeInMillis() - anniversary.getTimeInMillis();
        
        fraction = (double) millisBetween / millisInYear;
        
        return yearDiff + fraction;
    }
    
    /**
     * Calculate the simple difference in years by subtracting the year values
     * @return The absolute difference between the years
     */
    public int getDifferenceOfYears() {
        GregorianCalendar current = new GregorianCalendar();
        return Math.abs(year - current.get(Calendar.YEAR));
    }
    
    /**
     * Calculate the simple difference in years by subtracting the year values
     * @param d Another OCCCDate object
     * @return The absolute difference between the years
     */
    public int getDifferenceOfYears(OCCCDate d) {
        return Math.abs(year - d.year);
    }
    
    // equals
    public boolean equals(OCCCDate dob) {
        return dayOfMonth == dob.dayOfMonth && monthOfYear == dob.monthOfYear && year == dob.year;
    }
    
    // toString
    public String toString() {
        String s = "";
        if (dateDayName == SHOW_DAY_NAME) {
            s += getDayName() + ", ";
        }
        if (dateStyle == STYLE_NUMBERS) {
            if (dateFormat == FORMAT_US) {
                s += monthOfYear + "/" + dayOfMonth + "/" + year;
            } else {
                s += dayOfMonth + "/" + monthOfYear + "/" + year;
            }
        } else {
            if (dateFormat == FORMAT_US) {
                s += getMonthName() + " " + dayOfMonth + ", " + year;
            } else {
                s += dayOfMonth + " " + getMonthName() + " " + year;
            }
        }
        return s;
    }

    /**
     * Implements the Comparable interface to allow for sorting OCCCDate objects
     * @param other The other OCCCDate object to compare with
     * @return negative if this date is earlier, 0 if equal, positive if this date is later
     */
    @Override
    public int compareTo(OCCCDate other) {
        if (other == null) {
            return 1; // This date is greater than null
        }
        
        // Compare years
        if (this.year != other.year) {
            return this.year - other.year;
        }
        
        // If years are equal, compare months
        if (this.monthOfYear != other.monthOfYear) {
            return this.monthOfYear - other.monthOfYear;
        }
        
        // If months are equal, compare days
        return this.dayOfMonth - other.dayOfMonth;
    }
}
