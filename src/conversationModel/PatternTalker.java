

package conversationModel;

import java.sql.SQLException;
import pimpy.*;
import dataModel.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Alain
 */
public class PatternTalker implements Talker {

    private DebugLogger debugLogger;
    private double initProgress = 0.0;
    private PimpyDatabase pimpyDB;
    private ArrayList<SentencePattern> patterns;
    private boolean initialized = false;
    private Hashtable<String, String> sessionVars;
    private SentenceHistory history = null;
    private boolean patternSeenBefore = false;

    public PatternTalker(PimpyDatabase pimpyDB) {
        this.pimpyDB = pimpyDB;
        sessionVars = new Hashtable<String, String>();
        history = new SentenceHistory();
        this.debugLogger = new DebugLogger();
    }

    public PatternTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars) {
        this.pimpyDB = pimpyDB;
        this.sessionVars = sessionVars;
        history = new SentenceHistory();
        this.debugLogger = new DebugLogger();
    }

    public PatternTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars, SentenceHistory history) {
        this.pimpyDB = pimpyDB;
        this.sessionVars = sessionVars;
        this.history = history;
        this.debugLogger = new DebugLogger();
    }

    public PatternTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars, SentenceHistory history, DebugLogger logger) {
        this.pimpyDB = pimpyDB;
        this.sessionVars = sessionVars;
        this.history = history;
        this.debugLogger = logger;
    }

    public String getAnswer(String submitted) throws TalkerNotInitializedException {
        // We should browse the patterns in some random order...
        // I'm using Collection.shuffle().
        // This returns null if no answer is found (it could happen).
        if (!this.initialized) throw new TalkerNotInitializedException();
        Collections.shuffle(patterns);
        try {
            for (int x = 0; x < patterns.size(); x++) {
                if (patterns.get(x).isMatching(submitted)) {
                    // Fetch the answers of that pattern.
                    ArrayList<String> answers = pimpyDB.getPatternAnswers(patterns.get(x).getId());
                    if (answers.isEmpty()) {
                        // Not supposed to happen...
                        continue;
                    }
                    // Learn context vars (if any).
                    Random rand = new Random();
                    int chosen = rand.nextInt(answers.size());
                    // Check if pattern AND answer have been said before.
                    if (history.isInHistory(patterns.get(x), answers.get(chosen))) {
                        this.patternSeenBefore = true;
                    } else {
                        // Set history accordingly (save the pattern and answer) :
                        this.history.addPattern(patterns.get(x), answers.get(chosen));
                        this.patternSeenBefore = false;
                    }
                    this.debugLogger.logMessage(new DebugEvent("PatternTalker - Matched an existing pattern"));
                    return patterns.get(x).formatAnswer(submitted, answers.get(chosen), sessionVars);
                }
            }
        } catch (Exception ex) {
            this.debugLogger.logMessage(new DebugEvent("PatternTalker - ERROR - " + ex.toString(), DebugEvent.ERROR));
            return null;
        }
        this.debugLogger.logMessage(new DebugEvent("PatternTalker - No answer found - Returned null"));
        return null;
    }

    public void initialize() throws TalkerNotInitializedException {
        try {
            // Fetch the patterns and precompile them.
            this.patterns = pimpyDB.getAllPatterns();
            for (int x = 0; x < patterns.size(); x++) {
                try {
                    this.initProgress = x / (double)patterns.size();
                    patterns.get(x).compile();
                } catch (PatternSyntaxException ex) {
                    // This pattern is erratic : ignore it.
                    patterns.remove(x);
                    x--;
                }
            }
            this.initialized = true;
            this.initProgress = 1.0;
        } catch (SQLException ex) {
            initialized = false;
            throw new TalkerNotInitializedException("Database error occured.");
        }
    }

    public boolean hasPatterns() {
        if (this.patterns != null && this.patterns.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public double getInitProgress() {
        return initProgress;
    }

    /**
     * @param initProgress the initProgress to set
     */
    public void setInitProgress(double initProgress) {
        this.initProgress = initProgress;
    }

    /**
     * @return the patterns
     */
    public ArrayList<SentencePattern> getPatterns() {
        return patterns;
    }

    /**
     * @param patterns the patterns to set
     */
    public void setPatterns(ArrayList<SentencePattern> patterns) {
        this.patterns = patterns;
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the sessionVars
     */
    public Hashtable<String, String> getSessionVars() {
        return sessionVars;
    }

    /**
     * @param sessionVars the sessionVars to set
     */
    public void setSessionVars(Hashtable<String, String> sessionVars) {
        this.sessionVars = sessionVars;
    }

    /**
     * @return the history
     */
    public SentenceHistory getHistory() {
        return history;
    }

    /**
     * @param history the history to set
     */
    public void setHistory(SentenceHistory history) {
        this.history = history;
    }

    /**
     * @return the patternSeenBefore
     */
    public boolean isPatternSeenBefore() {
        return patternSeenBefore;
    }

    /**
     * @return the debugLogger
     */
    public DebugLogger getDebugLogger() {
        return debugLogger;
    }

    /**
     * @param debugLogger the debugLogger to set
     */
    public void setDebugLogger(DebugLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

}
