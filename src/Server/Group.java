package Server;

import java.util.ArrayList;

public class Group {
    private Client manager;
    private String name;
    private ArrayList<Client> clients = new ArrayList<Client>();

    public Group(String name, Client manager) {
        this.name = name;
        this.manager = manager;
        clients.add(manager);
    }

    public void join(Client client) {
        if (!exist(client)) {
            clients.add(client);
        }
    }

    public boolean exist(Client client) {
        if (clients.size() > 0) {
            for (Client client1 : clients) {
                if (client1.getName().equals(client.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param client
     * @return
     */
    public boolean leave(Client client) {
        if (exist(client)) {
            for (int i = 0; i < clients.size(); i++) {
                if (client.getName().equals(clients.get(i).getName())) {
                    clients.remove(client);
                    return true;
                }
            }
        }
        return false;
    }


    /** kick a certain member
     * @param userToKick
     * @return
     */
    public Client getMember(String userToKick) {
        if (clients.size() > 0) {
            for (Client client1 : clients) {
                if (client1.getName().equals(userToKick)) {
                    return client1;
                }
            }
        }
        return null;
    }

    /**
     * @return Get the manager of the group.
     */
    public Client getManager() {
        return manager;
    }

    /**
     * @return Get the name from the group
     */
    public String getName() {
        return name;
    }

    /**
     * @return Get all clients for a certain group
     */
    public ArrayList<Client> getClients() {
        return clients;
    }


    /**
     * @return The group information
     */
    @Override
    public String toString() {
        return "Group{" +
                "manager=" + manager +
                ", name='" + name + '\'' +
                ", clients=" + clients +
                '}';
    }


}
