package rrb.infra.devicemodel;

import java.util.HashMap;

/**
 *
 * @author root
 */
public abstract class NodeGetHandler {
    public abstract DeviceNode handle(HashMap<String,String> queryParameters);
}
