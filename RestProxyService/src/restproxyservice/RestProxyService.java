package restproxyservice;

import org.eclipse.jetty.http.HttpStatus;
import spark.HaltException;
import spark.Request;
import spark.Response;
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
    public RestProxyService() {
        // Set the web server port.
        port(81);

        // Initialize the websocket.
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
            byte[] body = request.bodyAsBytes();
            return null;
        });

        // Listen for all put requests.
        put("/rest/*", (request, response) -> {
            return null;
        });
    }

    public void handleBefore(Request request, Response response) {
        
    }

    public static void main(String[] args) {
        new RestProxyService();
    }
}
