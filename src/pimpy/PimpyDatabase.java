package pimpy;

import dataModel.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Alain
 */
public class PimpyDatabase {

    public static final int SQLITE_MODE = 1;
    private boolean permanentConnection = false;
    private Connection conn = null;
    private int databaseMode;

    public PimpyDatabase(int databaseMode) {
        this.databaseMode = databaseMode;
    }

    public void connect() throws SQLException {
        if (isConnected()) {
            return;
        }
        // This depends on database mode used.
        if (getDatabaseMode() == PimpyDatabase.SQLITE_MODE) {
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:knowledge.db");
                return;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                conn = null;
                return;
            }
        }
        throw new SQLException("Database system not supported.");
    }

    public void disconnect() throws SQLException {
        if (this.conn != null && !this.permanentConnection) {
            this.conn.close();
        }
    }

    public KnowledgeBaseInfo getFirstBaseInfo() throws SQLException {
        connect();
        KnowledgeBaseInfo info = new KnowledgeBaseInfo();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT value FROM db_info WHERE key_id = 'author'");
        if (rset.next()) {
            info.setAuthor(rset.getString("value"));
        } else {
            info.setAuthor("No author specified.");
        }
        rset.close();
        rset = stmt.executeQuery("SELECT value FROM db_info WHERE key_id = 'description'");
        if (rset.next()) {
            info.setDescription(rset.getString("value"));
        } else {
            info.setDescription("No description.");
        }
        rset.close();
        rset = stmt.executeQuery("SELECT value FROM db_info WHERE key_id = 'created'");
        if (rset.next()) {
            info.setCreated(Long.parseLong(rset.getString("value")));
        } else {
            info.setCreated(0);
        }
        rset.close();
        disconnect();
        return info;
    }

    public String getScreenName() {
        String ret = "Friederich";
        try {
            connect();
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery("SELECT value FROM db_info WHERE key_id = 'screen_name'");
            if (rset.next()) {
                ret = rset.getString("value");
            }
            rset.close();
            disconnect();
        } catch (SQLException ex) {
            // Do nothing and return default name.
        }
        return ret;
    }

    public int getWordCount() throws SQLException {
        connect();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT count(*) FROM word");
        int ret = 0;
        if (rset.next()) {
            return rset.getInt(1);
        }
        rset.close();
        disconnect();
        return ret;
    }

    public int getPatternCount() throws SQLException {
        connect();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT count(*) FROM pattern");
        int ret = 0;
        if (rset.next()) {
            return rset.getInt(1);
        }
        rset.close();
        disconnect();
        return ret;
    }

    public int getSentenceCount() throws SQLException {
        connect();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT count(*) FROM sentence");
        int ret = 0;
        if (rset.next()) {
            return rset.getInt(1);
        }
        rset.close();
        disconnect();
        return ret;
    }

    public int getMaxSentenceTypeId() throws SQLException {
        connect();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT max(id) AS maxid FROM sentence_type");
        if (rset.next()) {
            return rset.getInt("maxid");
        }
        disconnect();
        return 0;
    }

    public ArrayList<SentencePattern> getAllPatterns() throws SQLException {
        connect();
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery("SELECT id, regex, context_vars, case_s FROM pattern");
        ArrayList<SentencePattern> patList = new ArrayList<SentencePattern>();
        while (rset.next()) {
            SentencePattern pat = new SentencePattern();
            pat.setId(rset.getInt(1));
            pat.setRegex(rset.getString(2));
            pat.setContextVars(rset.getString(3));
            if (rset.getInt(4) == 0) {
                pat.setCaseInsensitive(true);
            } else {
                pat.setCaseInsensitive(false);
            }
            patList.add(pat);
        }
        rset.close();
        disconnect();
        return patList;
    }

    public ArrayList<WordMatchedSentence> findSentencesByWords(ArrayList<Word> wordList) throws SQLException {
        // This method does NOT populate wordLists in found sentences.
        ArrayList<WordMatchedSentence> ret = new ArrayList<WordMatchedSentence>();
        connect();
        // The wordList is supposed to already have the right ids preset.
        String wordWhere = "(";
        for (int x = 0; x < wordList.size(); x++) {
            // This will concat word_ids more than once if that id is present
            // multiple times in wordList. This is no trouble for the query
            // result but might be a performance issue.
            wordWhere = wordWhere.concat("sentence_words.word_id = " + wordList.get(x).getId());
            if (x != wordList.size() - 1) {
                wordWhere = wordWhere.concat(" OR ");
            }
        }
        wordWhere = wordWhere.concat(")");
        // Carefull with limit... It's not all database compliant...
        String sql = "SELECT count(sentence_words.sentence_id) AS match_count, sentence_words.sentence_id, " +
                "sentence.word_count, sentence.question, sentence.answer, sentence.answer_context, sentence.type " +
                "FROM sentence_words, sentence where sentence_words.sentence_id = sentence.id AND " +
                wordWhere + " GROUP BY sentence_words.sentence_id ORDER BY match_count DESC LIMIT 80";
        Statement stmt = conn.createStatement();
        ResultSet rset = stmt.executeQuery(sql);
        while (rset.next()) {
            WordMatchedSentence sent = new WordMatchedSentence();
            sent.setMatchCount(rset.getInt(1));
            sent.setId(rset.getInt(2));
            sent.setWordCount(rset.getInt(3));
            if (rset.getInt(4) == 0) {
                sent.setQuestion(false);
            } else {
                sent.setQuestion(true);
            }
            sent.setAnswer(rset.getString(5));
            sent.setAnswerContext(rset.getString(6));
            sent.setType(rset.getInt(7));
            ret.add(sent);
        }
        rset.close();
        disconnect();
        return ret;
    }

    public void fetchWordList(Sentence sentence) throws SQLException {
        // Fetches the wordList corresponding to given sentence.
        connect();
        PreparedStatement stmt = conn.prepareStatement("SELECT word.id, word.word FROM word, sentence_words WHERE sentence_words.sentence_id = ? AND word.id = sentence_words.word_id ORDER BY sentence_words.position ASC");
        stmt.setInt(1, sentence.getId());
        ResultSet rset = stmt.executeQuery();
        sentence.setWordList(new ArrayList<Word>());
        while (rset.next()) {
            sentence.getWordList().add(new Word(rset.getInt(1), rset.getString(2)));
        }
        rset.close();
        disconnect();
    }

    public void markExistingWords(ArrayList<Word> wordList, boolean fuzzyFinder) throws SQLException {
        // This goes through the word list and check for their
        // existence in database. Existing words will have
        // their id set.
        connect();
        Random rand = new Random();
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM word WHERE word = ?");
        for (int x = 0; x < wordList.size(); x++) {
            stmt.setString(1, wordList.get(x).getWord());
            ResultSet rset = stmt.executeQuery();
            if (rset.next()) {
                wordList.get(x).setId(rset.getInt(1));
            } else {
                if (fuzzyFinder) {
                    // We try to match a word that is close to the one we look for.
                    // If there's a lot of matches, we need to randomize the choices.
                    String orderBy = "ORDER BY id ASC";
                    if (rand.nextDouble() > 0.5) {
                        orderBy = "ORDER BY id DESC";
                    }
                    // Carefull with limit again...
                    PreparedStatement fuzz = conn.prepareStatement("SELECT id FROM word WHERE word LIKE ? " + orderBy + " LIMIT 30");
                    fuzz.setString(1, wordList.get(x).getWord().concat("_"));
                    ResultSet fuset = fuzz.executeQuery();
                    ArrayList<Integer> ids = new ArrayList<Integer>();
                    while (fuset.next()) {
                        ids.add(fuset.getInt(1));
                    }
                    fuset.close();
                    if (ids.size() > 0) {
                        wordList.get(x).setId(ids.get(rand.nextInt(ids.size())));
                    } else {
                        // Try to find a word that has one less letter.
                        // Not appliable to words with less thant 5 letters.
                        if (wordList.get(x).getWord().length() > 4) {
                            stmt.setString(1, wordList.get(x).getWord().substring(0, wordList.get(x).getWord().length() - 1));
                            ResultSet res = stmt.executeQuery();
                            if (res.next()) {
                                wordList.get(x).setId(res.getInt(1));
                            }
                            res.close();
                        }
                    }
                }
            }
            rset.close();
        }
        disconnect();
    }

    public ArrayList<String> getAnswersByType(int type) throws SQLException {
        connect();
        ArrayList<String> ret = new ArrayList<String>();
        PreparedStatement stmt = conn.prepareStatement("SELECT answer FROM sentence WHERE type = ?");
        stmt.setInt(1, type);
        ResultSet rset = stmt.executeQuery();
        while (rset.next()) {
            ret.add(rset.getString(1));
        }
        rset.close();
        disconnect();
        return ret;
    }

    public ArrayList<String> getPatternAnswers(int patternId) throws SQLException {
        ArrayList<String> ret = new ArrayList<String>();
        connect();
        PreparedStatement stmt = conn.prepareStatement("SELECT answer FROM pattern_answer WHERE pattern_id = ?");
        stmt.setInt(1, patternId);
        ResultSet rset = stmt.executeQuery();
        while (rset.next()) {
            ret.add(rset.getString(1));
        }
        rset.close();
        disconnect();
        return ret;
    }

    public boolean checkSentenceExistence(String answer) throws SQLException {
        this.connect();
        PreparedStatement prep = conn.prepareStatement("SELECT id FROM sentence WHERE answer = ?");
        prep.setString(1, answer);
        ResultSet rs = prep.executeQuery();
        boolean exists = false;
        if (rs.next()) {
            exists = true;
        }
        this.disconnect();
        return exists;
    }

    public boolean isConnected() {
        try {
            if (this.conn != null) {
                if (!this.conn.isClosed()) {
                    return true;
                }
            }
        } catch (SQLException ex) {
            // Ouai.
        }
        return false;
    }

    public boolean checkPatternExistenceById(int patternId) throws SQLException {
        connect();
        boolean exists = false;
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM pattern WHERE id = ?");
        stmt.setInt(1, patternId);
        ResultSet rset = stmt.executeQuery();
        if (rset.next()) {
            exists = true;
        }
        rset.close();
        disconnect();
        return exists;
    }

    public void persistNewPattern(SentencePattern pattern, String answer, boolean caseInsensitive) throws SQLException {
        connect();
        conn.setAutoCommit(false);
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO pattern (id, regex, context_vars, case_s) VALUES (NULL, ?, ?, ?)");
        stmt.setString(1, pattern.getRegex());
        stmt.setString(2, pattern.getContextVars());
        if (caseInsensitive) {
            stmt.setInt(3, 0);
        } else {
            stmt.setInt(3, 1);
        }
        stmt.executeUpdate();
        ResultSet rskey = stmt.getGeneratedKeys();
        if (rskey != null && rskey.next()) {
            int lastInsertedId = rskey.getInt(1);
            pattern.setId(lastInsertedId);
            rskey.close();
        }
        stmt = conn.prepareStatement("INSERT INTO pattern_answer (id, pattern_id, answer) VALUES (NULL, ?, ?)");
        stmt.setInt(1, pattern.getId());
        stmt.setString(2, answer);
        stmt.executeUpdate();
        conn.commit();
        conn.setAutoCommit(true);
        disconnect();
    }

    public void persistNewPatternAnswer(int patternId, String answer) throws SQLException {
        connect();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO pattern_answer (id, pattern_id, answer) VALUES (NULL, ?, ?)");
        stmt.setInt(1, patternId);
        stmt.setString(2, answer);
        stmt.executeUpdate();
        disconnect();
    }

    public void persistSentence(Sentence sentence) throws SQLException {
        // We persist the words first.
        // Let's see if some of them already exist.
        connect();
        // Prepare both statements for reuse.
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM word WHERE word.word = ?");
        PreparedStatement wpersist = conn.prepareStatement("INSERT INTO word (id, word) VALUES (NULL, ?)");
        conn.setAutoCommit(false);
        for (int x = 0; x < sentence.getWordCount(); x++) {
            stmt.setString(1, sentence.getWordList().get(x).getWord());
            ResultSet wtest = stmt.executeQuery();
            if (wtest.next()) {
                sentence.getWordList().get(x).setId(wtest.getInt("id"));
            } else {
                // Persist the word.
                wpersist.setString(1, sentence.getWordList().get(x).getWord());
                wpersist.executeUpdate();
                ResultSet rskey = wpersist.getGeneratedKeys();
                if (rskey != null && rskey.next()) {
                    int lastInsertedId = rskey.getInt(1);
                    sentence.getWordList().get(x).setId(lastInsertedId);
                    rskey.close();
                }
            }
            wtest.close();
        }
        // Now persist the sentence object :
        PreparedStatement spersist = conn.prepareStatement("INSERT INTO sentence (id, word_count, question, answer, answer_context, type) VALUES (NULL,?,?,?,?,?)");
        spersist.setInt(1, sentence.getWordCount());
        if (sentence.isQuestion()) {
            spersist.setInt(2, 1);
        } else {
            spersist.setInt(2, 0);
        }
        spersist.setString(3, sentence.getAnswer());
        spersist.setString(4, sentence.getAnswerContext());
//        if (sentence.getType() > 4) {
//            // This type doesn't exist.
//            sentence.setType(0);
//        }
        spersist.setInt(5, sentence.getType());
        spersist.executeUpdate();
        ResultSet rskey = spersist.getGeneratedKeys();
        if (rskey != null && rskey.next()) {
            int lastInsertedId = rskey.getInt(1);
            sentence.setId(lastInsertedId);
            rskey.close();
        }
        // Now we have word and sentence ids properly set.
        // Let's populate the relations table :
        PreparedStatement relations = conn.prepareStatement("INSERT INTO sentence_words (sentence_id, word_id, position) VALUES (?,?,?)");
        for (int x = 0; x < sentence.getWordCount(); x++) {
            relations.setInt(1, sentence.getId());
            relations.setInt(2, sentence.getWordList().get(x).getId());
            relations.setInt(3, x);
            relations.executeUpdate();
        }
        conn.commit();
        conn.setAutoCommit(true);
        disconnect();
    }

    /**
     * @return the permanentConnection
     */
    public boolean isPermanentConnection() {
        return permanentConnection;
    }

    /**
     * @param permanentConnection the permanentConnection to set
     */
    public void setPermanentConnection(boolean permanentConnection) {
        this.permanentConnection = permanentConnection;
    }

    /**
     * @return the databaseMode
     */
    public int getDatabaseMode() {
        return databaseMode;
    }

    /**
     * @param databaseMode the databaseMode to set
     */
    public void setDatabaseMode(int databaseMode) {
        if (this.isPermanentConnection()) {
            this.permanentConnection = false;
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    // Nothing.
                }
            }
            conn = null;
        }
        this.databaseMode = databaseMode;
    }
    
}
