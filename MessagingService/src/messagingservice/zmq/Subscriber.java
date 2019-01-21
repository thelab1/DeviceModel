package messagingservice.zmq;

import java.nio.charset.Charset;
import java.util.concurrent.CopyOnWriteArrayList;
import org.zeromq.ZMQ;

/**
 *
 * @author pobzeb
 */
public abstract class Subscriber extends Thread {
    private final ZMQ.Context context;
    private final ZMQ.Socket socket;
    private boolean running = false;
    private CopyOnWriteArrayList<String> subscriptions = new CopyOnWriteArrayList<>();

    public Subscriber() {
        this.context = ZMQ.context(1);
        this.socket = this.context.socket(ZMQ.SUB);
        this.socket.bind("tcp://localhost:8888");

        // Autosubscribe to subscribe and unsubscribe.
        this.subscriptions.add("subscribe");
        this.subscriptions.add("unsubscribe");

        this.start();
    }

    public void subscribe(String topic) {
        if (!this.subscriptions.contains(topic)) {
            this.socket.subscribe(topic);
            this.subscriptions.add(topic);
        }
    }

    public void unsubscribe(String topic) {
        if (this.subscriptions.remove(topic)) {
            this.socket.unsubscribe(topic);
        }
    }

    @Override
    public void run() {
        running = true;
        String topic, body;
        while (running && !this.isInterrupted()) {
            if ((topic = socket.recvStr()) != null && (body = socket.recvStr()) != null) {
                switch(topic.toLowerCase()) {
                    case "subscribe": {
                        subscribe(body);
                        break;
                    }
                    case "unsubscribe": {
                        unsubscribe(body);
                        break;
                    }
                    default: {
                        // Handle the message.
                        handleMessage(new Message(topic, body.getBytes(Charset.defaultCharset())));
                    }
                }
            }
        }
    }

    public abstract void handleMessage(Message message);
}
