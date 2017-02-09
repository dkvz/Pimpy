/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dataModel;

/**
 *
 * @author Alain
 */
public class WordMatchedSentence extends Sentence {

    private int matchCount = 0;

    public WordMatchedSentence() {
        super();
    }

    public WordMatchedSentence(String submitted) {
        super(submitted);
    }

    public WordMatchedSentence(String submitted, String answer) {
        super(submitted, answer);
    }

    public WordMatchedSentence(String submitted, String answer, String answerContext) {
        super(submitted, answer, answerContext);
    }

    public WordMatchedSentence(String submitted, String answer, String answerContext, int type) {
        super(submitted, answer, answerContext, type);
    }

    public WordMatchedSentence(String submitted, String answer, String answerContext, int type, int matchCount) {
        super(submitted, answer, answerContext, type);
        this.matchCount = matchCount;
    }

    /**
     * @return the matchCount
     */
    public int getMatchCount() {
        return matchCount;
    }

    /**
     * @param matchCount the matchCount to set
     */
    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }

}
