import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * The server that can be run both as a console application or a GUI
 */
public class Server {
    // a unique ID for each connection
    private static int idIncrementer;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> clientList;
    // to display time
    private SimpleDateFormat dateFormat;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned of to stop the server
    private boolean keepGoing;

    //private Queue<Integer> waitingQueue;
    //private TreeMap<Long, Integer> waitingQueue;
    private BlockingQueue<ClientThread> waitingQueue;

    /*
     *  server constructor that receive the port to listen to for connection as parameter
     *  in console
     */
    public Server(int port) {
        // the port
        this.port = port;
        // to display hh:mm:ss
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        // ArrayList for the Client list
        clientList = new ArrayList<ClientThread>();
        waitingQueue = new LinkedBlockingQueue<ClientThread>();
    }

    public void start() {
        keepGoing = true;
		/* create socket server and wait for connection requests */
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
                ClientThread clientThread = new ClientThread(socket);  // make a thread of it
                clientList.add(clientThread);									// save it in the ArrayList
                clientThread.start();
            }
            // I was asked to stop
            try {
                serverSocket.close();
                for(int i = 0; i < clientList.size(); ++i) {
                    ClientThread clientThread = clientList.get(i);
                    try {
                        clientThread.sInput.close();
                        clientThread.sOutput.close();
                        clientThread.socket.close();
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
            String msg = dateFormat.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }

    /*
     * Display an event (not a message) to the console or the GUI
     */
    private void display(String msg) {
        String time = dateFormat.format(new Date()) + " " + msg;
        System.out.println(time);
    }
    /*
     *  to broadcast a message to all Clients
     */
    private synchronized void broadcast(String message) {
        // add HH:mm:ss and \n to the message
        String time = dateFormat.format(new Date());
        String messageLf = time + " " + message + "\n";
        // display message on console or GUI
        System.out.print(messageLf);
        // we loop in reverse order in case we would have to remove a Client
        // because it has disconnected
        for(int i = clientList.size(); --i >= 0;) {
            ClientThread ct = clientList.get(i);
            // try to write to the Client if it fails remove it from the list
            if(!ct.writeMsg(messageLf)) {
                clientList.remove(i);
                display("Disconnected Client " + ct.username + " removed from list.");
            }
        }
    }

    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list until we found the Id
        for(int i = 0; i < clientList.size(); ++i) {
            ClientThread ct = clientList.get(i);
            // found it
            if(ct.id == id) {
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
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for deconnection)
        int id;
        // the Username of the Client
        String username;
        // the only type of message a will receive
        Message message;
        // the date I connect
        String date;
        boolean hasRaisedHand = false;

        // Constructore
        ClientThread(Socket socket) {
            // a unique id
            id = ++idIncrementer;
            this.socket = socket;
			/* Creating both Data Stream */
            System.out.println("Thread trying to create Object Input/Output Streams");
            try
            {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                display(username + " just connected.");
            }
            catch (IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but I read a String, I am sure it will work
            catch (ClassNotFoundException e) {
            }
            date = new Date().toString() + "\n";
        }

        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while(keepGoing) {
                // read a String (which is an object)
                try {
                    message = (Message) sInput.readObject();
                }
                catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
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
                        //TODO!!!!!!!!!!!!!!!!!!
                        //optionally check if ID is already in queue
                        //waitingQueue.add(Integer.valueOf(this.id));
                        //Long.valueOf(System.currentTimeMillis()),
                        try {
                            waitingQueue.put(this);
                            if (waitingQueue.size() == 1){
                                sOutput.writeObject(new Message(Message.PERMISSION, ""));
                                broadcast(this.username + " is typing!");
                            }
                            else{
                                synchronized (this){
                                    wait();
                                    sOutput.writeObject(new Message(Message.PERMISSION, ""));
                                    broadcast(this.username + " is typing!");
                                }
                            }
                            //wait until he is the single person in the queue
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        hasRaisedHand = true;
                        break;
                    case Message.BOWHAND:
                        broadcast(username + " bowed the hand!");
                        waitingQueue.remove(this);
                        hasRaisedHand = false;
                        if (waitingQueue.size() >= 1){
                            ClientThread nextClient = waitingQueue.peek();
                            synchronized (nextClient){
                                nextClient.notify();
                            }
                        }
                        break;
                    case Message.MESSAGE:
                        broadcast(username + ": " + message);
                    //case Message.BOWHAND:
                        waitingQueue.remove(this);
                        hasRaisedHand = false;
                        if (waitingQueue.size() >= 1){
                            ClientThread nextClient = waitingQueue.peek();
                            synchronized (nextClient){
                                nextClient.notify();
                            }
                        }
                        break;
                }
            }
            // remove myself from the arrayList containing the list of the
            // connected Clients
            remove(id);
            close();
        }

        // try to close everything
        private void close() {
            // try to close the connection
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(Exception e) {}
            try {
                if(sInput != null) sInput.close();
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
                sOutput.writeObject(new Message(Message.MESSAGE, msg));
            }
            // if an error occurs, do not abort just inform the user
            catch(IOException e) {
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}

