package messagingservice;

import messagingservice.zmq.ZmqProxy;

/**
 *
 * @author Pobzeb
 */
public class MessagingService {

    public static void main(String[] args) {
        System.out.println("Starting Messaging Service.");
        new ZmqProxy();
    }
}
