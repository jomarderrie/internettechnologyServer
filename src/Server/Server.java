package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Server {
    private ServerSocket serverSocket;
    private Set<Client> threads;
    private ArrayList<Group> groups = new ArrayList<Group>();
    public Server() {

    }

    public void run(){
        try {
            serverSocket = new ServerSocket(1337);
            threads = new HashSet<>();
            while (true){
                Socket socket = serverSocket.accept();
                Client client = new Client(socket,this);
                threads.add(client);
                new Thread(client).start();
                System.out.println("clients: " + threads.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Set<Client> getThreads() {
        return threads;
    }




    public ArrayList<Group> getGroups() {
        return groups;
    }
}
