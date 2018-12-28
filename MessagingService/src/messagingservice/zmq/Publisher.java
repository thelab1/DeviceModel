package messagingservice.zmq;

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
        this.socket.bind("tcp://localhost:9999");

        this.start();
    }

    @Override
    public void run() {
        running = true;
        Message message;
        while (running && !this.isInterrupted()) {
            try {
                message = pubQueue.poll(10, TimeUnit.MILLISECONDS);
                if (message != null) {
                    // Sennd the message.
                    this.socket.sendMore(message.path);
                    this.socket.send(message.body);
                }
            }
            catch (InterruptedException ex) {}
        }
    }

    public void sendMessage(String topic, byte[] body) {
        this.pubQueue.add(new Message(topic, body));
    }
}
