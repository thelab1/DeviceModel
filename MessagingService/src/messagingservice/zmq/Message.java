package messagingservice.zmq;

/**
 *
 * @author pobzeb
 */
public class Message {
    public String topic;
    public byte[] body;

    public Message(String topic, byte[] body) {
        this.topic = topic;
        this.body = body;
    }
}
