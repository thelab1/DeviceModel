package messagingservice.zmq;

import devicemodel.DeviceNode;
import devicemodel.conversions.XmlConversions;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.zeromq.ZMQ;

/**
 *
 * @author pobzeb
 */
public class Publisher extends Thread {
    private final BlockingQueue<Message> pubQueue = new LinkedBlockingQueue<>();
    private final ZMQ.Context context;
    private final ZMQ.Socket socket;
    private boolean running = false;

    public Publisher() {
        this.context = ZMQ.context(1);
        this.socket = this.context.socket(ZMQ.PUB);
//        this.socket.setRate(1000000);
        this.socket.bind("tcp://localhost:9999");

        this.start();
    }

    @Override
    public void run() {
        running = true;
        Message message;
        while (running && !this.isInterrupted()) {
            try {
                if ((message = pubQueue.poll(10, TimeUnit.MILLISECONDS)) != null) {
                    // Sennd the message.
                    if (!this.socket.sendMore(message.path) || !this.socket.send(message.body)) {
                        System.out.println("Could not send topic or message. Exiting...");
                        running = false;
                        break;
                    }
                }
                else {
                    Thread.currentThread().sleep(10);
                }
            }
            catch (InterruptedException ex) {
                running = false;
            }
        }

        // Shutdown
        this.socket.close();
        this.context.close();
    }

    public void sendMessage(String topic, byte[] body) {
        this.pubQueue.offer(new Message(topic, body));
    }

    public void sendMessage(String topic, DeviceNode body) {
        try {
            this.sendMessage(topic, XmlConversions.nodeToXmlString(body).getBytes(Charset.defaultCharset()));
        }
        catch(IOException ex) {
            System.err.println("Error parsing body");
            ex.printStackTrace(System.err);
        }
    }
}
