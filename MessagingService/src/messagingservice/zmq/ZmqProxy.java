package messagingservice.zmq;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
/**
 *
 * @author Pobzeb
 */
public class ZmqProxy extends Thread {
    public static final String PUB_URI = "tcp://localhost:8881";
    public static final String SUB_URI = "tcp://localhost:8882";

    public ZmqProxy() {}

    @Override
    public void run() {
        ZContext context = new ZContext();

        // Create the publisher.
        ZMQ.Socket xPub = context.createSocket(ZMQ.XPUB);
        xPub.bind(PUB_URI);

        // Create the subscriber.
        ZMQ.Socket xSub = context.createSocket(ZMQ.XSUB);
        xSub.bind(SUB_URI);

        // Create the proxy.
        ZMQ.proxy(xPub, xSub, null);

        // Close everything.
        xPub.close();
        xSub.close();
        context.close();
    }
}
