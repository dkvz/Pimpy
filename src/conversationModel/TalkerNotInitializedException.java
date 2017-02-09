
package conversationModel;

/**
 *
 * @author Alain
 */
public class TalkerNotInitializedException extends Exception {

    public TalkerNotInitializedException() {
        super("Talker was not initalized.");
    }

    public TalkerNotInitializedException(String message) {
        super("Talker was not initalized : " + message);
    }

}
