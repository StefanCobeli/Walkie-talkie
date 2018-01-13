import sun.applet.Main;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> clientList;
    // if I am in a GUI
    private ServerGUI serverGUI;
    // to display time
    private SimpleDateFormat dataFormat;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    private ApprovalSender approvalSender;
    private ServerSocket serverSocket;
    /*
     *  server constructor that receive the port to listen to for connection as parameter
     *  in console
     */
    public Server(int port) {
        this(port,null);
    }

    public Server(int port, ServerGUI serverGUI) {
        // GUI or not
        this.serverGUI = serverGUI;
        // the port
        this.port = port;
        // to display hh:mm:ss
        dataFormat = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        clientList = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
		/* create socket server and wait for connection requests */
        try
        {
            // the socket used by the server
            serverSocket = new ServerSocket(port);
            approvalSender = new ApprovalSender();
            approvalSender.start();
            // infinite loop to wait for connections
            while(keepGoing)
            {

                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if(!keepGoing)
                    break;
                ClientThread newClient = new ClientThread(socket);  // make a thread of it
                clientList.add(newClient);// save it in the ArrayList
                System.out.println("The clients connected are:");
                clientList.stream().forEach(clientThread -> System.out.println(clientThread.clientName + " " + clientThread.clientId));
                newClient.writeMsg(String.valueOf(newClient.clientId));
                newClient.start();

                //clientOutputStream.writeObject(msg);
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for(int i = 0; i < clientList.size(); ++i) {
                    ClientThread client = clientList.get(i);
                    try {
                        client.clientInputStream.close();
                        client.clientOutputStream.close();
                        client.socket.close();
                    }
                    catch(IOException ioE) {
                        // not much I can do
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // something went bad
        catch (IOException e) {
            String msg = dataFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    /*
     * For the GUI to stop the server
     */
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            // nothing I can really do
        }
    }
    /*
     * Display an event (not a message) to the console or the GUI
     */
    private void display(String msg) {
        String time = dataFormat.format(new Date()) + " " + msg;
        if(serverGUI == null) {
            System.out.println(time);
        }
        else {
            serverGUI.appendEvent(time + "\n");
        }
    }

    private void display(String msg, String clientName) {
        String time = dataFormat.format(new Date()) + " " + msg;
        if(serverGUI == null) {
            System.out.printf("%s hand was raised", clientName);
            System.out.println(time);
        }
        else {
            serverGUI.appendEvent(clientName + "'s hand was raised");
            serverGUI.appendRaisedHandClient(msg);
            serverGUI.appendEvent(time + "\n");
        }
    }

    /*
     *  to broadcast a message to all Clients
     */
    private synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = dataFormat.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI
        if(serverGUI == null)
            System.out.print(messageLf);
        else
            serverGUI.appendRoom(messageLf);     // append in the room window

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = clientList.size(); --i >= 0;) {
            ClientThread ct = clientList.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                clientList.remove(i);
                display("Disconnected Client " + ct.clientName + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for(int i = 0; i < clientList.size(); ++i) {
            ClientThread ct = clientList.get(i);
            // found it
            if(ct.clientId == id) {
                clientList.remove(i);
                return;
            }
        }
    }

    /*
     *  To run as a console application just open a console window and:
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        // start server on port 1500 unless a PortNumber is specified
        int portNumber = 1600;
        switch(args.length) {
            case 1:
                try {
                    portNumber = Integer.parseInt(args[0]);
                }
                catch(Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }

    /** One instance of this thread will run for each client */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream clientInputStream;
        ObjectOutputStream clientOutputStream;
        // my unique clientId (easier for deconnection)
        int clientId;
        // the Username of the Client
        String clientName;
        // the only type of message a will receive
        Message message;
        // the creationDate I connect
        String creationDate;

        //permission to speak
        boolean permissionToSpeak = false;
        boolean raisedHand = false;

        // Constructore
        ClientThread(Socket socket) {
            // a unique clientId
            clientId = (String.valueOf(++uniqueId)).hashCode();
            this.socket = socket;
			/* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                // create output first
                clientOutputStream = new ObjectOutputStream(socket.getOutputStream());
                clientInputStream = new ObjectInputStream(socket.getInputStream());
                // read the clientName
                clientName = (String) clientInputStream.readObject();
                display(clientName + " just connected.");
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            creationDate = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    message = (Message) clientInputStream.readObject();
                }
                catch (IOException e) {
                    display(clientName + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the Message
                String message = this.message.getMessage();

                // Switch on the type of message receive
                switch(this.message.getType()) {

                    case Message.RAISEHAND:
                        display(clientName + ": " + "raised the hand.");
                        raisedHand = true;
                        //serverGUI.appendRaisedHandClient(clientName);
                        waitForServerPermission();
                        raisedHand = false;
                        permissionToSpeak = false;
                        System.out.println("Permission granted!");
                        break;
                    case Message.MESSAGE:
                        broadcast(clientName + ": " + message);
                        break;
                    case Message.BOWHAND:
                        display(clientName + ": " + "bowed the hand.");
                        raisedHand = false;
                        permissionToSpeak = false;
                        System.out.println("The client refused to speak and bowed the hand!");
                        break;

                    /*case Message.LOGOUT:
                        display(clientName + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case Message.WHOISIN:
                        writeMsg("List of the users connected at " + dataFormat.format(new Date()) + "\n");
                        // scan clientList the users connected
                        for(int i = 0; i < clientList.size(); ++i) {
                            ClientThread ct = clientList.get(i);
                            writeMsg((i+1) + ") " + ct.clientName + " since " + ct.creationDate);
                        }
                        break;*/
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(clientId);
            close();
        }

        private synchronized void waitForServerPermission() {
            try {
                this.wait();
                System.out.println("The server accepted your request. You can type your message!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            while (true){
//                if(permissionToSpeak){
//                    System.out.println("The server accepted your request. You can type your message!");
//                    break;
////                }
////                try {
////                    String serverAcknowledgement = (String) clientInputStream.readObject();
////                    // if console mode print the message and add back the prompt
////                    if (serverAcknowledgement.equals(clientId)){
////                        permissionToSpeak = true;
////                        return;
////                    }
////                }
////                catch(IOException e) {
////                    System.out.println("Server was interrupt...");
////                    return;
////                } catch (ClassNotFoundException e) {
////                    e.printStackTrace();
//                }
//
//            }
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if(clientOutputStream != null) clientOutputStream.close();
            }
            catch(Exception e) {}
            try {
                if(clientInputStream != null) clientInputStream.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        /*
         * Write a String to the Client output stream
         */
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                clientOutputStream.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + clientName);
                display(e.toString());
            }
            return true;
        }
    }

    class ApprovalSender extends Thread {
        //private ObjectOutputStream sOutput;

        public synchronized void run() {
            Scanner scan = new Scanner(System.in);
            while (true) {
                String selectedClientId = scan.nextLine();
                if (selectedClientId != null) {
                    if(clientHasRaisedHand(selectedClientId)) {
                        ClientThread selectedClient = clientList.stream()
                                                            .filter(cl -> (String.valueOf(cl.clientId).equals(selectedClientId)))
                                                            .collect(Collectors.toList()).get(0);
                        selectedClient.permissionToSpeak = true;
                        synchronized ( selectedClient ){
                            selectedClient.notify();
                        }
                        //selectedClient.interrupt();


                        System.out.println("You accepted client " + selectedClientId);
                        sendAcceptance(selectedClientId);
                    }
                    System.out.println("The clients connected are:");
                    clientList.stream().forEach(clientThread -> System.out.println(clientThread.clientName + " " + clientThread.clientId));
                    System.out.println(selectedClientId);
                }
            }
        }

        private boolean clientHasRaisedHand(String clientId) {
            boolean clientExists = clientList.stream()
                                            .map(cl -> String.valueOf(cl.clientId))
                                            .collect(Collectors.toList())
                                            .contains(clientId);
            if (clientExists) {
                List<Integer> raisedHandClients = clientList.stream()
                                            .filter(client -> client.raisedHand)
                                            .map(client -> client.clientId)
                                            .collect(Collectors.toList())
                                            ;
                return raisedHandClients.contains(Integer.parseInt(clientId));
            }
            return false;
        }

        void sendAcceptance(String clientId) {
            //sOutput = new ObjectOutputStream(serverSocket.);
            ClientThread clientThread = clientList.stream()
                    .filter(cl -> (String.valueOf(cl.clientId).equals(clientId)))
                    .collect(Collectors.toList()).get(0);
            //Message acceptMessage = Message.ACCEPTCLIENT;
            clientThread.writeMsg(clientId);
        }
    }
}



/*
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class Server {
    // a unique ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    //private ArrayList<ClientThread> clientList;
    // if I am in a GUI
    //private ServerGUI serverGUI;
    // to display time
    private SimpleDateFormat dataFormat;
    // the port number to listen for connection
    private static int port;
    // the boolean that will be turned of to stop the server
    private static boolean keepGoing = true;

    public static void main(String[] args) {
        // start server on port 1500 unless a PortNumber is specified
        int port = 1501;
        switch (args.length) {
            case 1:
                try {
                    port = Integer.parseInt(args[0]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;

        }
        // create a server object and start it
        //Server server = new Server(portNumber);
        //server.start();
        try {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                System.out.println("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();    // accept connection

                ObjectOutputStream clientOutputStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream clientInputStream = new ObjectInputStream(socket.getInputStream());
                // read the clientName
                String clientName = (String) clientInputStream.readObject();
                //display(clientName + " just connected.");

                Scanner scan = new Scanner(System.in);
                String msg = scan.nextLine();
                clientOutputStream.writeObject(msg);
                // if I was asked to stop
                if (!keepGoing)
                    break;
                //ClientThread t = new ClientThread(socket);  // make a thread of it
                //clientList.add(t);									// save it in the ArrayList
                //t.start();
            }
            // I was asked to stop
            // something went bad
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

*/
/*    *//*
*/
/*
*//*

*/
/*
     *  server constructor that receive the port to listen to for connection as parameter
     *  in console
     *//*
*/
/*
*//*

*/
/*
    public Server(int port) {
        this(port,null);
    }

    public Server(int port, ServerGUI serverGUI) {
        // GUI or not
        this.serverGUI = serverGUI;
        // the port
        this.port = port;
        // to display hh:mm:ss
        dataFormat = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        clientList = new ArrayList<ClientThread>();
    }

    public void start() {
        keepGoing = true;
		*//*
*/
/*
*//*

*/
/* create socket server and wait for connection requests *//*
*/
/*
*//*

*/
/*
        try
        {
            // the socket used by the server
            ServerSocket serverSocket = new ServerSocket(port);

            // infinite loop to wait for connections
            while(keepGoing)
            {
                // format message saying we are waiting
                display("Server waiting for Clients on port " + port + ".");

                Socket socket = serverSocket.accept();  	// accept connection
                // if I was asked to stop
                if(!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket);  // make a thread of it
                clientList.add(t);									// save it in the ArrayList
                t.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for(int i = 0; i < clientList.size(); ++i) {
                    ClientThread tc = clientList.get(i);
                    try {
                        tc.clientInputStream.close();
                        tc.clientOutputStream.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {
                        // not much I can do
                    }
                }
            }
            catch(Exception e) {
                display("Exception closing the server and clients: " + e);
            }
        }
        // something went bad
        catch (IOException e) {
            String msg = dataFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    *//*
*/
/*
*//*

*/
/*
     * For the GUI to stop the server
     *//*
*/
/*
*//*

*/
/*
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try {
            new Socket("localhost", port);
        }
        catch(Exception e) {
            // nothing I can really do
        }
    }
    *//*
*/
/*
*//*

*/
/*
     * Display an event (not a message) to the console or the GUI
     *//*
*/
/*
*//*

*/
/*
    private void display(String msg) {
        String time = dataFormat.format(new Date()) + " " + msg;
        if(serverGUI == null) {
            System.out.println(time);
        }
        else {
            serverGUI.appendEvent(time + "\n");
        }
    }

    private void display(String msg, String clientName) {
        String time = dataFormat.format(new Date()) + " " + msg;
        if(serverGUI == null) {
            System.out.printf("%s hand was raised", clientName);
            System.out.println(time);
        }
        else {
            serverGUI.appendEvent(clientName + "'s hand was raised");
            serverGUI.appendRaisedHandClient(msg);
            serverGUI.appendEvent(time + "\n");
        }
    }

    *//*
*/
/*
*//*

*/
/*
     *  to broadcast a message to all Clients
     *//*
*/
/*
*//*

*/
/*
    private synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = dataFormat.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI
        if(serverGUI == null)
            System.out.print(messageLf);
        else
            serverGUI.appendRoom(messageLf);     // append in the room window

        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = clientList.size(); --i >= 0;) {
            ClientThread ct = clientList.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                clientList.remove(i);
                display("Disconnected Client " + ct.clientName + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int clientId) {
        // scan the array list until we found the Id
        for(int i = 0; i < clientList.size(); ++i) {
            ClientThread ct = clientList.get(i);
            // found it
            if(ct.clientId == clientId) {
                clientList.remove(i);
                return;
            }
        }
    }

    *//*
*/
/*
*//*

*/
/*
     *  To run as a console application just open a console window and:
     * > java Server
     * > java Server portNumber
     * If the port number is not specified 1500 is used
     *//*
*/
/*
*//*

*/
/*


    *//*
*/
/*
*//*

*/
/** One instance of this thread will run for each client *//*
*/
/*
*//*

*/
/*
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream clientInputStream;
        ObjectOutputStream clientOutputStream;
        // my unique clientId (easier for deconnection)
        int clientId;
        // the Username of the Client
        String clientName;
        // the only type of message a will receive
        Message message;
        // the creationDate I connect
        String creationDate;

        // Constructore
        ClientThread(Socket socket) {
            // a unique clientId
            clientId = ++uniqueId;
            this.socket = socket;
			*//*
*/
/*
*//*

*/
/* Creating both Data Stream *//*
*/
/*
*//*

*/
/*
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                // create output first
                clientOutputStream = new ObjectOutputStream(socket.getOutputStream());
                clientInputStream  = new ObjectInputStream(socket.getInputStream());
                // read the clientName
                clientName = (String) clientInputStream.readObject();
                display(clientName + " just connected.");
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            creationDate = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    message = (Message) clientInputStream.readObject();
                }
                catch (IOException e) {
                    display(clientName + " Exception reading Streams: " + e);
                    break;
                }
                catch(ClassNotFoundException e2) {
                    break;
                }
                // the messaage part of the Message
                String message = message.getMessage();

                // Switch on the type of message receive
                switch(message.getType()) {

                    case Message.RAISEHAND:
                        display(clientName + ": " + "raised the hand.");
                        serverGUI.appendRaisedHandClient(clientName);
                        break;
                    case Message.MESSAGE:
                        broadcast(clientName + ": " + message);
                        break;
                    case Message.LOGOUT:
                        display(clientName + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case Message.WHOISIN:
                        writeMsg("List of the users connected at " + dataFormat.format(new Date()) + "\n");
                        // scan clientList the users connected
                        for(int i = 0; i < clientList.size(); ++i) {
                            ClientThread ct = clientList.get(i);
                            writeMsg((i+1) + ") " + ct.clientName + " since " + ct.creationDate);
                        }
                        break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(clientId);
            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if(clientOutputStream != null) clientOutputStream.close();
            }
            catch(Exception e) {}
            try {
                if(clientInputStream != null) clientInputStream.close();
            }
            catch(Exception e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (Exception e) {}
        }

        *//*
*/
/*
*//*

*/
/*
         * Write a String to the Client output stream
         *//*
*/
/*
*//*

*/
/*
        private boolean writeMsg(String msg) {
            // if Client is still connected send the message to it
            if(!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                clientOutputStream.writeObject(msg);
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + clientName);
                display(e.toString());
            }
            return true;
        }
    }*//*


}

*/
