import java.io.*;

//Message formats that are sent via the Socket

public class Message implements Serializable {

    protected static final long serialVersionUID = 1112122200L;

    // The different types of messages
    // MESSAGE    an ordinary message
    // RAISEHAND  the client raises the hand
    // BOWHAND    the client bows the hand
    // PERMISSION the server give the permission to the client to speak

    static final int MESSAGE = 0, RAISEHAND = 1, BOWHAND = 2, PERMISSION = 3;
    private int type;
    private String message;

    // constructor
    Message(int type, String message) {
        this.type = type;
        this.message = message;
    }

    // getters
    int getType() {
        return type;
    }
    String getMessage() {
        return message;
    }
}
