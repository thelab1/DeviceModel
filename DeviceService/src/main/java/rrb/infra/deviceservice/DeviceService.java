package rrb.infra.deviceservice;

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.webSocket;

/**
 *
 * @author pobzeb
 */
public class DeviceService {
    public DeviceService() {
        // This service runs on port 83.
        port(83);

        // Create the WebSocket endpoint.
        webSocket("/ws", SocketHandler.class);

        // Build the routes.
        Router.buildRoutes();

        // Initialize the application.
        init();
    }

    public static void main(String[] args) {
        new DeviceService();
    }
    
}
