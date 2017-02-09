
package conversationModel;

import java.sql.SQLException;
import java.util.*;
import pimpy.*;

/**
 *
 * @author Alain
 */
public class PimpyAcanthusTalker implements Talker {

    // This class is the first imagined conversation decisionnal system.
    // Best used through its interface Talker, Acanthus uses other talkers to
    // find a suited answer.

    private DebugLogger debugLogger;
    private SentenceHistory history;
    private Hashtable<String, String> sessionVars;
    private PimpyDatabase pimpyDB;
    private PatternTalker patternTalker;
    private SentenceTalker sentenceTalker;
    private TrivialTalker trivialTalker;
    private boolean initialized = false;
    // To store the answers of kind "I don't understand" :
    private String [] notUnderstood = null;
    // Probability to return a trivial answer whatsoever :
    private double returnTrivialAnswerProbability = 0.05;
    // Sentence closeness beyond which we try not to use that sentence :
    private int closenessThreshhold = 5;
    // High closeness treshhold.
    private int highClosenessThreshhold = 10;
    // Probability to return a trivial if there's no sentence answer but there's a pattern.
    private double returnTrivialIfNoSentenceButPattern = 0.58;
    // Probability to use a notUnderstood if over high closeness treshhold.
    private double useNotUnderstoodProbability = 0.76;
    // Probavility to use a trviail if closeness is beyond low treshhold :
    private double returnTrivialIfHighCloseness = 0.685;

    public PimpyAcanthusTalker(PimpyDatabase pimpyDB) {
        this.history = new SentenceHistory(pimpyDB);
        this.pimpyDB = pimpyDB;
    }

    public String getAnswer(String submitted) throws TalkerNotInitializedException {
        // This magic method is what makes a talker out of Acanthus. Cool.
        if (!this.initialized) throw new TalkerNotInitializedException("Talker must be initalized first.");
        String pat = this.patternTalker.getAnswer(submitted);
        if (pat != null && !this.patternTalker.isPatternSeenBefore()) {
            // Pattern not seen before, we stop there.
            return pat;
        }
        // There is a probability to return a trivial answer whatsoever.
        Random rand = new Random();
        if (rand.nextDouble() <= this.returnTrivialAnswerProbability && this.trivialTalker.isUsable()) {
            return this.trivialTalker.getAnswer(submitted);
        }
        String sent = this.sentenceTalker.getAnswer(submitted);
        if (sent == null) {
            // We have no sentence answer available.
            if (pat != null && rand.nextDouble() <= this.returnTrivialIfNoSentenceButPattern) {
                // return pattern anyway.
                return pat;
            }
            return this.trivialTalker.getAnswer(submitted);
        }
        // We have a sentence answer. Let's see if closeness is high.
        this.debugLogger.logMessage(new DebugEvent("Sentence Talker had a closeness of " + this.sentenceTalker.getCloseness(), DebugEvent.INFO));
        if (this.sentenceTalker.getCloseness() >= this.highClosenessThreshhold) {
            // Closeness is very high.
            if (this.notUnderstood != null && this.notUnderstood.length > 1 && rand.nextDouble() <= this.useNotUnderstoodProbability) {
                this.debugLogger.logMessage(new DebugEvent("Closeness deemed too high by Acanthus, using notUnderstood list", DebugEvent.INFO));
                return this.notUnderstood[rand.nextInt(this.notUnderstood.length)];
            }
        }
        if (this.sentenceTalker.getCloseness() >= this.closenessThreshhold) {
            // Still quite high...
            if (trivialTalker.isUsable() && rand.nextDouble() <= this.returnTrivialIfHighCloseness) {
                this.debugLogger.logMessage(new DebugEvent("High closeness, Acanthus used a trivial answer"));
                return this.trivialTalker.getAnswer(submitted);
            }
        }
        // Finally return sentence answer :
        return sent;
    }

    public void initialize() throws TalkerNotInitializedException {
        this.initialized = false;
        this.setSessionVars(new Hashtable<String, String>());
        this.patternTalker = new PatternTalker(pimpyDB, sessionVars);
        this.patternTalker.setDebugLogger(debugLogger);
        this.sentenceTalker = new SentenceTalker(pimpyDB, sessionVars, this.history, debugLogger);
        this.trivialTalker = new TrivialTalker(pimpyDB, this.history, debugLogger);
        this.trivialTalker.initialize();
        this.notUnderstood = new String[1];
        try {
            this.notUnderstood = pimpyDB.getAnswersByType(5).toArray(this.notUnderstood);
        } catch (SQLException ex) {
            throw new TalkerNotInitializedException("Could not initialize 'not understood' premade answers.\n" + ex.toString());
        }
        // If not understood reply count is less than 3, don't even bother using it.
        if (notUnderstood == null || notUnderstood.length < 3) {
            notUnderstood = null;
        }
        this.patternTalker.initialize();
        this.sentenceTalker.initialize();
        this.initialized = true;
    }

    public double getInitProgress() {
        try {
            return (this.patternTalker.getInitProgress() + this.sentenceTalker.getInitProgress() + this.trivialTalker.getInitProgress()) / 3.0;
        } catch (NullPointerException ex) {
            return 0.0;
        }
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
     * @return the pimpyDB
     */
    public PimpyDatabase getPimpyDB() {
        return pimpyDB;
    }

    /**
     * @param pimpyDB the pimpyDB to set
     */
    public void setPimpyDB(PimpyDatabase pimpyDB) {
        this.pimpyDB = pimpyDB;
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
     * @return the patternTalker
     */
    public PatternTalker getPatternTalker() {
        return patternTalker;
    }

    /**
     * @return the sentenceTalker
     */
    public SentenceTalker getSentenceTalker() {
        return sentenceTalker;
    }

    /**
     * @return the trivialTalker
     */
    public TrivialTalker getTrivialTalker() {
        return trivialTalker;
    }

    /**
     * @return the notUnderstood
     */
    public String[] getNotUnderstood() {
        return notUnderstood;
    }

    /**
     * @param notUnderstood the notUnderstood to set
     */
    public void setNotUnderstood(String[] notUnderstood) {
        this.notUnderstood = notUnderstood;
    }

    /**
     * @return the returnTrivialAnswerProbability
     */
    public double getReturnTrivialAnswerProbability() {
        return returnTrivialAnswerProbability;
    }

    /**
     * @param returnTrivialAnswerProbability the returnTrivialAnswerProbability to set
     */
    public void setReturnTrivialAnswerProbability(double returnTrivialAnswerProbability) {
        this.returnTrivialAnswerProbability = returnTrivialAnswerProbability;
    }

    /**
     * @return the closenessThreshhold
     */
    public int getClosenessThreshhold() {
        return closenessThreshhold;
    }

    /**
     * @param closenessThreshhold the closenessThreshhold to set
     */
    public void setClosenessThreshhold(int closenessThreshhold) {
        this.closenessThreshhold = closenessThreshhold;
    }

    /**
     * @return the highClosenessThreshhold
     */
    public int getHighClosenessThreshhold() {
        return highClosenessThreshhold;
    }

    /**
     * @param highClosenessThreshhold the highClosenessThreshhold to set
     */
    public void setHighClosenessThreshhold(int highClosenessThreshhold) {
        this.highClosenessThreshhold = highClosenessThreshhold;
    }

    /**
     * @return the useNotUnderstoodProbability
     */
    public double getUseNotUnderstoodProbability() {
        return useNotUnderstoodProbability;
    }

    /**
     * @param useNotUnderstoodProbability the useNotUnderstoodProbability to set
     */
    public void setUseNotUnderstoodProbability(double useNotUnderstoodProbability) {
        this.useNotUnderstoodProbability = useNotUnderstoodProbability;
    }

    /**
     * @return the returnTrivialIfHighCloseness
     */
    public double getReturnTrivialIfHighCloseness() {
        return returnTrivialIfHighCloseness;
    }

    /**
     * @param returnTrivialIfHighCloseness the returnTrivialIfHighCloseness to set
     */
    public void setReturnTrivialIfHighCloseness(double returnTrivialIfHighCloseness) {
        this.returnTrivialIfHighCloseness = returnTrivialIfHighCloseness;
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
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
