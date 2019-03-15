package rrb.infra.devicemodel;

/**
 *
 * @author pobzeb
 */
public class NodeManager {
    private static final Object LOCK = new Object();
    private static NodeManager _instance;
    private final DeviceNode rootNode;

    private NodeManager(DeviceNode rootNode) {
        this.rootNode = rootNode;
    }

    public static NodeManager getInstance(DeviceNode rootNode) {
        synchronized(LOCK) {
            if (_instance == null) {
                _instance = new NodeManager(rootNode);
            }
        }

        return _instance;
    }

    public static NodeManager getInstance() {
        synchronized(LOCK) {
            if (_instance == null) {
                System.err.println("Cannot create new instance without RootNode!");
                System.exit(1);
            }
        }

        return _instance;
    }

    public static DeviceNode getRootNode() {
        return getInstance().rootNode;
    }

    public static DeviceNode toDeviceNode(DeviceModelProto.DeviceNode node) {
        DeviceNode ret = new DeviceNode(node.getName());
        if (node.getValue() != null && node.getValue().trim().length() > 0) ret.setValue(node.getValue());
        for (String key : node.getAttributesMap().keySet()) {
            ret.addAttribute(key, node.getAttributesMap().get(key));
        }
        for (DeviceModelProto.DeviceNode child : node.getChildrenList()) {
            ret.addChild(toDeviceNode(child));
        }
        return ret;
    }

    public static DeviceNode getChildByPath(String path) {
        try {
            return getInstance().rootNode.getChildByPath(path);
        }
        catch (Exception ex) {
            System.err.println("Error getting child by path: "+path);
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public static void update(String path, String value) {
        try {
            DeviceNode node = getInstance().rootNode.getChildByPath(path).cloneShallow();
            node.setValue(value);
            getInstance().rootNode.getChildByPath(path).update(node);
        }
        catch (Exception ex) {
            System.err.println("Error updating node: "+path);
            ex.printStackTrace(System.err);
        }
    }

    public static void update(String path, DeviceNode node) {
        try {
            getInstance().rootNode.getChildByPath(path).update(node);
        }
        catch (Exception ex) {
            System.err.println("Error updating node: "+path);
            ex.printStackTrace(System.err);
        }
    }

    public static void addChild(String path, DeviceNode child) {
        addChild(path, child, false);
    }

    public static void addChild(String path, DeviceNode child, boolean fireUpdate) {
        try {
            getInstance().rootNode.getChildByPath(path).addChild(child, fireUpdate);
        }
        catch (Exception ex) {
            System.err.println("Error adding child node: "+path);
            ex.printStackTrace(System.err);
        }
    }
}
