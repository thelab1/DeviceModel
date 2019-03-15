package rrb.infra.rapidsocket;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rrb.infra.devicemodel.DeviceModelProto.MessageBlock;
import rrb.infra.devicemodel.DeviceNode;

/**
 *
 * @author pobzeb
 */
public class RapidServerSocket implements PropertyChangeListener {
    public static final int DEFAULT_PORT = 8081;
    public static final String FROM_DEVICE_SERVICE = "DeviceService";
    public static final long CLIENT_HEARTBEAT_WARNING = 2000;
    public static final long CLIENT_HEARTBEAT_TIMEOUT = 4000;

    private static final Object LOCK = new Object();
    private static RapidServerSocket _instance;

    private final static ConcurrentHashMap<String, RapidClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private boolean running = false;

    private RapidServerSocket(int port) throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(port)) {
            while (true) {
                pool.execute(new RapidClientHandler(listener.accept(), this));
            }
        }
    }

    public static RapidServerSocket getInstance(int port) throws IOException {
        synchronized(LOCK) {
            if (_instance == null) {
                _instance = new RapidServerSocket(port);
            }
        }

        return _instance;
    }

    public static RapidServerSocket getInstance() {
        try { return getInstance(DEFAULT_PORT); }
        catch(IOException ex) {}

        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Get the message.
        MessageBlock msg = (MessageBlock)evt.getNewValue();
        switch(msg.getMessageType()) {
            case IPROC:
            case REPLY: {
                // Find the service this message is for.
                RapidClientHandler handler = this.clientHandlers.get(msg.getToServiceName());
                if (handler != null) handler.sendMessage(msg);
                else {
                    // This client is no longer available. Send
                    // a reply back to the from client.
                    String fromService = msg.getFromServiceName();
                    String toService = msg.getToServiceName();
                    msg = msg.toBuilder().setToServiceName(fromService)
                        .setFromServiceName(FROM_DEVICE_SERVICE)
                        .setMessageType(MessageBlock.MessageTypeType.REPLY)
                        .setBody((new DeviceNode("ERROR").addChildren(new DeviceNode[]{
                            new DeviceNode("Code", "404"),
                            new DeviceNode("Message", "Device "+toService+" is not available")
                        })).getDeviceNodeProtoBuf()).build();
                    this.clientHandlers.get(msg.getToServiceName()).sendMessage(msg);
                }
                break;
            }
        }
    }

    private static class RapidClientHandler implements Runnable {
        private final Socket client;
        private String clientName;
        private CodedInputStream in;
        private CodedOutputStream out;
        private PropertyChangeSupport support;
        private long lastHB;
        private Thread hbThread;
        private boolean running = false;

        public RapidClientHandler(Socket client, PropertyChangeListener listener) {
            this.client = client;
            this.support = new PropertyChangeSupport(this);
            this.support.addPropertyChangeListener(listener);
        }

        public void sendMessage(MessageBlock msg) {
            try {
                this.out.writeInt32NoTag(msg.getSerializedSize());
                msg.writeTo(out);
                out.flush();
            }
            catch(IOException ex) {
                System.err.println("Error writing message:\n"+msg.toString());
                ex.printStackTrace(System.err);
            }
        }

        private void startHBMonitor() {
            if (this.hbThread == null) {
                this.hbThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(running) {
                            // Check to see if the warning timeout has past.
                            long delta = new Date().getTime() - lastHB;
                            if (delta > CLIENT_HEARTBEAT_WARNING) {
                                // Check to see if we are past the timeout.
                                if (delta > CLIENT_HEARTBEAT_TIMEOUT) {
                                    // This client has not responded.
                                    // Disconnect them.
                                    running = false;
                                    break;
                                }
                                else {
                                    // Send a HB request to the client.
                                    MessageBlock.Builder msg = MessageBlock.newBuilder();
                                    msg.setMessageType(MessageBlock.MessageTypeType.HEARTBEAT);
                                    msg.setFromServiceName("RapidServerSocket");
                                    msg.setToServiceName(clientName);
                                    sendMessage(msg.build());
                                }
                            }
                        }
                    }
                }, this.clientName+"_HBMonitorThread");
                this.hbThread.start();
            }
        }

        @Override
        public void run() {
            try {
                // Start the running flag.
                running = true;

                // Get the input and output streams.
                in = CodedInputStream.newInstance(client.getInputStream());
                out = CodedOutputStream.newInstance(client.getOutputStream());

                // Start watching for a register message.
                MessageBlock msg;
                while(this.running) {
                    try {
                        // Wait for a register request.
                        msg = MessageBlock.parseFrom(in);
                        if (msg != null && msg.getMessageType() == MessageBlock.MessageTypeType.REGISTER) {
                            // This is a register message.
                            this.clientName = msg.getFromServiceName();
                            break;
                        }
                    }
                    catch(IOException ex) {
                        System.err.println("Error reading from input stream");
                        ex.printStackTrace(System.err);
                    }

                    try { Thread.currentThread().sleep(1); } catch(InterruptedException ex) {}
                }

                // Make sure we got a registration.
                if (this.clientName != null) {
                    // Start the HB monitor.
                    this.startHBMonitor();

                    // Start listening for all messages.
                    while(this.running) {
                        try {
                            // Read the next message.
                            msg = MessageBlock.parseFrom(in);
                            switch(msg.getMessageType()) {
                                case HEARTBEAT: {
                                    // Store this as the last HB time.
                                    this.lastHB = new Date().getTime();
                                    break;
                                }
                                case IPROC:
                                case REPLY: {
                                    this.support.firePropertyChange(this.clientName, null, msg);
                                    break;
                                }
                                case STREAM: {
                                    break;
                                }
                            }
                        }
                        catch(IOException ex) {
                            System.err.println("Error reading from input stream");
                            ex.printStackTrace(System.err);
                        }

                        try { Thread.currentThread().sleep(1); } catch(InterruptedException ex) {}
                    }
                }
            }
            catch (IOException ex) {
                System.err.println("Error communicating with client");
                ex.printStackTrace(System.err);
            }
            finally {
                if (this.clientName != null) {
                    clientHandlers.remove(this.clientName);
                }
                try { this.client.close(); } catch (IOException ex) {}
                for (PropertyChangeListener listener : this.support.getPropertyChangeListeners()) {
                    this.support.removePropertyChangeListener(listener);
                }
            }
        }
    }
}
