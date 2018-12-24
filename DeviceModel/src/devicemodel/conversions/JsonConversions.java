package devicemodel.conversions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import devicemodel.DeviceNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author root
 */
public class JsonConversions {

    public static String nodeToJson(DeviceNode node) {
        JsonObject o = new JsonObject();
        o.add(node.getName(), nodeToGson(node));
        return o.toString();
    }

    public static JsonObject nodeToGson(DeviceNode node) {
        JsonObject o = new JsonObject();

        if (node.getAttributes().size() > 0) {
            for (String key : node.getAttributes().keySet()) {
                o.addProperty(key, node.getAttribute(key));
            }

            if (node.getValue() != null) {
                o.addProperty("value", node.getValue().toString());
            }
        }

        if (node.getChildren().size() > 0) {
            List<String> childNames = node.getChildrenNamesSorted();
            String[] children = childNames.toArray(new String[0]);
            String child;
            for (int idx = 0; idx < children.length; idx++) {
                child = children[idx];
                if (Collections.frequency(childNames, child) > 1) {
                    JsonArray nodes = new JsonArray();
                    int baseIdx = idx;
                    for (; idx < children.length; idx++) {
                        child = children[idx];
                        nodes.add(nodeToGson(node.getChildren(child).get(idx - baseIdx)));
                    }
                    o.add(child, nodes);
                }
                else {
                    if (node.getChild(child).getChildren().size() > 0) {
                        o.add(child, nodeToGson(node.getChild(child)));
                    }
                    else {
                        if (node.getChild(child).getValue() != null) {
                            o.addProperty(child, node.getChild(child).getValue().toString());
                        }
                        if (node.getChild(child).getAttributes().size() > 0) {
                            o.add(child, nodeToGson(node.getChild(child)));
                        }
                    }
                }
            }
        }

        return o;
    }

    public static DeviceNode jsonToNode(String str) {
        JsonObject e = jsonToGson(str);

        Map.Entry<String, JsonElement> next = e.entrySet().iterator().next();

        return gsonToNode(next.getKey(), (JsonObject) next.getValue());
    }

    public static DeviceNode gsonToNode(String name, JsonObject e) {
        DeviceNode n = new DeviceNode(name);

        Iterator<Map.Entry<String, JsonElement>> iterator = e.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, JsonElement> next = iterator.next();

            if (next.getKey().equals("value")) {
                JsonPrimitive val = next.getValue().getAsJsonPrimitive();

                n.setValue(val.getAsString());
            } else {
                try {
                    if (((JsonObject) next.getValue()).isJsonPrimitive()) {
                        n.addAttribute(next.getKey(), ((JsonObject) next.getValue()).getAsString());
                    }
                    else {
                        n.addChild(gsonToNode(next.getKey(), (JsonObject) next.getValue()));
                    }
                } catch (Exception ex) {
                }
            }
        }

        return n;
    }

    public static JsonObject jsonToGson(String str) {
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(str);
    }

    public static DeviceNode jsonToNode(File f) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(f));

        JsonParser parser = new JsonParser();
        JsonObject gson = (JsonObject) parser.parse(reader);

        Map.Entry<String, JsonElement> next = gson.entrySet().iterator().next();

        return gsonToNode(next.getKey(), (JsonObject) next.getValue());
    }
}
