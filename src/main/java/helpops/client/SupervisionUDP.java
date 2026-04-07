package helpops.client;

import helpops.model.Token;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SupervisionUDP extends Thread {
    private DatagramSocket socket;
    private boolean actif = true;
    private final Token token;

    public SupervisionUDP(Token token) {
        this.token = token;
    }

    public void arreter() {
        actif = false;
        if (socket != null) socket.close();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            String inscription = "SUB: " + token.getLogin();
            byte[] bufSub = inscription.getBytes();
            InetAddress addr = InetAddress.getByName("localhost");

            socket.send(new DatagramPacket(bufSub, bufSub.length, addr, 5000));

            byte[] buffer = new byte[1024];
            while (actif) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                System.out.println("\n\033[0;33m>>> [ SUPERVISION ] " + message + "\033[0m");
                System.out.print("Choix : ");
            }
        } catch (Exception e) {
            if (actif) System.err.println("[UDP] Flux interrompu.");
        }
    }
}