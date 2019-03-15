package rrb.infra.deviceservice;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import spark.Session;

/**
 *
 * @author pobzeb
 */
@WebSocket
public class SocketHandler {
    @OnWebSocketConnect
    public void onConnect(Session session) {
        
    }

    @OnWebSocketClose
    public void onClose(Session session) {
        
    }

    @OnWebSocketError
    public void onError(Session session, int statusCode, String reason) {
        
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        
    }
}
