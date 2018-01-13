import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

/*
 * The server as a GUI
 */
public class ServerGUI extends JFrame implements ActionListener, WindowListener {

    private static final long serialVersionUID = 1L;
    // the stop and start buttons
    private JButton stopStart;
    // JTextArea for the chat room and the events
    private JTextArea chat, event;
    // The port number
    private JTextField tPortNumber;
    // my server
    private Server server;

    //raised hand clients
    private JPanel raisedHandClients;
    private HashMap<Integer, JButton> acceptButtons;
    private HashMap<Integer, JButton> rejectButtons;

    // server constructor that receive the port to listen to for connection as parameter
    ServerGUI(int port) {
        super("Chat Server");
        server = null;
        // in the NorthPanel the PortNumber the Start and Stop buttons
        JPanel north = new JPanel();
        north.add(new JLabel("Port number: "));
        tPortNumber = new JTextField("  " + port);
        north.add(tPortNumber);
        // to stop or start the server, we start with "Start"
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add(north, BorderLayout.NORTH);

        // Client with raised hands
        //raisedHandClients.setVisible(false);
        //add raised hands panel

        raisedHandClients = new JPanel(new GridLayout(5, 3));
        add(raisedHandClients, BorderLayout.SOUTH);

        // the event and chat room
        JPanel center = new JPanel(new GridLayout(2,1));
        chat = new JTextArea(80,80);
        chat.setEditable(false);
        appendRoom("Chat room.\n");
        center.add(new JScrollPane(chat));
        event = new JTextArea(80,80);
        event.setEditable(false);
        appendEvent("Events log.\n");
        center.add(new JScrollPane(event));
        add(center);

        // need to be informed when the user click the close button on the frame
        addWindowListener(this);
        setSize(400, 600);
        setVisible(true);


    }

    // append message to the two JTextArea
    // position at the end
    void appendRoom(String str) {
        chat.append(str);
        chat.setCaretPosition(chat.getText().length() - 1);
    }
    void appendEvent(String str) {
        event.append(str);
        event.setCaretPosition(chat.getText().length() - 1);

    }

    void appendRaisedHandClient(String clientName, int clientId){

        //raisedHandClients = new JPanel();
        //add(raisedHandClients);
        //JPanel mockPanel = new JPanel(new GridBagLayout());
        //Panel
        //add(mockPanel, BorderLayout.SOUTH);

        //raisedHandClients.add(new JLabel(new StringBuilder(clientName).reverse().));
        raisedHandClients.add(new JLabel(String.valueOf(clientId)));

        JButton rejectButton = new JButton("Reject");
        rejectButton.setName(String.valueOf(clientId));
        rejectButton.addActionListener(this);

        JButton acceptButton = new JButton("Accept");
        acceptButton.setName(String.valueOf(clientId));
        acceptButton.addActionListener(this);

        raisedHandClients.add(acceptButton);

        //rejectButton.setAction(rejectHand(clientName));
        //rejectButton.setAction(new rejectHand(clientName));
        raisedHandClients.add(rejectButton);
        raisedHandClients.setVisible(true);
        raisedHandClients.repaint();


        acceptButton.putClientProperty(Integer.valueOf(clientId), acceptButton);
        rejectButton.putClientProperty(Integer.valueOf(clientId), rejectButton);
    }

    void acceptHand(String clientId){
        //server.ApprovalSender.sen/
    }
    void rejectHand(String clientName){}

    // start or stop where clicked
    public void actionPerformed(ActionEvent e) {

        /*
        Set here the accept, reject behavior

        Object o = e.getSource();
        // if it is the Logout button
        if(o == logout) {
            raiseHandButton.setVisible(true);
            client.sendMessage(new Message(Message.LOGOUT, ""));
            return;
        }

        */

        // if running we have to stop
        if(server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        // OK start the server
        int port;
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
        }
        catch(Exception er) {
            appendEvent("Invalid port number");
            return;
        }
        // ceate a new Server
        server = new Server(port, this);
        // and start it as a thread
        new ServerRunning().start();
        stopStart.setText("Stop");
        tPortNumber.setEditable(false);
    }

    // entry point to start the Server
    public static void main(String[] arg) {
        // start server default port 1500
        new ServerGUI(1500);
    }

    /*
     * If the user click the X button to close the application
     * I need to close the connection with the server to free the port
     */
    public void windowClosing(WindowEvent e) {
        // if my Server exist
        if(server != null) {
            try {
                server.stop();			// ask the server to close the conection
            }
            catch(Exception eClose) {
            }
            server = null;
        }
        // dispose the frame
        dispose();
        System.exit(0);
    }
    // I can ignore the other WindowListener method
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    /*
     * A thread to run the Server
     */
    class ServerRunning extends Thread {
        public void run() {
            server.start();         // should execute until if fails
            // the server failed
            stopStart.setText("Start");
            tPortNumber.setEditable(true);
            appendEvent("Server crashed\n");
            server = null;
        }
    }

}
