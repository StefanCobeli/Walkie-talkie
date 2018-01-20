import com.oblac.nomen.Nomen;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/*
 * The Client with its GUI
 */
public class ClientGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    // will first hold "Username:", later on "Enter message"
    private JLabel userNameLabel;
    // to hold the Username and later on the messages
    private JTextField userNameField;
    // to hold the server address an the port number
    private JTextField tfServer, tfPort;

    public JButton raiseHandButton, bowHandButton;
    public JTextField messageField;

    // for the chat room
    private JTextArea ta;
    // if it is for connection
    private boolean connected;
    // the Client object
    public Client client;
    // the default port number
    private int defaultPort;
    private String defaultHost;
    private boolean permissionToSpeak = false;

    public void setPermissionToSpeak(boolean permissionToSpeak){
        this.permissionToSpeak = permissionToSpeak;
    }
    // Constructor connection receiving a socket number
    ClientGUI(String host, int port) {
        super("Chat Client");
        defaultPort = port;
        defaultHost = host;

        // The NorthPanel with:
        JPanel northPanel = new JPanel(new GridLayout(3,1));
        // the server name anmd the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:  "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));
        // adds the Server an port field to the GUI
        northPanel.add(serverAndPort);

        Nomen nomen = new Nomen();
        String userName = nomen.person().get();
        userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);

        // the Label and the TextField
        userNameLabel = new JLabel("User name: " + userName, SwingConstants.CENTER);

        //userNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        northPanel.add(userNameLabel);



        userNameField = new JTextField(userName);
        userNameField.setBackground(Color.WHITE);
        userNameField.setEnabled(false);
        //northPanel.add(userNameField);
        add(northPanel, BorderLayout.NORTH);

        // The CenterPanel which is the chat room
        ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add(centerPanel, BorderLayout.CENTER);

        // the 3 buttons-

        messageField = new JTextField("You  must raise your hand to speak!");
        messageField.setEnabled(false);
        messageField.requestFocus();

        raiseHandButton = new JButton("Raise hand!");
        raiseHandButton.addActionListener(this);

        bowHandButton= new JButton("Bow hand!");
        bowHandButton.addActionListener(this);
        bowHandButton.setEnabled(false);

        JPanel southPanel = new JPanel();

        southPanel.add(messageField);
        southPanel.add(raiseHandButton);
        southPanel.add(bowHandButton);

        add(southPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);
        //userNameField.requestFocus();
        connectToServer();

    }

    // called by the Client to append text in the TextArea
    void append(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    // called by the GUI is the connection failed
    // we reset our buttons, userNameLabel, textfield
    void connectionFailed() {

        // reset port number and host name as a construction time
        tfPort.setText("" + defaultPort);
        tfServer.setText(defaultHost);
        // let the user change them
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        // don't react to a <CR> after the username
        userNameField.removeActionListener(this);
        messageField.removeActionListener(this);
        connected = false;
    }

    void connectToServer(){
        {
            // ok it is a connection request
            String username = userNameField.getText().trim();
            // empty username ignore it
            if(username.length() == 0)
                return;
            // empty serverAddress ignore it
            String server = tfServer.getText().trim();
            if(server.length() == 0)
                return;
            // empty or invalid port numer, ignore it
            String portNumber = tfPort.getText().trim();
            if(portNumber.length() == 0)
                return;
            int port = 0;
            try {
                port = Integer.parseInt(portNumber);
            }
            catch(Exception en) {
                return;   // nothing I can do if port number is not valid
            }

            // try creating a new Client with GUI
            client = new Client(server, port, username, this);
            // test if we can start the Client
            if(!client.start())
                return;
            connected = true;

            // disable the Server and Port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // Action listener for when the user enter a message
            userNameField.addActionListener(this);
            messageField.addActionListener(this);
        }
    }

    void setGUIInitialConfiguration(){
        messageField.setEnabled(false);
        messageField.setText("You  must raise your hand to speak!");
        raiseHandButton.setEnabled(true);
        bowHandButton.setEnabled(false);
        permissionToSpeak = false;
        this.client.serverListener.clientWasKicked = false;
        synchronized (this.client.serverListener){
            this.client.serverListener.notify();
        }
    }
    /*
    * Button or JTextField clicked
    */
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if(o == raiseHandButton){
            raiseHandButton.setEnabled(false);
            bowHandButton.setEnabled(true);
            client.sendMessage(new Message(Message.RAISEHAND, ""));
            return;
        }

        if(o == bowHandButton){
            client.sendMessage(new Message(Message.BOWHAND, ""));

            setGUIInitialConfiguration();
            return;
        }

        if(permissionToSpeak){
            client.sendMessage(new Message(Message.MESSAGE, messageField.getText()));

            setGUIInitialConfiguration();
            return;
        }
    }

    // to start the whole thing the server
    public static void main(String[] args) {
        new ClientGUI("localhost", 1600);
    }

}
