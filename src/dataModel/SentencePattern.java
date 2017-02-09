package dataModel;

import java.util.regex.*;
import java.util.*;

/**
 *
 * @author Alain
 */
public class SentencePattern {

    private int id;
    private String regex;
    private String contextVars = "";
    private Pattern pattern;
    private boolean compiled;
    private boolean caseInsensitive = false;

    public SentencePattern() {
        this.id = -1;
        this.regex = "";
    }

    public SentencePattern(String regex) {
        id = -1;
        this.regex = regex;
    }

    public SentencePattern(String regex, boolean caseInsensitive) {
        id = -1;
        this.regex = regex;
        this.caseInsensitive = caseInsensitive;
    }

    public SentencePattern(int id, String regex) {
        this.id = id;
        this.regex = regex;
    }

    public SentencePattern(int id, String regex, boolean caseInsensitive) {
        this.id = id;
        this.regex = regex;
        this.caseInsensitive = caseInsensitive;
    }

    public void compile() throws PatternSyntaxException {
        // Try to compile the regex.
        if (this.caseInsensitive) {
            this.pattern = Pattern.compile(this.regex, Pattern.CASE_INSENSITIVE);
        } else {
            this.pattern = Pattern.compile(this.regex);
        }
        compiled = true;
    }

    public String formatAnswer(String submitted, String answer) throws Exception {
        // Returns null if did not match.
        if (compiled == false) {
            throw new Exception("Pattern has to be precompiled before using formatAnswer.");
        }
        Matcher matcher = pattern.matcher(submitted);
        if (matcher.find()) {
            // Should we use matcher.matches to match the whole submitted ?
            for (int x = 0; x < matcher.groupCount(); x++) {
                // Replace {x} in answer with found groups.
                String group = matcher.group(x + 1);
                answer = answer.replace("{" + Integer.toString(x + 1) + "}", group);
            }
            // Clear all remaiming replace groups.
            answer = answer.replaceAll("\\{\\d+\\}", "");
            // Remove double spaces :
            answer = answer.replace("  ", " ");
            return answer;
        } else {
            // Did not match.
            return null;
        }
    }

    public String formatAnswer(String submitted, String answer, Hashtable<String, String> contextVars) throws Exception {
        // Returns null if did not match.
        // Saves eventual contextVars in hashmap given as parameter.
        if (compiled == false) {
            throw new Exception("Pattern has to be precompiled before using formatAnswer.");
        }
        String [] vars = this.contextVars.split(",");
        for (int x = 0; x < vars.length; x++) {
            vars[x] = vars[x].trim();
        }
        Matcher matcher = pattern.matcher(submitted);
        if (matcher.find()) {
            // Should we use matcher.matches to match the whole submitted ?
            for (int x = 0; x < matcher.groupCount(); x++) {
                // Replace {x} in answer with found groups.
                String group = matcher.group(x + 1);
                answer = answer.replace("{" + Integer.toString(x + 1) + "}", group);
                if (x < vars.length) {
                    contextVars.put(vars[x], group);
                }
            }
            // Clear all remaiming replace groups.
            answer = answer.replaceAll("\\{\\d+\\}", "");
            // Remove double spaces :
            answer = answer.replace("  ", " ");
            return answer;
        } else {
            // Did not match.
            return null;
        }
    }

    public boolean isMatching(String submitted) throws Exception {
        if (compiled == false) {
            throw new Exception("Pattern has to be precompiled before using formatAnswer.");
        }
        Matcher matcher = pattern.matcher(submitted);
        return matcher.find();
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
     * @return the regex
     */
    public String getRegex() {
        return regex;
    }

    /**
     * @param regex the regex to set
     */
    public void setRegex(String regex) {
        this.regex = regex;
    }

    @Override
    public String toString() {
        return this.regex;
    }

    /**
     * @return the pattern
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the compiled
     */
    public boolean isCompiled() {
        return compiled;
    }

    /**
     * @param compiled the compiled to set
     */
    public void setCompiled(boolean compiled) {
        this.compiled = compiled;
    }

    /**
     * @return the contextVars
     */
    public String getContextVars() {
        return contextVars;
    }

    /**
     * @param contextVars the contextVars to set
     */
    public void setContextVars(String contextVars) {
        this.contextVars = contextVars;
    }

    /**
     * @return the caseInsensitive
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * @param caseInsensitive the caseInsensitive to set
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }
}
