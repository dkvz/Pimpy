package conversationModel;

import java.sql.SQLException;
import pimpy.*;
import java.util.*;

/**
 *
 * @author Alain
 */
public class TrivialTalker implements Talker {

    // The trivial talker chooses answers in arrays at random.
    // It does one distinction by identifying questions.
    // otherwise it is pretty trivial...

    // If answer has already been said, we can try to find something else.
    // Let's say if we have seen less than 60% of total possible answers.
    // Also the variable name is pretty stupid.
    // Looks like I was kinda drunk when I created this "functionnality".
    private double tooMuchHasBeenSaidTreshhold = 0.6;
    // Stop rerolling randoms after that much attempts :
    private int reRollLimit = 100;
    // These should not be modified during runtime :
    private String[] trivialAnswers;
    private String[] questionRebounds;
    // Probability of rebounding if we got a question :
    // (that would be answering with another question but it
    // depends on how the knowledge base has been built).
    private double reboundProbability = 0.76;
    private double initProgress;
    private PimpyDatabase pimpyDB = null;
    private SentenceHistory history = null;
    private DebugLogger debugLogger;

    public TrivialTalker() {
        this.pimpyDB = null;
        this.history = new SentenceHistory();
        this.debugLogger = new DebugLogger();
    }

    public TrivialTalker(PimpyDatabase pimpyDB) {
        this.pimpyDB = pimpyDB;
        this.history = new SentenceHistory();
        this.debugLogger = new DebugLogger();
    }

    public TrivialTalker(PimpyDatabase pimpyDB, SentenceHistory history) {
        this.pimpyDB = pimpyDB;
        this.history = history;
        this.debugLogger = new DebugLogger();
    }

    public TrivialTalker(PimpyDatabase pimpyDB, SentenceHistory history, DebugLogger logger) {
        this.pimpyDB = pimpyDB;
        this.history = history;
        this.debugLogger = logger;
    }

    public boolean isUsable() {
        // returns true if talker has at least one answer of each kind.
        if (this.questionRebounds.length > 0 && this.trivialAnswers.length > 0) {
            return true;
        }
        return false;
    }

    public String getAnswer(String submitted) throws TalkerNotInitializedException {
        // We need to determine if it's a question.
        // In fact that's the only thing we need to know about what was submitted...
        Random rand = new Random();
        if (submitted.trim().endsWith("?")) {
            // It's a question.
            if (questionRebounds != null && questionRebounds.length > 0 && rand.nextDouble() <= reboundProbability) {
                int ind = this.findAnswer(this.questionRebounds, 3);
                if (ind >= 0) {
                    this.history.addTrivial(3, ind);
                    return this.questionRebounds[ind];
                }
            }
        }
        // Let's throw one of our trivial answers.
        if (trivialAnswers != null && trivialAnswers.length > 0) {
            int ind = this.findAnswer(this.trivialAnswers, 4);
            if (ind >= 0) {
                this.history.addTrivial(4, ind);
                return this.trivialAnswers[ind];
            }
        }
        // If we found nothing :
        return null;
    }

    protected int findAnswer(String[] intoArray, int type) {
        Random rand = new Random();
        int trivCount = history.getTrivialCount(type);
        double ratio = 1.0;
        if (trivCount != 0) {
            ratio = (double)intoArray.length / trivCount;
        }
        int ind = rand.nextInt(intoArray.length);
        if (ratio >= this.tooMuchHasBeenSaidTreshhold) {
            return ind;
        } else {
            int count = 0;
            while(count < this.reRollLimit) {
                if (!history.isInHistory(type, ind, true)) {
                    // That sentence hasn't already been said.
                    return ind;
                }
                ind = rand.nextInt(intoArray.length);
                count++;
            }
        }
        return -1;
    }

    public void initialize() throws TalkerNotInitializedException {
        // Let's prefetch the answers.
        this.trivialAnswers = new String[1];
        this.questionRebounds = new String[1];
        if (pimpyDB != null) {
            try {
                initProgress = 0.0;
                this.trivialAnswers = pimpyDB.getAnswersByType(4).toArray(trivialAnswers);
                initProgress = 0.5;
                this.questionRebounds = pimpyDB.getAnswersByType(3).toArray(questionRebounds);
                initProgress = 1.0;
            } catch (SQLException ex) {
                throw new TalkerNotInitializedException("SQL error, knowledge base failure");
            }
        } else {
            this.setTrivialAnswers(null);
            this.setQuestionRebounds(null);
            this.initProgress = 1.0;
        }
    }

    public double getInitProgress() {
        return initProgress;
    }

    /**
     * @return the trivialAnswers
     */
    public String[] getTrivialAnswers() {
        return trivialAnswers;
    }

    /**
     * @param trivialAnswers the trivialAnswers to set
     */
    public void setTrivialAnswers(String[] trivialAnswers) {
        this.setTrivialAnswers(trivialAnswers);
    }

    /**
     * @return the questionRebounds
     */
    public String[] getQuestionRebounds() {
        return questionRebounds;
    }

    /**
     * @return the reboundProbability
     */
    public double getReboundProbability() {
        return reboundProbability;
    }

    /**
     * @param reboundProbability the reboundProbability to set
     */
    public void setReboundProbability(double reboundProbability) {
        this.reboundProbability = reboundProbability;
    }

    /**
     * @param questionRebounds the questionRebounds to set
     */
    public void setQuestionRebounds(String[] questionRebounds) {
        this.questionRebounds = questionRebounds;
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
     * @return the tooMuchHasBeenSaidTreshhold
     */
    public double getTooMuchHasBeenSaidTreshhold() {
        return tooMuchHasBeenSaidTreshhold;
    }

    /**
     * @param tooMuchHasBeenSaidTreshhold the tooMuchHasBeenSaidTreshhold to set
     */
    public void setTooMuchHasBeenSaidTreshhold(double tooMuchHasBeenSaidTreshhold) {
        this.tooMuchHasBeenSaidTreshhold = tooMuchHasBeenSaidTreshhold;
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
