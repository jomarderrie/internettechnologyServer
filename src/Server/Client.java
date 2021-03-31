package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class Client implements Runnable {
    private Socket socket;
    private Server server;
    private DataInputStream inputStream;
    private OutputStream outputStream;
    private String name;
    private String sender;
    private boolean sending = false;
    private volatile Client receiver;
    private volatile boolean pinging = false;
    private volatile boolean receiving = false;
    //used to log
    boolean logging = true;


    private ClientPinger clientPinger;

    private Set<Client> clientsThreads;
    private ArrayList<Group> groups = new ArrayList<>();
    String[] state = new String[]{"online", "offline", "login", "threadKill"};
    int index = 2; //default state when logging in

    public Client(Socket socket, Server server) {

        this.socket = socket;
        this.server = server;
        this.clientsThreads = this.server.getThreads();
        this.groups = this.server.getGroups();
    }


    /**
     * dogshit thread to run the code
     */
    @Override
    public void run() {
        try {
            outputStream = socket.getOutputStream();
            inputStream = new DataInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            sendMessageToClient("hello welcome to the Lao Tzu client");
            while (!state[index].equals("offline")) {
                String message = reader.readLine();
                String[] messageSplit = message.split(" ");

                if (logging){

                System.out.println(Arrays.toString(messageSplit));
                }
                switch (messageSplit[0]) {
                    case "CONN":
                        loginUser(messageSplit);
                        break;
                    case "BCST":
                        broadCast(messageSplit);
                        break;
                    case "LIU":
                        sendUsers();
                        break;
                    case "SPM":
                        sendPrivateMessage(messageSplit);
                        break;
                    case "QUIT":
                        quit();
                        break;
                    case "JOINGRP":
                        joinGroup(messageSplit);
                        break;
                    case "CRTGRP":
                        createGroup(messageSplit);
                        break;
                    case "GRPS":
                        listGroups();
                        break;
                    case "GMSG":
                        sendGroupMessage(messageSplit);
                        break;
                    case "LVGRP":
                        leaveGroup(messageSplit);
                        break;
                    case "KICK":
                        kickGroupMember(messageSplit);
                        break;
                    case "UPLOAD":
                        startUploading(messageSplit);
                        break;
                    case "DWNLD":
                        startDownloading(messageSplit);
                        break;
                    case "PONG":
                        pinging = true;
                        break;
                    case "DCL":
                        receiver.setReceiving(false);
                        break;
                    default:
                        error();
                        break;
                }
            }
            clientsThreads.remove(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void quit() {
        String[] quitMessage = new String[]{"BCST", " has left chat" + this.name};
        broadCast(quitMessage);
        sendMessageToClient("BYE bye see you soon");
        index = 3;
    }

    /**
     * Starts the clinet pinger
     */
    private void startPing() {
        clientPinger = new ClientPinger(this);
        clientPinger.run();
    }

    /**Download afile
     * @param messageSplit 
     * @throws IOException
     */
    private void startDownloading(String[] messageSplit) throws IOException {
        receiver = getClientThread(messageSplit[1]);
        sendCertainUserMessageTheRealDeal(receiver, "OPEN");
        try {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Socket socket = new Socket("localhost", 5001);
            File file = new File("smallDog.txt");
            byte[] bytes = new byte[8192];
            InputStream in = new FileInputStream(file);
            OutputStream out = socket.getOutputStream();
            int count;
            while ((count = in.read(bytes)) > 0) {
                out.write(bytes, 0, count);
            }

            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** upload a file
     * @param messageSplit
     * @throws IOException
     */
    private void startUploading(String[] messageSplit) throws IOException {
        receiver = getClientThread(messageSplit[2]);
        sender = this.getName();
        if ((receiver == null)) {
            sendMessageToClient("User doesnt even exist bro");
            return;
        }

        if (!receiver.isReceiving()) {
            setReceiving(true);

            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(5000);
            } catch (IOException ex) {
                System.out.println("Can't setup server on this port number. ");
            }

            Socket socket = null;
            InputStream in = null;
            FileOutputStream fileOutputStream = null;

            try {
                socket = serverSocket.accept();
            } catch (IOException ex) {
                System.out.println("Can't accept client connection. ");
            }
            try {
                in = socket.getInputStream();
            } catch (IOException ex) {
                System.out.println("Can't get socket input stream. ");
            }

            try {
                fileOutputStream = new FileOutputStream("smallDog.txt");
            } catch (FileNotFoundException ex) {
                System.out.println("File not found. ");
            }

            byte[] bytes = new byte[8192];
            int count;
            while ((count = in.read(bytes)) >= 0) {
                fileOutputStream.write(bytes, 0, count);
            }
            sendMessageToClient("asking user " + receiver + " if he wants to receive the file! ");
            fileOutputStream.close();
            in.close();
            socket.close();
            serverSocket.close();
            sendCertainUserMessage(receiver, "RQST " + sender + " " + " .ACCEPT to download the file or .DECLINE");

        } else {
            sendMessageToClient("CLient busy");
        }
    }


    private void sendCertainUserMessageTheRealDeal(Client clientToSend, String message) {
        for (Client clientsThread : clientsThreads) {
            System.out.println(clientsThread.getName());
            if (clientsThread.getName().equals(clientToSend.getName())) {
                clientsThread.sendMessageToClient(message);
                break;
            }
        }
    }

    private void sendCertainUserMessage(Client clientToSend, String message) {
        for (Client clientsThread : clientsThreads) {
            System.out.println(clientsThread.getName());
            if (clientsThread != this && clientsThread.getName().equals(clientToSend.getName())) {
                clientsThread.sendMessageToClient(message);
                break;
            }
        }
    }

    /**
     * @param messageSplit
     */
    private void kickGroupMember(String[] messageSplit) {
        String groupName = messageSplit[2];
        String userToKick = messageSplit[3];
        Group group = groupExist(groupName);
        assert group != null;
        boolean premium = true;
        if (premium) {
            System.out.println("Looks like you need to pay Lao tzu a visit for a premium subscription!");
        } else {
            if (group.exist(this) && groupExist(groupName) != null) {
                Client client = group.getMember(userToKick);
                if (client != null) {
                    sendGroupMessage(new String[]{"o", "t", group.getName(), "KICK init for user " + userToKick});
                    //send everyone a message to vote and then keep counting how many votes you have dont send the owner and
                    for (int i = 0; i < group.getClients().size(); i++) {
                        if (group.getClients().get(i).getName().equals(client.getName())) {
                            sendMessageToClient("KICK " + "You are about to get kicked prepare yourself");
                        }
                    }
                }
            } else {
                sendMessageToClient("Error Already part of the group");
            }
        }
    }

    private void leaveGroup(String[] messageSplit) {
        String groupName = messageSplit[2];
        Group group = groupExist(groupName);

        if (group.exist(this)) {
            if (group.leave(this)) {
                sendMessageToClient("LVGRP " + groupName + " group");
                sendGroupMessage(new String[]{"o", "t", group.getName(), "Left " + this.getName() + " " + group.getName()});
            } else {
                sendMessageToClient("Error occured please try again later");
            }
        } else {
            sendMessageToClient("Error Already part of the group");
        }
    }


    /**
     * Show a list of groups
     */
    private void listGroups() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("GRPS ");
        for (Group group : groups) {
            stringBuilder.append(group.getName()).append(" ");
        }
        sendMessageToClient(stringBuilder.toString());
    }


    /**Create a group
     * @param messageSplit
     */
    private void createGroup(String[] messageSplit) {
        String groupName = messageSplit[2];
        if ((groupName.matches("[a-zA-Z]{1,14}"))) {
            Group group = groupExist(groupName);
            if (group == null) {
                sendMessageToClient("CRTGRP " + groupName + " owner is " + this.getName());
                groups.add(new Group(groupName, this));
            }
        } else {
            sendMessageToClient("Error group doesnt match pattern regex");
        }
    }


    /**
     * @param groupName The group that has to be searched
     * @return the group if true else return null
     */
    private Group groupExist(String groupName) {
        for (Group group : groups) {
            if (group.getName().equals(groupName)) {
                return group;
            }
        }
        return null;
    }

    /**Join a group
     * @param messageSplit
     */
    private void joinGroup(String[] messageSplit) {
        String groupName = messageSplit[2];
        Group group = groupExist(groupName);
        if (group != null) {
            if (!group.exist(this)) {
                group.join(this);
                sendMessageToClient("JOIN " + groupName + " joined");
                sendGroupMessage(new String[]{"o", "t", group.getName(), "Joined " + this.getName() + " " + group.getName()});
            } else {
                sendMessageToClient("Error Already part of the group");
            }
        } else {
            sendMessageToClient("Error group doesnt exist");
        }
    }

    /**Sends a message to a certain group
     * @param messageSplit
     */
    private void sendGroupMessage(String[] messageSplit) {

        //check if its an actual message not dogshit
        if (checkMessageLength(messageSplit)) {
            sendMessageToClient("Error length to short");
        } else {
            String groupName = messageSplit[2];
            Group group = groupExist(groupName);
            StringBuilder stringBuilder = new StringBuilder();
            //find group
            if (group != null) {
                for (int i = 2; i < messageSplit.length; i++) {
                    stringBuilder.append(messageSplit[i]).append(" ");
                }
                //send message to all group members
                for (Client client : group.getClients()) {
                    if (client != this) {
                        client.sendMessageToClient("GMSG " + stringBuilder);
                    }
                }
            } else {
                sendMessageToClient("Error group doesnt exist");
            }
        }
    }

    private boolean checkMessageLength(String[] messageSplit) {
        if (messageSplit.length <= 2) {
            return true;
        }
        return false;
    }


    /**Sends a private message to a certain user
     * @param messageSplit
     */
    private void sendPrivateMessage(String[] messageSplit) {
        String guyToSendTo = messageSplit[2];
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 3; i < messageSplit.length; i++) {
            stringBuilder.append(messageSplit[i]).append(" ");
        }
        Client client = null;
        boolean userAlive = false;

        for (Client clientsThread : clientsThreads) {
            if (clientsThread != this && clientsThread.getName().equals(guyToSendTo)) {
                userAlive = true;
                client = clientsThread;
                break;
            }
        }
        if (userAlive) {
            client.sendMessageToClient("SPM private message from: " + getName() + " " + stringBuilder);
        } else {
            this.sendMessageToClient("Person doesnt exist try another person");
        }
    }

    private void error() {
        sendMessageToClient("Error invalid command");
    }

    /**
     * shows the list of users
     */
    private void sendUsers() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("LIU ").append("The list of users: ");
        for (Client clientsThread : clientsThreads) {
            if (clientsThread.getName() != this.getName()) {
                stringBuilder.append(clientsThread.getName()).append(" , ");
            }
        }
        sendMessageToClient(stringBuilder.toString());
    }


    /**
     * Sends a message to the client
     *
     * @param message The message you want to send
     */
    void sendMessageToClient(String message) {
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println(message);
        writer.flush();
    }

    /** Sends a message to all clients
     * @param message Message you want to send
     */
    private void broadCast(String[] message) {
        System.out.println(Arrays.toString(message));
        //build a string of the message
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BCST Broadcast message from: ").append(getName()).append(" Message: ");
        for (int i = 2; i < message.length; i++) {
            stringBuilder.append(message[i]).append(" ");
        }
        for (Client clientsThread : clientsThreads) {
            if (clientsThread != this) {
                clientsThread.sendMessageToClient(stringBuilder.toString());
            } else {
                sendMessageToClient("CNF Message sent to all clients");
            }
        }
    }

    private boolean isUserExist(String name) {
        for (Client clientsThread : clientsThreads) {
            if (clientsThread != this && name.equals(clientsThread.getName())) {
                return true;
            }
        }
        return false;
    }

    private Client getClientThread(String name) {
        for (Client clientsThread : clientsThreads) {
            if (name.equals(clientsThread.getName())) {
                return clientsThread;
            }
        }
        return null;
    }

    private void loginUser(String[] message) {
        //cant login twice buddy
        if (!state[index].equals("online")) {
            //check if the message matches the following regex
            if (!message[1].matches("[a-zA-Z]{1,14}")) {
                sendMessageToClient("invalid name follow the follow this regex [a-zA-Z]{1,14} ");
            }
//            else if (message.length>2)
            //check if the user already existing in the state
            else if (isUserExist(message[1])) {
                sendMessageToClient("user already alive");
            } else {
                name = message[1];
                sendMessageToClient("200 " + name);
                index = 0;
                startPing();
            }
        } else {
            sendMessageToClient("user already alive");
        }
    }

    public boolean isPinging() {
        return pinging;
    }


    public OutputStream getOutputStream() {
        return outputStream;
    }


    public void setPinging(boolean pinging) {
        this.pinging = pinging;
    }



    public String getName() {
        return name;
    }

    public boolean isReceiving() {
        return receiving;
    }


    public void setReceiving(boolean receiving) {
        this.receiving = receiving;
    }


}
