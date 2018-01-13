import java.io.*;


public class Message implements Serializable {

    //protected static final long serialVersionUID = 1112122200L;

    // The different types of message sent by the Client
    // WHOISIN to receive the list of the users connected
    // MESSAGE an ordinary message
    // LOGOUT to disconnect from the Server
    static final int RAISEHAND = 0;
    //static final Message ACCEPTCLIENT = 1;
    static final int MESSAGE = 2;
    static final int BOWHAND = 3;
    static final int TIMEOUT = 4;
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
