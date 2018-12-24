package restproxyservice;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 *
 * @author Pobzeb
 */
@WebSocket
public class SocketHandler {
    @OnWebSocketConnect
    public void connect(Session session) {
        
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        
    }
}
