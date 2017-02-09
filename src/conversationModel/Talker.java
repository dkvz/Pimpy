/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package conversationModel;

import pimpy.*;

/**
 *
 * @author Alain
 */
public interface Talker {

    public void setDebugLogger(DebugLogger debugLogger);
    public DebugLogger getDebugLogger();
    public String getAnswer(String submitted) throws TalkerNotInitializedException;
    public void initialize() throws TalkerNotInitializedException;
    public double getInitProgress();

}
