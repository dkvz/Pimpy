

package dataModel;

import java.util.*;

/**
 *
 * @author Alain
 */
public class Sentence {

    protected int id;
    protected ArrayList<Word> wordList;
    protected boolean question = false;
    protected String submitted = "";
    protected String answer = "";
    protected String answerContext = "";
    protected int wordCount = 0;
    protected int type = 0;

    public Sentence() {
        this.id = -1;
    }

    public Sentence(String submitted) {
        this.submitted = submitted;
        this.id = -1;
        this.parseSubmitted();
    }

    public Sentence(String submitted, String answer) {
        this.submitted = submitted;
        this.answer = answer;
        this.id = -1;
        this.parseSubmitted();
    }

    public Sentence(String submitted, String answer, String answerContext) {
        this.submitted = submitted;
        this.answer = answer;
        this.answerContext = answerContext;
        this.id = -1;
        this.parseSubmitted();
    }

    public Sentence(String submitted, String answer, String answerContext, int type) {
        this.submitted = submitted;
        this.answer = answer;
        this.answerContext = answerContext;
        this.type = type;
        this.id = -1;
        this.parseSubmitted();
    }

    public void parseSubmitted() {
        // Get a word list from a sentence.
        wordList = new ArrayList<Word>();
        wordCount = 0;
        this.epureSubmitted();
        String [] preWords = submitted.split(" ");
        for (int x = 0; x < preWords.length; x++) {
            preWords[x] = preWords[x].trim();
            if (preWords[x].length() > 1) {
                Word word = new Word(preWords[x]);
                wordList.add(word);
                wordCount++;
            }
        }
    }

    private void epureSubmitted() {
        // This function is ugly but it kinda makes me laugh so I love it.
        submitted = submitted.toLowerCase().trim();
        // We don't touch the "?" sign.
        submitted = submitted.replace("[", "");
        submitted = submitted.replace("]", "");
        submitted = submitted.replace("\n", " ");
        submitted = submitted.replace("\r", "");
        submitted = submitted.replace("\t", " ");
        submitted = submitted.replace("à", "a");
        submitted = submitted.replace("ç", "c");
        submitted = submitted.replace("ù", "u");
        submitted = submitted.replaceAll("[&,|,@,#,\",\\(,§,^,!,{,},\\),°,\\,,_,¨,$,\\*,%,µ,£,=,\\+,~,:,/,\\.,;,<,>,\\,0-9]", "");
        submitted = submitted.replace("-", " ");
        submitted = submitted.replace("'", " ");
//        submitted = submitted.replace("é", "e");
//        submitted = submitted.replace("è", "e");
        submitted = submitted.replace("ê", "e");
        submitted = submitted.replace("î", "i");
        submitted = submitted.replace("û", "u");
        submitted = submitted.replace("ô", "o");
        submitted = submitted.replace("â", "a");
        // Is it a question ?
        if (submitted.endsWith("?")) {
            this.question = true;
        } else {
            this.question = false;
        }
        //submitted = submitted.replace("?", "");
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the wordList
     */
    public ArrayList<Word> getWordList() {
        return wordList;
    }

    /**
     * @param wordList the wordList to set
     */
    public void setWordList(ArrayList<Word> wordList) {
//        if (wordList != null) {
//            this.wordCount = wordList.size();
//        }
        this.wordList = wordList;
    }

    /**
     * @return the question
     */
    public boolean isQuestion() {
        return question;
    }

    /**
     * @param question the question to set
     */
    public void setQuestion(boolean question) {
        this.question = question;
    }

    /**
     * @return the submitted
     */
    public String getSubmitted() {
        return submitted;
    }

    /**
     * @param submitted the submitted to set
     */
    public void setSubmitted(String submitted) {
        this.submitted = submitted;
    }

    /**
     * @return the answer
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * @param answer the answer to set
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * @return the answerContext
     */
    public String getAnswerContext() {
        return answerContext;
    }

    /**
     * @param answerContext the answerContext to set
     */
    public void setAnswerContext(String answerContext) {
        this.answerContext = answerContext;
    }

    /**
     * @return the wordCount
     */
    public int getWordCount() {
        return wordCount;
    }

    /**
     * @param wordCount the wordCount to set
     */
    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
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

    @Override
    public String toString() {
        String ret = "";
        for (int i = 0; i < this.wordList.size(); i++) {
            ret = ret.concat(this.wordList.get(i).getWord() + " ");
        }
        if (this.isQuestion()) ret = ret.concat("?");
        return ret;
    }

}
