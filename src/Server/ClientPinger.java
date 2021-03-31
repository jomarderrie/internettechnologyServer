package Server;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;

public class ClientPinger implements Runnable {
    private Client client;
    private volatile boolean running;

    public ClientPinger(Client client) {
        this.client = client;
        this.running = true;
        //No constructor needed for this class.
    }

    /**
     * Thread will check every 10 seconds if the client is still pinging
     */
    @Override
    public void run() {
        Timer t = new Timer();
        if (running){

            //used to ping the client
        t.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        if (!client.isPinging()) {
                            System.out.println("ping has ended from client " + getClient().getName());
                            t.cancel();
                            t.purge();
                        }
                        client.setPinging(false);
                        PrintWriter writer = new PrintWriter(client.getOutputStream());
                        writer.println("PING");
                        writer.flush();
                    }
                }, 3000,
                10000);
        }
    }

    public Client getClient() {
        return client;
    }

}
