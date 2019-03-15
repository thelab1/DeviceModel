package rrb.infra.deviceservice;

import static spark.Spark.get;
import static spark.Spark.put;

/**
 *
 * @author pobzeb
 */
public class Router {
    public static void buildRoutes() {
        get("/rest/*", "*/*", (request, response) -> {
            return null;
        });

        put("/rest/*", "*/*", (request, response) -> {
            return null;
        });
    }
}
