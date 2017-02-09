package pimpy;

import java.util.*;

/**
 *
 * @author Alain
 */
public class DebugLogger {

    private ArrayList<DebugEvent> events = new ArrayList<DebugEvent>();
    private DebugAppender appender = null;
    private boolean logToAppender = false;
    // Not implemented yet...
    private boolean logToFile = false;
    private boolean logToList = false;
    private boolean logToStdOut = false;
    private boolean logDates = true;
    // The variable that controls the debugging or not.
    private boolean debugMode = false;

    public void purgeList() {
        this.events.clear();
    }

    /**
     * Logs the message and checks the boolean fields to see what to do with it
     * @param evt
     */
    public void logMessage(DebugEvent evt) {
        if (isDebugMode()) {
            if (this.logToAppender && this.appender != null) {
                if (logDates) {
                    this.appender.append(evt.toString() + "\n");
                } else {
                    this.appender.append(evt.getTypeString() + " - " + evt.getDescription() + "\n");
                }
            }
            if (this.logToList) {
                this.events.add(evt);
            }
            if (this.logToStdOut) {
                if (logDates) {
                    System.out.println(evt.toString());
                } else {
                    System.out.println(evt.getTypeString() + " - " + evt.getDescription());
                }
            }
        }
    }

    public String dumpList() {
        String ret = "";
        for (int i = 0; i < this.events.size(); i++) {
            if (this.logDates) {
                ret = ret.concat(events.get(i).toString() + "\n");
            } else {
                ret = ret.concat(events.get(i).getTypeString() + " - " + events.get(i).getDescription() + "\n");
            }
        }
        return ret;
    }

    /**
     * @return the events
     */
    public ArrayList<DebugEvent> getEvents() {
        return events;
    }

    /**
     * @return the appender
     */
    public DebugAppender getAppender() {
        return appender;
    }

    /**
     * @param appender the appender to set
     */
    public void setAppender(DebugAppender appender) {
        this.appender = appender;
    }

    /**
     * @return the logToAppender
     */
    public boolean isLogToAppender() {
        return logToAppender;
    }

    /**
     * @param logToAppender the logToAppender to set
     */
    public void setLogToAppender(boolean logToAppender) {
        this.logToAppender = logToAppender;
    }

    /**
     * @return the logToFile
     */
    public boolean isLogToFile() {
        return logToFile;
    }

    /**
     * @param logToFile the logToFile to set
     */
    public void setLogToFile(boolean logToFile) {
        this.logToFile = logToFile;
    }

    /**
     * @return the logToList
     */
    public boolean isLogToList() {
        return logToList;
    }

    /**
     * @param logToList the logToList to set
     */
    public void setLogToList(boolean logToList) {
        this.logToList = logToList;
    }

    /**
     * @return the logDates
     */
    public boolean isLogDates() {
        return logDates;
    }

    /**
     * @param logDates the logDates to set
     */
    public void setLogDates(boolean logDates) {
        this.logDates = logDates;
    }

    /**
     * @return the debugMode
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * @param debugMode the debugMode to set
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * @return the logToStdOut
     */
    public boolean isLogToStdOut() {
        return logToStdOut;
    }

    /**
     * @param logToStdOut the logToStdOut to set
     */
    public void setLogToStdOut(boolean logToStdOut) {
        this.logToStdOut = logToStdOut;
    }
}
