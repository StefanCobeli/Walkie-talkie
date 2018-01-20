import java.net.*;
import java.io.*;
import java.util.*;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client  {

    // for I/O
    private ObjectInputStream inputStream;		// to read from the socket
    private ObjectOutputStream outputStream;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    private ClientGUI clientGUI;

    // the server, the port and the username
    private String server, username;
    private int port;
    public ListenFromServer serverListener;
    private long timeout;

    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  username: the username
     */
    Client(String server, int port, String username, ClientGUI clientGUI) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        this.clientGUI = clientGUI;
        loadProperties();
    }

    private void loadProperties(){
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);
            timeout = Long.valueOf(prop.getProperty("timeout"));

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * To start the dialog
     */
    public boolean start() {
        // try to connect to the server
        try {
            socket = new Socket(server, port);
        }
        // if it failed not much I can so
        catch(Exception ec) {
            display("Error connectiong to server:" + ec);
            return false;
        }

        String msg = "Connection accepted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

		/* Creating both Data Stream */
        try
        {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        serverListener = new ListenFromServer();
        serverListener.start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be Message objects
        try
        {
            outputStream.writeObject(username);
        }
        catch (IOException eIO) {
            display("Exception doing login : " + eIO);
            disconnect();
            return false;
        }
        // success we inform the caller that it worked
        return true;
    }

    /*
     * To send a message to the console or the GUI
     */
    private void display(String msg) {
        if(clientGUI == null)
            System.out.println(msg);      // println in console mode
        else
            clientGUI.append(msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
    }

    /*
     * To send a message to the server
     */
    void sendMessage(Message msg) {
        try {
            outputStream.writeObject(msg);
        }
        catch(IOException e) {
            display("Exception writing to server: " + e);
        }
    }

    /*
     * When something goes wrong
     * Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try {
            if(inputStream != null) inputStream.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(outputStream != null) outputStream.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {} // not much else I can do

        // inform the GUI
        if(clientGUI != null)
            clientGUI.connectionFailed();

    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {
        public boolean clientWasKicked = true;
        public void run() {
            while(true) {
                try {
                    Message cm = (Message) inputStream.readObject();
                    if(clientGUI == null){
                        System.out.println(cm.getMessage());
                        continue;
                    }
                    if(cm.getType() == Message.PERMISSION) {
                        clientGUI.setPermissionToSpeak(true);
                        clientGUI.messageField.setEnabled(true);
                        clientGUI.messageField.setText("");
                        clientGUI.bowHandButton.setEnabled(true);

                        synchronized (this){
                            wait(timeout);
                        }
                        if (clientWasKicked) {
                            String timeoutMessage = clientGUI.client.username + " was kicked for untyping!";
                            clientGUI.client.sendMessage(new Message(Message.MESSAGE, timeoutMessage));
                            clientGUI.messageField.setText("You  must raise your hand to speak!");
                            clientGUI.messageField.setEnabled(false);
                            clientGUI.raiseHandButton.setEnabled(true);
                            clientGUI.bowHandButton.setEnabled(false);
                        }
                        clientWasKicked = true;
                    }
                    else {
                        clientGUI.append(cm.getMessage());
                    }
                }

                catch(IOException e) {
                    display("Server has close the connection: " + e);
                    if(clientGUI != null)
                        clientGUI.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch(ClassNotFoundException e2) {
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
