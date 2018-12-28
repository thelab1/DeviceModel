package messagingservice.zmq;

/**
 *
 * @author pobzeb
 */
public class Message {
    public String path;
    public byte[] body;

    public Message(String path, byte[] body) {
        this.path = path;
        this.body = body;
    }
}
