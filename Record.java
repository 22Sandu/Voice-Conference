
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Record extends Thread {

    private TargetDataLine line;
    byte buffer[];
    int bufferSize;
    private final static int port = 12345;
    private final InetAddress host;
    DatagramSocket socket = null;
    private int sequence = 0;
    private static final int packetSize = 500;

    public Record(InetAddress host, DatagramSocket socket) {        // constructor - initializing parameters
        super("record");
        try {
            final AudioFormat format = getFormat();
            DataLine.Info info = new DataLine.Info(
                    TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            bufferSize = (int) packetSize;
        } catch (LineUnavailableException e) {
            Logger.getLogger(Record.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-2);
        }
        this.socket = socket;
        this.host = host;
    }

    @Override
    public void run() {
        try {
            
            while (true) {

                buffer = Serialize();                                                       // final stream of bytes to be sent         

                int count = line.read(buffer, 4, buffer.length - 4);                        // read audio target data line, count = 2000
                if (count > 0) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, host, port);
                    socket.send(packet);
                    sequence++;                                                             // increased for each packet sent

                }
            }
        } catch (Exception e) {
            Logger.getLogger(Record.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                Logger.getLogger(Record.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private AudioFormat getFormat() {                               // define format of the audio recorded
        float sampleRate = 48000;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate,
                sampleSizeInBits, channels, signed, bigEndian);
    }

    public static final byte[] intToByteArray(int value) {          // function to convert int to byte[] array
        return new byte[]{
            (byte) (value >>> 24),
            (byte) (value >>> 16),
            (byte) (value >>> 8),
            (byte) value};
    }

    private byte[] Serialize() {
        byte[] a = new byte[bufferSize];
        byte[] b = intToByteArray(sequence);                                        // sequence number for each packet

        return ByteBuffer.allocate(a.length + b.length).put(b).put(a).array();      // final stream of bytes to be sent 
    }
}
