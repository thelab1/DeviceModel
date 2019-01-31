package messagingservice.zmq;

import devicemodel.DeviceNode;
import devicemodel.conversions.XmlConversions;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author pobzeb
 */
public class Publisher extends Thread {
    private final BlockingQueue<Message> pubQueue = new LinkedBlockingQueue<>();
    private boolean running = false;

    public Publisher() {
        this.start();
    }

    @Override
    public void run() {
        ZContext context = new ZContext();
        ZMQ.Socket socket = context.createSocket(ZMQ.PUB);
        socket.bind(ZmqProxy.SUB_URI);
        running = true;
        Message message;
        while (running && !this.isInterrupted()) {
            try {
                if ((message = pubQueue.poll(10, TimeUnit.MILLISECONDS)) != null) {
                    // Sennd the message.
                    if (!socket.sendMore(message.topic) || !socket.send(message.body)) {
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
        socket.close();
        context.close();
    }

    public void sendMessage(String systemName, String serviceName, String method, String path, byte[] body) {
        DeviceNode msgHolder = new DeviceNode("Message");
        msgHolder.addAttribute("SystemName", systemName);
        msgHolder.addAttribute("ServiceName", serviceName);
        msgHolder.addAttribute("Method", method);
        msgHolder.addAttribute("Path", path);
        msgHolder.setValue(body);
        try {
            this.pubQueue.offer(new Message(systemName+"_"+serviceName, XmlConversions.nodeToXmlString(msgHolder).getBytes(Charset.defaultCharset())));
        }
        catch(IOException ex) {
            System.err.println("Error parsing body");
            ex.printStackTrace(System.err);
        }
    }

    public void sendMessage(String systemName, String serviceName, String method, String path, DeviceNode body) {
        try {
            this.sendMessage(systemName, serviceName, method, path, XmlConversions.nodeToXmlString(body).getBytes(Charset.defaultCharset()));
        }
        catch(IOException ex) {
            System.err.println("Error parsing body");
            ex.printStackTrace(System.err);
        }
    }
}
