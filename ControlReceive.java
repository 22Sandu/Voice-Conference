
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

class ControlReceive extends Thread {

    private final boolean flag;
    private final InetAddress group;
    private final int port1;
    private final int port2;

    ControlReceive(InetAddress group, int port1, int port2) {
        this.group = group;
        this.port1 = port1;
        this.port2 = port2;
        synchronized (Peer2.lock) {
            this.flag = Peer2.flag;
        }
    }

    @Override
    public void run() {

        try {

            byte[] data = new byte[256];                                            // byte array for control messages

            MulticastSocket socket1 = new MulticastSocket(port2);
            socket1.joinGroup(group);
            DatagramPacket packet2 = new DatagramPacket(data, data.length);
            
            socket1.receive(packet2);
            if (flag) {
                byte[] data2 = "decline".getBytes();                                // send decline if there is ongoing session (based on flag) else nothing
                socket1.send(new DatagramPacket(data2, data2.length, group, port2));
            } else {
                try {
                    MulticastSocket socket2 = new MulticastSocket(port1);
                    socket2.joinGroup(group);
                    Reception r2 = new Reception(socket2);                  // thread for receiving and transmission of packets
                    r2.start();
                } catch (Exception e) {
                    Logger.getLogger(Peer2.class.getName()).log(Level.SEVERE, null, e);
                }
            }

        } catch (Exception e) {
            Logger.getLogger(ControlReceive.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
