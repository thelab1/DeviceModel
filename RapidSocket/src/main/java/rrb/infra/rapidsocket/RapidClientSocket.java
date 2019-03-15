package rrb.infra.rapidsocket;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import rrb.infra.devicemodel.DeviceModelProto.MessageBlock;

/**
 *
 * @author pobzeb
 */
public class RapidClientSocket extends Thread {
    private final String clientName;
    private final RapidClientHandler clientHandler;
    private String hostname;
    private int port;
    private Socket clientSocket;
    private CodedInputStream in;
    private CodedOutputStream out;
    private Thread hbThread;

    public RapidClientSocket(String clientName, String hostname, int port, RapidClientHandler handler) {
        this.clientName = clientName;
        this.hostname = hostname;
        this.port = port;
        this.clientHandler = handler;
    }

    public boolean isConnected() {
        return this.clientSocket != null && !this.clientSocket.isClosed();
    }

    private void openSocket() {
        this.clientSocket = new Socket();
        try {
            this.clientSocket.connect(new InetSocketAddress(hostname, port));
            if (this.isConnected()) {
                this.in = CodedInputStream.newInstance(this.clientSocket.getInputStream());
                this.out = CodedOutputStream.newInstance(this.clientSocket.getOutputStream());
                this.clientHandler.onOpen();
            }
        }
        catch(IOException ex) {
            this.doError(ex);
            this.doClose();
        }
    }

    private void doClose() {
        try {
            if (this.clientSocket != null) this.clientSocket.close();
            this.clientHandler.onClose();
        }
        catch(IOException ex) {
            this.doError(ex);
        }
        finally {
            this.in = null;
            this.out = null;
            this.clientSocket = null;
        }
    }

    private void doError(Exception ex) {
        this.clientHandler.onError(ex);
    }

    private MessageBlock doMessage(MessageBlock msg) {
        return this.clientHandler.onMessage(msg);
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

    private void startHBThread() {
        if (this.hbThread == null) {
            this.hbThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Send a heartbeat.
                    MessageBlock.Builder msg = MessageBlock.newBuilder();
                    msg.setMessageType(MessageBlock.MessageTypeType.HEARTBEAT);
                    msg.setFromServiceName(clientName);
                    sendMessage(msg.build());
                }
            }, this.clientName+"_HBThread");
            this.hbThread.start();
        }
    }

    @Override
    public void run() {
        MessageBlock msg;
        while (true) {
            // Open the socket.
            this.openSocket();
            if (this.isConnected()) {
                // Start the HB thread.
                this.startHBThread();

                // Start listening for messages.
                while (this.isConnected()) {
                    try {
                        msg = MessageBlock.parseFrom(this.in);
                        if (msg != null) {
                            switch(msg.getMessageType()) {
                                case HEARTBEAT: {
                                    // If we get this message,
                                    // we might not be sending HB
                                    // messages. Start sending them.
                                    this.startHBThread();
                                    break;
                                }
                                case IPROC:
                                case WEB:
                                case REPLY: {
                                    MessageBlock reply = this.doMessage(msg);
                                    if (reply != null) this.sendMessage(reply);
                                    break;
                                }
                                case STREAM: {
                                    break;
                                }
                            }
                        }
                    }
                    catch(IOException ex) {
                        if (ex.getMessage().toLowerCase().contains("connection reset") ||
                            ex.getMessage().toLowerCase().contains("broken pipe")) {
                            // Server is gone. Try to reconnect.
                            this.doClose();
                            break;
                        }
                        else this.doError(ex);
                    }
                }
            }
        }
    }
}
