
package conversationModel;

import dataModel.*;
import java.sql.SQLException;
import java.util.*;
import pimpy.*;

public class SentenceHistory {

    public static final int PATTERN_TYPE = 1;
    public static final int SENTENCE_TYPE = 2;
    public static final int TRIVIAL_TYPE = 3;
    // Default array size. We're gathering history answers
    // by type so we can immediately know if an answer
    // of a particular type has been said before.
    public static final int DEFAULT_SIZE = 50;
    // There's the sentences returned by sentence talkers
    // We know everything about those, including database
    // Id's.
    private HashMap<Integer, Sentence>[] history;
    // And the sentences returned by trivial talkers. We
    // only know answers for those.
    private HashMap<Integer, Object>[] seenTrivials;
    // Pattern ids already seen :
    private HashMap<Integer, String> seenPatterns;
    private final PimpyDatabase pimpyDB = null;
    private int lastEntry = -1;
    private int lastEntryCode = 0;
    private int lastEntryType = 0;

    public SentenceHistory(PimpyDatabase pimpyDB) {
        try {
            int size = pimpyDB.getMaxSentenceTypeId();
            history = new HashMap[size + 1];
            seenTrivials = new HashMap[size + 1];
        } catch (SQLException ex) {
            history = new HashMap[SentenceHistory.DEFAULT_SIZE];
            seenTrivials = new HashMap[SentenceHistory.DEFAULT_SIZE];
        }
        initialize();
     }

    public SentenceHistory() {
        history = new HashMap[SentenceHistory.DEFAULT_SIZE];
        seenTrivials = new HashMap[SentenceHistory.DEFAULT_SIZE];
        initialize();
    }

    private void initialize() {
        for (int x = 0; x < history.length; x++) {
            history[x] = new HashMap<Integer, Sentence>();
        }
        for (int x = 0; x < seenTrivials.length; x++) {
            seenTrivials[x] = new HashMap<Integer, Object>();
        }
        this.seenPatterns = new HashMap<Integer, String>();
        lastEntry = -1;
    }

    public void addSentence(Sentence sentence) {
        this.lastEntry = sentence.getId();
        this.lastEntryType = SentenceHistory.SENTENCE_TYPE;
        this.lastEntryCode = sentence.getType();
        this.history[sentence.getType()].put(sentence.getId(), sentence);
    }

    public void addTrivial(int type, int index) {
        this.lastEntry = index;
        this.lastEntryType = SentenceHistory.TRIVIAL_TYPE;
        this.lastEntryCode = type;
        this.seenTrivials[type].put(index, null);
    }

    public void addPattern(SentencePattern pattern, String answer) {
        this.lastEntry = pattern.getId();
        this.lastEntryType = SentenceHistory.PATTERN_TYPE;
        this.seenPatterns.put(pattern.getId(), answer);
    }

    public boolean isInHistory(int type, int id, boolean trivial) {
        if (!trivial) {
            if (history[type].containsKey(id)) {
                return true;
            }
        } else {
            HashMap<Integer, Object> hash = seenTrivials[type];
            if (hash.containsKey(id)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInHistory(Sentence sentence) {
        return isInHistory(sentence.getType(), sentence.getId(), false);
    }

    public boolean isInHistory(SentencePattern pattern, String answer) {
        if (this.seenPatterns.containsKey(pattern.getId())) {
            if (this.seenPatterns.get(pattern.getId()).equals(answer)) {
                return true;
            }
        }
        return false;
    }

    public void deleteLastEntry() {
        // Obviously, we do nothing if there's no last entry.
        if (lastEntry >= 0) {
            switch (this.lastEntryType) {
                case SentenceHistory.PATTERN_TYPE:
                    this.seenPatterns.remove(this.lastEntry);
                    break;
                case SentenceHistory.SENTENCE_TYPE:
                    this.history[this.lastEntryCode].remove(this.lastEntry);
                    break;
                case SentenceHistory.TRIVIAL_TYPE:
                    this.seenTrivials[this.lastEntryCode].remove(this.lastEntry);
            }
            lastEntry = -1;
        }
    }

    public int getTrivialCount(int type) {
        return this.seenTrivials[type].size();
    }

    public Sentence lookForSentence(int type, int id) {
        HashMap<Integer, Sentence> srch = history[type];
        return srch.get(id);
    }

    /**
     * @return the lastEntry
     */
    public int getLastEntry() {
        return lastEntry;
    }

    /**
     * @return the seenPatterns
     */
    public HashMap<Integer, String> getSeenPatterns() {
        return seenPatterns;
    }

    /**
     * @return the lastEntryType
     */
    public int getLastEntryType() {
        return lastEntryType;
    }

    /**
     * @return the lastEntryCode
     */
    public int getLastEntryCode() {
        return lastEntryCode;
    }

}
