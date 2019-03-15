package rrb.infra.deviceservice;

import rrb.infra.devicemodel.DeviceModelProto.MessageBlock;
import spark.Request;
import static spark.Spark.get;
import static spark.Spark.put;

/**
 *
 * @author pobzeb
 */
public class Router {
    public static void buildRoutes() {
        get("/rest/*", "*/*", (request, response) -> {
            // Parse the request.
            MessageBlock msg = parseRequest(request);

            return null;
        });

        put("/rest/*", "*/*", (request, response) -> {
            // Parse the request.
            MessageBlock msg = parseRequest(request);

            return null;
        });
    }

    private static MessageBlock parseRequest(Request request) {
        MessageBlock.Builder msg = MessageBlock.newBuilder();

        // Parse the Url.
        String requestPath = request.pathInfo();
        if (requestPath.startsWith("/")) requestPath = requestPath.substring(1);
        if (requestPath.startsWith("rest/")) requestPath = requestPath.substring("rest/".length());
        String[] requestParts = requestPath.split("/");

        // Get the system name from the Url.
        String systemName = "boot";
        

        return msg.build();
    }
}
