package messagingservice.zmq;

import devicemodel.DeviceNode;
import devicemodel.conversions.XmlConversions;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jdom2.JDOMException;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author pobzeb
 */
public abstract class Subscriber extends Thread {
    private final ZContext context;
    private ZMQ.Socket socket;
    private boolean running = false;
    private CopyOnWriteArrayList<String> subscriptions = new CopyOnWriteArrayList<>();

    public Subscriber() {
        this(new String[0]);
    }

    public Subscriber(String[] subscriptions) {
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.SUB);
        this.socket.bind(ZmqProxy.PUB_URI);

        // Autosubscribe to subscribe and unsubscribe.
        this.subscriptions.add("subscribe");
        this.subscriptions.add("unsubscribe");

        // Add the subscriptions passed in.
        for (String subscription : subscriptions) {
            this.subscribe(subscription);
        }

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
        String topic;
        byte[] body;
        while (running && !this.isInterrupted()) {
            try {
                if ((topic = socket.recvStr()) != null && (body = socket.recv()) != null) {
                    switch(topic.toLowerCase()) {
                        case "subscribe": {
                            subscribe(new String(body));
                            break;
                        }
                        case "unsubscribe": {
                            unsubscribe(new String(body));
                            break;
                        }
                        default: {
                            try {
                                // Handle the message.
                                DeviceNode msgHolder = XmlConversions.xmlToNode(new String(body));
                                DeviceNode msgBody = XmlConversions.xmlToNode(msgHolder.getValue().toString());
                                if (msgBody != null) {
                                    handleMessage(msgHolder.getAttribute("SystemName"), msgHolder.getAttribute("ServiceName"), msgHolder.getAttribute("Method"), msgHolder.getAttribute("Path"), msgBody);
                                }
                                else {
                                    handleMessage(msgHolder.getAttribute("SystemName"), msgHolder.getAttribute("ServiceName"), msgHolder.getAttribute("Method"), msgHolder.getAttribute("Path"), msgHolder.getValue().toString().getBytes(Charset.defaultCharset()));
                                }
                            }
                            catch (IOException | JDOMException ex) {
                            }
                        }
                    }
                }
                else {
                    running = false;
                    break;
                }
                Thread.currentThread().sleep(10);
            }
            catch (InterruptedException ex) {
                running = false;
            }
        }

        // Shutdown
        this.socket.close();
        context.close();
    }

    public abstract void handleMessage(String systemName, String serviceName, String method, String path, byte[] body);

    public abstract void handleMessage(String systemName, String serviceName, String method, String path, DeviceNode body);
}
