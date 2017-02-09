package pimpy;

import conversationModel.*;
import java.sql.SQLException;
import dataModel.*;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author Alain
 */
public class PimpyController {
    
    // Application controller and property holder.
    
    protected PimpyDatabase pimpyDB = null;
    private Talker mainTalker = null;
    private boolean TalkerInitialized = false;
    private DebugLogger debugLogger = new DebugLogger();
    private String talkerName = "";

    public PimpyController() {
        pimpyDB = new PimpyDatabase(PimpyDatabase.SQLITE_MODE);
    }

    public PimpyController(int databaseMode) {
        pimpyDB = new PimpyDatabase(databaseMode);
    }

    public String getAnswer(String submitted) throws TalkerNotInitializedException {
        if (this.TalkerInitialized) {
            return mainTalker.getAnswer(submitted);
        } else {
            throw new TalkerNotInitializedException("Talker said it's not initalized properly.");
        }
    }

    public void initializeTalker() throws Exception {
        mainTalker = new PimpyAcanthusTalker(this.pimpyDB);
        mainTalker.setDebugLogger(debugLogger);
        this.talkerName = pimpyDB.getScreenName();
        debugLogger.logMessage(new DebugEvent("Found talker screen name : " + this.talkerName));
        try {
            mainTalker.initialize();
            this.TalkerInitialized = true;
        } catch (TalkerNotInitializedException ex) {
            this.TalkerInitialized = false;
            throw new Exception("Talker initalization failure : " + ex.toString());
        }
    }

    public boolean checkExistenceByAnswer(String answer) throws SQLException {
        // Check the existence of a candidate to learning by looking if the answer
        // already exists.
        return pimpyDB.checkSentenceExistence(answer);
    }

    public void learnNewSentence(String sentence, String answer, String contextAnswer, int type) throws SQLException {
        // Allow the learning of new sentences.
        if (answer.isEmpty()) answer = "...";
        Sentence subm = new Sentence(sentence, answer, contextAnswer, type);
        if (subm.getWordCount() < 1) {
            throw new SQLException("Sentence is empty.");
        } else {
            pimpyDB.persistSentence(subm);
        }
    }

    public SentencePattern[] getAllPatterns() throws SQLException {
        ArrayList<SentencePattern> patList = pimpyDB.getAllPatterns();
        SentencePattern [] patArray = new SentencePattern[patList.size()];
        patList.toArray(patArray);
        return patArray;
    }

    public void learnNewPattern(SentencePattern pattern, String answer, boolean caseInsensitive) throws SQLException, PatternSyntaxException {
        // Test if pattern is parsable.
        pattern.compile();
        // Looks like it is. So let's persist.
        pimpyDB.persistNewPattern(pattern, answer, caseInsensitive);
    }

    public void addAnswerToPattern(int patternId, String answer) throws SQLException {
        if (!pimpyDB.checkPatternExistenceById(patternId)) {
            throw new SQLException("The pattern to update doesn't exist in the first place.");
        }
        pimpyDB.persistNewPatternAnswer(patternId, answer);
    }

    public void close() {
        if (pimpyDB.isConnected()) try {
            pimpyDB.disconnect();
        } catch (SQLException ex) {
            //
        }
        System.exit(0);
    }

    public KnowledgeBaseInfo getKnowledgeBaseInfo() throws SQLException {
        return pimpyDB.getFirstBaseInfo();
    }

    public int getWordCount() throws SQLException {
        return pimpyDB.getWordCount();
    }

    public int getSentenceCount() throws SQLException {
        return pimpyDB.getSentenceCount();
    }

    public int getPatternCount() throws SQLException {
        return pimpyDB.getPatternCount();
    }

    /**
     * @return the pimpyDB
     */
    protected PimpyDatabase getPimpyDB() {
        return pimpyDB;
    }

    /**
     * @return the mainTalker
     */
    public Talker getMainTalker() {
        return mainTalker;
    }

    /**
     * @return the TalkerInitialized
     */
    public boolean isTalkerInitialized() {
        return TalkerInitialized;
    }

    /**
     * @param TalkerInitialized the TalkerInitialized to set
     */
    public void setTalkerInitialized(boolean TalkerInitialized) {
        this.TalkerInitialized = TalkerInitialized;
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

    /**
     * @return the talkerName
     */
    public String getTalkerName() {
        return talkerName;
    }

    /**
     * @param talkerName the talkerName to set
     */
    public void setTalkerName(String talkerName) {
        this.talkerName = talkerName;
    }
  
}
