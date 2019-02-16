package restproxyservice;

import devicemodel.DeviceNode;
import messagingservice.zmq.Publisher;
import messagingservice.zmq.Subscriber;
import org.eclipse.jetty.http.HttpStatus;
import spark.HaltException;
import spark.Request;
import spark.Response;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.port;
import static spark.Spark.put;
import static spark.Spark.webSocket;

/**
 *
 * @author Pobzeb
 */
public class RestProxyService {
    private Publisher publisher;

    public RestProxyService() {
        try {
            // Set the web server port.
            System.out.println("Starting Rest Proxy Service on port 8081");
            port(8081);

            // Initialize the websocket.
            System.out.println("Initializing WebSocket...");
            webSocket("/ws", SocketHandler.class);

            // Catch all requests for pre-processing.
            before("/rest/*", "*/*", (request, response) -> {
                try { handleBefore(request, response); }
                catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    HaltException hex = halt(HttpStatus.UNAUTHORIZED_401, ex.getMessage());
                    if (hex != null) hex.printStackTrace(System.err);
                }
            });

            // Listen for all get requests.
            get("/rest/*", (request, response) -> {
                // Process this request.
                return processRequest(request, response);
            });

            // Listen for all put requests.
            put("/rest/*", (request, response) -> {
                // Process this request.
                return processRequest(request, response);
            });

            awaitInitialization();

            // Create a publisher.
            this.publisher = new Publisher();

            new Subscriber(new String[]{"CLIENT"}) {
                @Override
                public void handleMessage(String systemName, String serviceName, String method, String path, byte[] body) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void handleMessage(String systemName, String serviceName, String method, String path, DeviceNode body) {
                    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                }
            };

            System.out.println("Rest Proxy Service Running");
        }
        catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    private Object processRequest(Request request, Response response) {
        // Build an object to return.
        String method = request.requestMethod();

        // Get the request path.
        String requestPath = request.pathInfo().substring("/rest/".length());

        // Set the system name if it is missing.
        String systemName = requestPath.split("/")[0];
        String nodePath = "";
        switch (systemName.split("-")[0].toLowerCase()) {
            case "boot":
            case "compute":
            case "reference": {
                // Name exists, use it but
                // remove it from the node path.
                nodePath = requestPath.substring(systemName.length());
                break;
            }
            default: {
                // Default to boot.
                systemName = "boot";
                nodePath = requestPath;
            }
        }

        // Set the service name.
        if (nodePath.startsWith("/")) nodePath = nodePath.substring(1);
        String serviceName = nodePath.split("/")[0];
        nodePath = nodePath.substring(serviceName.length());

        // Get the body of the request.
        byte[] body = request.bodyAsBytes();

        System.out.println("Received a "+method+" request for: "+systemName+"_"+serviceName);
        System.out.println("\tNodePath: "+nodePath);
        if (body.length > 0) System.out.println(new String(body));

        // Publish this message.
        this.publisher.sendMessage(systemName, serviceName, method, nodePath, body);

        return "";
    }

    public void handleBefore(Request request, Response response) {}

    public static void main(String[] args) {
        new RestProxyService();
    }
}
