package devicebase.socket;

import static devicemodel.DeviceModelProto.MessageBlock;
import devicemodel.DeviceNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 *
 * @author pobzeb
 */
public abstract class RapidSocket extends Thread {
    private final String host;
    private final int port;
    private Socket client;
    private InputStream in;
    private OutputStream out;

    public RapidSocket(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return this.client != null && !this.client.isClosed();
    }

    private void doOpen() {
        this.client = new Socket();
        try {
            this.client.connect(new InetSocketAddress(this.host, this.port));
            if (this.isConnected()) {
                this.in = this.client.getInputStream();
                this.out = this.client.getOutputStream();
                this.onOpen();
            }
        }
        catch (IOException ex) {
            this.doError(ex);
            this.doClose();
        }
    }

    public abstract void onOpen();

    private void doClose() {
        try {
            if (this.in != null) this.in.close();
            if (this.out != null) this.out.close();
            if (this.client != null) this.client.close();
            this.onClose();
        }
        catch (IOException ex) {
            this.doError(ex);
        }
        finally {
            this.client = null;
            this.in = null;
            this.out = null;
        }
    }

    public abstract void onClose();

    private void doError(Exception ex) {
        this.onError(ex);
    }

    public abstract void onError(Exception ex);

    private DeviceNode doMessage(MessageBlock msg) {
        return null;
    }

    @Override
    public void run() {
        MessageBlock msg;
        while (true) {
            this.doOpen();
            while (this.isConnected()) {
                try {
                    msg = MessageBlock.parseDelimitedFrom(in);
                    switch (msg.getMessageType()) {
                        case IPROC:
                        case REPLY: {
                            break;
                        }
                        case STREAM: {
                            break;
                        }
                        default: {
                            // Something wrong?
                        }
                    }
                }
                catch (IOException ex) {
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
