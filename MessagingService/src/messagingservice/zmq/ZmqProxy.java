package messagingservice.zmq;

import org.zeromq.ZMQ;
/**
 *
 * @author Pobzeb
 */
public class ZmqProxy extends Thread {
    private final ZMQ.Context context;
    private final ZMQ.Socket zSubscriber;
    private final ZMQ.Socket zPublisher;

    public ZmqProxy() {
        this.context = ZMQ.context(1);
        this.zPublisher = this.context.socket(ZMQ.XPUB);
        this.zPublisher.bind("tcp://*:8888");
        this.zSubscriber = this.context.socket(ZMQ.XSUB);
        this.zSubscriber.bind("tcp://*:9999");

        this.start();
    }

    @Override
    public void run() {
        ZMQ.proxy(zPublisher, zSubscriber, null);
        this.zPublisher.close();
        this.zSubscriber.close();
        this.context.close();
    }
}
