

package pimpy;

import java.util.*;
import java.text.DateFormat;

/**
 *
 * @author Alain
 */
public class DebugEvent {

    public static final int ERROR = 2;
    public static final int WARNING = 1;
    public static final int INFO = 0;
    private Date eventDate;
    private String description;
    private int type;

    public DebugEvent(String description) {
        this.eventDate = new Date();
        this.description = description;
        this.type = DebugEvent.INFO;
    }

    public DebugEvent(String description, int type) {
        this(description);
        this.type = type;
        this.eventDate = new Date();
    }

    /**
     * @return the eventDate
     */
    public Date getEventDate() {
        return eventDate;
    }

    /**
     * @param eventDate the eventDate to set
     */
    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    public String getTypeString() {
        String evtType = "";
        switch (this.type) {
            case DebugEvent.ERROR:
                evtType = "ERROR";
                break;
            case DebugEvent.WARNING:
                evtType = "WARNING";
                break;
            case DebugEvent.INFO:
                evtType = "INFO";
        }
        return evtType;
    }

    @Override
    public String toString() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        return (df.format(this.eventDate) + " - " + this.getTypeString() + " - " + this.description);
    }

}
