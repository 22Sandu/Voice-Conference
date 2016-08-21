
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class ControlSend extends Thread {

    private boolean flag;
    private final InetAddress group;
    private final int port1;
    private final int port2;

    ControlSend(InetAddress group, int port1, int port2) {
        this.group = group;
        this.port1 = port1;
        this.port2 = port2;
        synchronized (Peer2.lock) {
            this.flag = Peer2.flag;
        }
    }

    @Override
    public void run() {

        while (true) {

            MulticastSocket socket = null;
            String userInput = null;
            try {
                socket = new MulticastSocket();         // new socket for this process

                Scanner sin = new Scanner(System.in);
                userInput = sin.nextLine();             // get user's command line input

            } catch (Exception e) {
                Logger.getLogger(ControlSend.class.getName()).log(Level.SEVERE, null, e);
            }
            if (userInput.equalsIgnoreCase("end")) {        // if input is end, end the ongoing session
                this.flag = false;
                Thread t = getThreadByName("record");
                if (t.isAlive()) {                            // stop recording if there is a recording 
                    t.suspend();
                }
            } else if (userInput.equalsIgnoreCase("request")) { // to start a new session

                try {
                    byte[] data = userInput.getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, group, port2);
                    socket.send(packet);                        // request from other users

                } catch (Exception e) {
                    Logger.getLogger(ControlSend.class.getName()).log(Level.SEVERE, null, e);
                }
                String receivedData = null;
                try {

                    byte[] data = new byte[256];

                    MulticastSocket socket2 = new MulticastSocket(port2);
                    socket2.joinGroup(group);

                    DatagramPacket packet2 = new DatagramPacket(data, data.length);
                    socket2.setSoTimeout(2000);                                     // wait for a time out whether a decline message is received
                    socket2.receive(packet2);
                    receivedData = new String(packet2.getData());
                    
                } catch (SocketTimeoutException e) {
                    
                } catch (Exception e) {
                    Logger.getLogger(ControlSend.class.getName()).log(Level.SEVERE, null, e);
                }
                if (receivedData == null || !receivedData.equalsIgnoreCase("decline")) {
                    System.out.println("accept");
                    try {
                        MulticastSocket socket3 = new MulticastSocket();
                        Record r = new Record(group, socket3);
                        r.start();
                    } catch (IOException ex) {
                        Logger.getLogger(ControlSend.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("declined");                             // there is ongoing session
                }
            } else {
                System.out.println("Input valid commands : <End / Request>");
            }
        }
    }

    public Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) {
                return t;
            }
        }
        return null;
    }

}
