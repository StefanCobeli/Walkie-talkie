import sun.tools.jar.Main;

import java.net.*;
import java.io.*;
import java.util.*;

/*
 * The Client that can be run both as a console or a GUI
 */
public class Client  {

    // for I/O
    private ObjectInputStream sInput;		// to read from the socket
    private ObjectOutputStream sOutput;		// to write on the socket
    private Socket socket;

    // if I use a GUI or not
    private ClientGUI cg;

    // the server, the port and the clientName
    private String server, username;
    private int port;

    private int clientId;
    static boolean permissionToSpeak = false;
    static boolean raisedHand = false;

    private ListenFromServer serverListener;
    /*
     *  Constructor called by console mode
     *  server: the server address
     *  port: the port number
     *  clientName: the clientName
     */
    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port, username, null);
    }

    /*
     * Constructor call when used from a GUI
     * in console mode the ClienGUI parameter is null
     */
    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        this.cg = cg;
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
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            display("Exception creating new Input/output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        serverListener = new ListenFromServer(this);
        serverListener.start();
        // Send our clientName to the server this is the only message that we
        // will send as a String. All other messages will be Message objects
        try
        {
            sOutput.writeObject(username);
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
        if(cg == null)
            System.out.println(msg);      // println in console mode
        else
            cg.append(msg + "\n");		// append to the ClientGUI JTextArea (or whatever)
    }

    /*
     * To send a message to the server
     */
    void sendMessage(Message msg) {
        try {
            sOutput.writeObject(msg);
            //waitForServerApproval();
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
            if(sInput != null) sInput.close();
        }
        catch(Exception e) {} // not much else I can do
        try {
            if(sOutput != null) sOutput.close();
        }
        catch(Exception e) {} // not much else I can do
        try{
            if(socket != null) socket.close();
        }
        catch(Exception e) {} // not much else I can do

        // inform the GUI
        if(cg != null)
            cg.connectionFailed();

    }
    /*
     * To start the Client in console mode use one of the following command
     * > java Client
     * > java Client clientName
     * > java Client clientName portNumber
     * > java Client clientName portNumber serverAddress
     * at the console prompt
     * If the portNumber is not specified 1500 is used
     * If the serverAddress is not specified "localHost" is used
     * If the clientName is not specified "Anonymous" is used
     * > java Client
     * is equivalent to
     * > java Client Anonymous 1500 localhost
     * are eqquivalent
     *
     * In console mode, if an error occurs the program simply stops
     * when a GUI clientId used, the GUI is informed of the disconnection
     */
    public static void main(String[] args) {
        // default values
        int portNumber = 1600;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        // depending of the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client clientName portNumber serverAddr
            case 3:
                serverAddress = args[2];
                // > javac Client clientName portNumber
            case 2:
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [clientName] [portNumber] [serverAddress]");
                    return;
                }
                // > javac Client clientName
            case 1:
                userName = args[0];
                // > java Client
            case 0:
                break;
            // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [clientName] [portNumber] {serverAddress]");
                return;
        }
        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // test if we can start the connection to the Server
        // if it failed nothing we can do
        if (!client.start())
            return;

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.print("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if message is LOGOUT
            if(msg.equalsIgnoreCase("LOGOUT")) {
            //client.sendMessage(new Message(Message.LOGOUT, ""));
            // break to do the disconnect
                break;
            }
            //else
            if (msg.equalsIgnoreCase("RAISEHAND")) {
                System.out.println("hand raised!!!!!!!!!!!!!!");
                raisedHand = true;
                client.sendMessage(new Message(Message.RAISEHAND, "hand raised"));
                client.waitForServerApproval();
                client.writeMessage();
                //System.out.println("hand raised sent!!!!!!!!!!!!!!");

                System.out.println("I typed!");
            }
            // message WhoIsIn
            //else if(msg.equalsIgnoreCase("WHOISIN")) {
            //client.sendMessage(new Message(Message.WHOISIN, ""));
            //}
            else {                // default to ordinary message
                System.out.println("You do not have the right to speak. Please raise your hand first!");
                //client.sendMessage(new Message(Message.MESSAGE, msg));
            }

            // done disconnect
        }
        client.disconnect();
    }

    private void writeMessage() {
        Scanner scan = new Scanner(System.in);
        System.out.print("> ");
        String msg = scan.nextLine();
        if (msg.equalsIgnoreCase("BOWHAND")) {
            this.sendMessage(new Message(Message.BOWHAND, ""));
            System.out.println("You have bowed your hand and you are not able to speak anymore!");
            display("You have bowed your hand and you are not able to speak anymore!");
            return;
        }
        this.sendMessage(new Message(Message.MESSAGE, msg));
    }

    private synchronized void waitForServerApproval() {

        System.out.println("Waiting for permission granted!!!!!!!!!!!");
        try {
            this.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        while (true){
//            if (permissionToSpeak){
//                permissionToSpeak = false;
//                break;
//            }
//        }
    }

    /*
     * a class that waits for the message from the server and append them to the JTextArea
     * if we have a GUI or simply System.out.println() it in console mode
     */
    class ListenFromServer extends Thread {
        ListenFromServer(Client client){
            this.client = client;
        }
        private Client client;

        public void run() {
            try {
                clientId = Integer.parseInt((String) sInput.readObject());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            while(true) {
                try {
                    String msg = (String) sInput.readObject();
                    // if console mode print the message and add back the prompt
                    if(raisedHand && msg.equals(String.valueOf(clientId))){
                        System.out.println("You have received the permission to speak!");
                        permissionToSpeak = true;
                        synchronized (client) {
                            client.notify();

                            raisedHand = false;
//                            try {
//                                sleep(10000);
//                                System.out.println("You' re permission to speak has expired");
//                                //Main.main();
//                            } catch (InterruptedException ie) {
//                                continue;
//                            }
                            //Thread.sleep(1000);
                            continue;
                        }
                    }
                    if(cg == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    }
                    else {
                        cg.append(msg);
                    }
                }
                catch(IOException e) {
                    display("Server has close the connection: " + e);

                    if(cg != null)
                        cg.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch(Exception e2) {
                }

            }
        }
    }
}