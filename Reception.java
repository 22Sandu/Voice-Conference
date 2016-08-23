
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Reception extends Thread {

    DatagramSocket socket;
    private final static int packetsize = 504;      // audio buffer - 2000 + sequence number(int)
    public byte[] receivedData;
    AudioInputStream ais;
    SourceDataLine line;
    int bufferSize;
    byte buffer[];
    int prevSequence = -1;

    public Reception(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[packetsize], packetsize);

            int counter = 0;
            int size = 100;

            while (true) {

                socket.receive(packet);                                                // received packet - 2004 size

                receivedData = packet.getData();
                int sequence = Deserialize(receivedData);                               // received packet sequence number
                // packet loss not handled
                if (sequence <= prevSequence) {                                         // re-ordering not done, packets received late are discarded
                    continue;
                }

                counter++;

                if (sequence >= size) {
                    System.out.println(sequence - counter);                             // for testing purposes
                    size += size;
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();                // for play back
                out.write(receivedData, 4, receivedData.length - 4);                    // skip sequence number

                prevSequence = sequence;                                                // sequence no of previous packet

                out.close();

                this.initiatePlay(out);                                                 // initialize variables relevant for playback

                int count;
                while ((count = ais.read(
                        buffer, 0, buffer.length)) != -1) {
                    if (count > 0) {
                        line.write(buffer, 0, count);                                   // writing to target data line

                    }
                }
                line.drain();
                line.close();
            }
        } catch (IOException e) {
            Logger.getLogger(Reception.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-3);
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                Logger.getLogger(Reception.class.getName()).log(Level.SEVERE, null, e);
            }
        }

    }

    private AudioFormat getFormat() {
        float sampleRate = 48000;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate,
                sampleSizeInBits, channels, signed, bigEndian);
    }

    public void initiatePlay(ByteArrayOutputStream out) {
        try {
            byte audio[] = out.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            final AudioFormat format = getFormat();
            ais = new AudioInputStream(input, format, audio.length / format.getFrameSize());
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            bufferSize = (int) format.getSampleRate()
                    * format.getFrameSize();
            buffer = new byte[bufferSize];

        } catch (LineUnavailableException e) {
            Logger.getLogger(Reception.class.getName()).log(Level.SEVERE, null, e);
            System.exit(-4);
        }
    }

    int fromByteArray(byte[] bytes) {               // convert byte array to int
        return ByteBuffer.wrap(bytes).getInt();
    }

    private int Deserialize(byte[] receivedData) {
        return fromByteArray(Arrays.copyOfRange(receivedData, 0, 4));   // deserialize packet to get sequence number (header)
    }

}
