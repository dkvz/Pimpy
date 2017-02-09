package conversationModel;

import pimpy.*;
import java.util.*;
import dataModel.*;
import java.sql.SQLException;
import java.util.regex.*;

/**
 *
 * @author Alain
 */
public class SentenceTalker implements Talker {

    // This talker might be the most likely to answer null.
    // The probability of that happening would depend on the 
    // size of the knowledge base.
    // It is also very complicated and mainly empiric.
    private PimpyDatabase pimpyDB;
    // Probability to return null answer if unknown words are found.
    private double unknownWordsReturnsNullProbability = 0.1;
    // Probability to prefer answers with high order correlations.
    // The contrary would be to prefer answers with close word count
    // values.
    private double preferOrderToLengthCorrelation = 0.76;
    // If we got a context answer and all vars, probability of using it :
    private double useContextAnswerProbability = 0.88;
    // Probability of choosing a question answer if submitted is a question,
    // or a normal answer if submitted is not a question.
    private double matchQuestionStateProbability = 0.89;
    // Measure of how the answer was a close match to submitted sentence.
    // Lower is better.
    private int closeness = 0;
    private Hashtable<String, String> sessionVars;
    private SentenceHistory history = null;
    private DebugLogger debugLogger;

    public SentenceTalker(PimpyDatabase pimpyDB) {
        this.pimpyDB = pimpyDB;
        this.debugLogger = new DebugLogger();
    }

    public SentenceTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars) {
        this.pimpyDB = pimpyDB;
        this.sessionVars = sessionVars;
        this.debugLogger = new DebugLogger();
    }

    public SentenceTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars, SentenceHistory history) {
        this(pimpyDB, sessionVars);
        this.history = history;
        this.debugLogger = new DebugLogger();
    }

    public SentenceTalker(PimpyDatabase pimpyDB, Hashtable<String, String> sessionVars, SentenceHistory history, DebugLogger logger) {
        this(pimpyDB, sessionVars);
        this.history = history;
        this.debugLogger = logger;
    }

    public String getAnswer(String submitted) throws TalkerNotInitializedException {
        this.closeness = 0;
        // We don't allow empty strings, the trivial talker can do that.
        if (submitted.isEmpty()) {
            this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Submitted sentence was empty - returned null."));
            return null;
        }
        // First thing is to parse word list.
        Sentence sentence = new Sentence(submitted);
        Random rand = new Random();
        // As wordList is going to get cleaned up later on, we store its original size.
        int originalWordCount = sentence.getWordList().size();
        try {
            if (sentence.getWordCount() > 0) {
                // Check if some words are unknown by looking for their word_id in database.
                pimpyDB.markExistingWords(sentence.getWordList(), true);
                // Now what to do with unknown words...
                boolean testedYet = false;
                for (int x = 0; x < sentence.getWordList().size(); x++) {
                    if (sentence.getWordList().get(x).getId() < 0) {
                        // Unknown word.
                        if (!testedYet) {
                            if (rand.nextDouble() <= this.unknownWordsReturnsNullProbability) {
                                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Found an unknown word and matched probability to ignore unknown words - returned null."));
                                return null;
                            }
                            testedYet = true;
                        }
                        // Take the word out...
                        sentence.getWordList().remove(x);
                        sentence.setWordCount(sentence.getWordList().size());
                        x--;
                    }
                }
                if (debugLogger.isDebugMode()) {
                    String wList = "";
                    for (int x = 0; x < sentence.getWordList().size(); x++) {
                        wList += sentence.getWordList().get(x).getWord() + " ";
                    }
                    this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Epured word list : " + wList));
                }
                // We now have an epured word list.
                if (sentence.getWordCount() <= 0) {
                    // It was epured too much to be usable...
                    this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Word List emptied of unknown word was empty - returned null."));
                    return null;
                }
                // We need to continuously call findMatch until we get a match
                // or there is no more word in word list.
                boolean preferOrder = false;
                // We have 2 selection paths from now.
                // Make order or length correlation the priority.
                if (rand.nextDouble() <= this.preferOrderToLengthCorrelation) {
                    preferOrder = true;
                }
                if (sentence.isQuestion()) {
                    this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Submitted is a question."));
                }
                Sentence rep = this.findMatch(preferOrder, sentence.getWordList(), sentence.isQuestion(), originalWordCount);
                if (rep != null) {
                    // We got a possible answer. We need to determine if we're using
                    // context or not.
                    // Save history :
                    this.history.addSentence(rep);
//                    if (rep.getWordList() == null) {
//                        pimpyDB.fetchWordList(rep);
//                    }
                    this.debugLogger.logMessage(new DebugEvent("SentenceTalker returned sentenceId " + rep.getId(), DebugEvent.INFO));
                    if (this.debugLogger.isDebugMode()) {
                        if (rep.getWordList() == null || rep.getWordList().isEmpty()) {
                            this.pimpyDB.fetchWordList(rep);
                        }
                        this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Sentence to match was : " + rep.toString(), DebugEvent.INFO));
                    }
//                    for (int h = 0; h < rep.getWordList().size(); h++) {
//                        System.out.println(rep.getWordList().get(h).getWord());
//                    }
                    if (rep.getAnswerContext() != null && !rep.getAnswerContext().isEmpty()) {
                        // We're using context only if we got all variables
                        // and we get past the probability test.
                        // Let's identify the session vars :
                        Pattern pattern = Pattern.compile("\\{(\\w+)\\}");
                        Matcher matcher = pattern.matcher(rep.getAnswerContext());
                        String parsedAnswer = rep.getAnswerContext();
                        while (matcher.find()) {
                            if (this.getSessionVars().containsKey(matcher.group(1))) {
                                parsedAnswer = parsedAnswer.replace(matcher.group(1), getSessionVars().get(matcher.group(1)));
                            }
                        }
                        // Let's check if we replaced everything.
                        Matcher verify = pattern.matcher(parsedAnswer);
                        if (!verify.find()) {
                            // There is no context vars remaiming in answer.
                            if (rand.nextDouble() <= this.useContextAnswerProbability) {
                                return parsedAnswer;
                            }
                        }
                    }
                    // Not using context.
                    return rep.getAnswer();
                }
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Couldn't find a match - returned null."));
            }
        } catch (SQLException ex) {
            // Return null later.
            ex.printStackTrace();
            this.debugLogger.logMessage(new DebugEvent(ex.toString(), DebugEvent.ERROR));
        } catch (Exception ex) {
            ex.printStackTrace();
            this.debugLogger.logMessage(new DebugEvent(ex.toString(), DebugEvent.ERROR));
        }
        return null;
    }

    private Sentence findMatch(boolean preferOrder, ArrayList<Word> wordList, boolean question, int originalWordCount) throws SQLException {
        // Whatever the preference case, we have to find all sentences
        // containing those words.
        // This algorithm is like a coat hanger rape. Sorry about that.
        ArrayList<WordMatchedSentence> sents = pimpyDB.findSentencesByWords(wordList);
        // We're using the fact that this arrayList should be ordered by most matched words first.
        // First added element (most matched words) will have index 0 and so on.
        // We want a matchCount that's closest to the item count of wordList...
        int maxMatchedCount = 0;
        HashMap<Integer, Object> matchedCounts = new HashMap<Integer, Object>();
        for (int x = 0; x < sents.size(); x++) {
            int count = sents.get(x).getMatchCount();
            if (count > maxMatchedCount) {
                maxMatchedCount = count;
            }
            matchedCounts.put(count, null);
        }
        this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Max match count is " + maxMatchedCount));
        //this.debugLogger.logMessage(new DebugEvent("SentenceTalker - First max match count sentence is : " + sents.get(0).getId()));
        int originalLookingFor = wordList.size();
        int lookingFor = originalLookingFor;
        int increment = 0;
        boolean searchUpward = true;
        ArrayList<WordMatchedSentence> closests = new ArrayList<WordMatchedSentence>();
        boolean nothingFound = false;
        while (!nothingFound && closests.isEmpty()) {
            for (int x = 0; x < sents.size(); x++) {
                if (sents.get(x).getMatchCount() == lookingFor) {
                    closests.add(sents.get(x));
                }
                // We could add an else break; here, as the list is supposed to be ordered.
            }
            if (closests.isEmpty()) {
                // Change lookingFor for something else.
                do {
                    if (searchUpward) {
                        increment++;
                    }
                    if ((maxMatchedCount >= (originalLookingFor + increment)) && searchUpward) {
                        lookingFor = originalLookingFor + increment;
                        searchUpward = false;
                    } else if ((originalLookingFor - increment) > 1) {
                        lookingFor = originalLookingFor - increment;
                        searchUpward = true;
                    } else if (!searchUpward) {
                        searchUpward = true;
                        increment--;
                    } else {
                        nothingFound = true;
                    }
                } while (!nothingFound && !matchedCounts.containsKey(lookingFor));
            }
//            else {
//                break;
//            }
            if (nothingFound) {
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Circular matcher found nothing (supposed to be very unlikely -> File a bug report)"));
                return null;
            }
        }
        // We have the closest sentences. Let's see if some are in history and remove
        // those if we can afford it.
        Random rand = new Random();
        Collections.shuffle(closests, rand);
        for (int x = 0; (x < closests.size() && closests.size() > 1); x++) {
            if (history.isInHistory(closests.get(x))) {
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Sentence " + closests.get(x).getId() + " was evicted because of history."));
                closests.remove(x);
                x--;
            }
        }

        // We got closest matches in List "closests".
        // We got to fetch the right answer.
        ArrayList<WordMatchedSentence> fromQuestion = new ArrayList<WordMatchedSentence>();
        ArrayList<WordMatchedSentence> fromNormal = new ArrayList<WordMatchedSentence>();
        for (int x = 0; x < closests.size(); x++) {
            // Dump closests for debugging
            this.debugLogger.logMessage(new DebugEvent("SentenceTalker - In closests : " + closests.get(x).getId()));
            if (closests.get(x).isQuestion()) {
                fromQuestion.add(closests.get(x));
            } else {
                fromNormal.add(closests.get(x));
            }
        }
        WordMatchedSentence ret = null;
        // Let's separate the questions from the non questions.
        // If you don't want such possible discrimination, take these lines down :
        if (question && fromQuestion.size() > 0) {
            // Submitted is a question.
            if (rand.nextDouble() <= this.matchQuestionStateProbability) {
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Will pick in questions"));
                closests = fromQuestion;
            } else if (fromNormal.size() > 0) {
                closests = fromNormal;
            }
        } else if (!question && fromNormal.size() > 0) {
            if (rand.nextDouble() <= this.matchQuestionStateProbability) {
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Will pick in non questions"));
                closests = fromNormal;
            } else if (fromQuestion.size() > 0) {
                closests = fromQuestion;
            }
        }
        // End of question matters.
        if (preferOrder) {
            this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Word order criteria is being used."));
            // This is the trickiest way to choose.
            // Each sentence gets an order score, and we choose the highest.
            // If wordMatchedCount is less than 2 this doesn't make sense and
            // we pick at random.
            if (closests.get(0).getMatchCount() < 2) {
                this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Match count of 0 or 1 - picking at random."));
                ret = closests.get(rand.nextInt(closests.size()));
                // This is practically choosing at random...
                // Must have a high closeness score when compared
                // to the word count involved.
                // Except... If matched sentence has the same word count...
                if (wordList.size() != ret.getWordCount()) {
                    this.closeness += 6;
                }
            } else {
                // Compute order scores.
                // Fetch the word lists for selected sentences.
                for (int x = 0; x < closests.size(); x++) {
                    pimpyDB.fetchWordList(closests.get(x));
                }
                int highest = 0;
                // Score for each of the closests sentences.
                for (int x = 0; x < closests.size(); x++) {
                    int score = 0;
                    int prev = -1;
                    for (int y = 0; y < wordList.size(); y++) {
                        // In fact that word could be in multiple places in sentence...
                        // I mean, it could appear multiple times.
                        // Only usefull to check if y > z...
                        int prePrev = -1;
                        boolean lookingAhead = false;
                        for (int z = 0; z < closests.get(x).getWordList().size(); z++) {
                            // We could compare word Ids but wordmatching is supposed
                            // to be fuzzy.
                            // Let's prepare a word with one letter less.
                            String original = closests.get(x).getWordList().get(z).getWord();
                            String trimed = closests.get(x).getWordList().get(z).getWord().substring(0, original.length() - 1);
                            if (wordList.get(y).getWord().equals(original) || wordList.get(y).getWord().equals(trimed)) {
                                // Word found.
                                if (y > z && !lookingAhead && z != closests.get(x).getWordList().size() - 1) {
                                    // Word could appear further in sentence.
                                    // We try to take the next appearence rather than the first.
                                    // Notice there could still be occurences of this word further...
                                    lookingAhead = true;
                                    prePrev = z;
                                    continue;
                                }
                                if (prev >= 0) {
                                    if (z == (prev + 1)) {
                                        // The words are next to each other.
                                        score += 2;
                                    } else if (z > prev) {
                                        // Not next to each other, but in order.
                                        score += 1;
                                    }
                                }
                                prev = z;
                                // Try to find next word in wordList.
                                break;
                            }
                            if (lookingAhead && prev != z && z == closests.get(x).getWordList().size() - 1) {
                                // We were looking ahead but did not find anything interresting...
                                // So we take what we found earlier.
                                if (prev >= 0) {
                                    if (prePrev == (prev + 1)) {
                                        // The words are next to each other.
                                        score += 2;
                                    } else if (prePrev > prev) {
                                        // Not next to each other, but in order.
                                        score += 1;
                                    }
                                } else {
                                    prev = prePrev;
                                }
                            }
                        }
                    }
                    // Just finished verifying order for current sentence.
                    // Descrease score in case of big wordCount difference between the candidate
                    // sentence and the submitted sentence.
                    if (closests.get(x).getWordCount() >= Math.ceil((double)originalWordCount * 1.5)) {
                        int penalty = closests.get(x).getWordCount() - originalWordCount;
                        // The penalty is going to be harsh...
                        score -= penalty;
                        this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Detected big word count difference in sentence " + closests.get(x).getId() + " - Order penalty of " + penalty));
                        if (score < 0) {
                            score = 0;
                        }
                    }
                    this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Sentence " + closests.get(x).getId() + " gets order score " + score));
                    if (score > highest) {
                        //this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Found sentence " + x + "as closests yet."));
                        highest = score;
                        ret = closests.get(x);
                    }
                }
                // Higher the highestScore is, lower is the closeness score.
                // The highest possible score is (wordCount * 2) - 2.
                // As it produced scores a little too high, I divided the increment by two.
                this.closeness += (((wordList.size() * 2) - 2) - highest) / 2;
                // But if chosen sentence has much more words, closeness should be higher.
                // This closeness thing is the result of a lot of trial and error.
//                if (ret != null && ret.getWordCount() > wordList.size()) {
//                    this.closeness += ((ret.getWordCount() - wordList.size()) / 2);
//                }
//                if (ret != null) {
//                    this.closeness += (Math.abs(wordList.size() - ret.getWordCount()) / 2);
//                    System.out.println("Incrément : " + (Math.abs(wordList.size() - ret.getWordCount()) / 2));
//                    System.out.println("Taille wxordlist = " + wordList.size() + " Taille ret : " + ret.getWordCount());
//                }
            }
        } else {
            this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Length correlation criteria is being used."));
            // Just iterate and find the sentence with closest wordCount to submitted wordCount.
            int span = -1;
            for (int x = 0; x < closests.size(); x++) {
                //int curSpan = Math.abs(closests.get(x).getWordCount() - wordList.size());
                int curSpan = Math.abs(closests.get(x).getWordCount() - originalWordCount);
                if (curSpan < span || span < 0) {
                    span = curSpan;
                    ret = closests.get(x);
                }
            }
            this.closeness += span;
        }
        // If matchCount we found is far from word count in wordList, we should increase closeness...
        if (ret != null) {
            // matchCount will be increased multiple times if a sentence has a found word that appears
            // multiple times... "oui et toi" matched with "et et et" will have a matched count of 3.
            // But sentences with multiple occurences (more than 2) are rather uncommon.
            this.closeness += Math.abs(ret.getMatchCount() - originalWordCount);
            this.debugLogger.logMessage(new DebugEvent("SentenceTalker - Elected sentence with match count of " + ret.getMatchCount()));
        }
        return ret;
    }

    public void initialize() {
        // This talker doesn't need initialization.
        return;
    }

    public double getInitProgress() {
        // This talker doesn't need initialization.
        return 1.0;
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
     * @return the closeness
     */
    public int getCloseness() {
        // Closness is a measure of how much submitted sentence matched
        // the sentence that gave the answer returned by the previous
        // call to getAnswer().
        // Lower scores are better.
        return closeness;
    }

    /**
     * @param closeness the closeness to set
     */
    public void setCloseness(int closeness) {
        this.closeness = closeness;
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
        this.setSessionVars(sessionVars);
    }

    /**
     * @return the unknownWordsReturnsNullProbability
     */
    public double getUnknownWordsReturnsNullProbability() {
        return unknownWordsReturnsNullProbability;
    }

    /**
     * @param unknownWordsReturnsNullProbability the unknownWordsReturnsNullProbability to set
     */
    public void setUnknownWordsReturnsNullProbability(double unknownWordsReturnsNullProbability) {
        this.unknownWordsReturnsNullProbability = unknownWordsReturnsNullProbability;
    }

    /**
     * @return the preferOrderToLengthCorrelation
     */
    public double getPreferOrderToLengthCorrelation() {
        return preferOrderToLengthCorrelation;
    }

    /**
     * @param preferOrderToLengthCorrelation the preferOrderToLengthCorrelation to set
     */
    public void setPreferOrderToLengthCorrelation(double preferOrderToLengthCorrelation) {
        this.preferOrderToLengthCorrelation = preferOrderToLengthCorrelation;
    }

    /**
     * @return the useContextAnswerProbability
     */
    public double getUseContextAnswerProbability() {
        return useContextAnswerProbability;
    }

    /**
     * @param useContextAnswerProbability the useContextAnswerProbability to set
     */
    public void setUseContextAnswerProbability(double useContextAnswerProbability) {
        this.useContextAnswerProbability = useContextAnswerProbability;
    }

    /**
     * @return the matchQuestionStateProbability
     */
    public double getMatchQuestionStateProbability() {
        return matchQuestionStateProbability;
    }

    /**
     * @param matchQuestionStateProbability the matchQuestionStateProbability to set
     */
    public void setMatchQuestionStateProbability(double matchQuestionStateProbability) {
        this.matchQuestionStateProbability = matchQuestionStateProbability;
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
